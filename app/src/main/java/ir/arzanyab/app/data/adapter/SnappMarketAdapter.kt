package ir.arzanyab.app.data.adapter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * اسنپ‌مارکت — API عمومی جست‌وجو (بدون احراز هویت).
 * GET https://svc.snapp.market/mobile/v3/search?query=...&lat=..&long=..
 * قیمت‌ها به ریال‌اند (تقسیم بر ۱۰ = تومان). مختصات پیش‌فرض: تهران.
 */
class SnappMarketAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : SourceAdapter {

    override val sourceId = "snappmarket"
    override val displayName = "اسنپ‌مارکت"

    // تهران پیش‌فرض (مرکز تحویل)
    private val defaultLat = 35.6892
    private val defaultLong = 51.3890

    override suspend fun search(query: ProductQuery): List<RawListing> =
        withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(query.queryText, "UTF-8")
            val url = "https://svc.snapp.market/mobile/v3/search" +
                "?query=$encodedQuery&lat=$defaultLat&long=$defaultLong" +
                "&superType=%5B4%5D&new_search=1&page=0&size=3&client=PWA&deviceType=PWA"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Mobile Safari/537.36")
                .addHeader("Accept", "application/json")
                .build()

            val results = mutableListOf<RawListing>()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("SnappMarket", "خطا: کد ${response.code}")
                        return@withContext emptyList()
                    }
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val items = JSONObject(body)
                        .optJSONObject("data")
                        ?.optJSONObject("vendor_product_variations")
                        ?.optJSONArray("items") ?: return@withContext emptyList()

                    // حداکثر ۲ فروشگاه اول تا لیست شلوغ نشود
                    for (v in 0 until minOf(items.length(), 2)) {
                        val vendor = items.getJSONObject(v)
                        val vendorTitle = vendor.optString("title", "اسنپ‌مارکت")
                        val products = vendor.optJSONArray("products") ?: continue
                        for (i in 0 until minOf(products.length(), 8)) {
                            val p = products.getJSONObject(i)
                            val title = p.optString("title", "")
                            if (title.isBlank()) continue
                            val priceRial = p.optLong("price", 0L)
                            val priceToman = priceRial / 10
                            val stock = p.optInt("stock", 0)
                            val imageUrl = p.optJSONArray("images")
                                ?.optJSONObject(0)?.optString("main", null)
                            val productId = p.optLong("id", 0L)

                            results.add(
                                RawListing(
                                    title = title,
                                    priceText = if (priceToman > 0) "$priceToman تومان" else "ناموجود",
                                    url = "https://snapp.market/shopping-list/general-search?query=$encodedQuery",
                                    imageUrl = imageUrl,
                                    sellerName = vendorTitle,
                                    inStockHint = stock > 0,
                                    sourceId = sourceId
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SnappMarket", "خطای شبکه/پارس: ${e.message}", e)
                return@withContext emptyList()
            }
            results
        }

    override suspend fun fetchDetail(url: String): ListingDetail =
        ListingDetail("", "", emptyMap(), emptyList())
}
