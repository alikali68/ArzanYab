package ir.arzanyab.app.domain.normalizer

object PriceParser {
    data class ParsedPriceResult(
        val priceInToman: Long?,
        val isNegotiableOrCall: Boolean,
        val originalRaw: String
    )

    private val CALL_KEYWORDS = listOf("تماس بگیرید", "توافقی", "قیمت توافقی", "تماس", "رایگان")

    fun parse(rawPrice: String?): ParsedPriceResult {
        if (rawPrice.isNullOrBlank()) return ParsedPriceResult(null, false, "")

        val normalized = PersianNormalizer.normalize(rawPrice)
        for (kw in CALL_KEYWORDS) {
            if (normalized.contains(kw)) {
                return ParsedPriceResult(null, true, rawPrice)
            }
        }

        val englishDigitsText = PersianNormalizer.toEnglishDigits(normalized)
        val isRial = englishDigitsText.contains("ریال")

        // حذف جداکننده‌ها و گرفتن فقط اولین گروه عددی (قیمت‌های بازه‌ای)
        val withoutSeparators = englishDigitsText.replace(",", "").replace("٬", "")
        val firstNumber = Regex("[0-9]+").find(withoutSeparators)?.value
            ?: return ParsedPriceResult(null, false, rawPrice)

        val parsedAmount = firstNumber.toLongOrNull()
            ?: return ParsedPriceResult(null, false, rawPrice)
        val finalToman = if (isRial) parsedAmount / 10 else parsedAmount

        return ParsedPriceResult(finalToman, false, rawPrice)
    }
}
