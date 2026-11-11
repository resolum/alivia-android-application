package com.resolum.alivia.features.nursing

import com.resolum.alivia.features.nursing.data.datasources.remote.ActivityFacadeService
import com.resolum.alivia.features.nursing.data.datasources.remote.mappers.ActivityMapper
import com.resolum.alivia.features.nursing.data.datasources.remote.models.ActivityResponseDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.ActivityService
import com.resolum.alivia.features.nursing.data.repositories.ActivityRepositoryImpl
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.CarePriority
import com.resolum.alivia.features.nursing.domain.usecases.GetActivitiesUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CareAgendaTest {
    private class FakeService : ActivityService {
        val asked = mutableListOf<Pair<String, String>>()
        var failWith: Exception? = null
        override suspend fun agenda(from: String, to: String): List<ActivityResponseDto> {
            asked += from to to
            failWith?.let { throw it }
            return listOf(
                ActivityResponseDto(
                    activity = ActivityResponseDto.ActivityDto(5, " Enalapril 10 mg ", "MEDICATION"),
                    assignmentId = 2,
                    assistedPerson = ActivityResponseDto.AssistedPersonDto(21, null),
                    date = "2026-11-11", startTime = "08:00:00", endTime = "08:30:00",
                    responsible = ActivityResponseDto.ResponsibleDto("CAREGIVER", 13, null, null),
                    recurring = true, status = "COMPLETED", priority = "HIGH",
                ),
                ActivityResponseDto(date = "2026-11-12", startTime = "21:00", endTime = "nope",
                    responsible = ActivityResponseDto.ResponsibleDto("RELATIVE", 4, " ", "lucia@alivia.test"),
                    status = "WHATEVER", priority = "?", activity = ActivityResponseDto.ActivityDto(null, " ", "SPACEWALK")),
                ActivityResponseDto(date = "bad", startTime = "08:00"),
            )
        }
    }

    @Test
    fun mapsTheAgendaWithDefaultsForUnknownValues() = runBlocking {
        val service = FakeService()
        val useCase = GetActivitiesUseCase(ActivityRepositoryImpl(ActivityFacadeService(service)))
        val activities = (useCase(LocalDate.of(2026, 11, 11)) as NetworkResult.Success).data
        assertEquals("2026-11-10" to "2026-11-13", service.asked.last())
        assertEquals(2, activities.size)

        val first = activities[0]
        assertEquals("2-5-2026-11-11-08:00:00", first.id)
        assertEquals(5L, first.activityId)
        assertEquals("Enalapril 10 mg", first.title)
        assertEquals(CareActivityType.MEDICATION, first.type)
        assertEquals(CareActivityStatus.COMPLETED, first.status)
        assertEquals(CarePriority.HIGH, first.priority)
        assertEquals(21L, first.assistedPersonProfileId)
        assertEquals(13L, first.responsibleCaregiverId)
        assertNull(first.assistedPerson)
        assertNull(first.responsible)
        assertTrue(first.recurring)
        assertEquals(LocalTime.of(8, 30), first.endTime)

        val second = activities[1]
        assertEquals("Actividad", second.title)
        assertEquals(CareActivityType.OTHER, second.type)
        assertEquals(CareActivityStatus.SCHEDULED, second.status)
        assertEquals(CarePriority.ROUTINE, second.priority)
        assertEquals("lucia@alivia.test", second.responsible)
        assertNull(second.responsibleCaregiverId)
        assertNull(second.endTime)
        assertFalse(second.recurring)

        useCase.between(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
        assertEquals("2026-11-01" to "2026-11-30", service.asked.last())
        assertNull(ActivityMapper.toDomain(ActivityResponseDto(date = "2026-11-11", startTime = null)))
    }

    @Test
    fun passesFailuresThrough() = runBlocking {
        val service = FakeService().apply { failWith = httpError(500) }
        assertEquals(500, (GetActivitiesUseCase(ActivityRepositoryImpl(ActivityFacadeService(service)))(LocalDate.of(2026, 11, 11))
            as NetworkResult.Error).code)
    }
}
