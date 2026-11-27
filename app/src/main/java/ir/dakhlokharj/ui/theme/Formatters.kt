package ir.dakhlokharj.ui.theme

import androidx.compose.runtime.Composable
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.format.MoneyFormatter
import ir.dakhlokharj.core.format.PersianText
import ir.dakhlokharj.core.money.Money
import java.util.Locale

@Composable
fun moneyText(amount: Money, signed: Boolean = false, includeSymbol: Boolean = true): String {
    if (LocalHideBalances.current) return "••••••"
    return MoneyFormatter.format(
        amount = amount,
        unit = LocalCurrencyUnit.current,
        includeSymbol = includeSymbol,
        withSign = signed,
    )
}

@Composable
fun moneyShort(amount: Money): String {
    if (LocalHideBalances.current) return "••••"
    return MoneyFormatter.formatCompact(amount, LocalCurrencyUnit.current)
}

fun fa(value: Long): String = PersianText.toPersianDigits(PersianText.grouped(value))

fun fa(value: Int): String = PersianText.toPersianDigits(value.toString())

fun faPercent(percent: Int): String = PersianText.toPersianDigits("$percent٪")

fun faDate(date: JalaliDate, style: DateStyle = DateStyle.LONG): String = when (style) {
    DateStyle.SHORT -> PersianText.toPersianDigits(date.formatShort())
    DateStyle.LONG -> date.formatPersianDigits()
    DateStyle.FULL -> PersianText.toPersianDigits(date.formatFull())
}

fun faTime(minutes: Int): String {
    val hour = (minutes / 60).coerceIn(0, 23).toString().padStart(2, '0')
    val minute = (minutes % 60).coerceIn(0, 59).toString().padStart(2, '0')
    return PersianText.toPersianDigits("$hour:$minute")
}

enum class DateStyle { SHORT, LONG, FULL }

fun String.groupedDigits(): String {
    val latin = PersianText.toLatinDigits(this).filter { it.isDigit() }
    if (latin.isEmpty()) return this
    return PersianText.toPersianDigits(String.format(Locale.US, "%,d", latin.toLongOrNull() ?: 0L))
}
