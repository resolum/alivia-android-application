package com.resolum.alivia.features.operations.di

import com.resolum.alivia.features.operations.data.datasources.remote.TechnicalOrderFacadeService
import com.resolum.alivia.features.operations.data.datasources.remote.services.TechnicalOrderService
import com.resolum.alivia.features.operations.data.repositories.TechnicalOrderRepositoryImpl
import com.resolum.alivia.features.operations.domain.repositories.TechnicalOrderRepository
import com.resolum.alivia.features.operations.domain.usecases.CompleteInstallationUseCase
import com.resolum.alivia.features.operations.domain.usecases.GetMyOrdersUseCase
import com.resolum.alivia.features.operations.domain.usecases.GetOrderUseCase
import com.resolum.alivia.features.operations.domain.usecases.FindInstallationOfRequestUseCase
import com.resolum.alivia.features.operations.domain.usecases.RecordEvaluationUseCase
import com.resolum.alivia.features.operations.domain.usecases.RegisterDeviceUseCase
import com.resolum.alivia.features.operations.domain.usecases.RemoveDeviceUseCase
import com.resolum.alivia.features.operations.domain.usecases.StartOrderUseCase
import com.resolum.alivia.features.shared.di.SharedModule

object OperationsModule {
    private val repository: TechnicalOrderRepository by lazy {
        TechnicalOrderRepositoryImpl(TechnicalOrderFacadeService(SharedModule.retrofit.create(TechnicalOrderService::class.java)))
    }

    fun getMyOrders() = GetMyOrdersUseCase(repository)
    fun getOrder() = GetOrderUseCase(repository)
    fun startOrder() = StartOrderUseCase(repository)
    fun recordEvaluation() = RecordEvaluationUseCase(repository)
    fun findInstallationOfRequest() = FindInstallationOfRequestUseCase(repository)
    fun registerDevice() = RegisterDeviceUseCase(repository)
    fun removeDevice() = RemoveDeviceUseCase(repository)
    fun completeInstallation() = CompleteInstallationUseCase(repository)
}
