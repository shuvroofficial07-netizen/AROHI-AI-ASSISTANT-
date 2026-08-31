package com.example.data.repository

import com.example.data.local.dao.RoutineDao
import com.example.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val routineDao: RoutineDao) {
    val allRoutines: Flow<List<RoutineEntity>> = routineDao.getAllRoutines()
    val enabledRoutines: Flow<List<RoutineEntity>> = routineDao.getEnabledRoutines()

    suspend fun getRoutineById(id: Int): RoutineEntity? = routineDao.getRoutineById(id)

    suspend fun findByTrigger(phrase: String): RoutineEntity? = routineDao.findRoutineByTrigger(phrase)

    suspend fun addRoutine(name: String, description: String, trigger: String, actionsJson: String, icon: String = "routine"): Long {
        return routineDao.insertRoutine(
            RoutineEntity(
                name = name,
                description = description,
                triggerPhrase = trigger,
                actionsJson = actionsJson,
                iconName = icon
            )
        )
    }

    suspend fun toggleRoutine(id: Int, enabled: Boolean) {
        routineDao.toggleRoutine(id, enabled)
    }

    suspend fun updateRoutine(
        id: Int,
        name: String,
        description: String,
        trigger: String,
        actionsJson: String,
        icon: String
    ) {
        val existing = routineDao.getRoutineById(id) ?: return
        routineDao.updateRoutine(
            existing.copy(
                name = name.ifBlank { existing.name },
                description = description.ifBlank { existing.description },
                triggerPhrase = trigger.ifBlank { existing.triggerPhrase },
                actionsJson = actionsJson.ifBlank { existing.actionsJson },
                iconName = icon.ifBlank { existing.iconName }
            )
        )
    }

    suspend fun deleteRoutine(id: Int) {
        routineDao.deleteRoutineById(id)
    }
}
