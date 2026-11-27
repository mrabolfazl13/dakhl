package ir.dakhlokharj.data.repository

import ir.dakhlokharj.data.local.FinancialEventDao
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.domain.model.FinancialEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(private val dao: FinancialEventDao) {

    fun observeAll(): Flow<List<FinancialEvent>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun add(event: FinancialEvent): Long =
        dao.insert(event.copy(createdAt = System.currentTimeMillis()).toEntity())

    suspend fun update(event: FinancialEvent) = dao.update(event.toEntity())

    suspend fun delete(event: FinancialEvent) = dao.delete(event.toEntity())
}
