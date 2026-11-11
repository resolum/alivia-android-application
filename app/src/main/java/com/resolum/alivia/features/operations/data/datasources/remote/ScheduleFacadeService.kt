package com.resolum.alivia.features.operations.data.datasources.remote

import com.resolum.alivia.features.operations.data.datasources.remote.services.ScheduleService
import java.time.LocalDate

/** Entry point of the technician's working schedule. */
class ScheduleFacadeService(private val service: ScheduleService) {
    suspend fun mine(from: LocalDate, to: LocalDate) = service.mine(from.toString(), to.toString())
}
