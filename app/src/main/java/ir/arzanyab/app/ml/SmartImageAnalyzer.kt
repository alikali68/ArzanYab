package ir.arzanyab.app.ml

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

/**
 * تشخیص محتوای عکس روی گوشی با برچسب‌گذاری محلی (ML Kit)
 * → تبدیل به کلیدواژه فارسی → آماده برای جست‌وجوی قیمت.
 * کاملاً روی گوشی؛ هیچ عکسی آپلود نمی‌شود.
 * مدل داخل خود برنامه است و نیازی به دانلود جدا ندارد.
 */
class SmartImageAnalyzer(private val context: Context) {

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    /**
     * عکس را تحلیل می‌کند و کلیدواژه فارسی جست‌وجو برمی‌گرداند.
     * مثلاً عکس بطری نوشابه → «بطری نوشابه»
     */
    suspend fun analyzeToKeywords(imageUri: Uri): String =
        suspendCancellableCoroutine { cont ->
            val image = try {
                InputImage.fromFilePath(context, imageUri)
            } catch (e: Exception) {
                cont.resumeWithException(IllegalStateException("خواندن عکس ناموفق بود."))
                return@suspendCancellableCoroutine
            }
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val query = PersianLabelMap.buildQuery(
                        labels.sortedByDescending { it.confidence }
                            .map { it.text to it.confidence }
                    )
                    if (query.isBlank()) {
                        cont.resumeWithException(IllegalStateException("کالایی در عکس تشخیص داده نشد."))
                    } else {
                        cont.resume(query, null)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(
                        IllegalStateException("تحلیل عکس ناموفق بود: ${e.message}")
                    )
                }
        }

    fun close() {
        try {
            labeler.close()
        } catch (_: Exception) {
        }
    }
}
