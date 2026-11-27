package ir.dakhlokharj.engine

import com.google.common.truth.Truth.assertThat
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.ForecastEngine
import ir.dakhlokharj.domain.engine.ForecastInput
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.AccountType
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.FinancialEvent
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.PlannedEventKind
import ir.dakhlokharj.domain.model.Receivable
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.RecurringPeriod
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import org.junit.Test

private val today = JalaliDate(1403, 1, 10)

private fun account(toman: Long) = Account(
    name = "main",
    type = AccountType.BANK,
    openingBalance = Money.toman(toman),
    balance = Money.toman(toman),
    colorArgb = 0xFF34D399.toInt(),
    iconKey = "bank",
)

private fun income(toman: Long, day: Int = 5) = Transaction(
    type = TransactionType.INCOME,
    amount = Money.toman(toman),
    title = "income",
    accountId = 1,
    date = JalaliDate(1403, 1, day),
)

private fun expense(toman: Long, day: Int = 6) = Transaction(
    type = TransactionType.EXPENSE,
    amount = Money.toman(toman),
    title = "expense",
    accountId = 1,
    date = JalaliDate(1403, 1, day),
)

private fun input(
    balance: Long,
    transactions: List<Transaction> = emptyList(),
    events: List<FinancialEvent> = emptyList(),
    recurring: List<RecurringExpense> = emptyList(),
    installments: List<Installment> = emptyList(),
    debts: List<Debt> = emptyList(),
    receivables: List<Receivable> = emptyList(),
) = ForecastInput(
    today = today,
    accounts = listOf(account(balance)),
    transactions = transactions,
    plannedEvents = events,
    recurring = recurring,
    installments = installments,
    debts = debts,
    receivables = receivables,
)

class ForecastEngineTest {

    @Test
    fun emptyMonthKeepsCurrentBalance() {
        val result = ForecastEngine.forecast(input(1000))
        assertThat(result.currentBalance.rial).isEqualTo(Money.toman(1000).rial)
        assertThat(result.projectedEndBalance).isEqualTo(result.currentBalance)
        assertThat(result.upcomingExpenses.rial).isEqualTo(0L)
    }

    @Test
    fun definiteIncomeRaisesProjection() {
        val event = FinancialEvent(
            kind = PlannedEventKind.INCOME,
            title = "salary",
            amount = Money.toman(500),
            date = JalaliDate(1403, 1, 20),
            definite = true,
        )
        val result = ForecastEngine.forecast(input(1000, events = listOf(event)))
        assertThat(result.definiteFutureIncome.rial).isEqualTo(Money.toman(500).rial)
        assertThat(result.projectedEndBalance.rial).isEqualTo(Money.toman(1500).rial)
    }

    @Test
    fun probableIncomeIsOnlyOptimistic() {
        val event = FinancialEvent(
            kind = PlannedEventKind.INCOME,
            title = "bonus",
            amount = Money.toman(500),
            date = JalaliDate(1403, 1, 20),
            definite = false,
        )
        val result = ForecastEngine.forecast(input(1000, events = listOf(event)))
        assertThat(result.probableFutureIncome.rial).isEqualTo(Money.toman(500).rial)
        assertThat(result.projectedEndBalance.rial).isEqualTo(Money.toman(1000).rial)
        assertThat(result.optimisticEndBalance.rial).isEqualTo(Money.toman(1500).rial)
    }

    @Test
    fun monthlyRecurringOccursOnceThisMonth() {
        val recurring = RecurringExpense(
            title = "rent",
            amount = Money.toman(300),
            accountId = 1,
            period = RecurringPeriod.MONTHLY,
            nextDueDate = JalaliDate(1403, 1, 20),
        )
        val result = ForecastEngine.forecast(input(1000, recurring = listOf(recurring)))
        assertThat(result.upcomingRecurring.rial).isEqualTo(Money.toman(300).rial)
        assertThat(result.projectedEndBalance.rial).isEqualTo(Money.toman(700).rial)
    }

    @Test
    fun overdueRecurringIsCountedOnce() {
        val recurring = RecurringExpense(
            title = "old bill",
            amount = Money.toman(100),
            accountId = 1,
            period = RecurringPeriod.MONTHLY,
            nextDueDate = JalaliDate(1403, 1, 3),
        )
        val result = ForecastEngine.forecast(input(1000, recurring = listOf(recurring)))
        assertThat(result.upcomingRecurring.rial).isEqualTo(Money.toman(100).rial)
    }

    @Test
    fun paidInstallmentIsExcluded() {
        val installment = Installment(
            planId = 1,
            index = 1,
            amount = Money.toman(200),
            dueDate = JalaliDate(1403, 1, 25),
            paidDate = JalaliDate(1403, 1, 9),
        )
        val result = ForecastEngine.forecast(input(1000, installments = listOf(installment)))
        assertThat(result.upcomingInstallments.rial).isEqualTo(0L)
    }

    @Test
    fun debtAndInstallmentReduceProjection() {
        val debt = Debt(
            creditor = "bank",
            principal = Money.toman(400),
            paid = Money.toman(100),
            dueDate = JalaliDate(1403, 1, 28),
        )
        val installment = Installment(
            planId = 1,
            index = 1,
            amount = Money.toman(200),
            dueDate = JalaliDate(1403, 1, 25),
        )
        val result = ForecastEngine.forecast(
            input(1000, installments = listOf(installment), debts = listOf(debt)),
        )
        assertThat(result.upcomingDebts.rial).isEqualTo(Money.toman(300).rial)
        assertThat(result.upcomingInstallments.rial).isEqualTo(Money.toman(200).rial)
        assertThat(result.projectedEndBalance.rial).isEqualTo(Money.toman(500).rial)
    }

    @Test
    fun receivableAddsToProbableIncome() {
        val receivable = Receivable(
            debtor = "friend",
            amount = Money.toman(350),
            received = Money.toman(50),
            expectedDate = JalaliDate(1403, 1, 18),
        )
        val result = ForecastEngine.forecast(input(1000, receivables = listOf(receivable)))
        assertThat(result.probableFutureIncome.rial).isEqualTo(Money.toman(300).rial)
        assertThat(result.projectedEndBalance.rial).isEqualTo(Money.toman(1000).rial)
        assertThat(result.optimisticEndBalance.rial).isEqualTo(Money.toman(1300).rial)
    }

    @Test
    fun freeToSpendNeverNegative() {
        val debt = Debt(
            creditor = "bank",
            principal = Money.toman(5000),
            paid = Money.ZERO,
            dueDate = JalaliDate(1403, 1, 20),
        )
        val result = ForecastEngine.forecast(input(100, debts = listOf(debt)))
        assertThat(result.freeToSpend.rial).isEqualTo(0L)
        assertThat(result.projectedEndBalance.rial).isEqualTo(Money.toman(-4900).rial)
    }

    @Test
    fun daysRemainingIncludesToday() {
        val result = ForecastEngine.forecast(input(1000))
        assertThat(result.daysRemaining).isEqualTo(22)
    }

    @Test
    fun advanceHandlesAllPeriods() {
        val date = JalaliDate(1403, 1, 20)
        assertThat(ForecastEngine.advance(date, RecurringPeriod.WEEKLY)).isEqualTo(JalaliDate(1403, 1, 27))
        assertThat(ForecastEngine.advance(date, RecurringPeriod.MONTHLY)).isEqualTo(JalaliDate(1403, 2, 20))
        assertThat(ForecastEngine.advance(date, RecurringPeriod.YEARLY)).isEqualTo(JalaliDate(1404, 1, 20))
    }

    @Test
    fun receivedIncomeAndPaidExpenseAreReported() {
        val result = ForecastEngine.forecast(input(1000, transactions = listOf(income(800), expense(300))))
        assertThat(result.receivedIncome.rial).isEqualTo(Money.toman(800).rial)
        assertThat(result.paidExpenses.rial).isEqualTo(Money.toman(300).rial)
    }
}
