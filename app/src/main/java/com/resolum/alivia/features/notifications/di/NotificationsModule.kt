package com.resolum.alivia.features.notifications.di

import com.resolum.alivia.features.notifications.data.NotificationRepositoryImpl
import com.resolum.alivia.features.notifications.data.NotificationService
import com.resolum.alivia.features.notifications.data.PushDeviceRepositoryImpl
import com.resolum.alivia.features.notifications.data.PushDeviceService
import com.resolum.alivia.features.notifications.domain.GetNotificationsUseCase
import com.resolum.alivia.features.notifications.domain.MarkNotificationsReadUseCase
import com.resolum.alivia.features.notifications.domain.NotificationRepository
import com.resolum.alivia.features.notifications.domain.PushDeviceRepository
import com.resolum.alivia.features.notifications.domain.PushTokenProvider
import com.resolum.alivia.features.notifications.domain.RegisterPushDeviceUseCase
import com.resolum.alivia.features.notifications.domain.UnregisterPushDeviceUseCase
import com.resolum.alivia.features.notifications.push.FirebasePushTokenProvider
import com.resolum.alivia.features.shared.di.SharedModule

object NotificationsModule {
    private val repository: NotificationRepository by lazy {
        NotificationRepositoryImpl(SharedModule.retrofit.create(NotificationService::class.java))
    }

    private val pushDevices: PushDeviceRepository by lazy {
        PushDeviceRepositoryImpl(SharedModule.retrofit.create(PushDeviceService::class.java))
    }

    private val pushTokens: PushTokenProvider by lazy { FirebasePushTokenProvider() }

    fun getNotifications() = GetNotificationsUseCase(repository)
    fun markRead() = MarkNotificationsReadUseCase(repository)
    fun registerPushDevice() = RegisterPushDeviceUseCase(pushTokens, pushDevices)
    fun unregisterPushDevice() = UnregisterPushDeviceUseCase(pushTokens, pushDevices)
}
