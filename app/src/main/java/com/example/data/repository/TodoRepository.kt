package com.example.data.repository

import com.example.data.local.dao.TodoDao
import com.example.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()

    suspend fun getPendingTodos(): List<TodoEntity> = todoDao.getPendingTodos()

    suspend fun findOpenByTitle(query: String): TodoEntity? = todoDao.findOpenByTitle(query)

    suspend fun getById(id: Long): TodoEntity? = todoDao.getById(id)

    suspend fun addTodo(title: String, details: String = "", dueAt: Long? = null, priority: Int = 1): Long {
        return todoDao.insertTodo(
            TodoEntity(
                title = title.trim(),
                details = details.trim(),
                dueAt = dueAt,
                priority = priority
            )
        )
    }

    suspend fun toggleTodo(id: Long) {
        todoDao.getById(id)?.let { todo ->
            val nowDone = !todo.isDone
            todoDao.updateTodo(
                todo.copy(
                    isDone = nowDone,
                    completedAt = if (nowDone) System.currentTimeMillis() else null
                )
            )
        }
    }

    suspend fun completeByTitle(query: String): Boolean {
        val todo = todoDao.findOpenByTitle(query) ?: return false
        todoDao.updateTodo(todo.copy(isDone = true, completedAt = System.currentTimeMillis()))
        return true
    }

    suspend fun updateTodo(todo: TodoEntity) = todoDao.updateTodo(todo)

    suspend fun deleteTodo(id: Long) = todoDao.deleteById(id)

    suspend fun clearCompleted() = todoDao.deleteCompleted()

    suspend fun clearAll() = todoDao.clearAll()
}
