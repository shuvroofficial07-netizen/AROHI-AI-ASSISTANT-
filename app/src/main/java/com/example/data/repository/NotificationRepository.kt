package com.example.data.repository

import com.example.data.local.dao.NotificationDao
import com.example.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadNotifications: Flow<List<NotificationEntity>> = notificationDao.getUnreadNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun getRecentUnread(limit: Int = 10): List<NotificationEntity> {
        return notificationDao.getRecentUnread(limit)
    }

    suspend fun insertNotification(
        packageName: String,
        appName: String,
        title: String,
        text: String,
        subText: String = "",
        priority: Int = 0,
        key: String = ""
    ): Long {
        return notificationDao.insertNotification(
            NotificationEntity(
                packageName = packageName,
                appName = appName,
                title = title,
                text = text,
                subText = subText,
                priority = priority,
                notificationKey = key
            )
        )
    }

    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun markAsSpoken(id: Long) {
        notificationDao.markAsSpoken(id)
    }

    suspend fun deleteNotification(id: Long) {
        notificationDao.deleteNotificationById(id)
    }

    suspend fun clearAll() {
        notificationDao.clearAll()
    }
}
