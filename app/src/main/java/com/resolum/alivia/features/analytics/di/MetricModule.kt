package com.resolum.alivia.features.analytics.di

import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.analytics.data.datasources.remote.MetricFacadeService
import com.resolum.alivia.features.analytics.data.datasources.remote.services.MetricService
import com.resolum.alivia.features.analytics.data.repositories.MetricRepositoryImpl
import com.resolum.alivia.features.analytics.domain.repositories.MetricRepository
import com.resolum.alivia.features.analytics.domain.usecases.GetMetricsUseCase
import com.resolum.alivia.features.shared.di.SharedModule

object MetricModule {
    private val repository: MetricRepository by lazy {
        MetricRepositoryImpl(MetricFacadeService(SharedModule.retrofit.create(MetricService::class.java)), Formats.zone)
    }

    fun getMetrics() = GetMetricsUseCase(repository)
}
