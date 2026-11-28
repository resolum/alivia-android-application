package com.resolum.alivia.features.tracking.di

import com.resolum.alivia.BuildConfig
import com.resolum.alivia.features.shared.di.SharedModule
import com.resolum.alivia.features.tracking.data.datasources.remote.HomeEventsSocket
import com.resolum.alivia.features.tracking.data.datasources.remote.services.HomeHistoryService
import com.resolum.alivia.features.tracking.data.repositories.HomeHistoryRepositoryImpl
import com.resolum.alivia.features.tracking.domain.repositories.HomeHistoryRepository
import com.resolum.alivia.features.tracking.domain.usecases.FollowHomeHistoryUseCase
import com.resolum.alivia.features.tracking.domain.usecases.GetHistoryDevicesUseCase
import com.resolum.alivia.features.tracking.domain.usecases.GetHomeHistoryUseCase

object HomeHistoryModule {
    private val repository: HomeHistoryRepository by lazy {
        HomeHistoryRepositoryImpl(
            SharedModule.retrofit.create(HomeHistoryService::class.java),
            HomeEventsSocket(BuildConfig.API_BASE_URL, SharedModule.tokenDataStore),
        )
    }

    fun getHistory() = GetHomeHistoryUseCase(repository)
    fun getDevices() = GetHistoryDevicesUseCase(repository)
    fun follow() = FollowHomeHistoryUseCase(repository)
}
