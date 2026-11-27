package ir.dakhlokharj.core.money

/**
 * Canonical money representation for the whole app: a Long count of Rial minor units.
 * 1 Toman == 10 Rial. No floating point arithmetic is ever used for money.
 */
@JvmInline
value class Money(val rial: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(rial + other.rial)

    operator fun minus(other: Money): Money = Money(rial - other.rial)

    operator fun unaryMinus(): Money = Money(-rial)

    operator fun times(factor: Long): Money = Money(rial * factor)

    override fun compareTo(other: Money): Int = rial.compareTo(other.rial)

    val isNegative: Boolean get() = rial < 0
    val isPositive: Boolean get() = rial > 0
    val isZero: Boolean get() = rial == 0L

    fun toTomanRounded(): Long = rial / 10

    companion object {
        val ZERO: Money = Money(0)
        fun toman(value: Long): Money = Money(value * 10)
        fun rial(value: Long): Money = Money(value)
        fun minOf(a: Money, b: Money): Money = if (a <= b) a else b
        fun maxOf(a: Money, b: Money): Money = if (a >= b) a else b
        fun sum(values: Iterable<Money>): Money = Money(values.sumOf { it.rial })
    }
}
