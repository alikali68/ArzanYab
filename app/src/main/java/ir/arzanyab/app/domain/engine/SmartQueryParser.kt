package ir.arzanyab.app.domain.engine

import ir.arzanyab.app.domain.normalizer.PersianNormalizer

/**
 * پردازش پرس‌وجوی فارسی: استخراج بازه قیمت و مرتب‌سازی از متن جست‌وجو.
 *
 * - قید قیمت را از متن جدا می‌کند: «گوشی زیر ۱۰ میلیون» → سقف قیمت + کوئری تمیز
 * - «ارزان‌ترین» را به مرتب‌سازی ارزان‌ترین تبدیل می‌کند
 * - کوئری تمیز برای تطبیق بهتر عنوان‌ها به موتور جست‌وجو می‌رود
 */
data class ParsedQuery(
    val cleanQuery: String,
    val maxPriceToman: Long?,
    val minPriceToman: Long?,
    val preferCheapest: Boolean,
    val preferExpensive: Boolean
)

object SmartQueryParser {

    private val MILLION_WORDS = listOf("میلیون", "ملیون")
    private val THOUSAND_WORDS = listOf("هزار")

    fun parse(rawQuery: String?): ParsedQuery {
        if (rawQuery.isNullOrBlank()) {
            return ParsedQuery("", null, null, false, false)
        }
        // ارقام فارسی → انگلیسی برای استخراج عدد
        val en = PersianNormalizer.toEnglishDigits(rawQuery)
        var working = " $en "

        var maxPrice: Long? = null
        var minPrice: Long? = null

        // الگوهای سقف قیمت: «زیر ۱۰ میلیون»، «کمتر از ۵ میلیون»، «تا ۲ میلیون تومان»
        val maxPatterns = listOf(
            Regex("""زیر\s+([\d.,]+)\s*(میلیون|ملیون|هزار|تومان)?"""),
            Regex("""کمتر\s+از\s+([\d.,]+)\s*(میلیون|ملیون|هزار|تومان)?"""),
            Regex("""حداکثر\s+([\d.,]+)\s*(میلیون|ملیون|هزار|تومان)?"""),
            Regex("""تا\s+([\d.,]+)\s*(میلیون|ملیون|هزار)\s*(تومان)?""")
        )
        for (p in maxPatterns) {
            p.find(working)?.let { m ->
                maxPrice = toToman(m.groupValues[1], m.groupValues.getOrNull(2) ?: "")
                working = working.replace(m.value, " ")
            }
        }

        // الگوهای کف قیمت: «بالای ۲ میلیون»، «بیش از ۵۰۰ هزار»
        val minPatterns = listOf(
            Regex("""بالای\s+([\d.,]+)\s*(میلیون|ملیون|هزار|تومان)?"""),
            Regex("""بیش\s+از\s+([\d.,]+)\s*(میلیون|ملیون|هزار|تومان)?"""),
            Regex("""حداقل\s+([\d.,]+)\s*(میلیون|ملیون|هزار|تومان)?""")
        )
        for (p in minPatterns) {
            p.find(working)?.let { m ->
                minPrice = toToman(m.groupValues[1], m.groupValues.getOrNull(2) ?: "")
                working = working.replace(m.value, " ")
            }
        }

        // مرتب‌سازی خواسته‌شده
        val preferCheapest = working.contains("ارزان‌ترین") || working.contains("ارزون‌ترین") ||
            working.contains("ارزانترین") || working.contains("قیمت مناسب")
        val preferExpensive = working.contains("گران‌ترین") || working.contains("گرون‌ترین")
        if (preferCheapest) {
            working = working.replace("ارزان‌ترین", " ").replace("ارزون‌ترین", " ")
                .replace("ارزانترین", " ").replace("قیمت مناسب", " ")
        }
        if (preferExpensive) {
            working = working.replace("گران‌ترین", " ").replace("گرون‌ترین", " ")
        }

        val clean = PersianNormalizer.normalize(working).trim()
        return ParsedQuery(
            cleanQuery = clean.ifBlank { PersianNormalizer.normalize(rawQuery).trim() },
            maxPriceToman = maxPrice,
            minPriceToman = minPrice,
            preferCheapest = preferCheapest,
            preferExpensive = preferExpensive
        )
    }

    private fun toToman(numberText: String, unit: String): Long? {
        val num = numberText.replace(",", "").replace(".", "").toLongOrNull() ?: return null
        return when {
            MILLION_WORDS.any { unit.contains(it) } -> num * 1_000_000L
            THOUSAND_WORDS.any { unit.contains(it) } -> num * 1_000L
            unit.contains("تومان") -> num
            // بدون واحد: حدس هوشمند — عدد کوچک یعنی میلیون (۱۰ → ۱۰ میلیون)
            num < 1000 -> num * 1_000_000L
            else -> num
        }
    }
}
