package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TaskLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskLogDao {
    @Query("SELECT * FROM task_logs ORDER BY timestamp DESC")
    fun getAllTaskLogs(): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTasks(limit: Int = 10): List<TaskLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskLog(taskLog: TaskLogEntity): Long

    @Update
    suspend fun updateTaskLog(taskLog: TaskLogEntity)

    @Query("DELETE FROM task_logs WHERE id = :id")
    suspend fun deleteTaskLogById(id: Long)

    @Query("DELETE FROM task_logs")
    suspend fun clearAll()
}
