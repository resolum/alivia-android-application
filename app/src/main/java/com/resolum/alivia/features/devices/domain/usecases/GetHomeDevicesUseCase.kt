package com.resolum.alivia.features.devices.domain.usecases

import com.resolum.alivia.features.devices.domain.repositories.HomeDeviceRepository

class GetHomeDevicesUseCase(private val repository: HomeDeviceRepository) {
    suspend operator fun invoke() = repository.mine()
}
