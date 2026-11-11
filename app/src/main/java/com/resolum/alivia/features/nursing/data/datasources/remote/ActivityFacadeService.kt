package com.resolum.alivia.features.nursing.data.datasources.remote

import com.resolum.alivia.features.nursing.data.datasources.remote.services.ActivityService
import java.time.LocalDate

/** Entry point of the caregiver's care agenda. */
class ActivityFacadeService(private val service: ActivityService) {
    suspend fun agenda(from: LocalDate, to: LocalDate) = service.agenda(from.toString(), to.toString())
}
