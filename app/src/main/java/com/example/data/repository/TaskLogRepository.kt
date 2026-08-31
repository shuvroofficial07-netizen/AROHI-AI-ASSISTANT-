package com.example.data.repository

import com.example.data.local.dao.TaskLogDao
import com.example.data.local.entity.TaskLogEntity
import kotlinx.coroutines.flow.Flow

class TaskLogRepository(private val taskLogDao: TaskLogDao) {
    val allTaskLogs: Flow<List<TaskLogEntity>> = taskLogDao.getAllTaskLogs()

    suspend fun getRecentTasks(limit: Int = 20): List<TaskLogEntity> {
        return taskLogDao.getRecentTasks(limit)
    }

    suspend fun addTask(taskName: String): Long {
        return taskLogDao.insertTaskLog(
            TaskLogEntity(
                taskName = taskName.trim(),
                status = STATUS_PENDING,
                stepsJson = "[]"
            )
        )
    }

    suspend fun markExecuting(id: Long) {
        taskLogDao.getById(id)?.let { taskLogDao.updateTaskLog(it.copy(status = STATUS_EXECUTING)) }
    }

    suspend fun markFinished(id: Long, success: Boolean, resultSummary: String) {
        taskLogDao.getById(id)?.let {
            taskLogDao.updateTaskLog(
                it.copy(
                    status = if (success) STATUS_COMPLETED else STATUS_FAILED,
                    resultSummary = resultSummary
                )
            )
        }
    }

    suspend fun deleteTask(id: Long) {
        taskLogDao.deleteTaskLogById(id)
    }

    suspend fun clearAll() {
        taskLogDao.clearAll()
    }

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_EXECUTING = "EXECUTING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
