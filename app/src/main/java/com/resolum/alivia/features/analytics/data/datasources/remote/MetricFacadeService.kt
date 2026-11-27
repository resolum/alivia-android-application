package com.resolum.alivia.features.analytics.data.datasources.remote

import com.resolum.alivia.features.analytics.data.datasources.remote.services.MetricService
import java.time.LocalDate

/** Entry point of the home's autonomy metrics. */
class MetricFacadeService(private val service: MetricService) {
    suspend fun autonomy(from: LocalDate, to: LocalDate, deviceId: Long?) = service.autonomy(from.toString(), to.toString(), deviceId)
}
