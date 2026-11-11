package com.resolum.alivia.features.operations.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.operations.domain.models.WorkingDay
import com.resolum.alivia.features.operations.domain.usecases.GetMyScheduleUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduleUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val today: LocalDate = Formats.today(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val days: List<WorkingDay> = emptyList(),
)

class ScheduleViewModel(private val getMySchedule: GetMyScheduleUseCase) : ViewModel() {
    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Loads the two weeks again; the days already on screen stay visible meanwhile. */
    fun load(refresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!refresh) return
            loadJob?.cancel()
        }
        val today = Formats.today()
        _state.update { it.copy(loading = it.from == null, refreshing = refresh, error = null, today = today) }
        loadJob = viewModelScope.launch {
            when (val result = getMySchedule(today)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, refreshing = false, from = result.data.from, to = result.data.to, days = result.data.days)
                }
                is NetworkResult.Error -> _state.update { it.copy(loading = false, refreshing = false, error = result.message) }
            }
        }
    }
}
