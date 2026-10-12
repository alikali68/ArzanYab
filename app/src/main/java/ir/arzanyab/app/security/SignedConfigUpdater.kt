package ir.arzanyab.app.security

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.File

/**
 * دانلود و اعتبارسنجی کانفیگ آداپتورها با امضای Ed25519.
 * نکته: PUBLIC_KEY_BYTES نمونه است — کلید عمومی واقعی خودتان را جایگزین کنید
 * و با کلید خصوصی متناظر، فایل adapters.json را امضا کنید.
 */
object SignedConfigUpdater {
    private val PUBLIC_KEY_BYTES = byteArrayOf(
        0x3b, 0x6a, 0x27, 0xbc.toByte(), 0xce.toByte(), 0xb6.toByte(), 0xa3.toByte(), 0x8b.toByte(),
        0x39, 0x3a, 0x1e, 0x85.toByte(), 0x8f.toByte(), 0x1d, 0x06, 0x24,
        0x20, 0x42, 0xc3.toByte(), 0x7b, 0x62, 0x8b.toByte(), 0x76, 0xd0.toByte(),
        0x26, 0x1b, 0x60, 0x42, 0x0f, 0x3b, 0x65, 0x45
    )

    suspend fun fetchAndVerify(context: Context, configUrl: String, signatureHex: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(configUrl).build()
                val response = OkHttpClient().newCall(request).execute()
                if (!response.isSuccessful) return@withContext false

                val configBytes = response.body?.bytes() ?: return@withContext false
                val signatureBytes = hexToBytes(signatureHex)

                val pubKey = Ed25519PublicKeyParameters(PUBLIC_KEY_BYTES, 0)
                val verifier = Ed25519Signer()
                verifier.init(false, pubKey)
                verifier.update(configBytes, 0, configBytes.size)

                if (verifier.verifySignature(signatureBytes)) {
                    File(context.filesDir, "verified_adapters.json").writeBytes(configBytes)
                    ir.arzanyab.app.data.adapter.AdapterConfigLoader.invalidateCache()
                    true
                } else false
            } catch (_: Exception) {
                false
            }
        }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }
}
