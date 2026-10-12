package ir.arzanyab.app.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import ir.arzanyab.app.data.adapter.DigikalaAdapter
import ir.arzanyab.app.data.adapter.DivarAdapter
import ir.arzanyab.app.data.adapter.EmallsAdapter
import ir.arzanyab.app.data.adapter.OkalaAdapter
import ir.arzanyab.app.data.adapter.ProductQuery
import ir.arzanyab.app.data.adapter.SourceAdapter
import ir.arzanyab.app.data.adapter.RawListing
import ir.arzanyab.app.data.adapter.SnappMarketAdapter
import ir.arzanyab.app.data.adapter.TorobAdapter
import ir.arzanyab.app.data.adapter.TorobVisualAdapter
import ir.arzanyab.app.data.upload.AnonymousImageUploader
import ir.arzanyab.app.data.resolver.BarcodeProductResolver
import java.io.File
import java.io.FileOutputStream
import ir.arzanyab.app.data.local.ProductDao
import ir.arzanyab.app.domain.fusion.ModalityType
import ir.arzanyab.app.domain.fusion.RrfFusionEngine
import ir.arzanyab.app.domain.normalizer.PersianNormalizer
import ir.arzanyab.app.domain.normalizer.PriceParser
import ir.arzanyab.app.ml.VisualEmbeddingEngine
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MultiModalSearchRequest(
    val text: String? = null,
    val barcode: String? = null,
    val imageUri: Uri? = null
) : Parcelable

/**
 * ارکستر جست‌وجوی چندحالته:
 * نرمال‌سازی متن + تبدیل امن Uri به Bitmap + اجرای موازی شاخه‌ها
 * + تلفیق RRF + ارزیابی مستقیم با PriceTrustEngine
 */
class SearchOrchestrator(
    private val context: Context,
    private val productDao: ProductDao,
    private val embeddingEngine: VisualEmbeddingEngine,
    private val digikalaAdapter: DigikalaAdapter = DigikalaAdapter(),
    private val torobAdapter: TorobAdapter = TorobAdapter(),
    private val divarAdapter: DivarAdapter = DivarAdapter(context),
    private val emallsAdapter: EmallsAdapter = EmallsAdapter(context),
    private val snappMarketAdapter: SnappMarketAdapter = SnappMarketAdapter(),
    private val okalaAdapter: OkalaAdapter = OkalaAdapter(),
    private val torobVisualAdapter: TorobVisualAdapter = TorobVisualAdapter(),
    private val barcodeResolver: BarcodeProductResolver = BarcodeProductResolver()
) {

    suspend fun searchAndEvaluate(request: MultiModalSearchRequest): List<EvaluatedPriceItem> =
        withContext(Dispatchers.IO) {
            // ۱. نرمال‌سازی ورودی متنی
            val normalizedText = request.text
                ?.let { PersianNormalizer.normalize(it) }
                ?.takeIf { it.isNotBlank() }

            // ۲. تبدیل امن Uri به Bitmap
            val imageBitmap: Bitmap? = request.imageUri?.let { uri ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(context.contentResolver, uri)
                        ) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                } catch (_: Exception) {
                    null
                }
            }

            val modalityLists = mutableMapOf<ModalityType, List<RawListing>>()

            // هر منبع جداگانه محافظت می‌شود: خطای یک آداپتور نباید بقیه را از کار بیندازد
            val textAdapters: List<SourceAdapter> = listOf(
                digikalaAdapter, torobAdapter, divarAdapter,
                emallsAdapter, snappMarketAdapter, okalaAdapter
            )
            suspend fun safeSearchAll(q: ProductQuery): List<RawListing> {
                val out = mutableListOf<RawListing>()
                for (adapter in textAdapters) {
                    try {
                        out += adapter.search(q)
                    } catch (t: Throwable) {
                        Log.e("SearchOrchestrator", "آداپتور ${adapter.sourceId} خطا داد", t)
                    }
                }
                return out
            }

            // نام حل‌شده بارکد (برای موتور اعتماد و نمایش)؛ فقط شاخه بارکد می‌نویسد
            var resolvedBarcodeTitle: String? = null

            supervisorScope {
                val barcodeJob = async {
                    request.barcode?.let { code ->
                        // بارکد → نام کالا (حداکثر ۷ ثانیه)؛ اگر نشد با خودِ بارکد ادامه می‌دهیم
                        val resolved = try {
                            withTimeoutOrNull(7000) { barcodeResolver.resolveProductName(code) }
                        } catch (_: Throwable) { null }
                        if (resolved != null) resolvedBarcodeTitle = resolved
                        safeSearchAll(ProductQuery(queryText = resolved ?: code, barcode = code))
                    }
                }

                val textJob = async {
                    normalizedText?.let { txt ->
                        safeSearchAll(ProductQuery(queryText = txt))
                    }
                }

            val imageJob = async {
                imageBitmap?.let { bmp ->
                    try {
                        val embedding = embeddingEngine.extractEmbedding(bmp)
                        val visualMatcher = VisualMatcher(productDao)
                        when (val outcome = visualMatcher.match(embedding)) {
                            is VisualMatchOutcome.HighConfidence -> {
                                val q = ProductQuery(queryText = outcome.topMatch.title)
                                safeSearchAll(q)
                            }
                            is VisualMatchOutcome.ModerateConfidence -> {
                                outcome.suggestions.firstOrNull()?.let { top ->
                                    val q = ProductQuery(queryText = top.title)
                                    safeSearchAll(q)
                                } ?: emptyList()
                            }
                            is VisualMatchOutcome.LowConfidence -> {
                                outcome.categoryGuess?.let { cat ->
                                    val q = ProductQuery(queryText = cat)
                                    safeSearchAll(q)
                                } ?: emptyList()
                            }
                        }
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }

                try {
                    barcodeJob.await()?.let { if (it.isNotEmpty()) modalityLists[ModalityType.BARCODE] = it }
                } catch (t: Throwable) {
                    Log.e("SearchOrchestrator", "خطا در شاخه بارکد", t)
                }
                try {
                    textJob.await()?.let { if (it.isNotEmpty()) modalityLists[ModalityType.TEXT] = it }
                } catch (t: Throwable) {
                    Log.e("SearchOrchestrator", "خطا در شاخه متن", t)
                }
                try {
                    imageJob.await()?.let { if (it.isNotEmpty()) modalityLists[ModalityType.IMAGE] = it }
                } catch (t: Throwable) {
                    Log.e("SearchOrchestrator", "خطا در شاخه عکس", t)
                }
            }

            if (modalityLists.isEmpty()) return@withContext emptyList()

            // ۳. تلفیق RRF
            val rawFusedList: List<RawListing> = if (modalityLists.size == 1) {
                modalityLists.values.first()
            } else {
                RrfFusionEngine.fuseRankings(modalityLists).map { it.listing }
            }

            // ۴. ارزیابی مستقیم با PriceTrustEngine
            val inputItems = rawFusedList.map { it.toRawInputItem() }

            val effectiveQuery = normalizedText ?: resolvedBarcodeTitle ?: request.barcode ?: "کالای جست‌وجوشده"
            PriceTrustEngine.evaluateListings(effectiveQuery, inputItems)
        }

    /**
     * جست‌وجوی تصویری هوشمند ترب:
     * عکس → فشرده‌سازی → آپلود ناشناس → تطبیق تصویری ترب → ارزیابی قیمت.
     * رضایت کاربر برای آپلود باید پیش‌تر در UI گرفته شده باشد.
     */
    suspend fun visualSearch(imageUri: Uri): List<EvaluatedPriceItem> =
        withContext(Dispatchers.IO) {
            val publicUrl = uploadImageToPublicUrl(imageUri)
            val raw = torobVisualAdapter.searchByImageUrl(publicUrl)
            if (raw.isEmpty()) return@withContext emptyList()
            PriceTrustEngine.evaluateListings("جست‌وجوی تصویری", raw.map { it.toRawInputItem() })
        }

    /**
     * فشرده‌سازی عکس و آپلود ناشناس برای گرفتن URL عمومی.
     * برای جست‌وجوی تصویری ترب استفاده می‌شود.
     */
    suspend fun uploadImageToPublicUrl(imageUri: Uri): String =
        withContext(Dispatchers.IO) {
            val bitmap = uriToBitmap(imageUri)
                ?: throw IllegalStateException("خواندن عکس ناموفق بود.")
            // فشرده‌سازی برای آپلود سریع‌تر (بزرگ‌ترین ضلع حداکثر ۱۰۲۴)
            val scale = minOf(
                1f,
                1024f / bitmap.width.toFloat(),
                1024f / bitmap.height.toFloat()
            )
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
            val file = File(context.cacheDir, "visual_${System.currentTimeMillis()}.jpg")
            try {
                FileOutputStream(file).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
                }
                AnonymousImageUploader.uploadImage(file)
            } finally {
                file.delete()
            }
        }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun RawListing.toRawInputItem(): PriceTrustEngine.RawInputItem {
        val parsed = PriceParser.parse(priceText)
        return PriceTrustEngine.RawInputItem(
            title = title,
            rawPriceText = priceText,
            priceInToman = parsed.priceInToman,
            sellerName = sellerName ?: "فروشگاه آنلاین",
            isVerifiedSeller = sourceId == "digikala",
            sourceId = sourceId,
            sourceName = when (sourceId) {
                "digikala" -> "دیجی‌کالا"
                "torob" -> "ترب"
                "divar" -> "دیوار"
                "emalls" -> "ایمالز"
                "snappmarket" -> "اسنپ‌مارکت"
                "okala" -> "اکالا"
                else -> sellerName ?: sourceId
            },
            url = url,
            imageUrl = imageUrl
        )
    }
}
