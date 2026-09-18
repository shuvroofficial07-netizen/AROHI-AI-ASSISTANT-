package com.example.data.repository

import com.example.data.local.dao.MemoryDao
import com.example.data.local.entity.MemoryEntity
import com.example.privacy.LocalVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoryRepository(
    private val memoryDao: MemoryDao,
    private val vault: LocalVault
) {
    /** Decrypted for the UI — values on disk may be encrypted when the user enabled it. */
    val allMemories: Flow<List<MemoryEntity>> =
        memoryDao.getAllMemories().map { memories -> memories.map { vault.decryptMemory(it) } }

    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>> {
        return memoryDao.getMemoriesByCategory(category)
            .map { memories -> memories.map { vault.decryptMemory(it) } }
    }

    /**
     * SQL LIKE cannot see through encryption, so when the vault is on we scan the (small) memory
     * table and match against the decrypted values instead.
     */
    suspend fun search(query: String): List<MemoryEntity> {
        val trimmed = query.trim()
        if (vault.isEnabled()) {
            val needle = trimmed.lowercase()
            return memoryDao.getAllMemoriesOnce()
                .map { vault.decryptMemory(it) }
                .filter {
                    needle.isEmpty() ||
                        it.key.lowercase().contains(needle) ||
                        it.value.lowercase().contains(needle)
                }
        }
        return memoryDao.searchMemories(trimmed).map { vault.decryptMemory(it) }
    }

    suspend fun getByKey(key: String): MemoryEntity? {
        return memoryDao.getMemoryByKey(key)?.let { vault.decryptMemory(it) }
    }

    suspend fun saveMemory(category: String, key: String, value: String): Long {
        return memoryDao.insertMemory(
            vault.encryptMemory(
                MemoryEntity(
                    category = category,
                    key = key.trim(),
                    value = value.trim()
                )
            )
        )
    }

    suspend fun deleteById(id: Int) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun deleteByKey(key: String): Int {
        return memoryDao.deleteMemoryByKey(key.trim())
    }

    suspend fun clearAll() {
        memoryDao.clearAll()
    }
}
