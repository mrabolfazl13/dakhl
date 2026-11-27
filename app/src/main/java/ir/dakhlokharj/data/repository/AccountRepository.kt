package ir.dakhlokharj.data.repository

import androidx.room.withTransaction
import ir.dakhlokharj.data.local.AccountDao
import ir.dakhlokharj.data.local.DkDatabase
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepository(
    private val db: DkDatabase,
    private val dao: AccountDao,
) {

    fun observeActive(): Flow<List<Account>> = dao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<Account>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeById(id: Long): Flow<Account?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun getAll(): List<Account> = dao.getAll().map { it.toDomain() }

    suspend fun getById(id: Long): Account? = dao.getById(id)?.toDomain()

    suspend fun count(): Int = dao.count()

    suspend fun create(account: Account): Long = db.withTransaction {
        dao.insert(account.copy(balance = account.openingBalance).toEntity())
    }

    /**
     * Updates account metadata and applies the difference of a changed opening balance to
     * the current balance. Existing transaction history is never rewritten.
     */
    suspend fun update(account: Account) = db.withTransaction {
        val current = dao.getById(account.id) ?: error("حساب یافت نشد")
        val openingDelta = account.openingBalance.rial - current.openingBalanceRial
        val newBalance = current.balanceRial + openingDelta
        dao.update(account.copy(balance = ir.dakhlokharj.core.money.Money(newBalance)).toEntity())
    }

    suspend fun archive(account: Account, archived: Boolean = true) = dao.update(account.copy(archived = archived).toEntity())

    suspend fun delete(account: Account) = dao.delete(account.toEntity())

    suspend fun totalBalance(): Long = dao.getAll().filter { !it.archived }.sumOf { it.balanceRial }

    companion object {
        fun default(name: String = "کیف پول نقد"): Account = Account(
            name = name,
            type = AccountType.CASH,
            openingBalance = ir.dakhlokharj.core.money.Money.ZERO,
            balance = ir.dakhlokharj.core.money.Money.ZERO,
            colorArgb = 0xFF34D399.toInt(),
            iconKey = "wallet",
        )
    }
}
