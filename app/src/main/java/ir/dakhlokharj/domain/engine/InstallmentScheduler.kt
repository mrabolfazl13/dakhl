package ir.dakhlokharj.domain.engine

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money

data class ScheduledInstallment(
    val index: Int,
    val amount: Money,
    val dueDate: JalaliDate,
)

/**
 * Splits a financed amount into equal monthly instalments. Rounding residue is applied
 * to the last instalment so the sum always equals the financed amount exactly.
 */
object InstallmentScheduler {

    fun buildSchedule(
        total: Money,
        downPayment: Money,
        count: Int,
        firstDueDate: JalaliDate,
    ): List<ScheduledInstallment> {
        require(count > 0) { "تعداد اقساط باید بیشتر از صفر باشد" }
        require(downPayment.rial >= 0) { "پیش‌پرداخت نمی‌تواند منفی باشد" }
        require(downPayment.rial <= total.rial) { "پیش‌پرداخت نمی‌تواند از مبلغ کل بیشتر باشد" }
        val financed = total.rial - downPayment.rial
        val base = financed / count
        val remainder = financed - base * count
        return (1..count).map { index ->
            val amount = if (index == count) base + remainder else base
            ScheduledInstallment(
                index = index,
                amount = Money(amount),
                dueDate = firstDueDate.plusMonths(index - 1),
            )
        }
    }

    fun financedAmount(total: Money, downPayment: Money): Money =
        Money.maxOf(Money.ZERO, total - downPayment)

    fun monthlyAmount(total: Money, downPayment: Money, count: Int): Money {
        if (count <= 0) return Money.ZERO
        return Money(financedAmount(total, downPayment).rial / count)
    }
}
