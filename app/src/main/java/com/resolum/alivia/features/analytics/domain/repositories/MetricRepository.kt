package com.resolum.alivia.features.analytics.domain.repositories

import com.resolum.alivia.features.analytics.domain.models.AutonomyMetrics
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.LocalDate

interface MetricRepository {
    suspend fun autonomy(from: LocalDate, to: LocalDate, deviceId: Long?): NetworkResult<AutonomyMetrics>
}
