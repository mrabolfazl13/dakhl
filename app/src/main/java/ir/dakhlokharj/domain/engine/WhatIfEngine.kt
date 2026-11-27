package ir.dakhlokharj.domain.engine

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.BudgetScope
import ir.dakhlokharj.domain.model.BudgetStatus

data class PurchaseScenario(
    val title: String,
    val price: Money,
    val categoryId: Long? = null,
    val cash: Boolean = true,
    val downPayment: Money = Money.ZERO,
    val installmentCount: Int = 0,
)

data class BudgetImpact(
    val budgetName: String,
    val amount: Money,
    val beforePercent: Int,
    val afterPercent: Int,
    val afterStatus: BudgetStatus,
)

data class ProjectionPoint(
    val month: JalaliDate,
    val balance: Money,
)

data class WhatIfResult(
    val scenario: PurchaseScenario,
    val base: ForecastResult,
    val upfrontCost: Money,
    val monthlyInstallment: Money,
    val balanceAfterPurchase: Money,
    val projectedEndAfterPurchase: Money,
    val freeToSpendAfterPurchase: Money,
    val freeToSpendChange: Money,
    val totalCommitment: Money,
    val budgetImpacts: List<BudgetImpact>,
    val savingsMonthsDelayed: Int,
    val projections: List<ProjectionPoint>,
    val isAffordable: Boolean,
)

/**
 * Simulates a purchase without writing any transaction. Cash purchases reduce the balance
 * immediately; instalment purchases spread the cost over future months and only the down
 * payment affects the current balance.
 */
object WhatIfEngine {

    fun simulate(
        base: ForecastResult,
        scenario: PurchaseScenario,
        today: JalaliDate,
        budgets: List<ir.dakhlokharj.domain.model.Budget> = emptyList(),
        transactions: List<ir.dakhlokharj.domain.model.Transaction> = emptyList(),
        monthlySavings: Money = Money.ZERO,
        projectionMonths: Int = 6,
    ): WhatIfResult {
        val count = scenario.installmentCount.coerceAtLeast(0)
        val downPayment = if (scenario.cash) scenario.price else Money.minOf(scenario.downPayment, scenario.price)
        val financed = Money.maxOf(Money.ZERO, scenario.price - downPayment)
        val monthly = if (!scenario.cash && count > 0) Money(financed.rial / count) else Money.ZERO
        val upfront = downPayment

        val balanceAfter = base.currentBalance - upfront
        val projectedAfter = base.projectedEndBalance - upfront
        val freeAfter = Money.maxOf(Money.ZERO, projectedAfter)
        val freeChange = freeAfter - base.freeToSpend

        val matchingBudgets = budgets.filter { budget ->
            !budget.startDate.isAfter(today.endOfMonth) && !budget.endDate.isBefore(today.startOfMonth) &&
                (budget.scope == BudgetScope.OVERALL ||
                    (scenario.categoryId != null && budget.categoryId == scenario.categoryId))
        }
        val impacts = matchingBudgets.map { budget ->
            val progress = BudgetEngine.progress(budget, transactions, today)
            val afterSpent = progress.spent + upfront
            val percent = if (budget.amount.rial <= 0) 0
            else ((afterSpent.rial * 100) / budget.amount.rial).toInt().coerceIn(0, 999)
            BudgetImpact(
                budgetName = budget.name,
                amount = afterSpent,
                beforePercent = progress.percent,
                afterPercent = percent,
                afterStatus = BudgetEngine.statusFor(percent),
            )
        }

        val monthsDelayed = if (monthlySavings.isPositive && financed.isPositive) {
            Math.ceil(financed.rial.toDouble() / monthlySavings.rial).toInt()
        } else 0

        val projections = (0..projectionMonths.coerceAtLeast(count)).map { offset ->
            val date = today.startOfMonth.plusMonths(offset)
            val paidSoFar = if (scenario.cash) upfront.rial
            else upfront.rial + monthly.rial * offset.coerceAtMost(count)
            ProjectionPoint(date, base.projectedEndBalance - Money(paidSoFar))
        }.distinctBy { it.month.toEpochDay() }

        return WhatIfResult(
            scenario = scenario,
            base = base,
            upfrontCost = upfront,
            monthlyInstallment = monthly,
            balanceAfterPurchase = balanceAfter,
            projectedEndAfterPurchase = projectedAfter,
            freeToSpendAfterPurchase = freeAfter,
            freeToSpendChange = freeChange,
            totalCommitment = if (scenario.cash) scenario.price else downPayment + monthly * count.toLong(),
            budgetImpacts = impacts,
            savingsMonthsDelayed = monthsDelayed,
            projections = projections,
            isAffordable = projectedAfter.isPositive,
        )
    }
}