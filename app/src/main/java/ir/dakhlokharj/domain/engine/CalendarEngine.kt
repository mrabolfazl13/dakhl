package ir.dakhlokharj.domain.engine

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.FinancialEvent
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.PlannedEventKind
import ir.dakhlokharj.domain.model.Receivable
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType

enum class CalendarEventKind(val label: String, val income: Boolean) {
    INCOME("درآمد", true),
    EXPENSE("هزینه", false),
    TRANSFER("انتقال", false),
    INSTALLMENT("قسط", false),
    DEBT("بدهی", false),
    RECEIVABLE("طلب", true),
    RECURRING("هزینه تکرارشونده", false),
    PLANNED_INCOME("درآمد برنامه‌ریزی‌شده", true),
    PLANNED_EXPENSE("هزینه برنامه‌ریزی‌شده", false),
}

data class CalendarEvent(
    val title: String,
    val amount: Money,
    val kind: CalendarEventKind,
    val paid: Boolean = false,
    val referenceId: Long? = null,
)

data class CalendarDay(
    val date: JalaliDate,
    val events: List<CalendarEvent> = emptyList(),
) {
    val income: Money get() = Money.sum(events.filter { it.kind.income }.map { it.amount })
    val expense: Money get() = Money.sum(events.filter { !it.kind.income }.map { it.amount })
}

data class CalendarMonth(
    val year: Int,
    val month: Int,
    val days: List<CalendarDay>,
)

data class CalendarInput(
    val today: JalaliDate,
    val transactions: List<Transaction> = emptyList(),
    val installments: List<Installment> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val receivables: List<Receivable> = emptyList(),
    val recurring: List<RecurringExpense> = emptyList(),
    val planned: List<FinancialEvent> = emptyList(),
)

object CalendarEngine {

    fun buildMonth(input: CalendarInput, year: Int, month: Int): CalendarMonth {
        val first = JalaliDate(year, month, 1)
        val last = JalaliDate(year, month, JalaliDate(year, month, 1).monthLength)
        val buckets = mutableMapOf<Long, MutableList<CalendarEvent>>()
        fun add(date: JalaliDate, event: CalendarEvent) {
            if (date < first || date > last) return
            buckets.getOrPut(date.toEpochDay()) { mutableListOf() }.add(event)
        }

        input.transactions.forEach { transaction ->
            val kind = when (transaction.type) {
                TransactionType.INCOME -> CalendarEventKind.INCOME
                TransactionType.EXPENSE -> CalendarEventKind.EXPENSE
                TransactionType.TRANSFER -> CalendarEventKind.TRANSFER
                TransactionType.ADJUSTMENT -> null
            }
            if (kind != null) {
                add(transaction.date, CalendarEvent(transaction.title, transaction.amount, kind, paid = true, referenceId = transaction.id))
            }
        }

        input.installments.forEach { installment ->
            if (installment.isPaid) {
                installment.paidDate?.let { paid ->
                    add(paid, CalendarEvent("پرداخت قسط ${installment.index}", installment.amount, CalendarEventKind.INSTALLMENT, paid = true, referenceId = installment.id))
                }
            } else {
                add(installment.dueDate, CalendarEvent("قسط ${installment.index}", installment.amount, CalendarEventKind.INSTALLMENT, referenceId = installment.id))
            }
        }

        input.debts.filter { it.remaining.isPositive }.forEach { debt ->
            add(debt.dueDate, CalendarEvent("بدهی به ${debt.creditor}", debt.remaining, CalendarEventKind.DEBT, referenceId = debt.id))
        }

        input.receivables.filter { it.remaining.isPositive }.forEach { receivable ->
            add(receivable.expectedDate, CalendarEvent("طلب از ${receivable.debtor}", receivable.remaining, CalendarEventKind.RECEIVABLE, referenceId = receivable.id))
        }

        input.recurring.filter { it.active }.forEach { recurring ->
            var cursor = recurring.nextDueDate
            var guard = 0
            while (cursor < first && guard < 400) {
                cursor = ForecastEngine.advance(cursor, recurring.period)
                guard++
            }
            while (cursor <= last && guard < 420) {
                add(cursor, CalendarEvent(recurring.title, recurring.amount, CalendarEventKind.RECURRING, referenceId = recurring.id))
                cursor = ForecastEngine.advance(cursor, recurring.period)
                guard++
            }
        }

        input.planned.forEach { event ->
            val kind = if (event.kind == PlannedEventKind.INCOME) CalendarEventKind.PLANNED_INCOME else CalendarEventKind.PLANNED_EXPENSE
            add(event.date, CalendarEvent(event.title, event.amount, kind, referenceId = event.id))
        }

        val days = (1..last.monthLength).map { day ->
            val date = JalaliDate(year, month, day)
            CalendarDay(date, buckets[date.toEpochDay()]?.sortedBy { it.title } ?: emptyList())
        }
        return CalendarMonth(year, month, days)
    }
}
