package com.example.data.repository

import com.example.data.local.dao.NoteDao
import com.example.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun getNotes(): List<NoteEntity> = noteDao.getNotes()

    suspend fun search(query: String): List<NoteEntity> = noteDao.searchNotes(query)

    suspend fun addNote(title: String, content: String): Long {
        val cleanTitle = title.trim().ifBlank {
            content.trim().take(40).ifBlank { "নোট" }
        }
        return noteDao.insertNote(
            NoteEntity(
                title = cleanTitle,
                content = content.trim()
            )
        )
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: Long) = noteDao.deleteById(id)

    suspend fun clearAll() = noteDao.clearAll()
}
