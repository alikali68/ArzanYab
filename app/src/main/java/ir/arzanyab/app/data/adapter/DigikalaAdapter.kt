package ir.arzanyab.app.data.adapter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class DigikalaAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : SourceAdapter {

    override val sourceId = "digikala"
    override val displayName = "دیجی‌کالا"

    override suspend fun search(query: ProductQuery): List<RawListing> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query.queryText, "UTF-8")
        val url = "https://api.digikala.com/v1/search/?q=$encodedQuery&page=${query.page}"
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Mobile Safari/537.36")
            .addHeader("Accept", "application/json")
            .build()

        val results = mutableListOf<RawListing>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("DigikalaAdapter", "خطا: کد ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val products = JSONObject(body).optJSONObject("data")?.optJSONArray("products")
                    ?: return@withContext emptyList()

                for (i in 0 until products.length()) {
                    val p = products.getJSONObject(i)
                    val title = p.optString("title_fa", p.optString("title_en", ""))
                    val id = p.optInt("id", 0)
                    // images.main.url آرایه است؛ url.uri آدرس واقعی صفحه محصول است
                    val mainImage = p.optJSONObject("images")?.optJSONObject("main")?.optJSONArray("url")?.optString(0)
                    val productUri = p.optJSONObject("url")?.optString("uri", "") ?: ""
                    val productUrl = if (productUri.isNotBlank()) "https://www.digikala.com$productUri"
                        else "https://www.digikala.com/product/dkp-$id/"
                    val variant = p.optJSONObject("default_variant")
                    val sellingPrice = variant?.optJSONObject("price")?.optLong("selling_price", 0L) ?: 0L
                    // وضعیت‌های واقعی: marketable / sellable (نه in_stock)
                    val variantStatus = variant?.optString("status", "")
                    val isAvailable = variantStatus == "marketable" || variantStatus == "sellable"
                    val sellerTitle = variant?.optJSONObject("seller")?.optString("title", "دیجی‌کالا")

                    val priceToman = sellingPrice / 10 // ریال به تومان
                    val priceText = if (priceToman > 0) "$priceToman تومان" else "ناموجود"

                    results.add(
                        RawListing(
                            title = title,
                            priceText = priceText,
                            url = productUrl,
                            imageUrl = mainImage,
                            sellerName = sellerTitle,
                            inStockHint = isAvailable,
                            sourceId = sourceId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DigikalaAdapter", "خطای شبکه/پارس: ${e.message}", e)
            return@withContext emptyList()
        }
        results
    }

    override suspend fun fetchDetail(url: String): ListingDetail =
        ListingDetail("", "", emptyMap(), emptyList())
}
