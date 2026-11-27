package ir.dakhlokharj.data.repository

import androidx.room.withTransaction
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.data.local.DkDatabase
import ir.dakhlokharj.data.local.SavingsContributionDao
import ir.dakhlokharj.data.local.SavingsGoalDao
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.domain.model.SavingsContribution
import ir.dakhlokharj.domain.model.SavingsGoal
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavingsRepository(
    private val db: DkDatabase,
    private val goalDao: SavingsGoalDao,
    private val contributionDao: SavingsContributionDao,
    private val transactionRepository: TransactionRepository,
) {

    fun observeGoals(): Flow<List<SavingsGoal>> =
        goalDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeContributions(goalId: Long): Flow<List<SavingsContribution>> =
        contributionDao.observeForGoal(goalId).map { list -> list.map { it.toDomain() } }

    fun observeAllContributions(): Flow<List<SavingsContribution>> =
        contributionDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addGoal(goal: SavingsGoal): Long =
        goalDao.insert(goal.copy(createdAt = System.currentTimeMillis()).toEntity())

    suspend fun updateGoal(goal: SavingsGoal) = goalDao.update(goal.toEntity())

    suspend fun deleteGoal(goal: SavingsGoal) = goalDao.delete(goal.toEntity())

    /**
     * Moves money into a savings goal. When an account is selected the movement is stored as a
     * dedicated savings transaction (never as income/expense) so reports stay accurate.
     */
    suspend fun contribute(
        goalId: Long,
        amount: Money,
        date: JalaliDate,
        accountId: Long? = null,
    ): Long = db.withTransaction {
        require(amount.isPositive) { "مبلغ واریز باید مثبت باشد" }
        val goal = goalDao.getById(goalId)?.toDomain() ?: error("هدف پس‌انداز یافت نشد")
        val transactionId = if (accountId != null) {
            transactionRepository.add(
                Transaction(
                    type = TransactionType.SAVINGS_DEPOSIT,
                    amount = amount,
                    title = "واریز به هدف ${goal.title}",
                    accountId = accountId,
                    date = date,
                    savingsGoalId = goalId,
                ),
            )
        } else null
        contributionDao.insert(
            SavingsContribution(
                goalId = goalId,
                amount = amount,
                date = date,
                transactionId = transactionId,
                createdAt = System.currentTimeMillis(),
            ).toEntity(),
        )
        val newSaved = goal.saved + amount
        goalDao.update(
            goal.copy(saved = newSaved, achieved = newSaved >= goal.target).toEntity(),
        )
        transactionId ?: 0L
    }

    suspend fun withdraw(
        goalId: Long,
        amount: Money,
        date: JalaliDate,
        accountId: Long? = null,
    ): Long = db.withTransaction {
        require(amount.isPositive) { "مبلغ برداشت باید مثبت باشد" }
        val goal = goalDao.getById(goalId)?.toDomain() ?: error("هدف پس‌انداز یافت نشد")
        require(amount <= goal.saved) { "مبلغ برداشت از موجودی هدف بیشتر است" }
        val transactionId = if (accountId != null) {
            transactionRepository.add(
                Transaction(
                    type = TransactionType.SAVINGS_WITHDRAW,
                    amount = amount,
                    title = "برداشت از هدف ${goal.title}",
                    accountId = accountId,
                    date = date,
                    savingsGoalId = goalId,
                ),
            )
        } else null
        contributionDao.insert(
            SavingsContribution(
                goalId = goalId,
                amount = Money(-amount.rial),
                date = date,
                transactionId = transactionId,
                createdAt = System.currentTimeMillis(),
            ).toEntity(),
        )
        val newSaved = goal.saved - amount
        goalDao.update(
            goal.copy(saved = newSaved, achieved = newSaved >= goal.target).toEntity(),
        )
        transactionId ?: 0L
    }
}