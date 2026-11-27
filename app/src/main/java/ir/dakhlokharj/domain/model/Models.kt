package ir.dakhlokharj.domain.model

import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.core.date.JalaliDate

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val openingBalance: Money,
    val balance: Money,
    val colorArgb: Int,
    val iconKey: String,
    val archived: Boolean = false,
    val createdAt: Long = 0,
    val sortOrder: Int = 0,
)

data class Category(
    val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val iconKey: String,
    val colorArgb: Int,
    val essential: Boolean = true,
    val system: Boolean = false,
    val sortOrder: Int = 0,
)

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Money,
    val title: String,
    val categoryId: Long? = null,
    val accountId: Long,
    val targetAccountId: Long? = null,
    val date: JalaliDate,
    val timeMinutes: Int = 0,
    val note: String = "",
    val receiptUri: String? = null,
    val tag: String? = null,
    val recurringId: Long? = null,
    val debtId: Long? = null,
    val receivableId: Long? = null,
    val installmentId: Long? = null,
    val savingsGoalId: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class Budget(
    val id: Long = 0,
    val name: String,
    val scope: BudgetScope,
    val categoryId: Long? = null,
    val period: BudgetPeriod,
    val startDate: JalaliDate,
    val endDate: JalaliDate,
    val amount: Money,
    val alertAt50: Boolean = true,
    val alertAt75: Boolean = true,
    val alertAt90: Boolean = true,
    val alertAt100: Boolean = true,
    val repeatNextMonth: Boolean = false,
    val createdAt: Long = 0,
)

data class Envelope(
    val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val allocated: Money,
    val sortOrder: Int = 0,
)

data class Debt(
    val id: Long = 0,
    val creditor: String,
    val principal: Money,
    val paid: Money,
    val dueDate: JalaliDate,
    val installmentCount: Int = 1,
    val note: String = "",
    val createdAt: Long = 0,
) {
    val remaining: Money get() = Money.maxOf(Money.ZERO, principal - paid)

    fun status(today: JalaliDate): DebtStatus = when {
        remaining.isZero -> DebtStatus.SETTLED
        today > dueDate -> DebtStatus.OVERDUE
        paid.isPositive -> DebtStatus.PARTIAL
        else -> DebtStatus.UNPAID
    }
}

data class DebtPayment(
    val id: Long = 0,
    val debtId: Long,
    val amount: Money,
    val date: JalaliDate,
    val transactionId: Long? = null,
    val createdAt: Long = 0,
)

data class Receivable(
    val id: Long = 0,
    val debtor: String,
    val amount: Money,
    val received: Money,
    val expectedDate: JalaliDate,
    val note: String = "",
    val createdAt: Long = 0,
) {
    val remaining: Money get() = Money.maxOf(Money.ZERO, amount - received)

    fun status(today: JalaliDate): DebtStatus = when {
        remaining.isZero -> DebtStatus.SETTLED
        today > expectedDate -> DebtStatus.OVERDUE
        received.isPositive -> DebtStatus.PARTIAL
        else -> DebtStatus.UNPAID
    }
}

data class ReceivablePayment(
    val id: Long = 0,
    val receivableId: Long,
    val amount: Money,
    val date: JalaliDate,
    val transactionId: Long? = null,
    val createdAt: Long = 0,
)

data class InstallmentPlan(
    val id: Long = 0,
    val title: String,
    val total: Money,
    val downPayment: Money,
    val installmentCount: Int,
    val installmentAmount: Money,
    val firstDueDate: JalaliDate,
    val accountId: Long,
    val categoryId: Long? = null,
    val note: String = "",
    val createdAt: Long = 0,
)

data class Installment(
    val id: Long = 0,
    val planId: Long,
    val index: Int,
    val amount: Money,
    val dueDate: JalaliDate,
    val paidDate: JalaliDate? = null,
    val transactionId: Long? = null,
) {
    val isPaid: Boolean get() = paidDate != null
}

data class RecurringExpense(
    val id: Long = 0,
    val title: String,
    val amount: Money,
    val categoryId: Long? = null,
    val accountId: Long,
    val period: RecurringPeriod,
    val nextDueDate: JalaliDate,
    val kind: RecurringKind = RecurringKind.BILL,
    val active: Boolean = true,
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 2,
    val note: String = "",
    val lastPaidDate: JalaliDate? = null,
    val createdAt: Long = 0,
)

data class SavingsGoal(
    val id: Long = 0,
    val title: String,
    val target: Money,
    val saved: Money,
    val targetDate: JalaliDate? = null,
    val colorArgb: Int = 0xFF34D399.toInt(),
    val iconKey: String = "goal",
    val note: String = "",
    val achieved: Boolean = false,
    val createdAt: Long = 0,
) {
    val progress: Float
        get() = if (target.rial <= 0) 0f else (saved.rial.toDouble() / target.rial).toFloat().coerceIn(0f, 1f)

    val remaining: Money get() = Money.maxOf(Money.ZERO, target - saved)
}

data class SavingsContribution(
    val id: Long = 0,
    val goalId: Long,
    val amount: Money,
    val date: JalaliDate,
    val transactionId: Long? = null,
    val createdAt: Long = 0,
)

data class FinancialEvent(
    val id: Long = 0,
    val kind: PlannedEventKind,
    val title: String,
    val amount: Money,
    val date: JalaliDate,
    val definite: Boolean = true,
    val note: String = "",
    val createdAt: Long = 0,
)

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyUnit: CurrencyUnit = CurrencyUnit.TOMAN,
    val hideBalances: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinHash: String? = null,
    val screenshotProtection: Boolean = true,
    val reminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 21,
    val dailyReminderMinute: Int = 0,
    val defaultAccountId: Long? = null,
    val dashboardOrder: String = "hero,cashflow,forecast,health,transactions,budgets",
    val onboardingDone: Boolean = false,
    val languageTag: String = "fa",
)

data class BackupMetadata(
    val id: Long = 0,
    val fileName: String,
    val locationUri: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val schemaVersion: Int,
    val encrypted: Boolean,
)
