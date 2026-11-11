package com.resolum.alivia.features.nursing.di

import com.resolum.alivia.features.nursing.data.datasources.remote.ActivityFacadeService
import com.resolum.alivia.features.nursing.data.datasources.remote.services.ActivityService
import com.resolum.alivia.features.nursing.data.datasources.remote.services.CareNetworkService
import com.resolum.alivia.features.nursing.data.repositories.ActivityRepositoryImpl
import com.resolum.alivia.features.nursing.data.repositories.CareNetworkRepositoryImpl
import com.resolum.alivia.features.nursing.domain.repositories.CareNetworkRepository
import com.resolum.alivia.features.nursing.domain.usecases.GetAssistedPersonsUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetCareNetworkUseCase
import com.resolum.alivia.features.nursing.domain.repositories.ActivityRepository
import com.resolum.alivia.features.nursing.domain.usecases.GetActivitiesUseCase
import com.resolum.alivia.features.shared.di.SharedModule

object ActivityModule {
    private val repository: ActivityRepository by lazy {
        ActivityRepositoryImpl(ActivityFacadeService(SharedModule.retrofit.create(ActivityService::class.java)))
    }

    private val careNetworkRepository: CareNetworkRepository by lazy {
        CareNetworkRepositoryImpl(SharedModule.retrofit.create(CareNetworkService::class.java))
    }

    fun getActivities() = GetActivitiesUseCase(repository)
    fun getAssistedPersons() = GetAssistedPersonsUseCase(careNetworkRepository)
    fun getCareNetwork() = GetCareNetworkUseCase(careNetworkRepository)
}
