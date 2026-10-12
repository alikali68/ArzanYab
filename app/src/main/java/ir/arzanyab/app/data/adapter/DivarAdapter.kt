package ir.arzanyab.app.data.adapter

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DivarAdapter(
    private val context: Context? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : SourceAdapter {

    override val sourceId = "divar"
    override val displayName = "دیوار"

    private val defaultEndpoint = "https://api.divar.ir/v8/postlist/w/search"

    override suspend fun search(query: ProductQuery): List<RawListing> = withContext(Dispatchers.IO) {
        val dynamicConfig = context?.let { AdapterConfigLoader.getConfigForSource(it, sourceId) }
        val targetUrl = dynamicConfig?.searchUrlTemplate ?: defaultEndpoint

        val jsonPayload = JSONObject().apply {
            put("city_ids", listOf("1"))
            put("search_data", JSONObject().apply { put("query", query.queryText) })
        }

        val request = Request.Builder()
            .url(targetUrl)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Mobile")
            .build()

        val results = mutableListOf<RawListing>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("DivarAdapter", "خطا: کد ${response.code} روی $targetUrl")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val widgetList = JSONObject(body).optJSONArray("widget_list")
                if (widgetList == null) {
                    Log.w("DivarAdapter", "ساختار پاسخ تغییر کرده؛ widget_list یافت نشد.")
                    return@withContext emptyList()
                }

                for (i in 0 until widgetList.length()) {
                    val data = widgetList.getJSONObject(i).optJSONObject("data") ?: continue
                    val title = data.optString("title", "")
                    val token = data.optString("token", "")
                    val middleDesc = data.optString("middle_description", "")
                    val bottomDesc = data.optString("bottom_description", "")
                    val imageUrl = data.optJSONObject("image")?.optString("src", null)

                    val priceText = when {
                        middleDesc.contains("تومان") -> middleDesc
                        bottomDesc.contains("تومان") -> bottomDesc
                        else -> middleDesc.ifBlank { "توافقی" }
                    }

                    results.add(
                        RawListing(
                            title = title,
                            priceText = priceText,
                            url = "https://divar.ir/v/$token",
                            imageUrl = imageUrl,
                            sellerName = "آگهی دیوار",
                            inStockHint = true,
                            sourceId = sourceId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DivarAdapter", "خطای شبکه/پارس: ${e.message}", e)
            return@withContext emptyList()
        }
        results
    }

    override suspend fun fetchDetail(url: String): ListingDetail =
        ListingDetail("", "", emptyMap(), emptyList())
}
