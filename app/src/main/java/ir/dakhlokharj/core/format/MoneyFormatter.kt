package ir.dakhlokharj.core.format

import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.CurrencyUnit
import java.util.Locale

object MoneyFormatter {

    fun format(
        amount: Money,
        unit: CurrencyUnit = CurrencyUnit.TOMAN,
        includeSymbol: Boolean = true,
        persianDigits: Boolean = true,
        withSign: Boolean = false,
    ): String {
        val value = when (unit) {
            CurrencyUnit.TOMAN -> amount.toTomanRounded()
            CurrencyUnit.RIAL -> amount.rial
        }
        val sign = if (withSign && value > 0) "+" else ""
        val grouped = sign + PersianText.grouped(value)
        val text = if (includeSymbol) "$grouped ${unit.symbol}" else grouped
        return if (persianDigits) PersianText.toPersianDigits(text) else text
    }

    fun formatCompact(amount: Money, unit: CurrencyUnit = CurrencyUnit.TOMAN): String {
        val value = when (unit) {
            CurrencyUnit.TOMAN -> amount.toTomanRounded()
            CurrencyUnit.RIAL -> amount.rial
        }
        val absolute = kotlin.math.abs(value)
        val text = when {
            absolute >= 1_000_000_000L -> "${formatDecimal(value / 1_000_000_000.0)} میلیارد"
            absolute >= 1_000_000L -> "${formatDecimal(value / 1_000_000.0)} میلیون"
            absolute >= 1_000L -> "${formatDecimal(value / 1_000.0)} هزار"
            else -> PersianText.grouped(value)
        }
        return PersianText.toPersianDigits("$text ${unit.symbol}")
    }

    private fun formatDecimal(value: Double): String {
        val rounded = Math.round(value * 10.0) / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
    }

    /** Parses a user-entered amount in the given unit and returns canonical Rial money, or null. */
    fun parse(input: String, unit: CurrencyUnit): Money? {
        val normalized = PersianText.toLatinDigits(input)
            .replace(" ", "")
            .replace(",", "")
            .replace("٬", "")
        if (normalized.isBlank()) return null
        val value = normalized.toLongOrNull() ?: return null
        if (value < 0) return null
        return when (unit) {
            CurrencyUnit.TOMAN -> Money.toman(value)
            CurrencyUnit.RIAL -> Money.rial(value)
        }
    }
}
