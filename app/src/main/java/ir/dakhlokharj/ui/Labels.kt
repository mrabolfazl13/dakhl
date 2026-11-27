package ir.dakhlokharj.ui

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.domain.model.AccountType
import ir.dakhlokharj.domain.model.BudgetPeriod
import ir.dakhlokharj.domain.model.BudgetScope
import ir.dakhlokharj.domain.model.BudgetStatus
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.domain.model.CurrencyUnit
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.DebtStatus
import ir.dakhlokharj.domain.model.PlannedEventKind
import ir.dakhlokharj.domain.model.RecurringKind
import ir.dakhlokharj.domain.model.RecurringPeriod
import ir.dakhlokharj.domain.model.ThemeMode
import ir.dakhlokharj.domain.model.TransactionType

fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.CASH -> "نقدی"
    AccountType.BANK -> "حساب بانکی"
    AccountType.DIGITAL_WALLET -> "کیف پول دیجیتال"
    AccountType.SAVINGS -> "پس‌انداز"
    AccountType.CUSTOM -> "حساب اختصاصی"
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
    BudgetPeriod.CUSTOM -> "بازه اختصاصی"
}

fun budgetScopeLabel(scope: BudgetScope): String = when (scope) {
    BudgetScope.OVERALL -> "کلی"
    BudgetScope.CATEGORY -> "دسته‌بندی"
}

fun budgetStatusLabel(status: BudgetStatus): String = when (status) {
    BudgetStatus.SAFE -> "در محدوده"
    BudgetStatus.WARNING -> "نزدیک سقف"
    BudgetStatus.DANGER -> "هشدار"
    BudgetStatus.EXCEEDED -> "عبور از بودجه"
}

fun debtStatusLabel(status: DebtStatus): String = when (status) {
    DebtStatus.UNPAID -> "پرداخت‌نشده"
    DebtStatus.PARTIAL -> "پرداخت جزئی"
    DebtStatus.SETTLED -> "تسویه‌شده"
    DebtStatus.OVERDUE -> "معوق"
}

fun recurringPeriodLabel(period: RecurringPeriod): String = when (period) {
    RecurringPeriod.WEEKLY -> "هفتگی"
    RecurringPeriod.MONTHLY -> "ماهانه"
    RecurringPeriod.YEARLY -> "سالانه"
}

fun recurringKindLabel(kind: RecurringKind): String = when (kind) {
    RecurringKind.BILL -> "قبض و هزینه ثابت"
    RecurringKind.SUBSCRIPTION -> "اشتراک"
}

fun plannedEventKindLabel(kind: PlannedEventKind): String = when (kind) {
    PlannedEventKind.INCOME -> "درآمد برنامه‌ریزی‌شده"
    PlannedEventKind.EXPENSE -> "هزینه برنامه‌ریزی‌شده"
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

fun debtStatusOf(debt: Debt, today: JalaliDate = JalaliDate.today()): DebtStatus = debt.status(today)
