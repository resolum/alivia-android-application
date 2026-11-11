package com.resolum.alivia.features.profiles.di

import com.resolum.alivia.features.profiles.data.datasources.remote.ProfileFacadeService
import com.resolum.alivia.features.profiles.data.datasources.remote.services.HouseholdService
import com.resolum.alivia.features.profiles.data.datasources.remote.services.ProfileService
import com.resolum.alivia.features.profiles.data.repositories.HouseholdRepositoryImpl
import com.resolum.alivia.features.profiles.data.repositories.ProfileRepositoryImpl
import com.resolum.alivia.features.profiles.domain.repositories.HouseholdRepository
import com.resolum.alivia.features.profiles.domain.repositories.ProfileRepository
import com.resolum.alivia.features.profiles.domain.usecases.GetMyHouseholdUseCase
import com.resolum.alivia.features.profiles.domain.usecases.GetProfileUseCase
import com.resolum.alivia.features.profiles.domain.usecases.UpdateProfileUseCase
import com.resolum.alivia.features.shared.di.SharedModule

object ProfileModule {
    private val repository: ProfileRepository by lazy {
        ProfileRepositoryImpl(ProfileFacadeService(SharedModule.retrofit.create(ProfileService::class.java)))
    }

    private val householdRepository: HouseholdRepository by lazy {
        HouseholdRepositoryImpl(SharedModule.retrofit.create(HouseholdService::class.java))
    }

    fun getProfile() = GetProfileUseCase(repository)
    fun updateProfile() = UpdateProfileUseCase(repository)
    fun getMyHousehold() = GetMyHouseholdUseCase(householdRepository)
}
