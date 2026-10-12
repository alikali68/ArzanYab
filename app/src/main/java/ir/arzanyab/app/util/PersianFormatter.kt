package ir.arzanyab.app.util

import java.text.DecimalFormat

object PersianFormatter {
    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private const val PERSIAN_THOUSANDS_SEPARATOR = '٬'

    fun toPersianDigits(input: String): String {
        val builder = StringBuilder(input.length)
        for (char in input) {
            when (char) {
                in '0'..'9' -> builder.append(PERSIAN_DIGITS[char - '0'])
                ',' -> builder.append(PERSIAN_THOUSANDS_SEPARATOR)
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    fun toPersianDigits(number: Long): String = toPersianDigits(number.toString())

    fun formatPrice(amount: Long): String {
        val formatter = DecimalFormat("#,###")
        return "${toPersianDigits(formatter.format(amount))} تومان"
    }
}
