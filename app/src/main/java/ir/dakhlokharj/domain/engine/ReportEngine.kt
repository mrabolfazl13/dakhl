package ir.dakhlokharj.domain.engine

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType

data class MonthlyTotal(
    val month: JalaliDate,
    val income: Money,
    val expense: Money,
) {
    val net: Money get() = income - expense
}

data class CategoryTotal(
    val name: String,
    val colorArgb: Int,
    val amount: Money,
    val share: Float,
)

data class WeekdayTotal(
    val weekdayIndex: Int,
    val label: String,
    val amount: Money,
)

object ReportEngine {

    fun monthlyTotals(transactions: List<Transaction>, months: Int, today: JalaliDate): List<MonthlyTotal> {
        return (months - 1 downTo 0).map { offset ->
            val month = today.startOfMonth.plusMonths(-offset)
            val start = month.startOfMonth
            val end = month.endOfMonth
            val income = Money.sum(
                transactions.filter {
                    it.type == TransactionType.INCOME && !it.date.isBefore(start) && !it.date.isAfter(end)
                }.map { it.amount },
            )
            val expense = Money.sum(
                transactions.filter {
                    it.type == TransactionType.EXPENSE && !it.date.isBefore(start) && !it.date.isAfter(end)
                }.map { it.amount },
            )
            MonthlyTotal(month, income, expense)
        }
    }

    fun categoryTotals(
        transactions: List<Transaction>,
        categories: List<Category>,
        kind: CategoryKind,
        from: JalaliDate,
        to: JalaliDate,
    ): List<CategoryTotal> {
        val type = if (kind == CategoryKind.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME
        val relevant = transactions.filter {
            it.type == type && !it.date.isBefore(from) && !it.date.isAfter(to)
        }
        val total = Money.sum(relevant.map { it.amount }).rial
        val categoryMap = categories.associateBy { it.id }
        return relevant.groupBy { it.categoryId }
            .map { (categoryId, list) ->
                val category = categoryId?.let { categoryMap[it] }
                val amount = Money.sum(list.map { it.amount })
                CategoryTotal(
                    name = category?.name ?: "بدون دسته‌بندی",
                    colorArgb = category?.colorArgb ?: 0xFF64748B.toInt(),
                    amount = amount,
                    share = if (total > 0) amount.rial.toFloat() / total else 0f,
                )
            }
            .sortedByDescending { it.amount.rial }
    }

    fun weekdayTotals(transactions: List<Transaction>, from: JalaliDate, to: JalaliDate): List<WeekdayTotal> {
        val filtered = transactions.filter {
            it.type == TransactionType.EXPENSE && !it.date.isBefore(from) && !it.date.isAfter(to)
        }
        return (0..6).map { index ->
            val amount = Money.sum(
                filtered.filter { JalaliDate.weekdayIndex(it.date.toLocalDate().dayOfWeek) == index }
                    .map { it.amount },
            )
            WeekdayTotal(index, JalaliDate.WEEKDAY_NAMES[index], amount)
        }
    }

    fun dailyNet(
        transactions: List<Transaction>,
        from: JalaliDate,
        to: JalaliDate,
    ): List<Pair<JalaliDate, Money>> {
        val filtered = transactions.filter {
            (it.type == TransactionType.INCOME || it.type == TransactionType.EXPENSE) &&
                !it.date.isBefore(from) && !it.date.isAfter(to)
        }
        val grouped = filtered.groupBy { it.date.toEpochDay() }
        val days = mutableListOf<Pair<JalaliDate, Money>>()
        var cursor = from
        while (cursor <= to) {
            val dayTransactions = grouped[cursor.toEpochDay()] ?: emptyList()
            val income = Money.sum(dayTransactions.filter { it.type == TransactionType.INCOME }.map { it.amount })
            val expense = Money.sum(dayTransactions.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
            days += cursor to (income - expense)
            cursor = cursor.plusDays(1)
        }
        return days
    }
}
