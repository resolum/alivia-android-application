package com.resolum.alivia.features.analytics.data.repositories

import com.resolum.alivia.features.analytics.data.datasources.remote.MetricFacadeService
import com.resolum.alivia.features.analytics.data.datasources.remote.mappers.MetricMapper
import com.resolum.alivia.features.analytics.domain.models.AutonomyMetrics
import com.resolum.alivia.features.analytics.domain.repositories.MetricRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import java.time.LocalDate
import java.time.ZoneId

class MetricRepositoryImpl(private val facade: MetricFacadeService, private val zone: ZoneId) : BaseRepository(), MetricRepository {
    override suspend fun autonomy(from: LocalDate, to: LocalDate, deviceId: Long?): NetworkResult<AutonomyMetrics> =
        when (val result = safeCall { MetricMapper.toDomain(facade.autonomy(from, to, deviceId), zone) }) {
            is NetworkResult.Success -> result
            is NetworkResult.Error -> if (result.code == 404) NetworkResult.Error(NOT_AVAILABLE, 404) else result
        }

    companion object {
        const val NOT_AVAILABLE = "Las métricas de autonomía todavía no están disponibles para esta vivienda."
    }
}
