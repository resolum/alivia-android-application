package com.resolum.alivia.features.nursing

import com.resolum.alivia.features.nursing.data.datasources.remote.mappers.MedicationAdministrationMapper
import com.resolum.alivia.features.nursing.data.datasources.remote.services.MedicationAdministrationDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.MedicationAdministrationService
import com.resolum.alivia.features.nursing.data.datasources.remote.services.RecordMedicationOutcomeRequestDto
import com.resolum.alivia.features.nursing.data.repositories.MedicationAdministrationRepositoryImpl
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.CarePriority
import com.resolum.alivia.features.nursing.domain.models.MedicationResult
import com.resolum.alivia.features.nursing.domain.usecases.GetMedicationAdministrationUseCase
import com.resolum.alivia.features.nursing.domain.usecases.MedicationRecord
import com.resolum.alivia.features.nursing.domain.usecases.RecordMedicationOutcomeUseCase
import com.resolum.alivia.features.nursing.reminders.ReminderKind
import com.resolum.alivia.features.nursing.reminders.ReminderPlanner
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class MedicationEdgeCasesTest {
    private val date = LocalDate.of(2026, 11, 21)

    private val dose = CareActivity(
        id = "7-dose", activityId = 5, assignmentId = 7, title = "Enalapril", type = CareActivityType.MEDICATION,
        assistedPerson = null, responsible = null, date = date, startTime = LocalTime.of(23, 30), endTime = LocalTime.of(0, 30),
        status = CareActivityStatus.SCHEDULED, recurring = false, priority = CarePriority.HIGH, mine = true,
    )

    private fun dto(
        id: String? = "7-20261121", date: String? = "2026-11-21", start: String? = "08:00:00", end: String? = null,
        status: String? = null, outcome: MedicationAdministrationDto.OutcomeDto? = null,
    ) = MedicationAdministrationDto(
        administrationId = id, medication = " ", assistedPerson = MedicationAdministrationDto.PersonDto(1, " "),
        date = date, startTime = start, endTime = end, responsible = MedicationAdministrationDto.ResponsibleDto("CAREGIVER", 3, " Ana "),
        status = status, outcome = outcome,
    )

    /** Answers each call with the next prepared answer: a DTO or an exception. */
    private class ScriptedService(finds: List<Any>, private val record: Any = Unit) : MedicationAdministrationService {
        private val finds = finds.toMutableList()
        var recorded = 0
        override suspend fun find(id: String) = answer(finds.removeAt(0))
        override suspend fun record(id: String, body: RecordMedicationOutcomeRequestDto): MedicationAdministrationDto {
            recorded++
            return answer(record)
        }
        private fun answer(value: Any): MedicationAdministrationDto = when (value) {
            is Exception -> throw value
            else -> value as MedicationAdministrationDto
        }
    }

    private fun record(service: MedicationAdministrationService) =
        RecordMedicationOutcomeUseCase(MedicationAdministrationRepositoryImpl(service)) { LocalDateTime.of(date, LocalTime.of(23, 45)) }

    @Test
    fun mapsDefaultsAndDropsIncompleteDoses() {
        val mapped = MedicationAdministrationMapper.toDomain(dto(end = "bad", status = "WHATEVER"))!!
        assertEquals("Medicamento", mapped.medication)
        assertNull(mapped.assistedPerson)
        assertEquals("Ana", mapped.responsible)
        assertNull(mapped.endTime)
        assertEquals(CareActivityStatus.SCHEDULED, mapped.status)
        assertNull(mapped.outcome)
        assertEquals(LocalDateTime.of(date, LocalTime.of(9, 0)), mapped.windowEnd)

        assertNull(MedicationAdministrationMapper.toDomain(dto(id = " ")))
        assertNull(MedicationAdministrationMapper.toDomain(dto(id = null)))
        assertNull(MedicationAdministrationMapper.toDomain(dto(date = "nope")))
        assertNull(MedicationAdministrationMapper.toDomain(dto(start = null)))
        val bare = MedicationAdministrationMapper.toDomain(MedicationAdministrationDto("1-20261121", date = "2026-11-21", startTime = "08:00"))!!
        assertNull(bare.assistedPerson)
        assertNull(bare.responsible)
    }

    @Test
    fun mapsOutcomesAndIgnoresUnknownResults() {
        val unknown = MedicationAdministrationMapper.toDomain(dto(outcome = MedicationAdministrationDto.OutcomeDto("LOST", "bad", " ")))!!
        assertNull(unknown.outcome)
        assertTrue(unknown.pending)

        val omitted = MedicationAdministrationMapper.toDomain(
            dto(status = "OMITTED", outcome = MedicationAdministrationDto.OutcomeDto("OMITTED", "bad", " no ")),
        )!!
        assertEquals(MedicationResult.OMITTED, omitted.outcome?.result)
        assertNull(omitted.outcome?.occurredAt)
        assertEquals("no", omitted.outcome?.reason)
        assertFalse(omitted.recordedLate)

        val missed = MedicationAdministrationMapper.toDomain(dto(status = "MISSED"))!!
        assertFalse(missed.pending)
    }

    @Test
    fun aWindowCanCrossMidnight() {
        val crossing = MedicationAdministrationMapper.toDomain(dto(start = "23:30:00", end = "00:30:00"))!!
        assertEquals(LocalDateTime.of(date.plusDays(1), LocalTime.of(0, 30)), crossing.windowEnd)
        assertFalse(crossing.isLate(LocalDateTime.of(date.plusDays(1), LocalTime.of(0, 10))))
    }

    @Test
    fun anIncompleteAnswerIsAnError() = runBlocking {
        val result = MedicationAdministrationRepositoryImpl(ScriptedService(listOf(dto(id = null)))).find("x")
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun readsTheDoseOfAnOccurrence() = runBlocking {
        val get = { service: MedicationAdministrationService -> GetMedicationAdministrationUseCase(MedicationAdministrationRepositoryImpl(service)) }
        assertTrue(get(ScriptedService(listOf(dto())))(dose) is NetworkResult.Success)
        assertEquals("No encontramos esta administración.", (get(ScriptedService(listOf(httpError(404))))(dose) as NetworkResult.Error).message)
        assertEquals(403, (get(ScriptedService(listOf(httpError(403))))(dose) as NetworkResult.Error).code)
        assertTrue(get(ScriptedService(emptyList()))(dose.copy(assignmentId = null)) is NetworkResult.Error)
    }

    @Test
    fun rejectsWhatCannotBeRecorded() = runBlocking {
        val service = ScriptedService(emptyList())
        assertTrue(record(service)(dose.copy(assignmentId = null), MedicationResult.ADMINISTERED) is MedicationRecord.Rejected)
        assertTrue(record(service)(dose, MedicationResult.MISSED) is MedicationRecord.Rejected)
        assertTrue(record(service)(dose, MedicationResult.OMITTED, null) is MedicationRecord.Rejected)
        assertEquals(0, service.recorded)
    }

    @Test
    fun aFailedCheckRecordsNothing() = runBlocking {
        val notFound = ScriptedService(listOf(httpError(404)))
        assertEquals("No encontramos esta administración.", (record(notFound)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.Rejected).message)
        val forbidden = ScriptedService(listOf(httpError(403)))
        assertTrue((record(forbidden)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.Rejected).forbidden)
        val offline = ScriptedService(listOf(IOException()))
        assertFalse((record(offline)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.Rejected).forbidden)
        assertEquals(0, notFound.recorded + forbidden.recorded + offline.recorded)
    }

    @Test
    fun aConflictWithoutAVisibleResultIsReportedAsRecorded() = runBlocking {
        val stillPending = ScriptedService(listOf(dto(), dto()), record = httpError(409))
        val rejected = record(stillPending)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.Rejected
        assertEquals("Esta administración ya tiene un resultado registrado.", rejected.message)

        val checkFails = ScriptedService(listOf(dto(), IOException()), record = httpError(409))
        assertTrue(record(checkFails)(dose, MedicationResult.ADMINISTERED) is MedicationRecord.Rejected)

        val serverError = ScriptedService(listOf(dto()), record = httpError(500))
        assertFalse((record(serverError)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.Rejected).forbidden)
    }

    @Test
    fun keepsTheBackendReasonWhenNoneWasTyped() = runBlocking {
        val saved = dto(status = "COMPLETED", outcome = MedicationAdministrationDto.OutcomeDto("ADMINISTERED", null, "con agua"))
        val recorded = record(ScriptedService(listOf(dto()), record = saved))(dose, MedicationResult.ADMINISTERED) as MedicationRecord.Recorded
        assertEquals("con agua", recorded.administration.outcome?.reason)
        assertTrue(recorded.late)
    }

    @Test
    fun plansAroundMidnightAndWithoutNames() {
        val zone = ZoneId.of("America/Lima")
        val now = LocalDateTime.of(date, LocalTime.of(20, 0))
        val plan = ReminderPlanner.plan(listOf(dose, dose.copy(id = "open", endTime = null, assignmentId = null)), now, zone)
        val alert = plan.single { it.kind == ReminderKind.MEDICATION_ALERT }
        assertEquals(LocalDateTime.of(date.plusDays(1), LocalTime.of(0, 31)).atZone(zone).toInstant().toEpochMilli(), alert.triggerAt)
        assertFalse(alert.body.contains("para"))
        assertEquals(3, plan.size)

        val routine = ReminderPlanner.plan(listOf(dose.copy(id = "r", type = CareActivityType.MEAL, endTime = null)), now, zone).single()
        assertEquals("Recordatorio de actividad", routine.title)
        assertFalse(routine.body.contains("–"))
    }
}
