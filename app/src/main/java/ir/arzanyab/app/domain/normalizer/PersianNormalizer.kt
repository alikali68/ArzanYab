package ir.arzanyab.app.domain.normalizer

object PersianNormalizer {
    private const val ARABIC_YE = 'ي'
    private const val PERSIAN_YE = 'ی'
    private const val ARABIC_KAF = 'ك'
    private const val PERSIAN_KAF = 'ک'
    private const val ZWNJ = '‌'
    private const val ZWJ = '‍'

    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                ARABIC_YE, 'ى' -> sb.append(PERSIAN_YE)
                ARABIC_KAF -> sb.append(PERSIAN_KAF)
                'ة' -> sb.append('ه')
                'ؤ' -> sb.append('و')
                'إ', 'أ', 'آ' -> sb.append('ا')
                ZWJ -> { /* حذف اتصال نامرئی */ }
                'ً', 'ٌ', 'ٍ', 'َ', 'ُ', 'ِ', 'ّ', 'ْ' -> { /* حذف اعراب */ }
                'ـ' -> { /* حذف کشیدگی */ }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
            .replace(ZWNJ, ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun toEnglishDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                in '۰'..'۹' -> sb.append((ch - '۰').digitToChar())
                in '٠'..'٩' -> sb.append((ch - '٠').digitToChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
