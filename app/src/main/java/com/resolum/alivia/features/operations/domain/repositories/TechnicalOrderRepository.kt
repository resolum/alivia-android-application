package com.resolum.alivia.features.operations.domain.repositories

import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.EvaluationResult
import com.resolum.alivia.features.operations.domain.models.InstallationResult
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

interface TechnicalOrderRepository {
    suspend fun myOrders(): NetworkResult<List<TechnicalOrder>>
    suspend fun order(orderId: Long): NetworkResult<TechnicalOrder>
    suspend fun start(orderId: Long): NetworkResult<TechnicalOrder>
    suspend fun recordEvaluation(orderId: Long, result: EvaluationResult, notes: String?): NetworkResult<TechnicalOrder>
    suspend fun registerDevice(orderId: Long, alias: String, macAddress: String, type: DeviceType): NetworkResult<TechnicalOrder>
    suspend fun removeDevice(orderId: Long, macAddress: String): NetworkResult<TechnicalOrder>
    suspend fun recordInstallation(orderId: Long, result: InstallationResult, notes: String?): NetworkResult<TechnicalOrder>
}
