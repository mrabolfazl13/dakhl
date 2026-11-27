package ir.dakhlokharj.ui

import ir.dakhlokharj.domain.model.AccountType
import ir.dakhlokharj.domain.model.BudgetPeriod
import ir.dakhlokharj.domain.model.BudgetScope
import ir.dakhlokharj.domain.model.BudgetStatus
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.domain.model.CurrencyUnit
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.DebtStatus
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.domain.model.PlannedEventKind
import ir.dakhlokharj.domain.model.RecurringKind
import ir.dakhlokharj.domain.model.RecurringPeriod
import ir.dakhlokharj.domain.model.ThemeMode
import ir.dakhlokharj.domain.model.TransactionType

fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.CASH -> "نقدی"
    AccountType.BANK -> "حساب بانکی"
    AccountType.CREDIT_CARD -> "کارت اعتباری"
    AccountType.DIGITAL_WALLET -> "کیف پول دیجیتال"
    AccountType.SAVINGS -> "پس‌انداز"
    AccountType.INVESTMENT -> "سرمایه‌گذاری"
}

fun transactionTypeLabel(type: TransactionType): String = when (type) {
    TransactionType.INCOME -> "درآمد"
    TransactionType.EXPENSE -> "هزینه"
    TransactionType.TRANSFER -> "انتقال"
    TransactionType.ADJUSTMENT -> "تعدیل"
    TransactionType.SAVINGS_DEPOSIT -> "واریز به پس‌انداز"
    TransactionType.SAVINGS_WITHDRAW -> "برداشت از پس‌انداز"
}

fun categoryKindLabel(kind: CategoryKind): String = when (kind) {
    CategoryKind.INCOME -> "درآمد"
    CategoryKind.EXPENSE -> "هزینه"
}

fun budgetPeriodLabel(period: BudgetPeriod): String = when (period) {
    BudgetPeriod.MONTHLY -> "ماهانه"
    BudgetPeriod.WEEKLY -> "هفتگی"
    BudgetPeriod.YEARLY -> "سالانه"
    BudgetPeriod.CUSTOM -> "سفارشی"
}

fun budgetScopeLabel(scope: BudgetScope): String = when (scope) {
    BudgetScope.OVERALL -> "کلی"
    BudgetScope.CATEGORY -> "دسته‌بندی"
    BudgetScope.ACCOUNT -> "حساب"
}

fun budgetStatusLabel(status: BudgetStatus): String = when (status) {
    BudgetStatus.HEALTHY -> "سالم"
    BudgetStatus.WARNING -> "هشدار"
    BudgetStatus.CRITICAL -> "بحرانی"
}

fun debtStatusLabel(status: DebtStatus): String = when (status) {
    DebtStatus.ACTIVE -> "جاری"
    DebtStatus.DUE -> "نزدیک سررسید"
    DebtStatus.PAID -> "پرداخت‌شده"
    DebtStatus.OVERDUE -> "معوق"
}

fun recurringPeriodLabel(period: RecurringPeriod): String = when (period) {
    RecurringPeriod.DAILY -> "روزانه"
    RecurringPeriod.WEEKLY -> "هفتگی"
    RecurringPeriod.MONTHLY -> "ماهانه"
    RecurringPeriod.QUARTERLY -> "فصلی"
    RecurringPeriod.YEARLY -> "سالانه"
}

fun recurringKindLabel(kind: RecurringKind): String = when (kind) {
    RecurringKind.BILL -> "قبض"
    RecurringKind.SUBSCRIPTION -> "اشتراک"
    RecurringKind.INSTALLMENT -> "قسط"
    RecurringKind.OTHER -> "سایر"
}

fun plannedEventKindLabel(kind: PlannedEventKind): String = when (kind) {
    PlannedEventKind.INCOME -> "درآمد برنامه‌ریزی‌شده"
    PlannedEventKind.EXPENSE -> "هزینه برنامه‌ریزی‌شده"
    PlannedEventKind.INVESTMENT -> "سرمایه‌گذاری"
}

fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "هماهنگ با سیستم"
    ThemeMode.LIGHT -> "روشن"
    ThemeMode.DARK -> "تاریک"
}

fun currencyUnitLabel(unit: CurrencyUnit): String = when (unit) {
    CurrencyUnit.TOMAN -> "تومان"
    CurrencyUnit.RIAL -> "ریال"
}

fun debtStatusOf(debt: Debt, today: JalaliDate = JalaliDate.today()): DebtStatus = when {
    !debt.remaining.isPositive -> DebtStatus.PAID
    debt.dueDate < today -> DebtStatus.OVERDUE
    debt.dueDate <= today.plusDays(7) -> DebtStatus.DUE
    else -> DebtStatus.ACTIVE
}
