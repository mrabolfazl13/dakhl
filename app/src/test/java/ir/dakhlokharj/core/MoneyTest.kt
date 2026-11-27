package ir.dakhlokharj.core

import com.google.common.truth.Truth.assertThat
import ir.dakhlokharj.core.money.Money
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun tomanConvertsToRial() {
        assertThat(Money.toman(12).rial).isEqualTo(120L)
    }

    @Test
    fun plusAndMinusAreExact() {
        val a = Money.toman(10)
        val b = Money.toman(3)
        assertThat((a + b).rial).isEqualTo(130L)
        assertThat((a - b).rial).isEqualTo(70L)
    }

    @Test
    fun unaryMinusFlipsSign() {
        assertThat((-Money.toman(5)).rial).isEqualTo(-50L)
    }

    @Test
    fun sumOfEmptyListIsZero() {
        assertThat(Money.sum(emptyList<Money>())).isEqualTo(Money.ZERO)
    }

    @Test
    fun sumAddsAllValues() {
        val total = Money.sum(listOf(Money.toman(1), Money.toman(2), Money.toman(3)))
        assertThat(total.rial).isEqualTo(60L)
    }

    @Test
    fun minAndMaxCompareValues() {
        val small = Money.toman(1)
        val large = Money.toman(9)
        assertThat(Money.minOf(small, large)).isEqualTo(small)
        assertThat(Money.maxOf(small, large)).isEqualTo(large)
    }

    @Test
    fun comparisonIsNumeric() {
        assertThat(Money.toman(2) > Money.toman(1)).isTrue()
        assertThat(Money.toman(1) <= Money.toman(1)).isTrue()
    }

    @Test
    fun roundingToTomanTruncatesRial() {
        assertThat(Money.rial(129).toTomanRounded()).isEqualTo(12L)
    }

    @Test
    fun multiplicationKeepsLongPrecision() {
        assertThat((Money.toman(3) * 4).rial).isEqualTo(120L)
    }

    @Test
    fun signFlagsAreConsistent() {
        assertThat(Money.rial(-1).isNegative).isTrue()
        assertThat(Money.ZERO.isZero).isTrue()
        assertThat(Money.rial(1).isPositive).isTrue()
    }

    @Test
    fun negativeRialCanBeRepresented() {
        assertThat(Money.rial(-500).rial).isEqualTo(-500L)
    }
}
