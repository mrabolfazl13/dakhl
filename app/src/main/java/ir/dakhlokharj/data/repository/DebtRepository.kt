package ir.dakhlokharj.data.repository

import androidx.room.withTransaction
import ir.dakhlokharj.data.local.DebtDao
import ir.dakhlokharj.data.local.DebtPaymentDao
import ir.dakhlokharj.data.local.DkDatabase
import ir.dakhlokharj.data.local.ReceivableDao
import ir.dakhlokharj.data.local.ReceivablePaymentDao
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.DebtPayment
import ir.dakhlokharj.domain.model.Receivable
import ir.dakhlokharj.domain.model.ReceivablePayment
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DebtRepository(
    private val db: DkDatabase,
    private val debtDao: DebtDao,
    private val debtPaymentDao: DebtPaymentDao,
    private val receivableDao: ReceivableDao,
    private val receivablePaymentDao: ReceivablePaymentDao,
    private val transactionRepository: TransactionRepository,
) {

    fun observeDebts(): Flow<List<Debt>> = debtDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeReceivables(): Flow<List<Receivable>> =
        receivableDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observePayments(debtId: Long): Flow<List<DebtPayment>> =
        debtPaymentDao.observeForDebt(debtId).map { list -> list.map { it.toDomain() } }

    fun observeReceivablePayments(): Flow<List<ReceivablePayment>> =
        receivablePaymentDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeAllDebtPayments(): Flow<List<DebtPayment>> =
        debtPaymentDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addDebt(debt: Debt): Long = debtDao.insert(debt.copy(createdAt = System.currentTimeMillis()).toEntity())

    suspend fun updateDebt(debt: Debt) = debtDao.update(debt.toEntity())

    suspend fun deleteDebt(debt: Debt) = debtDao.delete(debt.toEntity())

    suspend fun addReceivable(receivable: Receivable): Long =
        receivableDao.insert(receivable.copy(createdAt = System.currentTimeMillis()).toEntity())

    suspend fun updateReceivable(receivable: Receivable) = receivableDao.update(receivable.toEntity())

    suspend fun deleteReceivable(receivable: Receivable) = receivableDao.delete(receivable.toEntity())

    /** Records a real expense transaction, reduces the debt balance and stores the payment atomically. */
    suspend fun payDebt(debtId: Long, amount: Money, date: JalaliDate, accountId: Long): Long = db.withTransaction {
        require(amount.isPositive) { "مبلغ پرداخت باید مثبت باشد" }
        val debt = debtDao.getById(debtId)?.toDomain() ?: error("بدهی یافت نشد")
        require(amount <= debt.remaining) { "مبلغ پرداخت از مانده بدهی بیشتر است" }
        val transactionId = transactionRepository.add(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = amount,
                title = "پرداخت بدهی به ${debt.creditor}",
                accountId = accountId,
                date = date,
                debtId = debtId,
            ),
        )
        debtDao.update(debt.copy(paid = debt.paid + amount).toEntity())
        debtPaymentDao.insert(
            DebtPayment(
                debtId = debtId,
                amount = amount,
                date = date,
                transactionId = transactionId,
                createdAt = System.currentTimeMillis(),
            ).toEntity(),
        )
        transactionId
    }

    /** Records a real income transaction and reduces the receivable balance atomically. */
    suspend fun receiveReceivable(receivableId: Long, amount: Money, date: JalaliDate, accountId: Long): Long =
        db.withTransaction {
            require(amount.isPositive) { "مبلغ دریافت باید مثبت باشد" }
            val receivable = receivableDao.getById(receivableId)?.toDomain() ?: error("طلب یافت نشد")
            require(amount <= receivable.remaining) { "مبلغ دریافت از مانده طلب بیشتر است" }
            val transactionId = transactionRepository.add(
                Transaction(
                    type = TransactionType.INCOME,
                    amount = amount,
                    title = "دریافت طلب از ${receivable.debtor}",
                    accountId = accountId,
                    date = date,
                    receivableId = receivableId,
                ),
            )
            receivableDao.update(receivable.copy(received = receivable.received + amount).toEntity())
            receivablePaymentDao.insert(
                ReceivablePayment(
                    receivableId = receivableId,
                    amount = amount,
                    date = date,
                    transactionId = transactionId,
                    createdAt = System.currentTimeMillis(),
                ).toEntity(),
            )
            transactionId
        }
}