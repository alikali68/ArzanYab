package ir.arzanyab.app.data.resolver

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * تبدیل بارکد به نام کالا با Open Food Facts (رایگان، بدون کلید API).
 * بارکدهای خوراکی و آشامیدنی — از جمله پیشوند 626 ایران — را پوشش می‌دهد.
 * اگر کالا پیدا نشود null برمی‌گرداند تا فراخواننده با خودِ بارکد ادامه دهد.
 */
class BarcodeProductResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    suspend fun resolveProductName(barcode: String): String? = withContext(Dispatchers.IO) {
        try {
            val clean = barcode.trim()
            if (clean.length < 8) return@withContext null
            val request = Request.Builder()
                .url("https://world.openfoodfacts.org/api/v0/product/$clean.json")
                .addHeader("User-Agent", "ArzanYab/1.0 (Android price comparison)")
                .addHeader("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val root = JSONObject(body)
                if (root.optInt("status", 0) != 1) return@withContext null
                val product = root.optJSONObject("product") ?: return@withContext null
                val nameFa = product.optString("product_name_fa", "").trim()
                val name = product.optString("product_name", "").trim()
                val raw = nameFa.ifBlank { name }.ifBlank { return@withContext null }
                mapToPersian(raw)
            }
        } catch (e: Exception) {
            Log.w("BarcodeResolver", "OFF lookup failed: ${e.message}")
            null
        }
    }

    /**
     * نگاشت برندهای پرتکرار انگلیسی به فارسی؛
     * اگر برند شناخته‌شده‌ای نبود، همان نام اصلی برمی‌گردد.
     */
    fun mapToPersian(rawName: String): String {
        val lower = rawName.lowercase()
        for ((en, fa) in BRAND_MAP) {
            if (lower.contains(en)) return fa
        }
        return rawName
    }

    companion object {
        // ترتیب مهم است: نام‌های طولانی‌تر اول
        private val BRAND_MAP = linkedMapOf(
            "coca-cola" to "کوکاکولا",
            "coca cola" to "کوکاکولا",
            "coke" to "کوکاکولا",
            "pepsi" to "پپسی",
            "mirinda" to "میریندا",
            "fanta" to "فانتا",
            "sprite" to "اسپرایت",
            "red bull" to "ردبول",
            "monster energy" to "مانستر",
            "nescafe" to "نسکافه",
            "nesquik" to "نسکوییک",
            "lipton" to "لیپتون",
            "nestle" to "نستله",
            "danone" to "دانون",
            "activia" to "اکتیویا",
            "nutella" to "نوتلا",
            "kinder" to "کیندر",
            "oreo" to "اورئو",
            "kitkat" to "کیت‌کت",
            "kit kat" to "کیت‌کت",
            "snickers" to "اسنیکرز",
            "twix" to "توییکس",
            "mars" to "مارس",
            "bounty" to "باونتی",
            "pringles" to "پرینگلز",
            "lays" to "لیز",
            "doritos" to "دوریتوس",
            "cheetos" to "چیتوز",
            "heinz" to "هاینز",
            "knorr" to "کنور",
            "maggi" to "مگی",
            "barilla" to "باریلا",
            "kellogg" to "کلگز",
            "corn flakes" to "کورن فلکس",
            "nesvita" to "نسویتا"
        )
    }
}
