package ir.arzanyab.app.ml

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed interface ModelDownloadState {
    data object Idle : ModelDownloadState
    data class Downloading(val progressPercent: Int) : ModelDownloadState
    data object Ready : ModelDownloadState
    data class Error(val message: String) : ModelDownloadState
}

/** دانلود مدل ONNX فقط با رضایت صریح کاربر، با نمایش پیشرفت و ذخیره اتمی */
class ModelDownloadManager(private val context: Context) {

    companion object {
        // مدل واقعی: MobileCLIP-S2 نسخه ONNX (vision encoder) — ورودی pixel_values با [1,3,224,224] و خروجی image_embeds با [1,512]
        // آدرس در 2026-10-11 بررسی و سالم تأیید شد (حجم واقعی ~۱۴۳ مگابایت)
        private const val MODEL_URL = "https://huggingface.co/memojo/mobileclip-s2-onnx/resolve/main/mobileclip_image_encoder.onnx"
        private const val MODEL_FILE_NAME = "visual_clip_mobile.onnx"
        private const val APPROX_SIZE_MB = 140
    }

    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    fun getApproxSizeText(): String = "حدود $APPROX_SIZE_MB مگابایت"

    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        _state.value = ModelDownloadState.Downloading(0)
        val finalFile = File(context.filesDir, MODEL_FILE_NAME)
        if (finalFile.exists()) {
            _state.value = ModelDownloadState.Ready
            return@withContext
        }
        val tempFile = File(context.cacheDir, "$MODEL_FILE_NAME.tmp")
        try {
            val request = Request.Builder().url(MODEL_URL).build()
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("خطای سرور: ${response.code}")
                val body = response.body ?: throw Exception("بدنه خالی")
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                tempFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0) {
                                val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                                _state.value = ModelDownloadState.Downloading(percent)
                            }
                        }
                    }
                }
            }
            // ذخیره اتمی: rename بعد از تکمیل
            if (!tempFile.renameTo(finalFile)) throw Exception("خطا در نهایی‌سازی فایل مدل")
            _state.value = ModelDownloadState.Ready
        } catch (e: Exception) {
            tempFile.delete()
            _state.value = ModelDownloadState.Error(e.message ?: "خطای نامشخص")
        }
    }

    fun isModelDownloaded(): Boolean = File(context.filesDir, MODEL_FILE_NAME).exists()
}
