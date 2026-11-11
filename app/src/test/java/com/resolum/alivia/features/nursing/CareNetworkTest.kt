package com.resolum.alivia.features.nursing

import com.resolum.alivia.features.nursing.data.datasources.remote.services.AssignCareActivityRequestDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.AssistedPersonDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.CareNetworkService
import com.resolum.alivia.features.nursing.data.datasources.remote.services.CaregiverDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.CompleteActivityRequestDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.CreateCareActivityRequestDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.HouseholdMemberDto
import com.resolum.alivia.features.nursing.data.repositories.CareNetworkRepositoryImpl
import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.CarePriority
import com.resolum.alivia.features.nursing.domain.models.CareRole
import com.resolum.alivia.features.nursing.domain.usecases.CompleteActivityUseCase
import com.resolum.alivia.features.nursing.domain.usecases.CreateCareActivityUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetAssistedPersonsUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetCareNetworkUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetCaregiversUseCase
import com.resolum.alivia.features.nursing.domain.usecases.NameAgendaPeopleUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class CareNetworkTest {
    private class FakeService : CareNetworkService {
        var failWith: Exception? = null
        var completeCode = 201
        val created = mutableListOf<CreateCareActivityRequestDto>()
        val assigned = mutableListOf<AssignCareActivityRequestDto>()
        val completed = mutableListOf<CompleteActivityRequestDto>()
        var assignFails: Exception? = null

        override suspend fun assistedPersons(): List<AssistedPersonDto> {
            failWith?.let { throw it }
            return listOf(
                AssistedPersonDto(11, 11, 21, 7, " Elena ", "Salazar", "73000001", "1940-07-30", "51930000001", "PARENT", "PARTIAL",
                    "Lucia Salazar", "51910000001"),
                AssistedPersonDto(null),
                AssistedPersonDto(12, null, null, null, null, null, " ", "bad", " ", "OTHER", null, " ", null),
            )
        }

        override suspend fun myCaregivers(): List<CaregiverDto> {
            failWith?.let { throw it }
            return listOf(CaregiverDto(13, 38, "ACTIVE"))
        }

        override suspend fun caregiversOf(assistedPersonId: Long): List<CaregiverDto> {
            failWith?.let { throw it }
            return listOf(CaregiverDto(12, 18, "ACTIVE"), CaregiverDto(13, 38, null), CaregiverDto(14, 48, "REVOKED"), CaregiverDto(null))
        }

        override suspend fun createActivity(body: CreateCareActivityRequestDto): Long {
            failWith?.let { throw it }
            created += body
            return 9
        }

        override suspend fun assign(activityId: Long, body: AssignCareActivityRequestDto): Long {
            assignFails?.let { throw it }
            assigned += body
            return 4
        }

        override suspend fun complete(body: CompleteActivityRequestDto): Response<Long> {
            completed += body
            return if (completeCode < 300) Response.success(1L)
            else Response.error(completeCode, "{}".toResponseBody("application/json".toMediaType()))
        }

        override suspend fun members(householdId: Long): List<HouseholdMemberDto> {
            failWith?.let { throw it }
            return listOf(
                HouseholdMemberDto(38, "CAREGIVER", "ACTIVE", "Farid Coronel"),
                HouseholdMemberDto(8, "PRINCIPAL", "ACTIVE", " Lucia Salazar "),
                HouseholdMemberDto(18, "CAREGIVER", null, " "),
                HouseholdMemberDto(28, "GUEST", "REVOKED", "Old guest"),
                HouseholdMemberDto(29, "ROBOT", "ACTIVE", null),
                HouseholdMemberDto(null, "GUEST", "ACTIVE", null),
            )
        }
    }

    private val today = LocalDate.of(2026, 11, 11)

    private fun activity(status: CareActivityStatus = CareActivityStatus.IN_PROGRESS, activityId: Long? = 5, assignmentId: Long? = 2) =
        CareActivity(
            id = "id", activityId = activityId, assignmentId = assignmentId, title = "Enalapril", type = CareActivityType.MEDICATION,
            assistedPerson = null, responsible = null, assistedPersonProfileId = 21, responsibleCaregiverId = 13, date = today,
            startTime = LocalTime.of(8, 0), endTime = LocalTime.of(8, 30), status = status, recurring = true, priority = CarePriority.HIGH,
        )

    @Test
    fun mapsAssistedPersonsAndTheirLabels() = runBlocking {
        val repository = CareNetworkRepositoryImpl(FakeService())
        val persons = (GetAssistedPersonsUseCase(repository)() as NetworkResult.Success).data
        assertEquals(2, persons.size)
        val elena = persons[0]
        assertEquals("Elena Salazar", elena.fullName)
        assertEquals("ES", elena.initials)
        assertEquals(86, elena.age(today))
        assertEquals("Madre o padre", elena.kinshipLabel)
        assertEquals("Movilidad parcial", elena.mobilityLabel)
        assertEquals(21L, elena.personProfileId)
        val other = persons[1]
        assertNull(other.age(today))
        assertNull(other.dni)
        assertNull(other.emergencyContactName)
        assertEquals("Otro", other.kinshipLabel)
        assertNull(other.mobilityLabel)
        listOf("GRANDPARENT" to "Abuela o abuelo", "AUNT_UNCLE" to "Tía o tío", "SPOUSE" to "Cónyuge", "SIBLING" to "Hermana o hermano",
            "X" to null).forEach { (kinship, label) -> assertEquals(label, elena.copy(kinship = kinship).kinshipLabel) }
        listOf("VERY_LIMITED" to "Movilidad muy limitada", "NO_LIMB_MOBILITY" to "Sin movilidad de extremidades")
            .forEach { (level, label) -> assertEquals(label, elena.copy(mobilityLevel = level).mobilityLabel) }
    }

    @Test
    fun listsActiveMembersAndCaregiversWithTheirNames() = runBlocking {
        val repository = CareNetworkRepositoryImpl(FakeService())
        val members = (GetCareNetworkUseCase(repository)(7) as NetworkResult.Success).data
        assertEquals(listOf(CareRole.PRINCIPAL, CareRole.CAREGIVER, CareRole.CAREGIVER), members.map { it.role })
        assertEquals("Lucia Salazar", members[0].fullName)
        assertNull(members[2].fullName)

        val elena = (GetAssistedPersonsUseCase(repository)() as NetworkResult.Success).data[0]
        val caregivers = (GetCaregiversUseCase(repository)(elena) as NetworkResult.Success).data
        assertEquals(listOf(13L, 12L), caregivers.map { it.id })
        assertTrue(caregivers[0].isMe)
        assertEquals("Farid Coronel", caregivers[0].fullName)
        assertNull(caregivers[1].fullName)
        assertEquals(setOf(13L), (repository.myCaregiverIds() as NetworkResult.Success).data)
    }

    @Test
    fun namesTheResponsibleAndTheAssistedPerson() = runBlocking {
        val repository = CareNetworkRepositoryImpl(FakeService())
        val named = NameAgendaPeopleUseCase(repository)(listOf(
            activity(),
            activity().copy(responsibleCaregiverId = 12),
            activity().copy(responsibleCaregiverId = 99, assistedPersonProfileId = 0),
            activity().copy(responsible = "Ya tiene", responsibleCaregiverId = null),
        ))
        assertEquals(listOf("Tú", "Otro cuidador", "Otro cuidador", "Ya tiene"), named.map { it.responsible })
        assertEquals("Elena Salazar", named[0].assistedPerson)
        assertNull(named[2].assistedPerson)
    }

    @Test
    fun createsOneTimeAndWeeklyActivities() = runBlocking {
        val service = FakeService()
        val repository = CareNetworkRepositoryImpl(service)
        val elena = (GetAssistedPersonsUseCase(repository)() as NetworkResult.Success).data[0]
        val create = CreateCareActivityUseCase(repository)

        val once = create(elena, CareActivityType.THERAPY, " Terapia ", "  ", 13, today.plusDays(1), emptySet(),
            LocalTime.of(23, 0), LocalTime.of(0, 0), today)
        assertEquals(NetworkResult.Success(9L), once)
        assertEquals(CreateCareActivityRequestDto(11, 11, "THERAPY", "Terapia", null), service.created.last())
        assertEquals(AssignCareActivityRequestDto(11, 13, "ONE_TIME", null, "2026-11-12", "23:00", "00:00"), service.assigned.last())

        create(elena, CareActivityType.MEAL, "Almuerzo", "Sin sal", 12, today.minusDays(3),
            setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY), LocalTime.of(13, 30), LocalTime.of(14, 0), today)
        assertEquals(AssignCareActivityRequestDto(11, 12, "WEEKLY", listOf("MONDAY", "FRIDAY"), null, "13:30", "14:00"),
            service.assigned.last())
        assertEquals("Sin sal", service.created.last().description)
    }

    @Test
    fun validatesTheFormBeforeCreating() = runBlocking {
        val service = FakeService()
        val repository = CareNetworkRepositoryImpl(service)
        val elena = (GetAssistedPersonsUseCase(repository)() as NetworkResult.Success).data[0]
        val create = CreateCareActivityUseCase(repository)
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(9, 30)
        fun message(result: NetworkResult<Long>) = (result as NetworkResult.Error).message

        assertEquals("No tienes una persona asistida asignada.", message(create(null, CareActivityType.MEAL, "x", "", 1, today, emptySet(), start, end, today)))
        assertEquals("Elige el tipo de actividad.", message(create(elena, null, "x", "", 1, today, emptySet(), start, end, today)))
        assertEquals("Escribe el nombre de la actividad.", message(create(elena, CareActivityType.MEAL, " ", "", 1, today, emptySet(), start, end, today)))
        assertEquals("El nombre admite hasta 120 caracteres.", message(create(elena, CareActivityType.MEAL, "a".repeat(121), "", 1, today, emptySet(), start, end, today)))
        assertEquals("La instrucción admite hasta 500 caracteres.", message(create(elena, CareActivityType.MEAL, "x", "a".repeat(501), 1, today, emptySet(), start, end, today)))
        assertEquals("La fecha no puede ser anterior a hoy.", message(create(elena, CareActivityType.MEAL, "x", "", 1, today.minusDays(1), emptySet(), start, end, today)))
        assertEquals("La hora de inicio y de fin no pueden ser iguales.", message(create(elena, CareActivityType.MEAL, "x", "", 1, today, emptySet(), start, start, today)))
        assertEquals("Elige al responsable de la actividad.", message(create(elena, CareActivityType.MEAL, "x", "", null, today, emptySet(), start, end, today)))
        assertTrue(service.created.isEmpty())

        service.assignFails = httpError(409)
        assertEquals("El responsable ya tiene otra actividad en ese horario.", message(create(elena, CareActivityType.MEAL, "x", "", 1, today, emptySet(), start, end, today)))
        service.assignFails = httpError(500)
        assertEquals(500, (create(elena, CareActivityType.MEAL, "x", "", 1, today, emptySet(), start, end, today) as NetworkResult.Error).code)
    }

    @Test
    fun completesARunningOccurrence() = runBlocking {
        val service = FakeService()
        val complete = CompleteActivityUseCase(CareNetworkRepositoryImpl(service))
        assertEquals(NetworkResult.Success(Unit), complete(activity(), " Tomó la pastilla "))
        assertEquals(CompleteActivityRequestDto(5, 2, "2026-11-11", "Tomó la pastilla"), service.completed.last())
        assertEquals("Esta actividad no se puede marcar desde la app.", (complete(activity(activityId = null)) as NetworkResult.Error).message)
        assertEquals("Esta actividad no se puede marcar desde la app.", (complete(activity(assignmentId = null)) as NetworkResult.Error).message)
        service.completeCode = 400
        assertTrue((complete(activity()) as NetworkResult.Error).message.startsWith("Solo puedes marcarla mientras está en curso"))
        service.completeCode = 403
        assertEquals(403, (complete(activity()) as NetworkResult.Error).code)
        assertNull(service.completed.last().notes)
    }

    @Test
    fun reportsNetworkFailures() = runBlocking {
        val service = FakeService().apply { failWith = httpError(500) }
        val repository = CareNetworkRepositoryImpl(service)
        assertFalse(GetAssistedPersonsUseCase(repository)() is NetworkResult.Success)
        assertFalse(GetCareNetworkUseCase(repository)(7) is NetworkResult.Success)
        assertFalse(repository.myCaregiverIds() is NetworkResult.Success)
        val person = AssistedPerson(11, 11, 21, 7, "Elena", "Salazar", null, null, null, null, null, null, null)
        assertFalse(GetCaregiversUseCase(repository)(person) is NetworkResult.Success)
        assertEquals(listOf("Igual"), NameAgendaPeopleUseCase(repository)(listOf(activity().copy(responsible = "Igual"))).map { it.responsible })
    }
}
