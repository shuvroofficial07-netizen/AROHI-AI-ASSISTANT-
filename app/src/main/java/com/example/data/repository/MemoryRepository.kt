package com.example.data.repository

import com.example.data.local.dao.MemoryDao
import com.example.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>> {
        return memoryDao.getMemoriesByCategory(category)
    }

    suspend fun search(query: String): List<MemoryEntity> {
        return memoryDao.searchMemories(query)
    }

    suspend fun getByKey(key: String): MemoryEntity? {
        return memoryDao.getMemoryByKey(key)
    }

    suspend fun saveMemory(category: String, key: String, value: String): Long {
        return memoryDao.insertMemory(
            MemoryEntity(
                category = category,
                key = key.trim(),
                value = value.trim()
            )
        )
    }

    suspend fun deleteById(id: Int) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun deleteByKey(key: String): Int {
        return memoryDao.deleteMemoryByKey(key.trim())
    }

    /** Real row count — also used by diagnostics to prove the database is readable. */
    suspend fun count(): Int = memoryDao.countMemories()

    suspend fun clearAll() {
        memoryDao.clearAll()
    }
}
