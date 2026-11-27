package ir.dakhlokharj.engine

import com.google.common.truth.Truth.assertThat
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.ForecastResult
import ir.dakhlokharj.domain.engine.PurchaseScenario
import ir.dakhlokharj.domain.engine.WhatIfEngine
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.BudgetPeriod
import ir.dakhlokharj.domain.model.BudgetScope
import ir.dakhlokharj.domain.model.BudgetStatus
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import org.junit.Test

private val today = JalaliDate(1403, 1, 10)

private fun base(balance: Long, projected: Long) = ForecastResult(
    currentBalance = Money.toman(balance),
    receivedIncome = Money.ZERO,
    paidExpenses = Money.ZERO,
    definiteFutureIncome = Money.ZERO,
    probableFutureIncome = Money.ZERO,
    upcomingRecurring = Money.ZERO,
    upcomingInstallments = Money.ZERO,
    upcomingDebts = Money.ZERO,
    upcomingPlannedExpenses = Money.ZERO,
    upcomingExpenses = Money.ZERO,
    projectedEndBalance = Money.toman(projected),
    optimisticEndBalance = Money.toman(projected),
    freeToSpend = Money.toman(if (projected > 0) projected else 0),
    remainingCommitments = Money.ZERO,
    budgetTotal = Money.ZERO,
    budgetSpent = Money.ZERO,
    budgetRemaining = Money.ZERO,
    savingsTotal = Money.ZERO,
    daysRemaining = 20,
    lines = emptyList(),
)

private fun cashScenario(toman: Long) = PurchaseScenario(
    title = "laptop",
    price = Money.toman(toman),
    cash = true,
)

class WhatIfEngineTest {

    @Test
    fun cashPurchaseDeductsFullPriceToday() {
        val result = WhatIfEngine.simulate(base(5000, 4000), cashScenario(1000), today)
        assertThat(result.upfrontCost.rial).isEqualTo(Money.toman(1000).rial)
        assertThat(result.monthlyInstallment.rial).isEqualTo(0L)
        assertThat(result.balanceAfterPurchase.rial).isEqualTo(Money.toman(4000).rial)
        assertThat(result.projectedEndAfterPurchase.rial).isEqualTo(Money.toman(3000).rial)
    }

    @Test
    fun cashPurchaseTotalCommitmentEqualsPrice() {
        val result = WhatIfEngine.simulate(base(5000, 4000), cashScenario(1000), today)
        assertThat(result.totalCommitment.rial).isEqualTo(Money.toman(1000).rial)
    }

    @Test
    fun purchaseAboveBalanceIsNotAffordable() {
        val result = WhatIfEngine.simulate(base(500, 400), cashScenario(1000), today)
        assertThat(result.isAffordable).isFalse()
        assertThat(result.projectedEndAfterPurchase.isNegative).isTrue()
    }

    @Test
    fun installmentPurchaseOnlyChargesDownPaymentToday() {
        val scenario = PurchaseScenario(
            title = "phone",
            price = Money.toman(1000),
            cash = false,
            downPayment = Money.toman(200),
            installmentCount = 4,
        )
        val result = WhatIfEngine.simulate(base(5000, 4000), scenario, today)
        assertThat(result.upfrontCost.rial).isEqualTo(Money.toman(200).rial)
        assertThat(result.monthlyInstallment.rial).isEqualTo(Money.toman(200).rial)
        assertThat(result.balanceAfterPurchase.rial).isEqualTo(Money.toman(4800).rial)
        assertThat(result.totalCommitment.rial).isEqualTo(Money.toman(1000).rial)
    }

    @Test
    fun savingsDelayUsesCeilingDivision() {
        val scenario = PurchaseScenario(
            title = "phone",
            price = Money.toman(1000),
            cash = false,
            downPayment = Money.toman(200),
            installmentCount = 4,
        )
        val result = WhatIfEngine.simulate(
            base = base(5000, 4000),
            scenario = scenario,
            today = today,
            monthlySavings = Money.toman(300),
        )
        assertThat(result.savingsMonthsDelayed).isEqualTo(3)
    }

    @Test
    fun budgetImpactShowsBeforeAndAfterPercent() {
        val budget = Budget(
            name = "monthly",
            scope = BudgetScope.OVERALL,
            period = BudgetPeriod.MONTHLY,
            startDate = today.startOfMonth,
            endDate = today.endOfMonth,
            amount = Money.toman(1000),
        )
        val spent = Transaction(
            type = TransactionType.EXPENSE,
            amount = Money.toman(300),
            title = "spent",
            accountId = 1,
            date = today,
        )
        val result = WhatIfEngine.simulate(
            base = base(5000, 4000),
            scenario = cashScenario(200),
            today = today,
            budgets = listOf(budget),
            transactions = listOf(spent),
        )
        assertThat(result.budgetImpacts).hasSize(1)
        assertThat(result.budgetImpacts.first().beforePercent).isEqualTo(30)
        assertThat(result.budgetImpacts.first().afterPercent).isEqualTo(50)
        assertThat(result.budgetImpacts.first().afterStatus).isEqualTo(BudgetStatus.SAFE)
    }

    @Test
    fun projectionsCoverInstallmentMonths() {
        val scenario = PurchaseScenario(
            title = "car",
            price = Money.toman(1000),
            cash = false,
            downPayment = Money.toman(100),
            installmentCount = 4,
        )
        val result = WhatIfEngine.simulate(base(5000, 4000), scenario, today, projectionMonths = 2)
        assertThat(result.projections).hasSize(5)
        assertThat(result.projections.first().month).isEqualTo(today.startOfMonth)
    }

    @Test
    fun freeToSpendChangeComparesWithBase() {
        val result = WhatIfEngine.simulate(base(5000, 4000), cashScenario(1000), today)
        assertThat(result.freeToSpendChange.rial).isEqualTo(Money.toman(-1000).rial)
    }
}
