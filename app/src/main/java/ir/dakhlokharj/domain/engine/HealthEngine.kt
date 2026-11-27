package ir.dakhlokharj.domain.engine

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.format.MoneyFormatter
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.CurrencyUnit
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.InsightTone
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.RecurringPeriod
import ir.dakhlokharj.domain.model.SavingsGoal
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType

data class HealthInput(
    val today: JalaliDate,
    val transactions: List<Transaction>,
    val budgets: List<Budget> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val recurring: List<RecurringExpense> = emptyList(),
    val installments: List<Installment> = emptyList(),
)

data class HealthComponent(
    val key: String,
    val title: String,
    val score: Int,
    val maxScore: Int,
    val explanation: String,
    val tone: InsightTone,
)

data class HealthResult(
    val score: Int,
    val grade: String,
    val summary: String,
    val components: List<HealthComponent>,
    val positives: List<String>,
    val improvements: List<String>,
)

/**
 * Explainable 0..100 financial health score. Every component is derived from real data,
 * has a fixed maximum and a human readable explanation. No randomness.
 */
object HealthEngine {

    fun calculate(input: HealthInput): HealthResult {
        val monthStart = input.today.startOfMonth
        val monthEnd = input.today.endOfMonth
        val monthTransactions = input.transactions.filter {
            it.date >= monthStart && it.date <= monthEnd &&
                (it.type == TransactionType.INCOME || it.type == TransactionType.EXPENSE)
        }
        val income = Money.sum(monthTransactions.filter { it.type == TransactionType.INCOME }.map { it.amount })
        val expense = Money.sum(monthTransactions.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
        val hasIncome = income.isPositive

        val monthlyObligations = Money.sum(
            input.recurring.filter { it.active }.map { monthlyEquivalent(it) },
        ) + Money.sum(input.installments.filter { !it.isPaid }.map { it.amount })

        val components = mutableListOf<HealthComponent>()

        // 1) Savings rate (25)
        val savingsRate = if (hasIncome) (income.rial - expense.rial).toDouble() / income.rial else 0.0
        val savingsPoints = when {
            !hasIncome -> 0
            savingsRate >= 0.30 -> 25
            savingsRate >= 0.20 -> 20
            savingsRate >= 0.10 -> 14
            savingsRate >= 0.0 -> 8
            else -> 0
        }
        components += HealthComponent(
            key = "savings",
            title = "نرخ پس‌انداز",
            score = savingsPoints,
            maxScore = 25,
            explanation = if (hasIncome) {
                "این ماه ${percentLabel(savingsRate)} از درآمد پس‌انداز شده است."
            } else {
                "برای محاسبه نرخ پس‌انداز، ابتدا درآمد این ماه را ثبت کنید."
            },
            tone = toneFor(savingsPoints, 25),
        )

        // 2) Expense to income (20)
        val expenseRatio = if (hasIncome) expense.rial.toDouble() / income.rial else 1.0
        val expensePoints = when {
            !hasIncome -> 0
            expenseRatio <= 0.60 -> 20
            expenseRatio <= 0.75 -> 15
            expenseRatio <= 0.90 -> 10
            expenseRatio <= 1.0 -> 5
            else -> 0
        }
        components += HealthComponent(
            key = "expense_ratio",
            title = "تعادل درآمد و هزینه",
            score = expensePoints,
            maxScore = 20,
            explanation = if (hasIncome) {
                "هزینه‌ها ${percentLabel(expenseRatio)} از درآمد را تشکیل می‌دهند."
            } else {
                "درآمدی برای سنجش نسبت هزینه ثبت نشده است."
            },
            tone = toneFor(expensePoints, 20),
        )

        // 3) Obligation ratio (20)
        val obligationRatio = if (hasIncome) monthlyObligations.rial.toDouble() / income.rial else 1.0
        val obligationPoints = when {
            !hasIncome -> 0
            obligationRatio <= 0.20 -> 20
            obligationRatio <= 0.35 -> 15
            obligationRatio <= 0.50 -> 10
            obligationRatio <= 0.70 -> 5
            else -> 0
        }
        components += HealthComponent(
            key = "obligations",
            title = "نسبت تعهدات به درآمد",
            score = obligationPoints,
            maxScore = 20,
            explanation = "تعهدات ماهانه حدود ${MoneyFormatter.format(monthlyObligations, includeSymbol = false)} تومان است" +
                if (hasIncome) " (${percentLabel(obligationRatio)} از درآمد)." else ".",
            tone = toneFor(obligationPoints, 20),
        )

        // 4) Budget control (15)
        val activeBudgets = input.budgets.filter { !it.endDate.isBefore(monthStart) && !it.startDate.isAfter(monthEnd) }
        val worstPercent = activeBudgets.maxOfOrNull { BudgetEngine.progress(it, input.transactions, input.today).percent } ?: 0
        val budgetPoints = when {
            activeBudgets.isEmpty() -> 7
            worstPercent <= 80 -> 15
            worstPercent <= 100 -> 10
            worstPercent <= 115 -> 5
            else -> 0
        }
        components += HealthComponent(
            key = "budget",
            title = "کنترل بودجه",
            score = budgetPoints,
            maxScore = 15,
            explanation = if (activeBudgets.isEmpty()) {
                "هنوز بودجه‌ای تعریف نشده است؛ برای امتیاز کامل بودجه ماهانه بسازید."
            } else {
                "بیشترین مصرف بودجه ${percentLabel(worstPercent / 100.0)} است."
            },
            tone = toneFor(budgetPoints, 15),
        )

        // 5) Emergency fund (10)
        val liquid = Money.sum(input.accounts.filter { !it.archived }.map { it.balance })
        val monthlyExpense = if (expense.isPositive) expense else averageMonthlyExpense(input.transactions, input.today)
        val coverMonths = if (monthlyExpense.isPositive) liquid.rial.toDouble() / monthlyExpense.rial else 0.0
        val emergencyPoints = when {
            coverMonths >= 6 -> 10
            coverMonths >= 3 -> 7
            coverMonths >= 1 -> 4
            liquid.isPositive -> 2
            else -> 0
        }
        components += HealthComponent(
            key = "emergency",
            title = "ذخیره اضطراری",
            score = emergencyPoints,
            maxScore = 10,
            explanation = if (monthlyExpense.isPositive) {
                "موجودی شما معادل ${formatOneDecimal(coverMonths)} ماه هزینه است."
            } else {
                "برای سنجش ذخیره اضطراری، چند هزینه ثبت کنید."
            },
            tone = toneFor(emergencyPoints, 10),
        )

        // 6) Payment discipline (10)
        val openDebts = input.debts.filter { it.remaining.isPositive }
        val overdue = openDebts.count { it.dueDate < input.today }
        val disciplinePoints = when {
            openDebts.isEmpty() -> 10
            overdue == 0 -> 10
            overdue.toDouble() / openDebts.size <= 0.25 -> 7
            overdue.toDouble() / openDebts.size <= 0.5 -> 4
            else -> 0
        }
        components += HealthComponent(
            key = "discipline",
            title = "نظم در پرداخت تعهدات",
            score = disciplinePoints,
            maxScore = 10,
            explanation = when {
                openDebts.isEmpty() -> "بدهی باز بدون سررسید گذشته ندارید."
                overdue == 0 -> "${openDebts.size} بدهی باز دارید و هیچ‌کدام معوق نیست."
                else -> "${overdue} بدهی از ${openDebts.size} بدهی سررسید گذشته است."
            },
            tone = toneFor(disciplinePoints, 10),
        )

        val total = components.sumOf { it.score }.coerceIn(0, 100)
        val grade = when {
            total >= 80 -> "عالی"
            total >= 65 -> "خوب"
            total >= 50 -> "متوسط"
            total >= 35 -> "ضعیف"
            else -> "بحرانی"
        }
        val positives = components.filter { it.score >= it.maxScore * 0.7 }.map { it.title }
        val improvements = components.filter { it.score < it.maxScore * 0.5 }.map { it.title }
        val summary = when {
            total >= 80 -> "وضعیت مالی شما پایدار است؛ همین روند را حفظ کنید."
            total >= 65 -> "وضعیت مالی قابل قبولی دارید، اما جای بهبود وجود دارد."
            total >= 50 -> "برای رسیدن به آرامش مالی، تعهدات و بودجه را دقیق‌تر مدیریت کنید."
            else -> "برای جلوگیری از فشار مالی، هزینه‌ها و تعهدات را بازبینی کنید."
        }
        return HealthResult(total, grade, summary, components, positives, improvements)
    }

    private fun monthlyEquivalent(recurring: RecurringExpense): Money = when (recurring.period) {
        RecurringPeriod.WEEKLY -> Money(recurring.amount.rial * 52 / 12)
        RecurringPeriod.MONTHLY -> recurring.amount
        RecurringPeriod.YEARLY -> Money(recurring.amount.rial / 12)
    }

    private fun averageMonthlyExpense(transactions: List<Transaction>, today: JalaliDate): Money {
        val totals = (0..2).map { offset ->
            val date = today.startOfMonth.plusMonths(-offset)
            val next = date.plusMonths(1)
            Money.sum(
                transactions.filter {
                    it.type == TransactionType.EXPENSE && it.date >= date && it.date < next
                }.map { it.amount },
            )
        }.filter { it.isPositive }
        return if (totals.isEmpty()) Money.ZERO else Money(totals.sumOf { it.rial } / totals.size)
    }

    private fun percentLabel(ratio: Double): String {
        val percent = Math.round(ratio * 100).toInt()
        return "${ir.dakhlokharj.core.format.PersianText.toPersianDigits(percent.toString())}٪"
    }

    private fun formatOneDecimal(value: Double): String {
        val rounded = Math.round(value * 10.0) / 10.0
        val raw = if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else String.format(java.util.Locale.US, "%.1f", rounded)
        return ir.dakhlokharj.core.format.PersianText.toPersianDigits(raw)
    }

    private fun toneFor(score: Int, max: Int): InsightTone = when {
        score >= max * 0.7 -> InsightTone.POSITIVE
        score >= max * 0.4 -> InsightTone.NEUTRAL
        else -> InsightTone.NEGATIVE
    }
}
