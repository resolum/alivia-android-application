package com.resolum.alivia.features.profiles.domain.usecases

import com.resolum.alivia.features.profiles.domain.repositories.HouseholdRepository

class GetMyHouseholdUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke() = repository.mine()
}
