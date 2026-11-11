package com.resolum.alivia.features.devices.di

import com.resolum.alivia.features.devices.data.datasources.remote.services.HomeDeviceService
import com.resolum.alivia.features.devices.data.repositories.HomeDeviceRepositoryImpl
import com.resolum.alivia.features.devices.domain.repositories.HomeDeviceRepository
import com.resolum.alivia.features.devices.domain.usecases.GetHomeDevicesUseCase
import com.resolum.alivia.features.shared.di.SharedModule

object DevicesModule {
    private val repository: HomeDeviceRepository by lazy {
        HomeDeviceRepositoryImpl(SharedModule.retrofit.create(HomeDeviceService::class.java))
    }

    fun getHomeDevices() = GetHomeDevicesUseCase(repository)
}
