package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.dao.TaskLogDao
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.TaskLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MemoryEntity::class,
        NotificationEntity::class,
        RoutineEntity::class,
        MessageEntity::class,
        TaskLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun routineDao(): RoutineDao
    abstract fun messageDao(): MessageDao
    abstract fun taskLogDao(): TaskLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arohi_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultData(database)
                    }
                }
            }

            private suspend fun populateDefaultData(database: AppDatabase) {
                val memoryDao = database.memoryDao()
                val routineDao = database.routineDao()

                // Default memory entries
                memoryDao.insertMemory(
                    MemoryEntity(
                        category = "PROFILE",
                        key = "creator",
                        value = "Shù Vrô (Shuvro)"
                    )
                )
                memoryDao.insertMemory(
                    MemoryEntity(
                        category = "PREFERENCES",
                        key = "primary_language",
                        value = "Bengali (বাংলা)"
                    )
                )
                memoryDao.insertMemory(
                    MemoryEntity(
                        category = "PREFERENCES",
                        key = "assistant_voice_persona",
                        value = "Arohi - warm, playful, witty, caring"
                    )
                )

                // Default routines
                routineDao.insertRoutine(
                    RoutineEntity(
                        name = "Start My Day",
                        description = "Checks battery, announces time, reads unread notifications and greets you",
                        triggerPhrase = "শুভ সকাল",
                        actionsJson = "[\"readDeviceState\", \"getNotifications\"]",
                        isEnabled = true,
                        iconName = "wb_sunny"
                    )
                )
                routineDao.insertRoutine(
                    RoutineEntity(
                        name = "Work Mode",
                        description = "Sets volume to low and enables quiet monitoring",
                        triggerPhrase = "কাজে বসছি",
                        actionsJson = "[\"setVolumeQuiet\"]",
                        isEnabled = true,
                        iconName = "work"
                    )
                )
                routineDao.insertRoutine(
                    RoutineEntity(
                        name = "Good Night",
                        description = "Announces battery level, turns off flashlight, silences interruptions",
                        triggerPhrase = "শুভ রাত্রি",
                        actionsJson = "[\"readDeviceState\", \"silence\"]",
                        isEnabled = true,
                        iconName = "nightlight"
                    )
                )
                routineDao.insertRoutine(
                    RoutineEntity(
                        name = "System Health Check",
                        description = "Runs live diagnostic checks across all assistant subsystems",
                        triggerPhrase = "সিস্টেম চেক করো",
                        actionsJson = "[\"diagnostics\"]",
                        isEnabled = true,
                        iconName = "health_and_safety"
                    )
                )
            }
        }
    }
}
