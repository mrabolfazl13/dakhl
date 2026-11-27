package ir.dakhlokharj.domain.engine

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.FinancialEvent
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.PlannedEventKind
import ir.dakhlokharj.domain.model.Receivable
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.RecurringPeriod
import ir.dakhlokharj.domain.model.SavingsGoal
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType

enum class ForecastLineKind { INCOME, RECURRING, INSTALLMENT, DEBT, PLANNED_EXPENSE, RECEIVABLE }

data class ForecastLine(
    val title: String,
    val amount: Money,
    val date: JalaliDate?,
    val kind: ForecastLineKind,
)

data class ForecastInput(
    val today: JalaliDate,
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val plannedEvents: List<FinancialEvent> = emptyList(),
    val recurring: List<RecurringExpense> = emptyList(),
    val installments: List<Installment> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val receivables: List<Receivable> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
)

data class ForecastResult(
    val currentBalance: Money,
    val receivedIncome: Money,
    val paidExpenses: Money,
    val definiteFutureIncome: Money,
    val probableFutureIncome: Money,
    val upcomingRecurring: Money,
    val upcomingInstallments: Money,
    val upcomingDebts: Money,
    val upcomingPlannedExpenses: Money,
    val upcomingExpenses: Money,
    val projectedEndBalance: Money,
    val optimisticEndBalance: Money,
    val freeToSpend: Money,
    val remainingCommitments: Money,
    val budgetTotal: Money,
    val budgetSpent: Money,
    val budgetRemaining: Money,
    val savingsTotal: Money,
    val daysRemaining: Int,
    val lines: List<ForecastLine>,
)

/**
 * Deterministic month-end forecast. Only received/paid transactions and definitively
 * planned items affect the base projection; probable income is reported separately.
 */
object ForecastEngine {

    fun forecast(input: ForecastInput): ForecastResult {
        val today = input.today
        val monthStart = today.startOfMonth
        val monthEnd = today.endOfMonth

        val monthTransactions = input.transactions.filter {
            it.date >= monthStart && it.date <= monthEnd &&
                (it.type == TransactionType.INCOME || it.type == TransactionType.EXPENSE)
        }
        val receivedIncome = Money.sum(
            monthTransactions.filter { it.type == TransactionType.INCOME }.map { it.amount },
        )
        val paidExpenses = Money.sum(
            monthTransactions.filter { it.type == TransactionType.EXPENSE }.map { it.amount },
        )
        val currentBalance = Money.sum(
            input.accounts.filter { !it.archived }.map { it.balance },
        )

        val lines = mutableListOf<ForecastLine>()
        var definiteIncome = Money.ZERO
        var probableIncome = Money.ZERO
        var plannedExpenses = Money.ZERO

        input.plannedEvents.forEach { event ->
            if (event.date < today || event.date > monthEnd) return@forEach
            when (event.kind) {
                PlannedEventKind.INCOME -> if (event.definite) {
                    definiteIncome += event.amount
                    lines += ForecastLine(event.title, event.amount, event.date, ForecastLineKind.INCOME)
                } else {
                    probableIncome += event.amount
                    lines += ForecastLine(event.title, event.amount, event.date, ForecastLineKind.RECEIVABLE)
                }
                PlannedEventKind.EXPENSE -> {
                    plannedExpenses += event.amount
                    lines += ForecastLine(event.title, event.amount, event.date, ForecastLineKind.PLANNED_EXPENSE)
                }
            }
        }

        var recurringTotal = Money.ZERO
        input.recurring.filter { it.active }.forEach { item ->
            var cursor = item.nextDueDate
            if (cursor < today) {
                recurringTotal += item.amount
                lines += ForecastLine(item.title, item.amount, cursor, ForecastLineKind.RECURRING)
                cursor = advance(cursor, item.period)
            }
            var guard = 0
            while (cursor <= monthEnd && guard < 40) {
                recurringTotal += item.amount
                lines += ForecastLine(item.title, item.amount, cursor, ForecastLineKind.RECURRING)
                cursor = advance(cursor, item.period)
                guard++
            }
        }

        var installmentTotal = Money.ZERO
        input.installments.filter { !it.isPaid && it.dueDate <= monthEnd }.forEach { installment ->
            installmentTotal += installment.amount
            lines += ForecastLine("قسط شماره ${installment.index}", installment.amount, installment.dueDate, ForecastLineKind.INSTALLMENT)
        }

        var debtTotal = Money.ZERO
        input.debts.filter { it.remaining.isPositive && it.dueDate <= monthEnd }.forEach { debt ->
            debtTotal += debt.remaining
            lines += ForecastLine("بدهی به ${debt.creditor}", debt.remaining, debt.dueDate, ForecastLineKind.DEBT)
        }

        input.receivables.filter {
            it.remaining.isPositive && it.expectedDate >= today && it.expectedDate <= monthEnd
        }.forEach { receivable ->
            probableIncome += receivable.remaining
            lines += ForecastLine("طلب از ${receivable.debtor}", receivable.remaining, receivable.expectedDate, ForecastLineKind.RECEIVABLE)
        }

        val upcomingExpenses = recurringTotal + installmentTotal + debtTotal + plannedExpenses
        val projectedEnd = currentBalance + definiteIncome - upcomingExpenses
        val optimisticEnd = projectedEnd + probableIncome

        val activeBudgets = input.budgets.filter { !it.endDate.isBefore(today) && !it.startDate.isAfter(monthEnd) }
        val budgetTotal = Money.sum(activeBudgets.map { it.amount })
        val budgetSpent = Money.sum(
            activeBudgets.map { BudgetEngine.progress(it, input.transactions, today).spent },
        )
        val budgetRemaining = Money.maxOf(Money.ZERO, budgetTotal - budgetSpent)

        val savingsTotal = Money.sum(input.goals.map { it.saved })

        return ForecastResult(
            currentBalance = currentBalance,
            receivedIncome = receivedIncome,
            paidExpenses = paidExpenses,
            definiteFutureIncome = definiteIncome,
            probableFutureIncome = probableIncome,
            upcomingRecurring = recurringTotal,
            upcomingInstallments = installmentTotal,
            upcomingDebts = debtTotal,
            upcomingPlannedExpenses = plannedExpenses,
            upcomingExpenses = upcomingExpenses,
            projectedEndBalance = projectedEnd,
            optimisticEndBalance = optimisticEnd,
            freeToSpend = Money.maxOf(Money.ZERO, projectedEnd),
            remainingCommitments = upcomingExpenses,
            budgetTotal = budgetTotal,
            budgetSpent = budgetSpent,
            budgetRemaining = budgetRemaining,
            savingsTotal = savingsTotal,
            daysRemaining = (monthEnd.day - today.day + 1).coerceAtLeast(1),
            lines = lines.sortedWith(compareBy({ it.date?.toEpochDay() ?: Long.MAX_VALUE }, { it.title })),
        )
    }

    fun advance(date: JalaliDate, period: RecurringPeriod): JalaliDate = when (period) {
        RecurringPeriod.WEEKLY -> date.plusDays(7)
        RecurringPeriod.MONTHLY -> date.plusMonths(1)
        RecurringPeriod.YEARLY -> date.plusMonths(12)
    }
}
