package ir.dakhlokharj.data.repository

import ir.dakhlokharj.data.local.BudgetDao
import ir.dakhlokharj.data.local.EnvelopeDao
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.Envelope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val envelopeDao: EnvelopeDao,
) {

    fun observeAll(): Flow<List<Budget>> = budgetDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActiveOn(epochDay: Long): Flow<List<Budget>> =
        budgetDao.observeActiveOn(epochDay).map { list -> list.map { it.toDomain() } }

    fun observeEnvelopes(): Flow<List<Envelope>> =
        envelopeDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun add(budget: Budget): Long = budgetDao.insert(budget.toEntity())

    suspend fun update(budget: Budget) = budgetDao.update(budget.toEntity())

    suspend fun delete(budget: Budget) = budgetDao.delete(budget.toEntity())

    suspend fun repeatNextMonth(budget: Budget): Long {
        val copy = budget.copy(
            id = 0,
            startDate = budget.startDate.plusMonths(1),
            endDate = budget.endDate.plusMonths(1),
            createdAt = System.currentTimeMillis(),
        )
        return budgetDao.insert(copy.toEntity())
    }

    suspend fun addEnvelope(envelope: Envelope): Long = envelopeDao.insert(envelope.toEntity())

    suspend fun updateEnvelope(envelope: Envelope) = envelopeDao.update(envelope.toEntity())

    suspend fun deleteEnvelope(envelope: Envelope) = envelopeDao.delete(envelope.toEntity())
}
