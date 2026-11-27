package ir.dakhlokharj.engine

import com.google.common.truth.Truth.assertThat
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.HealthEngine
import ir.dakhlokharj.domain.engine.HealthInput
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.AccountType
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import org.junit.Test

private val today = JalaliDate(1403, 1, 10)

private fun tx(type: TransactionType, toman: Long, categoryId: Long? = null) = Transaction(
    type = type,
    amount = Money.toman(toman),
    title = "t",
    categoryId = categoryId,
    accountId = 1,
    date = JalaliDate(1403, 1, 5),
)

private fun account(toman: Long) = Account(
    name = "cash",
    type = AccountType.CASH,
    openingBalance = Money.toman(toman),
    balance = Money.toman(toman),
    colorArgb = 0,
    iconKey = "cash",
)

private fun input(
    transactions: List<Transaction>,
    balance: Long = 0,
    debts: List<Debt> = emptyList(),
) = HealthInput(
    today = today,
    transactions = transactions,
    accounts = listOf(account(balance)),
    debts = debts,
)

class HealthEngineTest {

    @Test
    fun resultAlwaysHasSixExplainableComponents() {
        val result = HealthEngine.calculate(input(emptyList()))
        assertThat(result.components).hasSize(6)
        assertThat(result.components.map { it.key }).containsExactly(
            "savings", "expense_ratio", "obligations", "budget", "emergency", "discipline",
        ).inOrder()
        assertThat(result.components.all { it.explanation.isNotBlank() }).isTrue()
    }

    @Test
    fun scoreStaysInZeroToHundred() {
        val result = HealthEngine.calculate(input(listOf(tx(TransactionType.EXPENSE, 10_000))))
        assertThat(result.score).isAtLeast(0)
        assertThat(result.score).isAtMost(100)
    }

    @Test
    fun highSavingsRateEarnsFullSavingsPoints() {
        val result = HealthEngine.calculate(
            input(listOf(tx(TransactionType.INCOME, 1000), tx(TransactionType.EXPENSE, 500)), balance = 6000),
        )
        assertThat(result.components.first { it.key == "savings" }.score).isEqualTo(25)
    }

    @Test
    fun noIncomeMeansZeroSavingsPoints() {
        val result = HealthEngine.calculate(input(listOf(tx(TransactionType.EXPENSE, 100))))
        assertThat(result.components.first { it.key == "savings" }.score).isEqualTo(0)
        assertThat(result.components.first { it.key == "expense_ratio" }.score).isEqualTo(0)
    }

    @Test
    fun overspendingZeroesExpensePoints() {
        val result = HealthEngine.calculate(
            input(listOf(tx(TransactionType.INCOME, 100), tx(TransactionType.EXPENSE, 200))),
        )
        assertThat(result.components.first { it.key == "expense_ratio" }.score).isEqualTo(0)
    }

    @Test
    fun lowExpenseRatioEarnsFullExpensePoints() {
        val result = HealthEngine.calculate(
            input(listOf(tx(TransactionType.INCOME, 1000), tx(TransactionType.EXPENSE, 400))),
        )
        assertThat(result.components.first { it.key == "expense_ratio" }.score).isEqualTo(20)
    }

    @Test
    fun emergencyFundSixMonthsEarnsFullPoints() {
        val result = HealthEngine.calculate(
            input(
                listOf(tx(TransactionType.INCOME, 1000), tx(TransactionType.EXPENSE, 100)),
                balance = 600,
            ),
        )
        assertThat(result.components.first { it.key == "emergency" }.score).isEqualTo(10)
    }

    @Test
    fun overdueDebtLowersDiscipline() {
        val debt = Debt(
            creditor = "x",
            principal = Money.toman(100),
            paid = Money.ZERO,
            dueDate = JalaliDate(1403, 1, 1),
        )
        val result = HealthEngine.calculate(input(emptyList(), debts = listOf(debt)))
        assertThat(result.components.first { it.key == "discipline" }.score).isEqualTo(0)
    }

    @Test
    fun noDebtEarnsFullDiscipline() {
        val result = HealthEngine.calculate(input(emptyList()))
        assertThat(result.components.first { it.key == "discipline" }.score).isEqualTo(10)
    }

    @Test
    fun gradeMatchesScoreBands() {
        val result = HealthEngine.calculate(
            input(listOf(tx(TransactionType.INCOME, 1000), tx(TransactionType.EXPENSE, 500)), balance = 6000),
        )
        assertThat(result.grade).isAnyOf("عالی", "خوب", "متوسط", "ضعیف", "بحرانی")
        assertThat(result.summary).isNotEmpty()
    }

    @Test
    fun poorProfileListsImprovementAreas() {
        val result = HealthEngine.calculate(input(listOf(tx(TransactionType.EXPENSE, 500))))
        assertThat(result.improvements).isNotEmpty()
    }

    @Test
    fun strongProfileListsPositiveAreas() {
        val result = HealthEngine.calculate(
            input(listOf(tx(TransactionType.INCOME, 1000), tx(TransactionType.EXPENSE, 300)), balance = 6000),
        )
        assertThat(result.positives).isNotEmpty()
    }
}
