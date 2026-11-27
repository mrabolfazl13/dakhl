package ir.dakhlokharj.core.format

import java.util.Locale

object PersianText {

    private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"

    /** Converts every ASCII digit in [input] to its Persian counterpart. */
    fun toPersianDigits(input: String): String {
        val builder = StringBuilder(input.length)
        for (ch in input) {
            builder.append(if (ch in '0'..'9') PERSIAN_DIGITS[ch - '0'] else ch)
        }
        return builder.toString()
    }

    /** Normalizes Persian/Arabic digits and separators back to ASCII digits. */
    fun toLatinDigits(input: String): String {
        val builder = StringBuilder(input.length)
        for (ch in input) {
            val index = PERSIAN_DIGITS.indexOf(ch).takeIf { it >= 0 } ?: ARABIC_DIGITS.indexOf(ch)
            when {
                index >= 0 -> builder.append(index)
                ch == '٬' || ch == ',' || ch == '،' || ch == ' ' || ch == '\u200f' || ch == '\u200e' -> Unit
                ch == '٫' -> builder.append('.')
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    /** Groups [value] with thousands separators, e.g. 12500000 -> "12,500,000". */
    fun grouped(value: Long): String = String.format(Locale.US, "%,d", value)

    fun normalizePersian(input: String): String = input
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace('\u200c', ' ')
        .trim()
}
