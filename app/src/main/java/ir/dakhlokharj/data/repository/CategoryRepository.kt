package ir.dakhlokharj.data.repository

import ir.dakhlokharj.data.local.CategoryDao
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val dao: CategoryDao) {

    fun observeAll(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Category> = dao.getAll().map { it.toDomain() }

    suspend fun add(category: Category): Long = dao.insert(category.toEntity())

    suspend fun update(category: Category) = dao.update(category.toEntity())

    suspend fun delete(category: Category) = dao.delete(category.toEntity())
}
