package ir.dakhlokharj.engine

import com.google.common.truth.Truth.assertThat
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.BudgetEngine
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.BudgetPeriod
import ir.dakhlokharj.domain.model.BudgetScope
import ir.dakhlokharj.domain.model.BudgetStatus
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import org.junit.Test

private val start = JalaliDate(1403, 1, 1)
private val end = JalaliDate(1403, 1, 31)

private fun expense(toman: Long, day: Int, categoryId: Long? = 1L) = Transaction(
    type = TransactionType.EXPENSE,
    amount = Money.toman(toman),
    title = "test",
    categoryId = categoryId,
    accountId = 1,
    date = JalaliDate(1403, 1, day),
)

private fun budget(
    toman: Long,
    scope: BudgetScope = BudgetScope.OVERALL,
    categoryId: Long? = null,
) = Budget(
    name = "budget",
    scope = scope,
    categoryId = categoryId,
    period = BudgetPeriod.MONTHLY,
    startDate = start,
    endDate = end,
    amount = Money.toman(toman),
)

class BudgetEngineTest {

    @Test
    fun overallBudgetCountsAllExpenses() {
        val progress = BudgetEngine.progress(
            budget(1000),
            listOf(expense(100, 2, 7), expense(200, 3, 8)),
            JalaliDate(1403, 1, 10),
        )
        assertThat(progress.spent.rial).isEqualTo(Money.toman(300).rial)
    }

    @Test
    fun categoryBudgetOnlyCountsMatchingCategory() {
        val progress = BudgetEngine.progress(
            budget(1000, BudgetScope.CATEGORY, 7),
            listOf(expense(100, 2, 7), expense(200, 3, 8)),
            JalaliDate(1403, 1, 10),
        )
        assertThat(progress.spent.rial).isEqualTo(Money.toman(100).rial)
    }

    @Test
    fun transactionsOutsideRangeAreIgnored() {
        val progress = BudgetEngine.progress(
            budget(1000),
            listOf(
                expense(100, 1),
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money.toman(500),
                    title = "outside",
                    categoryId = 1,
                    accountId = 1,
                    date = JalaliDate(1403, 2, 1),
                ),
            ),
            JalaliDate(1403, 1, 10),
        )
        assertThat(progress.spent.rial).isEqualTo(Money.toman(100).rial)
    }

    @Test
    fun percentIsIntegerAndCapped() {
        assertThat(
            BudgetEngine.progress(budget(1000), listOf(expense(250, 2)), JalaliDate(1403, 1, 3)).percent,
        ).isEqualTo(25)
        assertThat(
            BudgetEngine.progress(budget(100), listOf(expense(500, 2)), JalaliDate(1403, 1, 3)).percent,
        ).isEqualTo(999)
    }

    @Test
    fun remainingNeverGoesNegative() {
        val progress = BudgetEngine.progress(budget(100), listOf(expense(500, 2)), JalaliDate(1403, 1, 3))
        assertThat(progress.remaining.rial).isEqualTo(0L)
    }

    @Test
    fun statusThresholdsAreExact() {
        assertThat(BudgetEngine.statusFor(0)).isEqualTo(BudgetStatus.SAFE)
        assertThat(BudgetEngine.statusFor(74)).isEqualTo(BudgetStatus.SAFE)
        assertThat(BudgetEngine.statusFor(75)).isEqualTo(BudgetStatus.WARNING)
        assertThat(BudgetEngine.statusFor(89)).isEqualTo(BudgetStatus.WARNING)
        assertThat(BudgetEngine.statusFor(90)).isEqualTo(BudgetStatus.DANGER)
        assertThat(BudgetEngine.statusFor(99)).isEqualTo(BudgetStatus.DANGER)
        assertThat(BudgetEngine.statusFor(100)).isEqualTo(BudgetStatus.EXCEEDED)
    }

    @Test
    fun projectionScalesSpendToWholeMonth() {
        val progress = BudgetEngine.progress(
            budget(3100),
            listOf(expense(100, 1)),
            JalaliDate(1403, 1, 10),
        )
        assertThat(progress.daysTotal).isEqualTo(31)
        assertThat(progress.daysElapsed).isEqualTo(10)
        assertThat(progress.projectedSpend.rial).isEqualTo(Money.toman(310).rial)
    }

    @Test
    fun projectionAfterEndEqualsActualSpend() {
        val progress = BudgetEngine.progress(
            budget(1000),
            listOf(expense(200, 5)),
            JalaliDate(1403, 2, 5),
        )
        assertThat(progress.projectedSpend.rial).isEqualTo(Money.toman(200).rial)
    }

    @Test
    fun overallPercentAggregatesBudgets() {
        val percent = BudgetEngine.overallPercent(
            listOf(budget(1000), budget(1000)),
            listOf(expense(500, 2), expense(300, 3)),
            JalaliDate(1403, 1, 10),
        )
        assertThat(percent).isEqualTo(40)
    }

    @Test
    fun overallPercentWithoutBudgetsIsZero() {
        assertThat(BudgetEngine.overallPercent(emptyList(), emptyList(), JalaliDate(1403, 1, 1))).isEqualTo(0)
    }
}
