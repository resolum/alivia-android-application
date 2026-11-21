package com.resolum.alivia.features.nursing

import com.resolum.alivia.features.nursing.data.datasources.remote.services.MedicationAdministrationDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.MedicationAdministrationService
import com.resolum.alivia.features.nursing.data.datasources.remote.services.RecordMedicationOutcomeRequestDto
import com.resolum.alivia.features.nursing.data.repositories.MedicationAdministrationRepositoryImpl
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.CarePriority
import com.resolum.alivia.features.nursing.domain.models.MedicationAdministration
import com.resolum.alivia.features.nursing.domain.models.MedicationResult
import com.resolum.alivia.features.nursing.domain.usecases.MedicationRecord
import com.resolum.alivia.features.nursing.domain.usecases.RecordMedicationOutcomeUseCase
import com.resolum.alivia.features.nursing.presentation.activities.canRecordMedication
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MedicationAdministrationTest {
    private val date = LocalDate.of(2026, 11, 21)

    private val dose = CareActivity(
        id = "7-5-2026-11-21-08:00:00", activityId = 5, assignmentId = 7, title = "Enalapril 10 mg",
        type = CareActivityType.MEDICATION, assistedPerson = "Rosa", responsible = "Tú", date = date,
        startTime = LocalTime.of(8, 0), endTime = LocalTime.of(8, 30), status = CareActivityStatus.SCHEDULED,
        recurring = true, priority = CarePriority.HIGH, mine = true,
    )

    private class FakeService : MedicationAdministrationService {
        var outcome: MedicationAdministrationDto.OutcomeDto? = null
        var status = "SCHEDULED"
        var failFind: Exception? = null
        var failRecord: Exception? = null
        val recorded = mutableListOf<Pair<String, RecordMedicationOutcomeRequestDto>>()

        private fun dto(id: String) = MedicationAdministrationDto(
            administrationId = id, medication = "Enalapril 10 mg", assistedPerson = MedicationAdministrationDto.PersonDto(21, "Rosa"),
            date = "2026-11-21", startTime = "08:00:00", endTime = "08:30:00", status = status, outcome = outcome,
        )

        override suspend fun find(id: String): MedicationAdministrationDto {
            failFind?.let { throw it }
            return dto(id)
        }

        override suspend fun record(id: String, body: RecordMedicationOutcomeRequestDto): MedicationAdministrationDto {
            failRecord?.let { throw it }
            recorded += id to body
            outcome = MedicationAdministrationDto.OutcomeDto(body.result, "2026-11-21T13:05:00", null)
            status = if (body.result == "ADMINISTERED") "COMPLETED" else "OMITTED"
            return dto(id)
        }
    }

    private fun useCase(service: FakeService, at: LocalDateTime = LocalDateTime.of(date, LocalTime.of(8, 10))) =
        RecordMedicationOutcomeUseCase(MedicationAdministrationRepositoryImpl(service)) { at }

    @Test
    fun identifiesTheDoseAsTheBackendDoes() {
        assertEquals("7-20261121", MedicationAdministration.idOf(7, date))
    }

    @Test
    fun confirmsAPendingDoseWithTheRealTime() = runBlocking {
        val service = FakeService()
        val record = useCase(service)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.Recorded

        assertEquals("7-20261121", service.recorded.single().first)
        assertEquals("ADMINISTERED", service.recorded.single().second.result)
        assertEquals("2026-11-21T08:10", service.recorded.single().second.occurredAt)
        val outcome = record.administration.outcome!!
        assertEquals(MedicationResult.ADMINISTERED, outcome.result)
        assertEquals(LocalDateTime.of(date, LocalTime.of(8, 10)), outcome.occurredAt)
        assertEquals("Tú", outcome.recordedBy)
        assertFalse(record.late)
    }

    @Test
    fun aRecordAfterTheWindowIsLateAndKeepsItsTime() = runBlocking {
        val late = LocalDateTime.of(date, LocalTime.of(11, 0))
        val record = useCase(FakeService(), late)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.Recorded
        assertTrue(record.late)
        assertTrue(record.administration.recordedLate)
        assertEquals(late, record.administration.outcome?.occurredAt)
    }

    @Test
    fun anOmissionNeedsAReasonAndKeepsIt() = runBlocking {
        val service = FakeService()
        val withoutReason = useCase(service)(dose, MedicationResult.OMITTED, "  ")
        assertTrue(withoutReason is MedicationRecord.Rejected)
        assertTrue(service.recorded.isEmpty())

        val record = useCase(service)(dose, MedicationResult.OMITTED, " Se negó a tomarlo ") as MedicationRecord.Recorded
        assertEquals("Se negó a tomarlo", service.recorded.single().second.reason)
        assertEquals("Se negó a tomarlo", record.administration.outcome?.reason)
        assertEquals(MedicationResult.OMITTED, record.administration.outcome?.result)
    }

    @Test
    fun aDoseWithAResultIsNotConfirmedAgain() = runBlocking {
        val service = FakeService().apply {
            status = "COMPLETED"
            outcome = MedicationAdministrationDto.OutcomeDto("ADMINISTERED", "2026-11-21T08:05:00", null)
        }
        val record = useCase(service)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.AlreadyRecorded
        assertTrue(service.recorded.isEmpty())
        assertEquals(LocalDateTime.of(date, LocalTime.of(8, 5)), record.administration.outcome?.occurredAt)
    }

    @Test
    fun aConflictShowsTheResultThatWon() = runBlocking {
        // Another device recorded the dose between the check and the post.
        val service = object : MedicationAdministrationService {
            var calls = 0
            override suspend fun find(id: String): MedicationAdministrationDto {
                calls++
                val closed = calls > 1
                return MedicationAdministrationDto(
                    administrationId = id, date = "2026-11-21", startTime = "08:00:00", status = if (closed) "OMITTED" else "SCHEDULED",
                    outcome = if (closed) MedicationAdministrationDto.OutcomeDto("OMITTED", "2026-11-21T08:02:00", null) else null,
                )
            }
            override suspend fun record(id: String, body: RecordMedicationOutcomeRequestDto): MedicationAdministrationDto =
                throw httpError(409, """{"detail":"closed"}""")
        }
        val record = RecordMedicationOutcomeUseCase(MedicationAdministrationRepositoryImpl(service)) { LocalDateTime.now() }(
            dose, MedicationResult.ADMINISTERED,
        )
        assertEquals(MedicationResult.OMITTED, (record as MedicationRecord.AlreadyRecorded).administration.outcome?.result)
    }

    @Test
    fun aForbiddenRecordChangesNothing() = runBlocking {
        val service = FakeService().apply { failRecord = httpError(403, """{"detail":"denied"}""") }
        val record = useCase(service)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.Rejected
        assertTrue(record.forbidden)
        assertEquals(RecordMedicationOutcomeUseCase.FORBIDDEN, record.message)
        assertTrue(service.recorded.isEmpty())
    }

    @Test
    fun aMissedDoseIsClosedWithoutARecord() = runBlocking {
        val service = FakeService().apply {
            status = "MISSED"
            outcome = MedicationAdministrationDto.OutcomeDto("OMITTED", null, null)
        }
        val record = useCase(service)(dose, MedicationResult.ADMINISTERED) as MedicationRecord.AlreadyRecorded
        assertEquals(MedicationResult.MISSED, record.administration.outcome?.result)
        assertNull(record.administration.outcome?.occurredAt)
        assertTrue(service.recorded.isEmpty())
    }

    @Test
    fun onlyTheResponsibleOrThePrincipalRelativeRecords() {
        assertTrue(dose.canRecordMedication(isPrincipalRelative = false))
        val other = dose.copy(mine = false)
        assertFalse(other.canRecordMedication(isPrincipalRelative = false))
        assertTrue(other.canRecordMedication(isPrincipalRelative = true))
        assertFalse(dose.copy(type = CareActivityType.MEAL).canRecordMedication(isPrincipalRelative = true))
    }
}
