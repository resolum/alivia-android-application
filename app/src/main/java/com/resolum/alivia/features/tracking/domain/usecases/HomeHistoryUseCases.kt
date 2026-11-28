package com.resolum.alivia.features.tracking.domain.usecases

import com.resolum.alivia.features.tracking.domain.repositories.HomeHistoryRepository

class GetHomeHistoryUseCase(private val repository: HomeHistoryRepository) {
    suspend operator fun invoke(personId: Long) = repository.recent(personId)
}

class GetHistoryDevicesUseCase(private val repository: HomeHistoryRepository) {
    suspend operator fun invoke(personId: Long) = repository.devices(personId)
}

class FollowHomeHistoryUseCase(private val repository: HomeHistoryRepository) {
    operator fun invoke(personId: Long) = repository.live(personId)
}
