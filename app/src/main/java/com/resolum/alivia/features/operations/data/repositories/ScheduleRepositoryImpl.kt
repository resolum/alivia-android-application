package com.resolum.alivia.features.operations.data.repositories

import com.resolum.alivia.features.operations.data.datasources.remote.ScheduleFacadeService
import com.resolum.alivia.features.operations.data.datasources.remote.mappers.WorkingDayMapper
import com.resolum.alivia.features.operations.domain.models.WorkingDay
import com.resolum.alivia.features.operations.domain.repositories.ScheduleRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import java.time.LocalDate

class ScheduleRepositoryImpl(private val facade: ScheduleFacadeService) : BaseRepository(), ScheduleRepository {
    override suspend fun mySchedule(from: LocalDate, to: LocalDate): NetworkResult<List<WorkingDay>> =
        safeCall { facade.mine(from, to).mapNotNull(WorkingDayMapper::toDomain).sortedBy { it.date } }
}
