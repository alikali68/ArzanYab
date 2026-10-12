package ir.arzanyab.app.data.adapter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * جست‌وجوی تصویری ترب — همان API که خود سایت ترب استفاده می‌کند.
 * GET https://api.torob.com/v4/base-product/search-by-image/?image_url=<url>
 * بدون احراز هویت، بدون بات‌چک. ورودی باید URL عمومی قابل دسترس عکس باشد.
 */
class TorobVisualAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {
    suspend fun searchByImageUrl(publicImageUrl: String): List<RawListing> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(publicImageUrl, "UTF-8")
            val url =
                "https://api.torob.com/v4/base-product/search-by-image/?image_url=$encoded&discover_method=search_image_url"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Mobile Safari/537.36")
                .addHeader("Accept", "application/json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("TorobVisual", "خطا: کد ${response.code}")
                        return@withContext emptyList()
                    }
                    val body = response.body?.string() ?: return@withContext emptyList()
                    parseTorobBaseProductResults(body)
                }
            } catch (e: Exception) {
                Log.e("TorobVisual", "خطای شبکه/پارس: ${e.message}", e)
                emptyList()
            }
        }
}
