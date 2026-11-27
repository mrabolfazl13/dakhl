package ir.dakhlokharj.domain.model

enum class AccountType(val label: String) {
    BANK("حساب بانکی"),
    CASH("پول نقد"),
    DIGITAL_WALLET("کیف پول دیجیتال"),
    SAVINGS("حساب پس‌انداز"),
    CUSTOM("حساب اختصاصی"),
}

enum class TransactionType(val label: String) {
    INCOME("درآمد"),
    EXPENSE("هزینه"),
    TRANSFER("انتقال"),
    ADJUSTMENT("اصلاح موجودی"),
    SAVINGS_DEPOSIT("واریز به پس‌انداز"),
    SAVINGS_WITHDRAW("برداشت از پس‌انداز"),
}

enum class CategoryKind(val label: String) {
    INCOME("درآمد"),
    EXPENSE("هزینه"),
}

enum class BudgetPeriod(val label: String) {
    MONTHLY("ماهانه"),
    WEEKLY("هفتگی"),
    CUSTOM("بازه اختصاصی"),
}

enum class BudgetScope(val label: String) {
    OVERALL("کلی"),
    CATEGORY("دسته‌بندی"),
}

enum class DebtStatus(val label: String) {
    UNPAID("پرداخت‌نشده"),
    PARTIAL("پرداخت جزئی"),
    SETTLED("تسویه‌شده"),
    OVERDUE("عقب‌افتاده"),
}

enum class RecurringPeriod(val label: String) {
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه"),
    YEARLY("سالانه"),
}

enum class RecurringKind(val label: String) {
    BILL("قبض و هزینه ثابت"),
    SUBSCRIPTION("اشتراک"),
}

enum class PlannedEventKind(val label: String) {
    INCOME("درآمد برنامه‌ریزی‌شده"),
    EXPENSE("هزینه برنامه‌ریزی‌شده"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("هماهنگ با سیستم"),
    LIGHT("روشن"),
    DARK("تاریک"),
}

enum class CurrencyUnit(val label: String, val symbol: String) {
    TOMAN("تومان", "تومان"),
    RIAL("ریال", "ریال"),
}

enum class InsightTone { POSITIVE, NEUTRAL, NEGATIVE }

enum class BudgetStatus(val label: String) {
    SAFE("در محدوده"),
    WARNING("نزدیک سقف"),
    DANGER("هشدار"),
    EXCEEDED("عبور از بودجه"),
}