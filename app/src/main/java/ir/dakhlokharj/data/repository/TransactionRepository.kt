package ir.dakhlokharj.data.repository

import androidx.room.withTransaction
import ir.dakhlokharj.data.local.AccountDao
import ir.dakhlokharj.data.local.DkDatabase
import ir.dakhlokharj.data.local.TransactionDao
import ir.dakhlokharj.data.local.TransactionEntity
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(
    private val db: DkDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
) {

    fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeRecent(limit: Int = 20): Flow<List<Transaction>> =
        transactionDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<Transaction>> =
        transactionDao.observeRange(startEpochDay, endEpochDay).map { list -> list.map { it.toDomain() } }

    fun observeForAccount(accountId: Long): Flow<List<Transaction>> =
        transactionDao.observeForAccount(accountId).map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Transaction> = transactionDao.getAll().map { it.toDomain() }

    suspend fun getById(id: Long): Transaction? = transactionDao.getById(id)?.toDomain()

    suspend fun add(transaction: Transaction): Long = db.withTransaction {
        validate(transaction)
        val timestamp = System.currentTimeMillis()
        val id = transactionDao.insert(transaction.copy(createdAt = timestamp, updatedAt = timestamp).toEntity())
        applyEffects(transaction, 1)
        id
    }

    suspend fun update(transaction: Transaction): Unit = db.withTransaction {
        validate(transaction)
        val old = transactionDao.getById(transaction.id)?.toDomain() ?: error("تراکنش یافت نشد")
        applyEffects(old, -1)
        transactionDao.update(transaction.copy(updatedAt = System.currentTimeMillis()).toEntity())
        applyEffects(transaction, 1)
    }

    suspend fun delete(id: Long): Unit = db.withTransaction {
        val old = transactionDao.getById(id)?.toDomain() ?: return@withTransaction
        applyEffects(old, -1)
        transactionDao.delete(old.toEntity())
    }

    suspend fun insertRaw(entity: TransactionEntity) = transactionDao.insert(entity)

    private fun validate(transaction: Transaction) {
        require(!transaction.amount.isZero) { "مبلغ تراکنش نمی‌تواند صفر باشد" }
        when (transaction.type) {
            TransactionType.ADJUSTMENT -> Unit
            else -> require(transaction.amount.isPositive) { "مبلغ تراکنش باید مثبت باشد" }
        }
        if (transaction.type == TransactionType.TRANSFER) {
            require(transaction.targetAccountId != null) { "حساب مقصد را انتخاب کنید" }
            require(transaction.targetAccountId != transaction.accountId) { "حساب مبدأ و مقصد نباید یکسان باشند" }
        }
    }

    private suspend fun applyEffects(transaction: Transaction, sign: Int) {
        val amount = transaction.amount.rial * sign
        when (transaction.type) {
            TransactionType.INCOME -> accountDao.adjustBalance(transaction.accountId, amount)
            TransactionType.EXPENSE -> accountDao.adjustBalance(transaction.accountId, -amount)
            TransactionType.TRANSFER -> {
                accountDao.adjustBalance(transaction.accountId, -amount)
                transaction.targetAccountId?.let { accountDao.adjustBalance(it, amount) }
            }
            TransactionType.ADJUSTMENT -> accountDao.adjustBalance(transaction.accountId, amount)
            TransactionType.SAVINGS_DEPOSIT -> accountDao.adjustBalance(transaction.accountId, -amount)
            TransactionType.SAVINGS_WITHDRAW -> accountDao.adjustBalance(transaction.accountId, amount)
        }
    }
}
