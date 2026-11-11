package com.resolum.alivia.features.nursing.domain.usecases

import com.resolum.alivia.features.nursing.domain.repositories.CareNetworkRepository

class GetAssistedPersonsUseCase(private val repository: CareNetworkRepository) {
    suspend operator fun invoke() = repository.assistedPersons()
}

class GetCareNetworkUseCase(private val repository: CareNetworkRepository) {
    suspend operator fun invoke(householdId: Long) = repository.members(householdId)
}
