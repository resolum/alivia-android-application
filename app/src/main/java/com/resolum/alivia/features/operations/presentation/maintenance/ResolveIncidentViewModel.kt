package com.resolum.alivia.features.operations.presentation.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.usecases.GetOrderUseCase
import com.resolum.alivia.features.operations.domain.usecases.ResolveIncidentUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResolveIncidentUiState(
    val order: TechnicalOrder? = null,
    val loadError: String? = null,
    val diagnosis: String = "",
    val workPerformed: String = "",
    val saving: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = order != null && diagnosis.isNotBlank() && workPerformed.isNotBlank() && !saving
}

/** The technician closes the incident of a started maintenance order; the family then sees it resolved. */
class ResolveIncidentViewModel(
    private val orderId: Long,
    private val getOrder: GetOrderUseCase,
    private val resolveIncident: ResolveIncidentUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ResolveIncidentUiState())
    val state: StateFlow<ResolveIncidentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = getOrder(orderId)) {
                is NetworkResult.Success -> _state.update { it.copy(order = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(loadError = result.message) }
            }
        }
    }

    fun updateDiagnosis(value: String) =
        _state.update { it.copy(diagnosis = value.take(ResolveIncidentUseCase.MAX_RESOLUTION), error = null) }

    fun updateWorkPerformed(value: String) =
        _state.update { it.copy(workPerformed = value.take(ResolveIncidentUseCase.MAX_RESOLUTION), error = null) }

    fun submit(onResolved: (Long) -> Unit) {
        val current = _state.value
        val order = current.order ?: return
        if (current.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when (val outcome = resolveIncident(order, current.diagnosis, current.workPerformed)) {
                is NetworkResult.Error -> _state.update { it.copy(saving = false, error = outcome.message) }
                is NetworkResult.Success -> {
                    _state.update { it.copy(saving = false, order = outcome.data) }
                    onResolved(outcome.data.id)
                }
            }
        }
    }
}
