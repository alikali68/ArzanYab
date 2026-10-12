package ir.arzanyab.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** کلید AES-256 در Keystore + پس‌فریز رمزنگاری‌شده دیتابیس در SharedPreferences */
object SecurityKeyManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ArzanYabDbKey"
    private const val PREFS_NAME = "arzanyab_secure_prefs"
    private const val ENCRYPTED_PASSPHRASE_KEY = "encrypted_db_pass"
    private const val IV_KEY = "db_pass_iv"

    fun getDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPassHex = prefs.getString(ENCRYPTED_PASSPHRASE_KEY, null)
        val ivHex = prefs.getString(IV_KEY, null)
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (encryptedPassHex == null || ivHex == null) {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                keyGen.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                keyGen.generateKey()
            }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val rawPass = ByteArray(32).apply { SecureRandom().nextBytes(this) }
            val encryptedBytes = cipher.doFinal(rawPass)

            prefs.edit()
                .putString(ENCRYPTED_PASSPHRASE_KEY, toHex(encryptedBytes))
                .putString(IV_KEY, toHex(cipher.iv))
                .apply()
            return rawPass
        } else {
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, fromHex(ivHex)))
            return cipher.doFinal(fromHex(encryptedPassHex))
        }
    }

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private fun fromHex(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }
}
