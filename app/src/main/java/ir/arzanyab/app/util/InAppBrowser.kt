package ir.arzanyab.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/**
 * باز کردن لینک فروشگاه داخل خود برنامه (Custom Tab):
 * دکمه ✕ دارد و با برگشت مستقیم به ارزان‌یاب برمی‌گردد —
 * دیگر لازم نیست برنامه را کامل بست.
 * اگر Custom Tab در دسترس نبود، به مرورگر معمولی سقوط می‌کند.
 */
fun openListingUrl(context: Context, url: String) {
    val uri = try {
        Uri.parse(url)
    } catch (_: Exception) {
        return
    }
    try {
        val params = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(Color.parseColor("#0B3D2E")) // سبز زمردی تم
            .build()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(params)
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()
        customTabsIntent.launchUrl(context, uri)
    } catch (_: Exception) {
        // سقوط به مرورگر معمولی
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
        }
    }
}
