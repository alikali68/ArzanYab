package ir.arzanyab.app.domain.engine

import ir.arzanyab.app.data.adapter.DigikalaAdapter
import ir.arzanyab.app.data.adapter.ProductQuery
import ir.arzanyab.app.data.adapter.TorobAdapter

sealed interface BarcodeResolution {
    data class Resolved(val productTitle: String) : BarcodeResolution
    data object Unresolved : BarcodeResolution
}

/** حل بارکد در ۳ مرحله: کش محلی ← وب‌سرویس ← سقوط به جست‌وجوی متنی */
class BarcodeResolver(
    private val digikalaAdapter: DigikalaAdapter = DigikalaAdapter(),
    private val torobAdapter: TorobAdapter = TorobAdapter()
) {
    suspend fun resolve(barcode: String): BarcodeResolution {
        // مرحله ۱: کش محلی (در این نسخه ساده‌سازی شده)
        // مرحله ۲: جست‌وجو در آداپتورها با خودِ کد بارکد
        val query = ProductQuery(queryText = barcode, barcode = barcode)
        val results = digikalaAdapter.search(query) + torobAdapter.search(query)
        val top = results.firstOrNull()
        return if (top != null) {
            BarcodeResolution.Resolved(top.title)
        } else {
            // مرحله ۳: Unresolved → صفحه اسکنر به جست‌وجوی متنی با خودِ کد سقوط می‌کند
            BarcodeResolution.Unresolved
        }
    }
}
