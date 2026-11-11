package com.resolum.alivia.features.operations.data.datasources.remote.mappers

import com.resolum.alivia.features.operations.data.datasources.remote.models.WorkingDayDto
import com.resolum.alivia.features.operations.domain.models.WorkingDay
import java.time.LocalDate
import java.time.LocalTime

object WorkingDayMapper {
    /** Null when the date is not valid; blocks with an invalid time are left out. */
    fun toDomain(dto: WorkingDayDto): WorkingDay? {
        val date = runCatching { LocalDate.parse(dto.date) }.getOrNull() ?: return null
        val shifts = dto.shifts.orEmpty().mapNotNull { shift ->
            val start = runCatching { LocalTime.parse(shift.start) }.getOrNull() ?: return@mapNotNull null
            val end = runCatching { LocalTime.parse(shift.end) }.getOrNull() ?: return@mapNotNull null
            WorkingDay.Shift(start, end)
        }.sortedBy { it.start }
        return WorkingDay(date, shifts)
    }
}
