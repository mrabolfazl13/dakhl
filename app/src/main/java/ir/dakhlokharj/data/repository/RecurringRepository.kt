package ir.dakhlokharj.data.repository

import androidx.room.withTransaction
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.data.local.DkDatabase
import ir.dakhlokharj.data.local.RecurringDao
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.domain.engine.ForecastEngine
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringRepository(
    private val db: DkDatabase,
    private val dao: RecurringDao,
    private val transactionRepository: TransactionRepository,
) {

    fun observeAll(): Flow<List<RecurringExpense>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun add(recurring: RecurringExpense): Long =
        dao.insert(recurring.copy(createdAt = System.currentTimeMillis()).toEntity())

    suspend fun update(recurring: RecurringExpense) = dao.update(recurring.toEntity())

    suspend fun delete(recurring: RecurringExpense) = dao.delete(recurring.toEntity())

    suspend fun setActive(recurring: RecurringExpense, active: Boolean) =
        dao.update(recurring.copy(active = active).toEntity())

    /**
     * Pays the next occurrence, creates the real expense transaction, then advances the due
     * date. Paying twice on the same day is rejected instead of duplicating the transaction.
     */
    suspend fun pay(recurringId: Long, accountId: Long, date: JalaliDate): Long = db.withTransaction {
        val item = dao.getById(recurringId)?.toDomain() ?: error("هزینه تکرارشونده یافت نشد")
        require(item.active) { "این هزینه غیرفعال است" }
        require(item.lastPaidDate != date) { "این هزینه برای امروز ثبت شده است" }
        val transactionId = transactionRepository.add(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = item.amount,
                title = item.title,
                accountId = accountId,
                categoryId = item.categoryId,
                date = date,
                recurringId = recurringId,
            ),
        )
        dao.update(
            item.copy(
                lastPaidDate = date,
                nextDueDate = ForecastEngine.advance(item.nextDueDate, item.period),
            ).toEntity(),
        )
        transactionId
    }
}
