package ir.dakhlokharj.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.di.AppContainer
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.DebtPayment
import ir.dakhlokharj.domain.model.Envelope
import ir.dakhlokharj.domain.model.FinancialEvent
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.InstallmentPlan
import ir.dakhlokharj.domain.model.Receivable
import ir.dakhlokharj.domain.model.ReceivablePayment
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.SavingsContribution
import ir.dakhlokharj.domain.model.SavingsGoal
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PlanningViewModel(private val container: AppContainer) : ViewModel() {

    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.asState(): StateFlow<List<T>> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> = container.accountRepository.observeActive().asState()
    val categories: StateFlow<List<Category>> = container.categoryRepository.observeAll().asState()
    val budgets: StateFlow<List<Budget>> = container.budgetRepository.observeAll().asState()
    val envelopes: StateFlow<List<Envelope>> = container.budgetRepository.observeEnvelopes().asState()
    val debts: StateFlow<List<Debt>> = container.debtRepository.observeDebts().asState()
    val receivables: StateFlow<List<Receivable>> = container.debtRepository.observeReceivables().asState()
    val debtPayments: StateFlow<List<DebtPayment>> = container.debtRepository.observeAllDebtPayments().asState()
    val receivablePayments: StateFlow<List<ReceivablePayment>> =
        container.debtRepository.observeReceivablePayments().asState()
    val plans: StateFlow<List<InstallmentPlan>> = container.installmentRepository.observePlans().asState()
    val installments: StateFlow<List<Installment>> = container.installmentRepository.observeAll().asState()
    val recurring: StateFlow<List<RecurringExpense>> = container.recurringRepository.observeAll().asState()
    val goals: StateFlow<List<SavingsGoal>> = container.savingsRepository.observeGoals().asState()
    val contributions: StateFlow<List<SavingsContribution>> =
        container.savingsRepository.observeAllContributions().asState()
    val events: StateFlow<List<FinancialEvent>> = container.eventRepository.observeAll().asState()
    val transactions: StateFlow<List<Transaction>> = container.transactionRepository.observeAll().asState()
    val settings: StateFlow<UserSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    suspend fun saveAccount(account: Account, editing: Boolean): Result<Unit> = runCatching {
        if (editing) container.accountRepository.update(account) else container.accountRepository.create(account)
        Unit
    }

    suspend fun archiveAccount(account: Account, archived: Boolean): Result<Unit> = runCatching {
        container.accountRepository.archive(account, archived)
    }

    suspend fun saveBudget(budget: Budget, editing: Boolean): Result<Unit> = runCatching {
        if (editing) container.budgetRepository.update(budget) else container.budgetRepository.add(budget)
        Unit
    }

    suspend fun deleteBudget(budget: Budget): Result<Unit> = runCatching {
        container.budgetRepository.delete(budget)
    }

    suspend fun repeatBudgetNextMonth(budget: Budget): Result<Unit> = runCatching {
        container.budgetRepository.repeatNextMonth(budget)
        Unit
    }

    suspend fun saveEnvelope(envelope: Envelope, editing: Boolean): Result<Unit> = runCatching {
        if (editing) container.budgetRepository.updateEnvelope(envelope)
        else container.budgetRepository.addEnvelope(envelope)
        Unit
    }

    suspend fun deleteEnvelope(envelope: Envelope): Result<Unit> = runCatching {
        container.budgetRepository.deleteEnvelope(envelope)
    }

    suspend fun saveDebt(debt: Debt, editing: Boolean): Result<Unit> = runCatching {
        if (editing) container.debtRepository.updateDebt(debt) else container.debtRepository.addDebt(debt)
        Unit
    }

    suspend fun deleteDebt(debt: Debt): Result<Unit> = runCatching {
        container.debtRepository.deleteDebt(debt)
    }

    suspend fun payDebt(debtId: Long, amountToman: Long, date: JalaliDate, accountId: Long): Result<Unit> = runCatching {
        container.debtRepository.payDebt(debtId, Money.toman(amountToman), date, accountId)
        Unit
    }

    suspend fun saveReceivable(receivable: Receivable, editing: Boolean): Result<Unit> = runCatching {
        if (editing) container.debtRepository.updateReceivable(receivable)
        else container.debtRepository.addReceivable(receivable)
        Unit
    }

    suspend fun deleteReceivable(receivable: Receivable): Result<Unit> = runCatching {
        container.debtRepository.deleteReceivable(receivable)
    }

    suspend fun receiveReceivable(
        receivableId: Long,
        amountToman: Long,
        date: JalaliDate,
        accountId: Long,
    ): Result<Unit> = runCatching {
        container.debtRepository.receiveReceivable(receivableId, Money.toman(amountToman), date, accountId)
        Unit
    }

    suspend fun createInstallmentPlan(
        title: String,
        totalToman: Long,
        downPaymentToman: Long,
        count: Int,
        firstDueDate: JalaliDate,
        accountId: Long,
        categoryId: Long?,
        note: String,
    ): Result<Unit> = runCatching {
        require(title.isNotBlank()) { "عنوان طرح را وارد کنید" }
        require(count in 1..120) { "تعداد اقساط باید بین ۱ تا ۱۲۰ باشد" }
        container.installmentRepository.createPlan(
            InstallmentPlan(
                title = title,
                total = Money.toman(totalToman),
                downPayment = Money.toman(downPaymentToman),
                installmentCount = count,
                installmentAmount = Money.ZERO,
                firstDueDate = firstDueDate,
                accountId = accountId,
                categoryId = categoryId,
                note = note,
            ),
        )
        Unit
    }

    suspend fun payInstallment(installmentId: Long, accountId: Long, date: JalaliDate): Result<Unit> = runCatching {
        container.installmentRepository.payInstallment(installmentId, accountId, date)
        Unit
    }

    suspend fun deletePlan(plan: InstallmentPlan): Result<Unit> = runCatching {
        container.installmentRepository.deletePlan(plan)
    }

    suspend fun saveRecurring(recurring: RecurringExpense, editing: Boolean): Result<Unit> = runCatching {
        if (editing) container.recurringRepository.update(recurring)
        else container.recurringRepository.add(recurring)
        Unit
    }

    suspend fun deleteRecurring(recurring: RecurringExpense): Result<Unit> = runCatching {
        container.recurringRepository.delete(recurring)
    }

    suspend fun toggleRecurring(recurring: RecurringExpense, active: Boolean): Result<Unit> = runCatching {
        container.recurringRepository.setActive(recurring, active)
    }

    suspend fun payRecurring(recurring: RecurringExpense, accountId: Long, date: JalaliDate): Result<Unit> = runCatching {
        container.recurringRepository.pay(recurring.id, accountId, date)
        Unit
    }

    suspend fun saveGoal(goal: SavingsGoal, editing: Boolean): Result<Unit> = runCatching {
        require(goal.title.isNotBlank()) { "عنوان هدف را وارد کنید" }
        if (editing) container.savingsRepository.updateGoal(goal) else container.savingsRepository.addGoal(goal)
        Unit
    }

    suspend fun deleteGoal(goal: SavingsGoal): Result<Unit> = runCatching {
        container.savingsRepository.deleteGoal(goal)
    }

    suspend fun contribute(
        goalId: Long,
        amountToman: Long,
        date: JalaliDate,
        accountId: Long?,
    ): Result<Unit> = runCatching {
        container.savingsRepository.contribute(goalId, Money.toman(amountToman), date, accountId)
        Unit
    }

    suspend fun withdraw(
        goalId: Long,
        amountToman: Long,
        date: JalaliDate,
        accountId: Long?,
    ): Result<Unit> = runCatching {
        container.savingsRepository.withdraw(goalId, Money.toman(amountToman), date, accountId)
        Unit
    }

    suspend fun saveEvent(event: FinancialEvent, editing: Boolean): Result<Unit> = runCatching {
        require(event.title.isNotBlank()) { "عنوان رویداد را وارد کنید" }
        if (editing) container.eventRepository.update(event) else container.eventRepository.add(event)
        Unit
    }

    suspend fun deleteEvent(event: FinancialEvent): Result<Unit> = runCatching {
        container.eventRepository.delete(event)
    }
}
