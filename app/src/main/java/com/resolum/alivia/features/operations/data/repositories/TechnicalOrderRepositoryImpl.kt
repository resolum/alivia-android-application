package com.resolum.alivia.features.operations.data.repositories

import com.resolum.alivia.features.operations.data.datasources.remote.TechnicalOrderFacadeService
import com.resolum.alivia.features.operations.data.datasources.remote.mappers.TechnicalOrderMapper
import com.resolum.alivia.features.operations.data.datasources.remote.models.TechnicalOrderDto
import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.EvaluationResult
import com.resolum.alivia.features.operations.domain.models.InstallationResult
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.repositories.TechnicalOrderRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.repositories.BaseRepository

class TechnicalOrderRepositoryImpl(private val facade: TechnicalOrderFacadeService) : BaseRepository(), TechnicalOrderRepository {

    override suspend fun myOrders(): NetworkResult<List<TechnicalOrder>> =
        safeCall { facade.mine().mapNotNull(TechnicalOrderMapper::toDomain) }

    override suspend fun order(orderId: Long) = one { facade.get(orderId) }

    override suspend fun start(orderId: Long) = one { facade.start(orderId) }

    override suspend fun recordEvaluation(orderId: Long, result: EvaluationResult, notes: String?) =
        one { facade.recordEvaluation(orderId, result.name, notes) }

    override suspend fun registerDevice(orderId: Long, alias: String, macAddress: String, type: DeviceType) =
        one { facade.registerDevice(orderId, alias, macAddress, type.name) }

    override suspend fun removeDevice(orderId: Long, macAddress: String) = one { facade.removeDevice(orderId, macAddress) }

    override suspend fun recordInstallation(orderId: Long, result: InstallationResult, notes: String?) =
        one { facade.recordInstallation(orderId, result.name, notes) }

    private suspend fun one(call: suspend () -> TechnicalOrderDto): NetworkResult<TechnicalOrder> =
        when (val result = safeCall(call)) {
            is NetworkResult.Error -> result
            is NetworkResult.Success -> TechnicalOrderMapper.toDomain(result.data)?.let { NetworkResult.Success(it) }
                ?: NetworkResult.Error("La respuesta del servidor no es válida.")
        }
}
