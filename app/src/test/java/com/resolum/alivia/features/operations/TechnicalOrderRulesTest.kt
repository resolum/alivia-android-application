package com.resolum.alivia.features.operations

import com.resolum.alivia.features.operations.data.datasources.remote.mappers.TechnicalOrderMapper
import com.resolum.alivia.features.operations.data.datasources.remote.models.InstalledDeviceDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.TechnicalOrderDto
import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.EvaluationResult
import com.resolum.alivia.features.operations.domain.models.InstallationResult
import com.resolum.alivia.features.operations.domain.models.InstalledDevice
import com.resolum.alivia.features.operations.domain.models.MacAddress
import com.resolum.alivia.features.operations.domain.models.OrderProgress
import com.resolum.alivia.features.operations.domain.models.ServiceType
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.repositories.TechnicalOrderRepository
import com.resolum.alivia.features.operations.domain.usecases.CompleteInstallationUseCase
import com.resolum.alivia.features.operations.domain.usecases.FindInstallationOfRequestUseCase
import com.resolum.alivia.features.operations.domain.usecases.RecordEvaluationUseCase
import com.resolum.alivia.features.operations.domain.usecases.RegisterDeviceUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalOrderRulesTest {

    private fun dto(id: Long = 2, type: String = "INSTALLATION", status: String = "SCHEDULED", startedAt: String? = null) =
        TechnicalOrderDto(
            id = id, code = "ORD-2026-000$id", draftId = "draft", serviceType = type, incidentId = null, status = status,
            date = "2026-11-03", startTime = "09:00", endTime = "11:00", technicianId = 7, technicianName = "Carla Mendoza",
            contactName = "María", contactPhone = "984123456", directions = " ", evaluationResult = null,
            installationResult = null, completionNotes = null, startedAt = startedAt, completedAt = null,
            installedDevices = listOf(InstalledDeviceDto("Micrófono", "00:1A:2B:3C:4D:5E", "MICROPHONE"), InstalledDeviceDto("x", "y", "TOASTER")),
        )

    private fun order(type: ServiceType = ServiceType.INSTALLATION, devices: List<InstalledDevice> = emptyList()) =
        TechnicalOrderMapper.toDomain(dto(type = type.name))!!.copy(installedDevices = devices)

    @Test
    fun formatsTheMacAddressWhileTyping() {
        assertEquals("00:1A:2B", MacAddress.format("001a2b"))
        assertEquals("00:1A:2B:3C:4D:5E", MacAddress.format("00-1a-2b-3c-4d-5e-ff"))
        assertTrue(MacAddress.isValid("00:1A:2B:3C:4D:5E"))
        assertFalse(MacAddress.isValid("00:1A:2B:3C:4D"))
    }

    @Test
    fun mapsTheOrderAndItsProgress() {
        val scheduled = TechnicalOrderMapper.toDomain(dto())!!
        assertEquals(OrderProgress.SCHEDULED, scheduled.progress)
        assertNull(scheduled.directions)
        assertEquals("Hogar de María", scheduled.homeName)
        assertEquals(listOf(InstalledDevice("Micrófono", "00:1A:2B:3C:4D:5E", DeviceType.MICROPHONE)), scheduled.installedDevices)
        assertEquals(OrderProgress.IN_PROGRESS, TechnicalOrderMapper.toDomain(dto(startedAt = "2026-11-03T15:07:30Z"))!!.progress)
        assertEquals(OrderProgress.COMPLETED, TechnicalOrderMapper.toDomain(dto(status = "COMPLETED"))!!.progress)
        assertNull(TechnicalOrderMapper.toDomain(dto(type = "UNKNOWN")))
    }

    @Test
    fun aViableEvaluationContinuesWithTheInstallationOfTheSameRequest() = runBlocking {
        val repository = FakeRepository()
        val evaluation = order(ServiceType.EVALUATION)
        repository.mine = listOf(evaluation, order(ServiceType.INSTALLATION).copy(id = 5))

        val missingNotes = RecordEvaluationUseCase(repository)(evaluation, EvaluationResult.VIABLE_WITH_ADAPTATION, " ")
        assertTrue(missingNotes is NetworkResult.Error)

        val viable = RecordEvaluationUseCase(repository)(evaluation, EvaluationResult.VIABLE, "")
        assertTrue(viable is NetworkResult.Success)
        assertEquals(EvaluationResult.VIABLE, repository.lastResult)

        val rejected = RecordEvaluationUseCase(repository)(evaluation, EvaluationResult.NOT_VIABLE, "Sin red")
        assertTrue(rejected is NetworkResult.Success)
        assertEquals(EvaluationResult.NOT_VIABLE, repository.lastResult)

        // The installation of the same request appears once the family confirmed on the web.
        val find = FindInstallationOfRequestUseCase(repository)
        assertEquals(5L, (find(evaluation.draftId) as NetworkResult.Success).data?.id)
        assertNull((find("other") as NetworkResult.Success).data)
    }

    @Test
    fun validatesDevicesAndAllowsInstallationClosingWithoutDevices() = runBlocking {
        val repository = FakeRepository()
        val withDevice = order(devices = listOf(InstalledDevice("Mic", "00:1A:2B:3C:4D:5E", DeviceType.MICROPHONE)))
        val register = RegisterDeviceUseCase(repository)
        assertTrue(register(withDevice, " ", "001A2B3C4D5E", DeviceType.HUB) is NetworkResult.Error)
        assertTrue(register(withDevice, "Hub", "001A2B", DeviceType.HUB) is NetworkResult.Error)
        assertTrue(register(withDevice, "Hub", "00-1a-2b-3c-4d-5e", DeviceType.HUB) is NetworkResult.Error)
        assertTrue(register(withDevice, "Hub", "AABBCCDDEEFF", null) is NetworkResult.Error)
        assertTrue(register(withDevice, "Hub", "aabbccddeeff", DeviceType.HUB) is NetworkResult.Success)
        assertEquals("AA:BB:CC:DD:EE:FF", repository.lastMac)

        val complete = CompleteInstallationUseCase(repository)
        assertTrue(complete(order(), InstallationResult.APPROVED, "") is NetworkResult.Success)
        assertTrue(complete(order(), InstallationResult.REJECTED, "") is NetworkResult.Success)
        assertTrue(complete(withDevice, InstallationResult.APPROVED, "") is NetworkResult.Success)
    }

    private class FakeRepository : TechnicalOrderRepository {
        var mine: List<TechnicalOrder> = emptyList()
        var lastResult: EvaluationResult? = null
        var lastMac: String? = null
        private fun anyOrder() = TechnicalOrderMapper.toDomain(
            TechnicalOrderDto(1, null, "draft", "EVALUATION", null, "COMPLETED", "2026-11-03", "09:00", "11:00", 7, "Carla",
                null, null, null, null, null, null, null, null, null),
        )!!

        override suspend fun myOrders() = NetworkResult.Success(mine)
        override suspend fun order(orderId: Long) = NetworkResult.Success(anyOrder())
        override suspend fun start(orderId: Long) = NetworkResult.Success(anyOrder())
        override suspend fun recordEvaluation(orderId: Long, result: EvaluationResult, notes: String?): NetworkResult<TechnicalOrder> {
            lastResult = result
            return NetworkResult.Success(anyOrder())
        }
        override suspend fun registerDevice(orderId: Long, alias: String, macAddress: String, type: DeviceType): NetworkResult<TechnicalOrder> {
            lastMac = macAddress
            return NetworkResult.Success(anyOrder())
        }
        override suspend fun removeDevice(orderId: Long, macAddress: String) = NetworkResult.Success(anyOrder())
        override suspend fun recordInstallation(orderId: Long, result: InstallationResult, notes: String?) = NetworkResult.Success(anyOrder())
        override suspend fun evaluateIncident(orderId: Long) = NetworkResult.Success(anyOrder())
        override suspend fun confirmVisit(orderId: Long) = NetworkResult.Success(anyOrder())
        override suspend fun resolveIncident(orderId: Long, diagnosis: String, workPerformed: String) = NetworkResult.Success(anyOrder())
    }
}
