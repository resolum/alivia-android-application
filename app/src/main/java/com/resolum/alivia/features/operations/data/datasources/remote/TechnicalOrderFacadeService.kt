package com.resolum.alivia.features.operations.data.datasources.remote

import com.resolum.alivia.features.operations.data.datasources.remote.models.EvaluationResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.InstallationResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.MaintenanceResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.RegisterDeviceRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.services.TechnicalOrderService

/** Entry point of the technical-order endpoints the technician uses. */
class TechnicalOrderFacadeService(private val service: TechnicalOrderService) {
    suspend fun mine() = service.mine()
    suspend fun get(orderId: Long) = service.get(orderId)
    suspend fun start(orderId: Long) = service.start(orderId)
    suspend fun recordEvaluation(orderId: Long, result: String, notes: String?) =
        service.recordEvaluation(orderId, EvaluationResultRequestDto(result, notes))
    suspend fun registerDevice(orderId: Long, alias: String, macAddress: String, type: String) =
        service.registerDevice(orderId, RegisterDeviceRequestDto(alias, macAddress, type))
    suspend fun removeDevice(orderId: Long, macAddress: String) = service.removeDevice(orderId, macAddress)
    suspend fun recordInstallation(orderId: Long, result: String, notes: String?) =
        service.recordInstallation(orderId, InstallationResultRequestDto(result, notes))
    suspend fun evaluateIncident(orderId: Long) = service.evaluateIncident(orderId)
    suspend fun confirmVisit(orderId: Long) = service.confirmVisit(orderId)
    suspend fun recordMaintenance(orderId: Long, diagnosis: String, workPerformed: String) =
        service.recordMaintenance(orderId, MaintenanceResultRequestDto(diagnosis, workPerformed))
}
