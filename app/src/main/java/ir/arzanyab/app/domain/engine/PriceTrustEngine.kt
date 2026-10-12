package ir.arzanyab.app.domain.engine

import ir.arzanyab.app.domain.normalizer.PersianNormalizer
import kotlin.math.min

data class EvaluatedPriceItem(
    val title: String,
    val rawPriceText: String,
    val priceInToman: Long?,
    val trustScore: Int,
    val isBait: Boolean,
    val baitReason: String?,
    val sellerName: String,
    val sourceId: String,
    val sourceName: String,
    val url: String,
    val imageUrl: String?
)

object PriceTrustEngine {

    private val SUSPICIOUS_EXACT_PRICES = setOf(1L, 10L, 100L, 1000L)

    fun evaluateListings(query: String, rawItems: List<RawInputItem>): List<EvaluatedPriceItem> {
        val validPrices = rawItems.mapNotNull { it.priceInToman }.filter { it > 0 }.sorted()

        val median = calculateMedian(validPrices)
        val (q1, q3) = calculateQuartiles(validPrices)
        val iqr = q3 - q1
        val lowerBound = q1 - (1.5 * iqr)

        val queryTokens = PersianNormalizer.normalize(query).split(" ").filter { it.isNotBlank() }

        return rawItems.map { item ->
            val price = item.priceInToman

            // قانون ۱: قیمت توافقی / تماس
            if (price == null) {
                return@map item.toEvaluated(30, true, "قیمت توافقی یا تماس بگیرید")
            }

            // قانون ۲: قیمت‌های قطعی طعمه
            if (price <= 1000L || (median > 50000L && price in SUSPICIOUS_EXACT_PRICES)) {
                return@map item.toEvaluated(0, true, "قیمت نمادین و نامعتبر")
            }

            // قانون ۳: امتیاز پایه آماری
            var score = if (median > 0) {
                if (price < median) (100.0 * min(1.0, price.toDouble() / median.toDouble())).toInt()
                else 100
            } else 70

            var reason: String? = null

            // جریمه پرت آماری
            if (validPrices.size >= 4 && price < lowerBound) {
                score = min(score, 35)
                reason = "قیمت به شکل غیرمعمول کمتر از عرف بازار است"
            }

            // قانون ۴: تطابق عنوان
            val titleNormalized = PersianNormalizer.normalize(item.title)
            val matchCount = queryTokens.count { titleNormalized.contains(it) }
            val matchRatio = if (queryTokens.isNotEmpty()) matchCount.toDouble() / queryTokens.size else 1.0
            if (matchRatio < 0.4) {
                score -= 30
                reason = reason ?: "احتمالاً کالای جانبی یا متفاوت است"
            }

            if (item.isVerifiedSeller) score += 10

            val finalScore = score.coerceIn(0, 100)
            item.toEvaluated(finalScore, finalScore < 40, reason)
        }
    }

    private fun RawInputItem.toEvaluated(score: Int, bait: Boolean, reason: String?) =
        EvaluatedPriceItem(title, rawPriceText, priceInToman, score, bait, reason, sellerName, sourceId, sourceName, url, imageUrl)

    private fun calculateMedian(sortedList: List<Long>): Long {
        if (sortedList.isEmpty()) return 0L
        val size = sortedList.size
        return if (size % 2 == 1) sortedList[size / 2]
        else (sortedList[(size / 2) - 1] + sortedList[size / 2]) / 2
    }

    private fun calculateQuartiles(sortedList: List<Long>): Pair<Double, Double> {
        if (sortedList.isEmpty()) return Pair(0.0, 0.0)
        if (sortedList.size == 1) return Pair(sortedList[0].toDouble(), sortedList[0].toDouble())
        val mid = sortedList.size / 2
        val lowerHalf = sortedList.subList(0, mid)
        val upperHalf = if (sortedList.size % 2 == 0) sortedList.subList(mid, sortedList.size)
        else sortedList.subList(mid + 1, sortedList.size)
        return Pair(calculateMedian(lowerHalf).toDouble(), calculateMedian(upperHalf).toDouble())
    }

    data class RawInputItem(
        val title: String,
        val rawPriceText: String,
        val priceInToman: Long?,
        val sellerName: String,
        val isVerifiedSeller: Boolean,
        val sourceId: String,
        val sourceName: String,
        val url: String,
        val imageUrl: String?
    )
}
