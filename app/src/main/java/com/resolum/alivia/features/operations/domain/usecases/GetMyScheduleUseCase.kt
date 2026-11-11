package com.resolum.alivia.features.operations.domain.usecases

import com.resolum.alivia.features.operations.domain.models.WorkingDay
import com.resolum.alivia.features.operations.domain.repositories.ScheduleRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** The technician's working days of this week and the next one, Monday to Sunday. */
class GetMyScheduleUseCase(private val repository: ScheduleRepository) {
    data class Schedule(val from: LocalDate, val to: LocalDate, val days: List<WorkingDay>)

    suspend operator fun invoke(today: LocalDate): NetworkResult<Schedule> {
        val from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val to = from.plusDays(DAYS - 1)
        return when (val result = repository.mySchedule(from, to)) {
            is NetworkResult.Success -> NetworkResult.Success(Schedule(from, to, result.data))
            is NetworkResult.Error -> if (result.code == 403) {
                NetworkResult.Error("Solo los técnicos tienen un horario asignado.", 403)
            } else result
        }
    }

    companion object {
        const val DAYS = 14L
    }
}
