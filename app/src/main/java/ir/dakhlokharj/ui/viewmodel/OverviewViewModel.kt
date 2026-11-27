package ir.dakhlokharj.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.di.AppContainer
import ir.dakhlokharj.domain.engine.AlertEngine
import ir.dakhlokharj.domain.engine.BudgetEngine
import ir.dakhlokharj.domain.engine.BudgetProgress
import ir.dakhlokharj.domain.engine.CalendarEngine
import ir.dakhlokharj.domain.engine.CalendarInput
import ir.dakhlokharj.domain.engine.CalendarMonth
import ir.dakhlokharj.domain.engine.CategoryTotal
import ir.dakhlokharj.domain.engine.FinancialAlert
import ir.dakhlokharj.domain.engine.ForecastEngine
import ir.dakhlokharj.domain.engine.ForecastInput
import ir.dakhlokharj.domain.engine.ForecastResult
import ir.dakhlokharj.domain.engine.HealthEngine
import ir.dakhlokharj.domain.engine.HealthInput
import ir.dakhlokharj.domain.engine.HealthResult
import ir.dakhlokharj.domain.engine.Insight
import ir.dakhlokharj.domain.engine.InsightsEngine
import ir.dakhlokharj.domain.engine.InsightsInput
import ir.dakhlokharj.domain.engine.MonthlyTotal
import ir.dakhlokharj.domain.engine.ReportEngine
import ir.dakhlokharj.domain.engine.WeekdayTotal
import ir.dakhlokharj.domain.engine.WhatIfEngine
import ir.dakhlokharj.domain.engine.WhatIfResult
import ir.dakhlokharj.domain.engine.PurchaseScenario
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.FinancialEvent
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.InstallmentPlan
import ir.dakhlokharj.domain.model.Receivable
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.SavingsGoal
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import ir.dakhlokharj.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OverviewUiState(
    val ready: Boolean = false,
    val today: JalaliDate = JalaliDate.today(),
    val settings: UserSettings = UserSettings(),
    val accounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val budgetProgresses: List<BudgetProgress> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val receivables: List<Receivable> = emptyList(),
    val installments: List<Installment> = emptyList(),
    val plans: List<InstallmentPlan> = emptyList(),
    val recurring: List<RecurringExpense> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val plannedEvents: List<FinancialEvent> = emptyList(),
    val forecast: ForecastResult? = null,
    val health: HealthResult? = null,
    val insights: List<Insight> = emptyList(),
    val alerts: List<FinancialAlert> = emptyList(),
    val monthlyTotals: List<MonthlyTotal> = emptyList(),
    val expenseSlices: List<CategoryTotal> = emptyList(),
    val incomeSlices: List<CategoryTotal> = emptyList(),
    val weekdayTotals: List<WeekdayTotal> = emptyList(),
    val calendarMonth: CalendarMonth? = null,
    val whatIf: WhatIfResult? = null,
) {
    val totalBalance: Money get() = Money.sum(accounts.map { it.balance })
    val recentTransactions: List<Transaction>
        get() = transactions.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt }).take(5)
    val monthIncome: Money
        get() = Money.sum(
            transactions.filter {
                it.type == TransactionType.INCOME && it.date >= today.startOfMonth && it.date <= today
            }.map { it.amount },
        )
    val monthExpense: Money
        get() = Money.sum(
            transactions.filter {
                it.type == TransactionType.EXPENSE && it.date >= today.startOfMonth && it.date <= today
            }.map { it.amount },
        )
    val monthSavings: Money get() = monthIncome - monthExpense
    val categoryById: Map<Long, Category> get() = categories.associateBy { it.id }
    val accountById: Map<Long, Account> get() = accounts.associateBy { it.id }
    val planById: Map<Long, InstallmentPlan> get() = plans.associateBy { it.id }
}

class OverviewViewModel(private val container: AppContainer) : ViewModel() {

    private val today = JalaliDate.today()
    private val calendarCursor = MutableStateFlow(today.startOfMonth)
    private val whatIfResult = MutableStateFlow<WhatIfResult?>(null)

    private data class Base(
        val accounts: List<Account>,
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val budgets: List<Budget>,
        val debts: List<Debt>,
    )

    private data class Planning(
        val receivables: List<Receivable>,
        val installments: List<Installment>,
        val plans: List<InstallmentPlan>,
        val recurring: List<RecurringExpense>,
        val goals: List<SavingsGoal>,
    )

    private data class Extra(
        val events: List<FinancialEvent>,
        val settings: UserSettings,
    )

    private val baseFlow = combine(
        container.accountRepository.observeActive(),
        container.transactionRepository.observeAll(),
        container.categoryRepository.observeAll(),
        container.budgetRepository.observeAll(),
        container.debtRepository.observeDebts(),
    ) { accounts, transactions, categories, budgets, debts ->
        Base(accounts, transactions, categories, budgets, debts)
    }

    private val planningFlow = combine(
        container.debtRepository.observeReceivables(),
        container.installmentRepository.observeAll(),
        container.installmentRepository.observePlans(),
        container.recurringRepository.observeAll(),
        container.savingsRepository.observeGoals(),
    ) { receivables, installments, plans, recurring, goals ->
        Planning(receivables, installments, plans, recurring, goals)
    }

    private val extraFlow = combine(
        container.eventRepository.observeAll(),
        container.settingsRepository.settings,
    ) { events, settings -> Extra(events, settings) }

    val uiState: StateFlow<OverviewUiState> = combine(
        baseFlow,
        planningFlow,
        extraFlow,
        calendarCursor,
        whatIfResult,
    ) { base, planning, extra, month, whatIf ->
        buildState(base, planning, extra, month, whatIf)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    private fun buildState(
        base: Base,
        planning: Planning,
        extra: Extra,
        month: JalaliDate,
        whatIf: WhatIfResult?,
    ): OverviewUiState {
        val forecast = ForecastEngine.forecast(
            ForecastInput(
                today = today,
                accounts = base.accounts,
                transactions = base.transactions,
                plannedEvents = extra.events,
                recurring = planning.recurring,
                installments = planning.installments,
                debts = base.debts,
                receivables = planning.receivables,
                budgets = base.budgets,
                goals = planning.goals,
            ),
        )
        val health = HealthEngine.calculate(
            HealthInput(
                today = today,
                transactions = base.transactions,
                budgets = base.budgets,
                debts = base.debts,
                accounts = base.accounts,
                goals = planning.goals,
                recurring = planning.recurring,
                installments = planning.installments,
            ),
        )
        val insights = InsightsEngine.generate(
            InsightsInput(
                today = today,
                transactions = base.transactions,
                categories = base.categories,
                recurring = planning.recurring,
                budgets = base.budgets,
            ),
        )
        val progresses = base.budgets
            .filter { today >= it.startDate && today <= it.endDate }
            .map { BudgetEngine.progress(it, base.transactions, today) }
            .sortedByDescending { it.percent }
        val alerts = AlertEngine.generate(
            today = today,
            transactions = base.transactions,
            budgets = base.budgets,
            installments = planning.installments,
            debts = base.debts,
            recurring = planning.recurring,
            categories = base.categories,
            forecast = forecast,
        )
        val monthStart = today.startOfMonth
        val expenseSlices = ReportEngine.categoryTotals(
            transactions = base.transactions.filter { it.type == TransactionType.EXPENSE },
            categories = base.categories,
            kind = ir.dakhlokharj.domain.model.CategoryKind.EXPENSE,
            from = monthStart,
            to = today,
        )
        val incomeSlices = ReportEngine.categoryTotals(
            transactions = base.transactions.filter { it.type == TransactionType.INCOME },
            categories = base.categories,
            kind = ir.dakhlokharj.domain.model.CategoryKind.INCOME,
            from = monthStart,
            to = today,
        )
        val calendar = CalendarEngine.buildMonth(
            CalendarInput(
                today = today,
                transactions = base.transactions,
                installments = planning.installments,
                debts = base.debts,
                receivables = planning.receivables,
                recurring = planning.recurring,
                planned = extra.events,
            ),
            month.year,
            month.month,
        )
        return OverviewUiState(
            ready = true,
            today = today,
            settings = extra.settings,
            accounts = base.accounts,
            transactions = base.transactions,
            categories = base.categories,
            budgets = base.budgets,
            budgetProgresses = progresses,
            debts = base.debts,
            receivables = planning.receivables,
            installments = planning.installments,
            plans = planning.plans,
            recurring = planning.recurring,
            goals = planning.goals,
            plannedEvents = extra.events,
            forecast = forecast,
            health = health,
            insights = insights,
            alerts = alerts,
            monthlyTotals = ReportEngine.monthlyTotals(base.transactions, 6, today),
            expenseSlices = expenseSlices,
            incomeSlices = incomeSlices,
            weekdayTotals = ReportEngine.weekdayTotals(
                base.transactions.filter { it.type == TransactionType.EXPENSE },
                monthStart,
                today,
            ),
            calendarMonth = calendar,
            whatIf = whatIf,
        )
    }

    fun nextMonth() {
        calendarCursor.value = calendarCursor.value.plusMonths(1)
    }

    fun previousMonth() {
        calendarCursor.value = calendarCursor.value.plusMonths(-1)
    }

    fun goToCurrentMonth() {
        calendarCursor.value = today.startOfMonth
    }

    fun setHideBalances(hidden: Boolean) {
        viewModelScope.launch { container.settingsRepository.setHideBalances(hidden) }
    }

    fun simulate(
        title: String,
        priceToman: Long,
        payCash: Boolean,
        downPaymentToman: Long,
        installmentCount: Int,
    ) {
        val state = uiState.value
        val forecast = state.forecast ?: return
        val scenario = PurchaseScenario(
            title = title.ifBlank { "خرید" },
            price = Money.toman(priceToman),
            cash = payCash,
            downPayment = Money.toman(downPaymentToman),
            installmentCount = if (payCash) 0 else installmentCount,
        )
        whatIfResult.value = WhatIfEngine.simulate(
            base = forecast,
            scenario = scenario,
            today = today,
            budgets = state.budgets,
            transactions = state.transactions,
            monthlySavings = averageMonthlySavings(state.transactions),
            projectionMonths = 12,
        )
    }

    fun clearSimulation() {
        whatIfResult.value = null
    }

    private fun averageMonthlySavings(transactions: List<Transaction>): Money {
        val months = (0..2).map { today.startOfMonth.plusMonths(-it) }
        val totals = months.map { start ->
            val end = start.endOfMonth
            val income = transactions.filter {
                it.type == TransactionType.INCOME && it.date >= start && it.date <= end
            }.sumOf { it.amount.rial }
            val expense = transactions.filter {
                it.type == TransactionType.EXPENSE && it.date >= start && it.date <= end
            }.sumOf { it.amount.rial }
            income - expense
        }
        return Money(totals.sumOf { it } / months.size)
    }
}