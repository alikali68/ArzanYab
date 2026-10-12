package ir.arzanyab.app.data.adapter

import android.util.Log
import ir.arzanyab.app.domain.normalizer.PersianNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * اکالا — جست‌وجوی متنی آزاد فقط برای کاربران لاگین‌دار است (مهمان: 401).
 * مسیر مهمان: تطبیق پرس‌وجو با درخت دسته‌بندی‌ها ← دریافت کالاهای نزدیک همان دسته.
 * قیمت‌ها به ریال‌اند (تقسیم بر ۱۰ = تومان). مختصات پیش‌فرض: تهران.
 */
class OkalaAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : SourceAdapter {

    override val sourceId = "okala"
    override val displayName = "اکالا"

    private val baseUrl = "https://apigateway.okala.com/api"
    private val defaultLat = 35.6892
    private val defaultLon = 51.3890
    private val userUniqueId = UUID.randomUUID().toString()

    private fun baseRequest(url: String): Request.Builder =
        Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Mobile Safari/537.36")
            .addHeader("Accept", "application/json")
            .addHeader("source", "okala")
            .addHeader("x-user-unique-id", userUniqueId)

    override suspend fun search(query: ProductQuery): List<RawListing> =
        withContext(Dispatchers.IO) {
            try {
                // ۱. درخت دسته‌بندی‌ها
                val categoriesBody = baseRequest(
                    "$baseUrl/unicorn/v3/categories/nearby?lat=$defaultLat&lon=$defaultLon"
                ).build().let { req ->
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@withContext emptyList()
                        resp.body?.string() ?: return@withContext emptyList()
                    }
                }
                val slug = matchCategorySlug(query.queryText, categoriesBody)
                    ?: return@withContext emptyList()

                // ۲. کالاهای نزدیک همان دسته
                val productsBody = baseRequest(
                    "$baseUrl/unicorn/v2/products/nearby?slug=$slug&lat=$defaultLat&lon=$defaultLon"
                ).build().let { req ->
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@withContext emptyList()
                        resp.body?.string() ?: return@withContext emptyList()
                    }
                }
                parseNearbyProducts(productsBody)
            } catch (e: Exception) {
                Log.e("Okala", "خطای شبکه/پارس: ${e.message}", e)
                emptyList()
            }
        }

    /** بهترین دسته منطبق با پرس‌وجو را از درخت دسته‌ها پیدا می‌کند */
    private fun matchCategorySlug(queryText: String, categoriesBody: String): String? {
        val tokens = PersianNormalizer.normalize(queryText)
            .split(" ").filter { it.length >= 2 }
        if (tokens.isEmpty()) return null

        var bestSlug: String? = null
        var bestScore = 0

        fun walk(categories: JSONArray) {
            for (i in 0 until categories.length()) {
                val cat = categories.getJSONObject(i)
                val name = PersianNormalizer.normalize(cat.optString("name", ""))
                val latin = cat.optString("latinName", "")
                var score = 0
                for (t in tokens) {
                    if (name.contains(t)) score += t.length * 2
                }
                // تطبیق عمیق‌تر (دسته فرزند) امتیاز بیشتری می‌گیرد
                if (score > bestScore && latin.isNotBlank()) {
                    bestScore = score
                    bestSlug = latin
                }
                cat.optJSONArray("children")?.let { walk(it) }
            }
        }
        try {
            val cats = JSONObject(categoriesBody)
                .optJSONObject("data")?.optJSONArray("categories")
                ?: return null
            walk(cats)
        } catch (_: Exception) {
            return null
        }
        return bestSlug
    }

    private fun parseNearbyProducts(body: String): List<RawListing> {
        val results = mutableListOf<RawListing>()
        val data = JSONObject(body).optJSONArray("data") ?: return results
        // حداکثر ۳ فروشگاه اول
        for (s in 0 until minOf(data.length(), 3)) {
            val entry = data.getJSONObject(s)
            val store = entry.optJSONObject("store") ?: continue
            val storeName = store.optString("storeName", "اکالا")
            val storeId = store.optLong("storeId", 0L)
            val products = entry.optJSONArray("products") ?: continue
            for (i in 0 until minOf(products.length(), 8)) {
                val p = products.getJSONObject(i)
                val name = p.optString("name", "")
                if (name.isBlank()) continue
                val okPrice = p.optLong("okPrice", 0L)
                val price = p.optLong("price", 0L)
                val effectiveRial = if (okPrice > 0) okPrice else price
                val priceToman = effectiveRial / 10
                val productId = p.optLong("id", 0L)
                val imageUrl = p.optString("imageUrl", null)?.takeIf { it.isNotBlank() }
                val hasQuantity = p.optBoolean("hasQuantity", false)

                results.add(
                    RawListing(
                        title = name,
                        priceText = if (priceToman > 0) "$priceToman تومان" else "ناموجود",
                        url = "https://www.okala.com/product/$productId?storeId=$storeId",
                        imageUrl = imageUrl,
                        sellerName = storeName,
                        inStockHint = hasQuantity,
                        sourceId = sourceId
                    )
                )
            }
        }
        return results
    }

    override suspend fun fetchDetail(url: String): ListingDetail =
        ListingDetail("", "", emptyMap(), emptyList())
}
