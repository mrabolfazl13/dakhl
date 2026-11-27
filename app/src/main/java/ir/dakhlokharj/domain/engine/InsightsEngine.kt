package ir.dakhlokharj.domain.engine

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.format.MoneyFormatter
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.domain.model.InsightTone
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.RecurringPeriod
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType

data class Insight(
    val title: String,
    val description: String,
    val tone: InsightTone,
    val amount: Money? = null,
    val iconKey: String = "insight",
)

data class InsightsInput(
    val today: JalaliDate,
    val transactions: List<Transaction>,
    val categories: List<Category> = emptyList(),
    val recurring: List<RecurringExpense> = emptyList(),
    val budgets: List<Budget> = emptyList(),
)

object InsightsEngine {

    fun generate(input: InsightsInput): List<Insight> {
        val today = input.today
        val currentStart = today.startOfMonth
        val currentEnd = today.endOfMonth
        val previousStart = currentStart.plusMonths(-1)
        val previousEnd = previousStart.endOfMonth

        val categoryNames = input.categories.associateBy { it.id }
        val expenses = input.transactions.filter { it.type == TransactionType.EXPENSE }
        val currentExpenses = expenses.filter { !it.date.isBefore(currentStart) && !it.date.isAfter(currentEnd) }
        val previousExpenses = expenses.filter { !it.date.isBefore(previousStart) && !it.date.isAfter(previousEnd) }
        val income = Money.sum(
            input.transactions.filter {
                it.type == TransactionType.INCOME && !it.date.isBefore(currentStart) && !it.date.isAfter(currentEnd)
            }.map { it.amount },
        )

        val insights = mutableListOf<Insight>()

        // Top expense category this month
        val grouped = currentExpenses.groupBy { it.categoryId }
            .mapValues { entry -> Money.sum(entry.value.map { it.amount }) }
            .toList()
            .sortedByDescending { it.second.rial }
        grouped.firstOrNull()?.let { (categoryId, amount) ->
            val name = categoryId?.let { categoryNames[it]?.name } ?: "بدون دسته‌بندی"
            val share = if (Money.sum(currentExpenses.map { it.amount }).isPositive) {
                amount.rial.toDouble() / Money.sum(currentExpenses.map { it.amount }).rial
            } else 0.0
            insights += Insight(
                title = "بیشترین هزینه ماه",
                description = "بیشترین هزینه شما در دسته «$name» با ${MoneyFormatter.format(amount)} و سهم ${percent(share)} از کل هزینه‌ها بوده است.",
                tone = InsightTone.NEUTRAL,
                amount = amount,
                iconKey = "category",
            )
        }

        // Fastest growing category
        val previousGrouped = previousExpenses.groupBy { it.categoryId }
            .mapValues { entry -> Money.sum(entry.value.map { it.amount }) }
        val growth = grouped.mapNotNull { (categoryId, amount) ->
            val before = previousGrouped[categoryId] ?: return@mapNotNull null
            if (!before.isPositive) return@mapNotNull null
            val ratio = (amount.rial - before.rial).toDouble() / before.rial
            Triple(categoryId, amount, ratio)
        }.filter { it.third > 0.15 }.maxByOrNull { it.third }
        growth?.let { (categoryId, amount, ratio) ->
            val name = categoryId?.let { categoryNames[it]?.name } ?: "بدون دسته‌بندی"
            insights += Insight(
                title = "بیشترین رشد هزینه",
                description = "هزینه دسته «$name» نسبت به ماه قبل ${percent(ratio)} افزایش یافته و به ${MoneyFormatter.format(amount)} رسیده است.",
                tone = InsightTone.NEGATIVE,
                amount = amount,
                iconKey = "trend",
            )
        }

        // Weekday spending pattern
        val weekdayTotals = currentExpenses.groupBy { JalaliDate.weekdayIndex(it.date.toLocalDate().dayOfWeek) }
            .mapValues { entry -> Money.sum(entry.value.map { it.amount }) }
        weekdayTotals.maxByOrNull { it.value.rial }?.let { (index, amount) ->
            insights += Insight(
                title = "روزهای پرخرج هفته",
                description = "بیشترین هزینه‌ها در روزهای ${JalaliDate.WEEKDAY_NAMES[index]} ثبت شده است: ${MoneyFormatter.format(amount)}.",
                tone = InsightTone.NEUTRAL,
                amount = amount,
                iconKey = "calendar",
            )
        }

        // Non-essential spending
        val nonEssentialIds = input.categories.filter { !it.essential }.map { it.id }.toSet()
        val nonEssential = Money.sum(currentExpenses.filter { it.categoryId in nonEssentialIds }.map { it.amount })
        if (nonEssential.isPositive) {
            val total = Money.sum(currentExpenses.map { it.amount })
            val share = if (total.isPositive) nonEssential.rial.toDouble() / total.rial else 0.0
            insights += Insight(
                title = "هزینه‌های غیرضروری",
                description = "${MoneyFormatter.format(nonEssential)} از هزینه‌های این ماه (${percent(share)}) در دسته‌های غیرضروری ثبت شده است.",
                tone = if (share > 0.3) InsightTone.NEGATIVE else InsightTone.NEUTRAL,
                amount = nonEssential,
                iconKey = "leaf",
            )
        }

        // Savings trend over three months
        val trend = (2 downTo 0).map { offset -> today.startOfMonth.plusMonths(-offset) }.map { month ->
            val start = month.startOfMonth
            val end = month.endOfMonth
            val monthIncome = Money.sum(
                input.transactions.filter {
                    it.type == TransactionType.INCOME && !it.date.isBefore(start) && !it.date.isAfter(end)
                }.map { it.amount },
            )
            val monthExpense = Money.sum(
                input.transactions.filter {
                    it.type == TransactionType.EXPENSE && !it.date.isBefore(start) && !it.date.isAfter(end)
                }.map { it.amount },
            )
            month to (monthIncome - monthExpense)
        }
        val positiveMonths = trend.count { it.second.isPositive }
        insights += Insight(
            title = "روند پس‌انداز سه‌ماهه",
            description = trend.joinToString("، ") { (month, net) ->
                "${month.monthName}: ${MoneyFormatter.format(net, withSign = true)}"
            } + ".",
            tone = if (positiveMonths >= 2) InsightTone.POSITIVE else InsightTone.NEGATIVE,
            amount = trend.lastOrNull()?.second,
            iconKey = "savings",
        )

        // Fixed cost ratio
        val monthlyFixed = Money.sum(
            input.recurring.filter { it.active }.map {
                when (it.period) {
                    RecurringPeriod.WEEKLY -> Money(it.amount.rial * 52 / 12)
                    RecurringPeriod.MONTHLY -> it.amount
                    RecurringPeriod.YEARLY -> Money(it.amount.rial / 12)
                }
            },
        )
        if (monthlyFixed.isPositive) {
            val share = if (income.isPositive) monthlyFixed.rial.toDouble() / income.rial else 0.0
            insights += Insight(
                title = "هزینه‌های ثابت",
                description = if (income.isPositive) {
                    "ماهانه حدود ${MoneyFormatter.format(monthlyFixed)} صرف هزینه‌های ثابت و اشتراک‌ها می‌شود؛ برابر ${percent(share)} از درآمد."
                } else {
                    "ماهانه حدود ${MoneyFormatter.format(monthlyFixed)} صرف هزینه‌های ثابت و اشتراک‌ها می‌شود."
                },
                tone = if (share > 0.5) InsightTone.NEGATIVE else InsightTone.NEUTRAL,
                amount = monthlyFixed,
                iconKey = "repeat",
            )
        }

        // Budget overruns
        val overrun = input.budgets.filter {
            BudgetEngine.progress(it, input.transactions, today).status ==
                ir.dakhlokharj.domain.model.BudgetStatus.EXCEEDED
        }
        if (overrun.isNotEmpty()) {
            insights += Insight(
                title = "بودجه‌های عبورشده",
                description = "${overrun.size} بودجه از سقف تعیین‌شده عبور کرده است: ${overrun.joinToString("،") { it.name }}.",
                tone = InsightTone.NEGATIVE,
                iconKey = "alert",
            )
        }

        if (insights.isEmpty()) {
            insights += Insight(
                title = "داده کافی نیست",
                description = "با ثبت درآمد و هزینه، تحلیل رفتار مالی شما به‌صورت خودکار ساخته می‌شود.",
                tone = InsightTone.NEUTRAL,
                iconKey = "insight",
            )
        }
        return insights
    }

    private fun percent(ratio: Double): String {
        val percent = Math.round(ratio * 100).toInt()
        return "${ir.dakhlokharj.core.format.PersianText.toPersianDigits(percent.toString())}٪"
    }
}
