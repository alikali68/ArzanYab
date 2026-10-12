package ir.arzanyab.app.data.upload

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * آپلود ناشناس عکس در هاست رایگان برای گرفتن URL عمومی.
 * لازم برای جست‌وجوی تصویری ترب (ورودی‌اش URL عمومی عکس است).
 * هیچ حساب کاربری لازم نیست و هیچ داده‌ای به هویت کاربر گره نمی‌خورد.
 *
 * هر لینک قبل از تحویل «تأیید» می‌شود: باید واقعاً عکس برگرداند،
 * وگرنه سراغ هاست بعدی می‌رویم. خطای همه هاست‌ها در پیام نهایی می‌آید
 * تا مشخص شود کدام هاست از شبکه کاربر در دسترس است.
 */
object AnonymousImageUploader {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /** آپلود فایل و برگرداندن URL عمومی تأییدشده؛ در صورت شکست همه مسیرها استثنا می‌دهد */
    suspend fun uploadImage(file: File): String = withContext(Dispatchers.IO) {
        val attempts: List<Pair<String, (File) -> String>> = listOf(
            "catbox" to ::uploadToCatbox,
            "tmpfiles" to ::uploadToTmpFiles,
            "uguu" to ::uploadToUguu
        )
        val errors = mutableListOf<String>()
        for ((name, attempt) in attempts) {
            try {
                val url = attempt(file)
                if (verifyImageUrl(url)) return@withContext url
                errors += "$name: لینک برگشت ولی عکس را تحویل نداد ($url)"
                Log.w("ImgUpload", errors.last())
            } catch (e: Exception) {
                errors += "$name: ${e.message?.take(120)}"
                Log.w("ImgUpload", errors.last())
            }
        }
        throw IllegalStateException(
            "آپلود عکس به هیچ هاستی موفق نبود. جزئیات: " + errors.joinToString(" | ")
        )
    }

    /**
     * تأیید اینکه URL واقعاً یک عکس برمی‌گرداند.
     * با GET کامل ولی بستن سریع؛ فقط هدر Content-Type بررسی می‌شود.
     */
    private fun verifyImageUrl(url: String): Boolean {
        return try {
            val req = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Mobile")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val ct = resp.header("Content-Type") ?: ""
                ct.startsWith("image/")
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun uploadToCatbox(file: File): String {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart(
                "fileToUpload", file.name,
                file.asRequestBody("image/jpeg".toMediaType())
            )
            .build()
        val request = Request.Builder()
            .url("https://catbox.moe/user/api.php")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val result = response.body?.string()?.trim()
                ?: throw IllegalStateException("پاسخ خالی از catbox")
            if (!response.isSuccessful || !result.startsWith("http")) {
                throw IllegalStateException("catbox خطا داد (کد ${response.code}): ${result.take(100)}")
            }
            return result
        }
    }

    private fun uploadToTmpFiles(file: File): String {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                file.asRequestBody("image/jpeg".toMediaType())
            )
            .build()
        val request = Request.Builder()
            .url("https://tmpfiles.org/api/v1/upload")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string()
                ?: throw IllegalStateException("پاسخ خالی از tmpfiles")
            if (!response.isSuccessful) throw IllegalStateException("tmpfiles خطا داد (کد ${response.code})")
            val url = JSONObject(raw).optJSONObject("data")?.optString("url")
                ?: throw IllegalStateException("URL در پاسخ tmpfiles نبود")
            // لینک مستقیم دانلود
            return url.replace("tmpfiles.org/", "tmpfiles.org/dl/")
        }
    }

    private fun uploadToUguu(file: File): String {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "files[]", file.name,
                file.asRequestBody("image/jpeg".toMediaType())
            )
            .build()
        val request = Request.Builder()
            .url("https://uguu.se/upload.php")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string()?.trim()
                ?: throw IllegalStateException("پاسخ خالی از uguu")
            if (!response.isSuccessful) throw IllegalStateException("uguu خطا داد (کد ${response.code})")
            // پاسخ JSON: {"success":true,"files":[{"url":"...","name":"..."}]}
            val url = try {
                JSONObject(raw).optJSONArray("files")
                    ?.optJSONObject(0)?.optString("url")
            } catch (_: Exception) { null }
                ?: try {
                    // حالت جایگزین: آرایه مستقیم
                    JSONArray(raw).optJSONObject(0)?.optString("url")
                } catch (_: Exception) { null }
            if (url.isNullOrBlank() || !url.startsWith("http")) {
                throw IllegalStateException("uguu لینک معتبر نداد: ${raw.take(100)}")
            }
            return url
        }
    }
}
