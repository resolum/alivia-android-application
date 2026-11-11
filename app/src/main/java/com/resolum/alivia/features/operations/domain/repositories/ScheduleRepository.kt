package com.resolum.alivia.features.operations.domain.repositories

import com.resolum.alivia.features.operations.domain.models.WorkingDay
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.LocalDate

interface ScheduleRepository {
    suspend fun mySchedule(from: LocalDate, to: LocalDate): NetworkResult<List<WorkingDay>>
}
