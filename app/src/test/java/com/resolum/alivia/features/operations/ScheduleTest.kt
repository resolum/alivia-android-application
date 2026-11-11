package com.resolum.alivia.features.operations

import com.resolum.alivia.features.operations.data.datasources.remote.ScheduleFacadeService
import com.resolum.alivia.features.operations.data.datasources.remote.mappers.WorkingDayMapper
import com.resolum.alivia.features.operations.data.datasources.remote.models.WorkingDayDto
import com.resolum.alivia.features.operations.data.datasources.remote.services.ScheduleService
import com.resolum.alivia.features.operations.data.repositories.ScheduleRepositoryImpl
import com.resolum.alivia.features.operations.domain.models.WorkingDay
import com.resolum.alivia.features.operations.domain.usecases.GetMyScheduleUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleTest {
    private class FakeService : ScheduleService {
        var asked: Pair<String, String>? = null
        var failWith: Exception? = null
        override suspend fun mine(from: String, to: String): List<WorkingDayDto> {
            asked = from to to
            failWith?.let { throw it }
            return listOf(
                WorkingDayDto("2026-11-11", listOf(WorkingDayDto.ShiftDto("14:00", "18:00"), WorkingDayDto.ShiftDto("08:00", "12:00"))),
                WorkingDayDto("2026-11-10", listOf(WorkingDayDto.ShiftDto("08:00", "bad"))),
                WorkingDayDto("bad", null),
            )
        }
    }

    @Test
    fun asksForThisWeekAndTheNextAndSortsTheDays() = runBlocking {
        val service = FakeService()
        val schedule = (GetMyScheduleUseCase(ScheduleRepositoryImpl(ScheduleFacadeService(service)))(LocalDate.of(2026, 11, 11))
            as NetworkResult.Success).data
        assertEquals("2026-11-09" to "2026-11-22", service.asked)
        assertEquals(LocalDate.of(2026, 11, 9), schedule.from)
        assertEquals(LocalDate.of(2026, 11, 22), schedule.to)
        assertEquals(listOf(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11)), schedule.days.map { it.date })
        assertEquals(emptyList<WorkingDay.Shift>(), schedule.days[0].shifts)
        assertEquals(LocalTime.of(8, 0), schedule.days[1].shifts.first().start)
        assertNull(WorkingDayMapper.toDomain(WorkingDayDto(null, null)))
    }

    @Test
    fun explainsThatOnlyTechniciansHaveASchedule() = runBlocking {
        val service = FakeService().apply { failWith = httpError(403) }
        val useCase = GetMyScheduleUseCase(ScheduleRepositoryImpl(ScheduleFacadeService(service)))
        assertEquals("Solo los técnicos tienen un horario asignado.", (useCase(LocalDate.of(2026, 11, 15)) as NetworkResult.Error).message)
        assertEquals("2026-11-09" to "2026-11-22", service.asked)
        service.failWith = httpError(500)
        assertEquals(500, (useCase(LocalDate.of(2026, 11, 15)) as NetworkResult.Error).code)
    }
}
