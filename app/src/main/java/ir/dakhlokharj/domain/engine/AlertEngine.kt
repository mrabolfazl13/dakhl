package ir.dakhlokharj.domain.engine

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.InsightTone
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType

data class FinancialAlert(
    val title: String,
    val message: String,
    val tone: InsightTone,
    val iconKey: String = "alert",
    val priority: Int = 0,
)

/** Builds preventive alerts from real data. Pure function, safe to unit test. */
object AlertEngine {

    fun generate(
        today: JalaliDate,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        installments: List<Installment>,
        debts: List<Debt>,
        recurring: List<RecurringExpense>,
        categories: List<Category>,
        forecast: ForecastResult? = null,
    ): List<FinancialAlert> {
        val alerts = mutableListOf<FinancialAlert>()
        val soon = today.plusDays(5)

        val unpaidInstallments = installments.filter { it.paidDate == null && it.dueDate <= soon }
        if (unpaidInstallments.isNotEmpty()) {
            val overdue = unpaidInstallments.filter { it.dueDate < today }
            alerts += FinancialAlert(
                title = if (overdue.isNotEmpty()) "قسط معوق دارید" else "قسط نزدیک سررسید",
                message = if (overdue.isNotEmpty()) {
                    "${overdue.size} قسط از تاریخ سررسید گذشته است. برای جلوگیری از جریمه، پرداخت را انجام دهید."
                } else {
                    "${unpaidInstallments.size} قسط تا پنج روز آینده سررسید می‌شود."
                },
                tone = if (overdue.isNotEmpty()) InsightTone.NEGATIVE else InsightTone.NEUTRAL,
                iconKey = "installment",
                priority = if (overdue.isNotEmpty()) 100 else 80,
            )
        }

        val dueDebts = debts.filter { it.remaining.isPositive && it.dueDate != null && it.dueDate <= soon }
        if (dueDebts.isNotEmpty()) {
            alerts += FinancialAlert(
                title = "بدهی نزدیک سررسید",
                message = "${dueDebts.size} بدهی در پنج روز آینده سررسید می‌شود؛ مجموع مانده ${Money.sum(dueDebts.map { it.remaining }).let { format(it) }}.",
                tone = InsightTone.NEGATIVE,
                iconKey = "debt",
                priority = 90,
            )
        }

        budgets.filter { it.endDate >= today }.forEach { budget ->
            val progress = BudgetEngine.progress(budget, transactions, today)
            when {
                progress.percent >= 100 -> alerts += FinancialAlert(
                    title = "بودجه ${budget.name} تمام شد",
                    message = "بیش از سقف تعیین‌شده خرج کرده‌اید. تا پایان دوره هزینه‌های این دسته را متوقف کنید.",
                    tone = InsightTone.NEGATIVE,
                    iconKey = "budget",
                    priority = 95,
                )
                progress.percent >= 85 -> alerts += FinancialAlert(
                    title = "بودجه ${budget.name} در آستانه اتمام",
                    message = "${progress.percent}٪ از بودجه مصرف شده و فقط ${format(progress.remaining)} باقی مانده است.",
                    tone = InsightTone.NEUTRAL,
                    iconKey = "budget",
                    priority = 70,
                )
            }
        }

        recurring.filter { it.active && it.nextDueDate <= soon }.forEach { item ->
            alerts += FinancialAlert(
                title = "تمدید ${item.title}",
                message = "این هزینه در ${item.nextDueDate.formatPersianDigits()} سررسید می‌شود.",
                tone = InsightTone.NEUTRAL,
                iconKey = item.iconKey,
                priority = 60,
            )
        }

        val thisMonthStart = today.startOfMonth()
        val lastMonthStart = thisMonthStart.plusMonths(-1)
        val lastMonthEnd = thisMonthStart.plusDays(-1)
        val thisExpense = sumExpenses(transactions, thisMonthStart, today)
        val lastExpense = sumExpenses(transactions, lastMonthStart, lastMonthEnd)
        if (lastExpense.rial > 0 && thisExpense.rial > lastExpense.rial * 1.3 && thisExpense.rial - lastExpense.rial > 100_000) {
            val growth = ((thisExpense.rial - lastExpense.rial) * 100.0 / lastExpense.rial).toInt()
            alerts += FinancialAlert(
                title = "رشد سریع هزینه‌ها",
                message = "هزینه‌های این ماه ${growth}٪ بیشتر از مدت مشابه ماه گذشته است.",
                tone = InsightTone.NEGATIVE,
                iconKey = "trend",
                priority = 75,
            )
        }

        if (forecast != null && forecast.projectedEndBalance.isNegative) {
            alerts += FinancialAlert(
                title = "هشدار نقدینگی",
                message = "با تعهدات فعلی، موجودی پایان ماه به ${format(forecast.projectedEndBalance)} می‌رسد. پیش از خرید بزرگ، برنامه را بازبینی کنید.",
                tone = InsightTone.NEGATIVE,
                iconKey = "forecast",
                priority = 110,
            )
        }

        val topCategory = ReportEngine.categoryTotals(
            transactions = transactions.filter { it.type == TransactionType.EXPENSE },
            categories = categories,
            from = thisMonthStart,
            to = today,
        ).firstOrNull()
        if (topCategory != null && topCategory.share > 0.45f && topCategory.amount.rial > 1_000_000) {
            alerts += FinancialAlert(
                title = "تمرکز هزینه در ${topCategory.name}",
                message = "${(topCategory.share * 100).toInt()}٪ هزینه‌های این ماه در این دسته بوده است.",
                tone = InsightTone.NEUTRAL,
                iconKey = "category",
                priority = 50,
            )
        }

        return alerts.sortedByDescending { it.priority }.distinctBy { it.title }.take(6)
    }

    private fun sumExpenses(transactions: List<Transaction>, from: JalaliDate, to: JalaliDate): Money =
        Money.sum(
            transactions.filter {
                it.type == TransactionType.EXPENSE && it.date >= from && it.date <= to
            }.map { it.amount },
        )

    private fun format(money: Money): String =
        ir.dakhlokharj.core.format.MoneyFormatter.format(money, includeSymbol = true)
}
