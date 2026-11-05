package com.resolum.alivia.features.operations

import com.resolum.alivia.features.operations.data.datasources.remote.TechnicalOrderFacadeService
import com.resolum.alivia.features.operations.data.datasources.remote.models.EvaluationResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.InstallationResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.MaintenanceResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.InstalledDeviceDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.RegisterDeviceRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.TechnicalOrderDto
import com.resolum.alivia.features.operations.data.datasources.remote.services.TechnicalOrderService
import com.resolum.alivia.features.operations.data.repositories.TechnicalOrderRepositoryImpl
import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.EvaluationResult
import com.resolum.alivia.features.operations.domain.models.InstallationResult
import com.resolum.alivia.features.operations.domain.models.OrderProgress
import com.resolum.alivia.features.operations.domain.models.ServiceType
import com.resolum.alivia.features.operations.domain.usecases.CompleteInstallationUseCase
import com.resolum.alivia.features.operations.domain.usecases.GetMyOrdersUseCase
import com.resolum.alivia.features.operations.domain.usecases.GetOrderUseCase
import com.resolum.alivia.features.operations.domain.usecases.RecordEvaluationUseCase
import com.resolum.alivia.features.operations.domain.usecases.RegisterDeviceUseCase
import com.resolum.alivia.features.operations.domain.usecases.RemoveDeviceUseCase
import com.resolum.alivia.features.operations.domain.usecases.StartOrderUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalOrderRepositoryTest {
    private fun dto(id: Long, type: String = "INSTALLATION", status: String = "SCHEDULED", contact: String? = "María",
                    completedAt: String? = null, devices: List<InstalledDeviceDto> = emptyList()) = TechnicalOrderDto(
        id, null, "draft", type, null, status, "2026-11-05", "08:00", "10:00", 5, "Elena Vega", contact, null,
        "Av. Los Jazmines 450", "VIABLE", null, null, null, completedAt, devices,
    )

    private class FakeService : TechnicalOrderService {
        val calls = mutableListOf<String>()
        var failWith: Exception? = null
        var invalid = false
        private fun reply(call: String, id: Long): TechnicalOrderDto {
            calls += call
            failWith?.let { throw it }
            return if (invalid) TechnicalOrderDto(id, null, null, "TOASTER", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null)
            else TechnicalOrderDto(id, "ORD-2026-000$id", "draft", "EVALUATION", null, "COMPLETED", "2026-11-05", "08:00:00",
                "10:00:00", 5, "Elena Vega", null, null, null, "NOT_VIABLE", "REJECTED", "Sin red", "2026-11-05T13:00:00Z",
                "2026-11-05T14:00:00Z", listOf(InstalledDeviceDto("Mic", "00:1A:2B:3C:4D:5E", "MICROPHONE")))
        }
        override suspend fun mine(): List<TechnicalOrderDto> {
            calls += "mine"
            failWith?.let { throw it }
            return listOf(reply("one", 1), reply("two", 2))
        }
        override suspend fun get(orderId: Long) = reply("get", orderId)
        override suspend fun start(orderId: Long) = reply("start", orderId)
        override suspend fun recordEvaluation(orderId: Long, body: EvaluationResultRequestDto) = reply("evaluation:${body.result}:${body.notes}", orderId)
        override suspend fun registerDevice(orderId: Long, body: RegisterDeviceRequestDto) = reply("register:${body.type}:${body.macAddress}", orderId)
        override suspend fun removeDevice(orderId: Long, macAddress: String) = reply("remove:$macAddress", orderId)
        override suspend fun recordInstallation(orderId: Long, body: InstallationResultRequestDto) = reply("installation:${body.result}", orderId)
        override suspend fun evaluateIncident(orderId: Long) = reply("evaluate", orderId)
        override suspend fun confirmVisit(orderId: Long) = reply("confirm", orderId)
        override suspend fun recordMaintenance(orderId: Long, body: MaintenanceResultRequestDto) =
            reply("maintenance:${body.diagnosis}:${body.workPerformed}", orderId)
    }

    @Test
    fun callsEachEndpointAndMapsTheOrders() = runBlocking {
        val service = FakeService()
        val repository = TechnicalOrderRepositoryImpl(TechnicalOrderFacadeService(service))
        val orders = (GetMyOrdersUseCase(repository)() as NetworkResult.Success).data
        assertEquals(2, orders.size)
        val order = (GetOrderUseCase(repository)(1) as NetworkResult.Success).data
        assertEquals(OrderProgress.COMPLETED, order.progress)
        assertEquals(EvaluationResult.NOT_VIABLE, order.evaluationResult)
        assertEquals(InstallationResult.REJECTED, order.installationResult)
        assertEquals("Hogar de la solicitud", order.homeName)
        assertTrue(StartOrderUseCase(repository)(1) is NetworkResult.Success)
        assertTrue(RecordEvaluationUseCase(repository)(order, EvaluationResult.VIABLE, " Todo bien ") is NetworkResult.Success)
        assertTrue(RegisterDeviceUseCase(repository)(order.copy(installedDevices = emptyList()), "Hub", "aabbccddeeff", DeviceType.HUB) is NetworkResult.Success)
        assertTrue(RemoveDeviceUseCase(repository)(1, "00:1A:2B:3C:4D:5E") is NetworkResult.Success)
        assertTrue(CompleteInstallationUseCase(repository)(order, InstallationResult.APPROVED, "") is NetworkResult.Success)
        assertEquals(listOf("mine", "one", "two", "get", "start", "evaluation:VIABLE:Todo bien", "register:HUB:AA:BB:CC:DD:EE:FF",
            "remove:00:1A:2B:3C:4D:5E", "installation:APPROVED"), service.calls)
    }

    @Test
    fun reportsFailuresAndInvalidAnswers() = runBlocking {
        val service = FakeService()
        val repository = TechnicalOrderRepositoryImpl(TechnicalOrderFacadeService(service))
        service.invalid = true
        assertEquals("La respuesta del servidor no es válida.", (repository.order(9) as NetworkResult.Error).message)
        assertEquals(emptyList<Any>(), (repository.myOrders() as NetworkResult.Success).data)
        service.invalid = false
        service.failWith = httpError(404)
        assertEquals("No encontramos la orden.", (repository.start(9) as NetworkResult.Error).message)
        assertEquals(404, (repository.myOrders() as NetworkResult.Error).code)
    }

    @Test
    fun appliesTheLimitsOfNotesAliasesAndDuplicates() = runBlocking {
        val repository = TechnicalOrderRepositoryImpl(TechnicalOrderFacadeService(FakeService()))
        val order = (repository.order(1) as NetworkResult.Success).data
        val long = "x".repeat(301)
        assertTrue(RecordEvaluationUseCase(repository)(order, EvaluationResult.NOT_VIABLE, long) is NetworkResult.Error)
        assertTrue(CompleteInstallationUseCase(repository)(order, InstallationResult.REJECTED, long) is NetworkResult.Error)
        assertTrue(RegisterDeviceUseCase(repository)(order, "x".repeat(61), "001A2B3C4D5F", DeviceType.SENSOR) is NetworkResult.Error)
        assertTrue(RegisterDeviceUseCase(repository)(order, "Mic 2", "00:1A:2B:3C:4D:5E", DeviceType.MICROPHONE) is NetworkResult.Error)
    }

    @Test
    fun namesTheHomeAndTellsWhichOrdersAreOpen() {
        val mapper = com.resolum.alivia.features.operations.data.datasources.remote.mappers.TechnicalOrderMapper
        val scheduled = mapper.toDomain(dto(1))!!
        assertEquals("Hogar de María", scheduled.homeName)
        assertTrue(scheduled.isOpen)
        assertEquals(ServiceType.INSTALLATION, scheduled.serviceType)
        assertEquals(false, mapper.toDomain(dto(2, status = "CANCELED"))!!.isOpen)
        assertEquals(OrderProgress.CANCELED, mapper.toDomain(dto(2, status = "CANCELED"))!!.progress)
        assertNull(mapper.toDomain(dto(3, completedAt = "not a date"))!!.completedAt)
        assertNull(mapper.toDomain(dto(4).copy(date = "x")))
        assertNull(mapper.toDomain(dto(4).copy(id = null)))
        assertEquals("ORD-4", mapper.toDomain(dto(4))!!.code)
        assertNull(mapper.toDomain(dto(5, contact = " "))!!.contactName)
    }
}
