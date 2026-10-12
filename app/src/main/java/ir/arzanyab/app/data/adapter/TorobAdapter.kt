package ir.arzanyab.app.data.adapter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class TorobAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : SourceAdapter {

    override val sourceId = "torob"
    override val displayName = "ترب"

    override suspend fun search(query: ProductQuery): List<RawListing> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query.queryText, "UTF-8")
        // نکته: پارامتر درست q است (نه query) و page از ۰ شروع می‌شود
        val zeroBasedPage = (query.page - 1).coerceAtLeast(0)
        val url = "https://api.torob.com/v4/base-product/search/?q=$encodedQuery&page=$zeroBasedPage"
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Mobile Safari/537.36")
            .addHeader("Accept", "application/json")
            .build()

        val results: List<RawListing>
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("TorobAdapter", "خطا: کد ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                results = parseTorobBaseProductResults(body)
            }
        } catch (e: Exception) {
            Log.e("TorobAdapter", "خطای شبکه/پارس: ${e.message}", e)
            return@withContext emptyList()
        }
        results
    }

    override suspend fun fetchDetail(url: String): ListingDetail =
        ListingDetail("", "", emptyMap(), emptyList())
}

/** پارس مشترک پاسخ‌های base-product ترب (جست‌وجوی متنی و تصویری) */
internal fun parseTorobBaseProductResults(body: String): List<RawListing> {
    val results = mutableListOf<RawListing>()
    val resultsArray = JSONObject(body).optJSONArray("results") ?: return results
    for (i in 0 until resultsArray.length()) {
        val item = resultsArray.getJSONObject(i)
        val title = item.optString("name1", "")
        if (title.isBlank()) continue
        val price = item.optLong("price", 0L)
        val priceText = item.optString("price_text", "")
        // فیلد واقعی آدرس صفحه محصول در API ترب
        val pageUrl = item.optString("web_client_absolute_url", item.optString("page_url", ""))
        val webUrl = if (pageUrl.startsWith("http")) pageUrl else "https://torob.com$pageUrl"

        results.add(
            RawListing(
                title = title,
                priceText = if (price > 0) "$price تومان" else priceText,
                url = webUrl,
                imageUrl = item.optString("image_url", null),
                sellerName = item.optString("shop_text", "فروشگاه‌های ترب"),
                inStockHint = price > 0,
                sourceId = "torob"
            )
        )
    }
    return results
}
