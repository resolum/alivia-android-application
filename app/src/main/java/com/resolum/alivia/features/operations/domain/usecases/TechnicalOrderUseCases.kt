package com.resolum.alivia.features.operations.domain.usecases

import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.EvaluationResult
import com.resolum.alivia.features.operations.domain.models.InstallationResult
import com.resolum.alivia.features.operations.domain.models.MacAddress
import com.resolum.alivia.features.operations.domain.models.ServiceType
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.repositories.TechnicalOrderRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

class GetMyOrdersUseCase(private val repository: TechnicalOrderRepository) {
    suspend operator fun invoke() = repository.myOrders()
}

class GetOrderUseCase(private val repository: TechnicalOrderRepository) {
    suspend operator fun invoke(orderId: Long) = repository.order(orderId)
}

class StartOrderUseCase(private val repository: TechnicalOrderRepository) {
    suspend operator fun invoke(orderId: Long) = repository.start(orderId)
}

/**
 * Records the technician's decision at the home. A viable home moves the request to the family's confirmation
 * on the web; the installation order opens only after the family confirms.
 */
class RecordEvaluationUseCase(private val repository: TechnicalOrderRepository) {
    suspend operator fun invoke(order: TechnicalOrder, result: EvaluationResult, notes: String): NetworkResult<TechnicalOrder> {
        val cleanNotes = notes.trim().ifEmpty { null }
        if (result == EvaluationResult.VIABLE_WITH_ADAPTATION && cleanNotes == null) {
            return NetworkResult.Error("Describe la adaptación que necesita el hogar.")
        }
        if ((cleanNotes?.length ?: 0) > 300) return NetworkResult.Error("Las notas no pueden pasar de 300 caracteres.")
        return repository.recordEvaluation(order.id, result, cleanNotes)
    }
}

/** The open installation order of the same request, once the family confirmed and the backend opened it. */
class FindInstallationOfRequestUseCase(private val repository: TechnicalOrderRepository) {
    suspend operator fun invoke(draftId: String): NetworkResult<TechnicalOrder?> = when (val orders = repository.myOrders()) {
        is NetworkResult.Success -> NetworkResult.Success(
            orders.data.firstOrNull { it.draftId == draftId && it.serviceType == ServiceType.INSTALLATION && it.isOpen },
        )
        is NetworkResult.Error -> orders
    }
}

class RegisterDeviceUseCase(private val repository: TechnicalOrderRepository) {
    suspend operator fun invoke(order: TechnicalOrder, alias: String, macAddress: String, type: DeviceType?): NetworkResult<TechnicalOrder> {
        val cleanAlias = alias.trim()
        val mac = MacAddress.format(macAddress)
        return when {
            cleanAlias.isEmpty() -> NetworkResult.Error("Asigna un alias al dispositivo.")
            cleanAlias.length > 60 -> NetworkResult.Error("El alias no puede pasar de 60 caracteres.")
            !MacAddress.isValid(mac) -> NetworkResult.Error("La dirección MAC necesita 12 dígitos hexadecimales.")
            order.installedDevices.any { it.macAddress == mac } -> NetworkResult.Error("Ese dispositivo ya está registrado en esta orden.")
            type == null -> NetworkResult.Error("Elige el tipo de dispositivo.")
            else -> repository.registerDevice(order.id, cleanAlias, mac, type)
        }
    }
}

class RemoveDeviceUseCase(private val repository: TechnicalOrderRepository) {
    suspend operator fun invoke(orderId: Long, macAddress: String) = repository.removeDevice(orderId, macAddress)
}

class CompleteInstallationUseCase(private val repository: TechnicalOrderRepository) {
    suspend operator fun invoke(order: TechnicalOrder, result: InstallationResult, notes: String): NetworkResult<TechnicalOrder> {
        val cleanNotes = notes.trim().ifEmpty { null }
        if ((cleanNotes?.length ?: 0) > 300) return NetworkResult.Error("Las notas no pueden pasar de 300 caracteres.")
        return repository.recordInstallation(order.id, result, cleanNotes)
    }
}
