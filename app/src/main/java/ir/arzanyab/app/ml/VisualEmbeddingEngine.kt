package ir.arzanyab.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * استخراج امبدینگ تصویری با مدل CLIP موبایل (ONNX).
 * مدل با رضایت کاربر دانلود می‌شود و در حافظه داخلی نگه‌داری می‌شود.
 */
class VisualEmbeddingEngine(private val context: Context) {

    companion object {
        private const val MODEL_FILE_NAME = "visual_clip_mobile.onnx"
        private const val INPUT_SIZE = 224
        private const val EXPECTED_EMBEDDING_DIM = 512
    }

    private var ortSession: OrtSession? = null

    private fun ensureSession(): OrtSession {
        ortSession?.let { return it }
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        if (!modelFile.exists()) {
            throw IllegalStateException("مدل دانلود نشده است. ابتدا از صفحه جست‌وجوی تصویری مدل را دانلود کنید.")
        }
        val env = OrtEnvironment.getEnvironment()
        val options = OrtSession.SessionOptions()
        val session = env.createSession(modelFile.absolutePath, options)
        ortSession = session
        return session
    }

    suspend fun extractEmbedding(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        val session = ensureSession()

        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val floatBuffer = FloatBuffer.allocate(3 * INPUT_SIZE * INPUT_SIZE)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // نرمال‌سازی ImageNet
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        val rChannel = FloatArray(INPUT_SIZE * INPUT_SIZE)
        val gChannel = FloatArray(INPUT_SIZE * INPUT_SIZE)
        val bChannel = FloatArray(INPUT_SIZE * INPUT_SIZE)

        for (i in pixels.indices) {
            val p = pixels[i]
            rChannel[i] = (((p shr 16) and 0xFF) / 255.0f - mean[0]) / std[0]
            gChannel[i] = (((p shr 8) and 0xFF) / 255.0f - mean[1]) / std[1]
            bChannel[i] = ((p and 0xFF) / 255.0f - mean[2]) / std[2]
        }
        floatBuffer.put(rChannel).put(gChannel).put(bChannel).rewind()

        val inputName = session.inputNames.iterator().next()
        val inputTensor = OnnxTensor.createTensor(
            ai.onnxruntime.OrtEnvironment.getEnvironment(),
            floatBuffer,
            longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        )

        val output = session.run(mapOf(inputName to inputTensor))
        inputTensor.close()

        val embeddingTensor = output.get(0) as OnnxTensor
        val raw = embeddingTensor.floatBuffer.array()
        output.close()

        if (raw.size != EXPECTED_EMBEDDING_DIM) {
            Log.e("VisualEngine", "ابعاد خروجی مدل (${raw.size}) با انتظار ($EXPECTED_EMBEDDING_DIM) فرق دارد.")
            throw IllegalStateException("ابعاد خروجی مدل ناسازگار است: ${raw.size}")
        }

        // L2 نرمال‌سازی
        val norm = sqrt(raw.sumOf { (it * it).toDouble() }).toFloat().coerceAtLeast(1e-9f)
        FloatArray(raw.size) { raw[it] / norm }
    }

    fun isModelReady(): Boolean = File(context.filesDir, MODEL_FILE_NAME).exists()

    fun close() {
        ortSession?.close()
        ortSession = null
    }
}
