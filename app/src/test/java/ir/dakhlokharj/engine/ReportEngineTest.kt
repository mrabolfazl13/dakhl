package ir.dakhlokharj.engine

import com.google.common.truth.Truth.assertThat
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.ReportEngine
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import org.junit.Test

private fun tx(
    type: TransactionType,
    toman: Long,
    date: JalaliDate,
    categoryId: Long? = 1,
) = Transaction(
    type = type,
    amount = Money.toman(toman),
    title = "t",
    categoryId = categoryId,
    accountId = 1,
    date = date,
)

private val categories = listOf(
    Category(id = 1, name = "food", kind = CategoryKind.EXPENSE, iconKey = "food", colorArgb = 0xFF34D399.toInt()),
    Category(id = 2, name = "salary", kind = CategoryKind.INCOME, iconKey = "salary", colorArgb = 0xFF60A5FA.toInt()),
)

class ReportEngineTest {

    @Test
    fun monthlyTotalsReturnsRequestedNumberOfMonths() {
        val totals = ReportEngine.monthlyTotals(emptyList(), 6, JalaliDate(1403, 1, 15))
        assertThat(totals).hasSize(6)
        assertThat(totals.last().month).isEqualTo(JalaliDate(1403, 1, 1))
    }

    @Test
    fun monthlyTotalsSumsIncomeAndExpensePerMonth() {
        val totals = ReportEngine.monthlyTotals(
            listOf(
                tx(TransactionType.INCOME, 1000, JalaliDate(1403, 1, 5), 2),
                tx(TransactionType.EXPENSE, 400, JalaliDate(1403, 1, 6)),
                tx(TransactionType.EXPENSE, 100, JalaliDate(1403, 1, 7)),
            ),
            1,
            JalaliDate(1403, 1, 15),
        )
        assertThat(totals.single().income.rial).isEqualTo(Money.toman(1000).rial)
        assertThat(totals.single().expense.rial).isEqualTo(Money.toman(500).rial)
        assertThat(totals.single().net.rial).isEqualTo(Money.toman(500).rial)
    }

    @Test
    fun categoryTotalsShareSumsToOne() {
        val slices = ReportEngine.categoryTotals(
            listOf(
                tx(TransactionType.EXPENSE, 300, JalaliDate(1403, 1, 5), 1),
                tx(TransactionType.EXPENSE, 100, JalaliDate(1403, 1, 6), null),
            ),
            categories,
            CategoryKind.EXPENSE,
            JalaliDate(1403, 1, 1),
            JalaliDate(1403, 1, 31),
        )
        assertThat(slices.sumOf { it.share.toDouble() }).isWithin(0.0001).of(1.0)
    }

    @Test
    fun uncategorizedTransactionsGetFallbackName() {
        val slices = ReportEngine.categoryTotals(
            listOf(tx(TransactionType.EXPENSE, 100, JalaliDate(1403, 1, 5), null)),
            categories,
            CategoryKind.EXPENSE,
            JalaliDate(1403, 1, 1),
            JalaliDate(1403, 1, 31),
        )
        assertThat(slices.single().name).isEqualTo("بدون دسته‌بندی")
    }

    @Test
    fun categoryTotalsRespectKind() {
        val slices = ReportEngine.categoryTotals(
            listOf(
                tx(TransactionType.EXPENSE, 100, JalaliDate(1403, 1, 5), 1),
                tx(TransactionType.INCOME, 900, JalaliDate(1403, 1, 6), 2),
            ),
            categories,
            CategoryKind.INCOME,
            JalaliDate(1403, 1, 1),
            JalaliDate(1403, 1, 31),
        )
        assertThat(slices).hasSize(1)
        assertThat(slices.single().amount.rial).isEqualTo(Money.toman(900).rial)
    }

    @Test
    fun weekdayTotalsStartWithSaturday() {
        val saturday = JalaliDate(1403, 1, 4)
        assertThat(saturday.toLocalDate().dayOfWeek).isEqualTo(java.time.DayOfWeek.SATURDAY)
        val totals = ReportEngine.weekdayTotals(
            listOf(tx(TransactionType.EXPENSE, 100, saturday)),
            JalaliDate(1403, 1, 1),
            JalaliDate(1403, 1, 31),
        )
        assertThat(totals[0].label).isEqualTo("شنبه")
        assertThat(totals[0].amount.rial).isEqualTo(Money.toman(100).rial)
    }

    @Test
    fun dailyNetIncludesEmptyDays() {
        val days = ReportEngine.dailyNet(
            listOf(
                tx(TransactionType.INCOME, 100, JalaliDate(1403, 1, 1), 2),
                tx(TransactionType.EXPENSE, 40, JalaliDate(1403, 1, 3)),
            ),
            JalaliDate(1403, 1, 1),
            JalaliDate(1403, 1, 3),
        )
        assertThat(days).hasSize(3)
        assertThat(days[1].second.rial).isEqualTo(0L)
        assertThat(days[0].second.rial).isEqualTo(Money.toman(100).rial)
        assertThat(days[2].second.rial).isEqualTo(Money.toman(-40).rial)
    }

    @Test
    fun transfersAreNotCountedAsIncomeOrExpense() {
        val totals = ReportEngine.monthlyTotals(
            listOf(tx(TransactionType.TRANSFER, 500, JalaliDate(1403, 1, 5))),
            1,
            JalaliDate(1403, 1, 15),
        )
        assertThat(totals.single().income.rial).isEqualTo(0L)
        assertThat(totals.single().expense.rial).isEqualTo(0L)
    }
}
