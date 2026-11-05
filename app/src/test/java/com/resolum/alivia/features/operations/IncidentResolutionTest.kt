package com.resolum.alivia.features.operations

import com.resolum.alivia.features.operations.data.datasources.remote.TechnicalOrderFacadeService
import com.resolum.alivia.features.operations.data.datasources.remote.mappers.TechnicalOrderMapper
import com.resolum.alivia.features.operations.data.datasources.remote.models.EvaluationResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.IncidentSummaryDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.InstallationResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.InstalledDeviceDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.MaintenanceResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.RegisterDeviceRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.TechnicalOrderDto
import com.resolum.alivia.features.operations.data.datasources.remote.services.TechnicalOrderService
import com.resolum.alivia.features.operations.data.repositories.TechnicalOrderRepositoryImpl
import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.IncidentAction
import com.resolum.alivia.features.operations.domain.models.IncidentProblem
import com.resolum.alivia.features.operations.domain.models.IncidentStatus
import com.resolum.alivia.features.operations.domain.models.InstalledDevice
import com.resolum.alivia.features.operations.domain.models.OrderProgress
import com.resolum.alivia.features.operations.domain.models.ServiceType
import com.resolum.alivia.features.operations.domain.usecases.AdvanceIncidentUseCase
import com.resolum.alivia.features.operations.domain.usecases.ResolveIncidentUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** A maintenance order carries the family's incident; the technician starts it and records the resolution. */
class IncidentResolutionTest {

    private val incident = IncidentSummaryDto(
        id = 84, code = "INC-2026-0084", status = "SCHEDULED", problemType = "BATTERY",
        description = "El sensor no detecta la ventana desde anoche.", occurredAt = "2026-11-04T13:00:00Z",
        device = InstalledDeviceDto("Sensor de ventana", "00:1A:2B:3C:4D:5E", "SENSOR"),
    )

    private fun dto(status: String = "SCHEDULED", startedAt: String? = null, summary: IncidentSummaryDto? = incident) =
        TechnicalOrderDto(
            id = 9, code = "ORD-2026-0009", draftId = "draft", serviceType = "MAINTENANCE", incidentId = 84, status = status,
            date = "2026-11-05", startTime = "10:00:00", endTime = "12:00:00", technicianId = 5, technicianName = "Elena Vega",
            contactName = "María", contactPhone = "984123456", directions = "Av. Del Parque Norte 742", evaluationResult = null,
            installationResult = null, completionNotes = null, startedAt = startedAt, completedAt = null, installedDevices = null,
            incident = summary,
        )

    private class FakeService(private val answer: () -> TechnicalOrderDto) : TechnicalOrderService {
        val calls = mutableListOf<String>()
        override suspend fun mine() = listOf(answer())
        override suspend fun get(orderId: Long) = answer()
        override suspend fun start(orderId: Long) = answer().also { calls += "start:$orderId" }
        override suspend fun recordEvaluation(orderId: Long, body: EvaluationResultRequestDto) = answer()
        override suspend fun registerDevice(orderId: Long, body: RegisterDeviceRequestDto) = answer()
        override suspend fun removeDevice(orderId: Long, macAddress: String) = answer()
        override suspend fun recordInstallation(orderId: Long, body: InstallationResultRequestDto) = answer()
        override suspend fun evaluateIncident(orderId: Long) = answer().also { calls += "evaluate:$orderId" }
        override suspend fun confirmVisit(orderId: Long) = answer().also { calls += "confirm:$orderId" }
        override suspend fun recordMaintenance(orderId: Long, body: MaintenanceResultRequestDto) =
            answer().also { calls += "resolve:$orderId:${body.diagnosis}:${body.workPerformed}" }
    }

    @Test
    fun theOrderShowsTheIncidentTheFamilyReported() {
        val order = TechnicalOrderMapper.toDomain(dto())!!
        val summary = order.incident!!
        assertEquals("INC-2026-0084", summary.code)
        assertEquals(IncidentStatus.SCHEDULED, summary.status)
        assertEquals(IncidentProblem.BATTERY, summary.problem)
        assertEquals("Batería baja o agotada", summary.problem.label)
        assertEquals(Instant.parse("2026-11-04T13:00:00Z"), summary.occurredAt)
        assertEquals(InstalledDevice("Sensor de ventana", "00:1A:2B:3C:4D:5E", DeviceType.SENSOR), summary.device)
        assertFalse(order.awaitsResolution)

        val unknown = TechnicalOrderMapper.toDomain(dto(summary = IncidentSummaryDto(85, null, "WEIRD", "ROBOT", null, "no", null)))!!.incident!!
        assertEquals("INC-85", unknown.code)
        assertNull(unknown.status)
        assertEquals(IncidentProblem.OTHER, unknown.problem)
        assertEquals("", unknown.description)
        assertNull(unknown.occurredAt)
        assertNull(unknown.device)
        assertNull(TechnicalOrderMapper.toDomain(dto(summary = IncidentSummaryDto(null, null, null, null, null, null, null)))!!.incident)
        assertNull(TechnicalOrderMapper.toDomain(dto(summary = null))!!.incident)
    }

    @Test
    fun onlyAStartedMaintenanceOrderAwaitsTheResolution() {
        assertTrue(TechnicalOrderMapper.toDomain(dto(startedAt = "2026-11-05T15:10:00Z"))!!.awaitsResolution)
        assertFalse(TechnicalOrderMapper.toDomain(dto(status = "COMPLETED", startedAt = "2026-11-05T15:10:00Z"))!!.awaitsResolution)
        val installation = TechnicalOrderMapper.toDomain(dto(startedAt = "2026-11-05T15:10:00Z"))!!.copy(serviceType = ServiceType.INSTALLATION)
        assertFalse(installation.awaitsResolution)
    }

    @Test
    fun theTechnicianEvaluatesTheReportAndConfirmsTheVisitBeforeStartingIt() = runBlocking {
        var answer = dto(summary = incident.copy(status = "OPEN"))
        val service = FakeService { answer }
        val repository = TechnicalOrderRepositoryImpl(TechnicalOrderFacadeService(service))
        val advance = AdvanceIncidentUseCase(repository)

        val registered = (repository.order(9) as NetworkResult.Success).data
        assertEquals(IncidentAction.EVALUATE, registered.incidentAction)
        assertEquals("Registrada", registered.incident?.status?.label)

        answer = dto(summary = incident.copy(status = "IN_EVALUATION"))
        val evaluated = (advance(registered) as NetworkResult.Success).data
        assertEquals(IncidentAction.CONFIRM_VISIT, evaluated.incidentAction)

        answer = dto(summary = incident.copy(status = "SCHEDULED"))
        val confirmed = (advance(evaluated) as NetworkResult.Success).data
        assertEquals(IncidentAction.START, confirmed.incidentAction)
        assertTrue(advance(confirmed) is NetworkResult.Error)
        assertEquals(listOf("evaluate:9", "confirm:9"), service.calls)

        val started = TechnicalOrderMapper.toDomain(dto(startedAt = "2026-11-05T15:10:00Z"))!!
        assertEquals(IncidentAction.RESOLVE, started.incidentAction)
        assertNull(TechnicalOrderMapper.toDomain(dto(status = "COMPLETED"))!!.incidentAction)
        assertNull(started.copy(serviceType = ServiceType.INSTALLATION).incidentAction)
        assertEquals(IncidentAction.START, TechnicalOrderMapper.toDomain(dto(summary = null))!!.incidentAction)
    }

    @Test
    fun theTechnicianStartsTheVisitAndResolvesTheIncident() = runBlocking {
        var answer = dto()
        val service = FakeService { answer }
        val repository = TechnicalOrderRepositoryImpl(TechnicalOrderFacadeService(service))
        val resolve = ResolveIncidentUseCase(repository)

        val scheduled = (repository.order(9) as NetworkResult.Success).data
        assertEquals("Inicia la atención antes de registrar la resolución.",
            (resolve(scheduled, "Pila agotada", "Cambio de pila") as NetworkResult.Error).message)

        answer = dto(startedAt = "2026-11-05T15:10:00Z", summary = incident.copy(status = "IN_PROGRESS"))
        val started = (repository.start(9) as NetworkResult.Success).data
        assertEquals(OrderProgress.IN_PROGRESS, started.progress)
        assertEquals(IncidentStatus.IN_PROGRESS, started.incident?.status)

        assertEquals("Describe el diagnóstico del dispositivo.", (resolve(started, " ", "x") as NetworkResult.Error).message)
        assertEquals("Describe el trabajo realizado.", (resolve(started, "Pila agotada", "") as NetworkResult.Error).message)
        assertEquals("Cada campo admite hasta 500 caracteres.",
            (resolve(started, "a".repeat(501), "Cambio de pila") as NetworkResult.Error).message)

        answer = dto(status = "COMPLETED", startedAt = "2026-11-05T15:10:00Z", summary = incident.copy(status = "RESOLVED"))
        val resolved = (resolve(started, "  Pila agotada ", " Cambio de pila y prueba ") as NetworkResult.Success).data
        assertEquals(OrderProgress.COMPLETED, resolved.progress)
        assertEquals(IncidentStatus.RESOLVED, resolved.incident?.status)
        assertEquals(listOf("start:9", "resolve:9:Pila agotada:Cambio de pila y prueba"), service.calls)
    }
}
