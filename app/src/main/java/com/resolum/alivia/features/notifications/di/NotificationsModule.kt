package com.resolum.alivia.features.notifications.di

import com.resolum.alivia.features.notifications.data.NotificationRepositoryImpl
import com.resolum.alivia.features.notifications.data.NotificationService
import com.resolum.alivia.features.notifications.domain.GetNotificationsUseCase
import com.resolum.alivia.features.notifications.domain.MarkNotificationsReadUseCase
import com.resolum.alivia.features.notifications.domain.NotificationRepository
import com.resolum.alivia.features.shared.di.SharedModule

object NotificationsModule {
    private val repository: NotificationRepository by lazy {
        NotificationRepositoryImpl(SharedModule.retrofit.create(NotificationService::class.java))
    }

    fun getNotifications() = GetNotificationsUseCase(repository)
    fun markRead() = MarkNotificationsReadUseCase(repository)
}
