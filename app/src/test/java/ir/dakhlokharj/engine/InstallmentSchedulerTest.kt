package ir.dakhlokharj.engine

import com.google.common.truth.Truth.assertThat
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.InstallmentScheduler
import org.junit.Assert.assertThrows
import org.junit.Test

class InstallmentSchedulerTest {

    private val first = JalaliDate(1403, 1, 15)

    @Test
    fun scheduleSumEqualsFinancedAmount() {
        val schedule = InstallmentScheduler.buildSchedule(
            total = Money.toman(10_000),
            downPayment = Money.toman(1_000),
            count = 7,
            firstDueDate = first,
        )
        assertThat(schedule.sumOf { it.amount.rial }).isEqualTo(Money.toman(9_000).rial)
    }

    @Test
    fun remainderGoesToLastInstallment() {
        val schedule = InstallmentScheduler.buildSchedule(
            total = Money.rial(100),
            downPayment = Money.ZERO,
            count = 3,
            firstDueDate = first,
        )
        assertThat(schedule[0].amount.rial).isEqualTo(33L)
        assertThat(schedule[2].amount.rial).isEqualTo(34L)
    }

    @Test
    fun dueDatesAdvanceMonthly() {
        val schedule = InstallmentScheduler.buildSchedule(
            total = Money.toman(300),
            downPayment = Money.ZERO,
            count = 3,
            firstDueDate = first,
        )
        assertThat(schedule.map { it.dueDate }).containsExactly(
            JalaliDate(1403, 1, 15),
            JalaliDate(1403, 2, 15),
            JalaliDate(1403, 3, 15),
        ).inOrder()
    }

    @Test
    fun indexesStartAtOne() {
        val schedule = InstallmentScheduler.buildSchedule(
            total = Money.toman(100),
            downPayment = Money.ZERO,
            count = 4,
            firstDueDate = first,
        )
        assertThat(schedule.map { it.index }).containsExactly(1, 2, 3, 4).inOrder()
    }

    @Test
    fun zeroCountIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            InstallmentScheduler.buildSchedule(Money.toman(100), Money.ZERO, 0, first)
        }
    }

    @Test
    fun downPaymentAboveTotalIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            InstallmentScheduler.buildSchedule(Money.toman(100), Money.toman(200), 2, first)
        }
    }

    @Test
    fun fullDownPaymentProducesZeroInstallments() {
        val schedule = InstallmentScheduler.buildSchedule(
            total = Money.toman(100),
            downPayment = Money.toman(100),
            count = 3,
            firstDueDate = first,
        )
        assertThat(schedule.all { it.amount.rial == 0L }).isTrue()
    }

    @Test
    fun monthlyAmountUsesIntegerDivision() {
        val monthly = InstallmentScheduler.monthlyAmount(Money.toman(1000), Money.toman(100), 3)
        assertThat(monthly.rial).isEqualTo(Money.rial(9000).rial / 3)
    }

    @Test
    fun monthlyAmountWithZeroCountIsZero() {
        assertThat(InstallmentScheduler.monthlyAmount(Money.toman(100), Money.ZERO, 0)).isEqualTo(Money.ZERO)
    }
}
