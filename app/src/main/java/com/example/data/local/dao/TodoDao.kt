package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isDone ASC, priority DESC, createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isDone = 0 ORDER BY priority DESC, createdAt DESC")
    suspend fun getPendingTodos(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE isDone = 0 AND title LIKE '%' || :query || '%' LIMIT 1")
    suspend fun findOpenByTitle(query: String): TodoEntity?

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM todos WHERE isDone = 1")
    suspend fun deleteCompleted()

    @Query("DELETE FROM todos")
    suspend fun clearAll()
}
