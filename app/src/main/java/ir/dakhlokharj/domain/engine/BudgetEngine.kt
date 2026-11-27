package ir.dakhlokharj.domain.engine

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.BudgetScope
import ir.dakhlokharj.domain.model.BudgetStatus
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType

data class BudgetProgress(
    val budget: Budget,
    val spent: Money,
    val remaining: Money,
    val percent: Int,
    val projectedSpend: Money,
    val projectedPercent: Int,
    val status: BudgetStatus,
    val daysTotal: Int,
    val daysElapsed: Int,
)

object BudgetEngine {

    fun progress(
        budget: Budget,
        transactions: List<Transaction>,
        today: JalaliDate,
    ): BudgetProgress {
        val spent = Money.sum(
            transactions.filter { transaction ->
                transaction.type == TransactionType.EXPENSE &&
                    !transaction.date.isBefore(budget.startDate) &&
                    !transaction.date.isAfter(budget.endDate) &&
                    (budget.scope == BudgetScope.OVERALL ||
                        (budget.categoryId != null && transaction.categoryId == budget.categoryId))
            }.map { it.amount },
        )
        val total = budget.amount.rial
        val percent = if (total <= 0) 0 else ((spent.rial * 100) / total).toInt().coerceIn(0, 999)
        val remaining = Money.maxOf(Money.ZERO, budget.amount - spent)
        val daysTotal = (budget.endDate.toEpochDay() - budget.startDate.toEpochDay() + 1).toInt().coerceAtLeast(1)
        val elapsedRaw = (today.toEpochDay() - budget.startDate.toEpochDay() + 1).toInt()
        val daysElapsed = elapsedRaw.coerceIn(1, daysTotal)
        val projected = if (today > budget.endDate) spent else Money(spent.rial * daysTotal / daysElapsed)
        val projectedPercent = if (total <= 0) 0 else ((projected.rial * 100) / total).toInt().coerceIn(0, 999)
        return BudgetProgress(
            budget = budget,
            spent = spent,
            remaining = remaining,
            percent = percent,
            projectedSpend = projected,
            projectedPercent = projectedPercent,
            status = statusFor(percent),
            daysTotal = daysTotal,
            daysElapsed = daysElapsed,
        )
    }

    fun statusFor(percent: Int): BudgetStatus = when {
        percent >= 100 -> BudgetStatus.EXCEEDED
        percent >= 90 -> BudgetStatus.DANGER
        percent >= 75 -> BudgetStatus.WARNING
        else -> BudgetStatus.SAFE
    }

    fun overallPercent(budgets: List<Budget>, transactions: List<Transaction>, today: JalaliDate): Int {
        if (budgets.isEmpty()) return 0
        val total = Money.sum(budgets.map { it.amount }).rial
        if (total <= 0) return 0
        val counted = transactions.filter { transaction ->
            transaction.type == TransactionType.EXPENSE &&
                budgets.any { budget ->
                    !transaction.date.isBefore(budget.startDate) &&
                        !transaction.date.isAfter(budget.endDate) &&
                        (budget.scope == BudgetScope.OVERALL ||
                            (budget.categoryId != null && transaction.categoryId == budget.categoryId))
                }
        }
        val spent = Money.sum(counted.map { it.amount }).rial
        return ((spent * 100) / total).toInt().coerceIn(0, 999)
    }
}