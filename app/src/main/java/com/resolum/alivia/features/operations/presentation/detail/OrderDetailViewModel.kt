package com.resolum.alivia.features.operations.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.usecases.AdvanceIncidentUseCase
import com.resolum.alivia.features.operations.domain.usecases.GetOrderUseCase
import com.resolum.alivia.features.operations.domain.usecases.StartOrderUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val loading: Boolean = true,
    val order: TechnicalOrder? = null,
    val error: String? = null,
    val confirmingStart: Boolean = false,
    val starting: Boolean = false,
    val startError: String? = null,
    val advancing: Boolean = false,
    val advanceError: String? = null,
)

class OrderDetailViewModel(
    private val orderId: Long,
    private val getOrder: GetOrderUseCase,
    private val startOrder: StartOrderUseCase,
    private val advanceIncident: AdvanceIncidentUseCase? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(OrderDetailUiState())
    val state: StateFlow<OrderDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = it.order == null, error = null) }
        viewModelScope.launch {
            when (val result = getOrder(orderId)) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, order = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    /** Evaluates the incident or confirms its visit, the step the order shows; the order comes back updated. */
    fun advance() {
        val order = _state.value.order ?: return
        val useCase = advanceIncident ?: return
        if (_state.value.advancing) return
        _state.update { it.copy(advancing = true, advanceError = null) }
        viewModelScope.launch {
            when (val result = useCase(order)) {
                is NetworkResult.Success -> _state.update { it.copy(advancing = false, order = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(advancing = false, advanceError = result.message) }
            }
        }
    }

    fun askToStart() = _state.update { it.copy(confirmingStart = true, startError = null) }
    fun cancelStart() = _state.update { it.copy(confirmingStart = false, startError = null) }

    /** Starts the visit and, once the backend records it, lets the screen move on to the order's process. */
    fun start(onStarted: (TechnicalOrder) -> Unit) {
        _state.update { it.copy(starting = true, startError = null) }
        viewModelScope.launch {
            when (val result = startOrder(orderId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(starting = false, confirmingStart = false, order = result.data) }
                    onStarted(result.data)
                }
                is NetworkResult.Error -> _state.update { it.copy(starting = false, startError = result.message) }
            }
        }
    }
}
