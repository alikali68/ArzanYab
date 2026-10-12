package ir.arzanyab.app.data.adapter

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class EmallsAdapter(
    private val context: Context? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : SourceAdapter {

    override val sourceId = "emalls"
    override val displayName = "ایمالز"

    private val defaultBaseUrl = "https://emalls.ir/Search/?query="

    override suspend fun search(query: ProductQuery): List<RawListing> = withContext(Dispatchers.IO) {
        val dynamicConfig = context?.let { AdapterConfigLoader.getConfigForSource(it, sourceId) }
        val encoded = URLEncoder.encode(query.queryText, "UTF-8")
        val targetUrl = dynamicConfig?.searchUrlTemplate?.replace("{query}", encoded)
            ?: "$defaultBaseUrl$encoded"

        val itemSelector = dynamicConfig?.itemSelector ?: ".item"
        val titleSelector = dynamicConfig?.titleSelector ?: ".title a"
        val priceSelector = dynamicConfig?.priceSelector ?: ".price"
        val imageSelector = dynamicConfig?.imageSelector ?: ".image img"

        val request = Request.Builder()
            .url(targetUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val results = mutableListOf<RawListing>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("EmallsAdapter", "خطا: کد ${response.code} روی $targetUrl")
                    return@withContext emptyList()
                }
                val html = response.body?.string() ?: return@withContext emptyList()
                val items = Jsoup.parse(html).select(itemSelector)
                if (items.isEmpty()) {
                    Log.w("EmallsAdapter", "سلکتور '$itemSelector' چیزی پیدا نکرد؛ احتمال تغییر قالب.")
                    return@withContext emptyList()
                }

                for (el in items) {
                    val titleEl = el.selectFirst(titleSelector) ?: continue
                    val title = titleEl.text().trim()
                    val href = titleEl.attr("href")
                    val price = el.selectFirst(priceSelector)?.text()?.trim() ?: "ناموجود"

                    results.add(
                        RawListing(
                            title = title,
                            priceText = price,
                            url = if (href.startsWith("http")) href else "https://emalls.ir$href",
                            imageUrl = el.selectFirst(imageSelector)?.attr("src"),
                            sellerName = "فروشندگان ایمالز",
                            inStockHint = !price.contains("ناموجود"),
                            sourceId = sourceId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("EmallsAdapter", "خطای شبکه/پارس HTML: ${e.message}", e)
            return@withContext emptyList()
        }
        results
    }

    override suspend fun fetchDetail(url: String): ListingDetail =
        ListingDetail("", "", emptyMap(), emptyList())
}
