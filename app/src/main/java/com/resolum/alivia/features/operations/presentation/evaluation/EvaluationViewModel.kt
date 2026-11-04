package com.resolum.alivia.features.operations.presentation.evaluation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.features.operations.domain.models.EvaluationResult
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.usecases.GetOrderUseCase
import com.resolum.alivia.features.operations.domain.usecases.RecordEvaluationUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EvaluationUiState(
    val order: TechnicalOrder? = null,
    val loadError: String? = null,
    val result: EvaluationResult? = null,
    val notes: String = "",
    val confirmingRejection: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

/** Where the technician goes after the decision: the summary, which waits for the family when the home is viable. */
sealed interface EvaluationNext {
    data class Completed(val orderId: Long) : EvaluationNext
}

class EvaluationViewModel(
    private val orderId: Long,
    private val getOrder: GetOrderUseCase,
    private val recordEvaluation: RecordEvaluationUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(EvaluationUiState())
    val state: StateFlow<EvaluationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = getOrder(orderId)) {
                is NetworkResult.Success -> _state.update { it.copy(order = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(loadError = result.message) }
            }
        }
    }

    fun select(result: EvaluationResult) = _state.update { it.copy(result = result, error = null) }
    fun updateNotes(notes: String) = _state.update { it.copy(notes = notes.take(300), error = null) }
    fun dismissRejection() = _state.update { it.copy(confirmingRejection = false) }

    /** A not viable home asks for a second confirmation, because it closes the request and releases the payment. */
    fun submit(onNext: (EvaluationNext) -> Unit) {
        val current = _state.value
        val result = current.result ?: return _state.update { it.copy(error = "Elige el resultado de la evaluación.") }
        if (result == EvaluationResult.NOT_VIABLE && !current.confirmingRejection) {
            _state.update { it.copy(confirmingRejection = true) }
            return
        }
        val order = current.order ?: return
        _state.update { it.copy(saving = true, error = null, confirmingRejection = false) }
        viewModelScope.launch {
            when (val outcome = recordEvaluation(order, result, current.notes)) {
                is NetworkResult.Error -> _state.update { it.copy(saving = false, error = outcome.message) }
                is NetworkResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    onNext(EvaluationNext.Completed(order.id))
                }
            }
        }
    }
}
