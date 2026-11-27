package com.resolum.alivia.features.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.analytics.domain.models.AutonomyDevice
import com.resolum.alivia.features.analytics.domain.models.MetricPeriod
import com.resolum.alivia.features.analytics.domain.usecases.AutonomyReport
import com.resolum.alivia.features.analytics.domain.usecases.GetMetricsUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AutonomyUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val period: MetricPeriod = MetricPeriod.WEEK,
    val deviceId: Long? = null,
    /** Devices seen so far; kept while a device filter is on, since the filtered answer may list only that one. */
    val devices: List<AutonomyDevice> = emptyList(),
    val report: AutonomyReport? = null,
)

class AutonomyViewModel(
    private val getMetrics: GetMetricsUseCase,
    private val today: () -> LocalDate = Formats::today,
) : ViewModel() {
    private val _state = MutableStateFlow(AutonomyUiState())
    val state: StateFlow<AutonomyUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun load(refresh: Boolean = false) {
        job?.cancel()
        val current = _state.value
        _state.update { it.copy(loading = it.report == null, refreshing = refresh, error = null) }
        job = viewModelScope.launch {
            when (val result = getMetrics(current.period, current.deviceId, today())) {
                is NetworkResult.Success -> _state.update { state ->
                    val known = (state.devices + result.data.metrics.devices).distinctBy { it.id }.sortedBy { it.name }
                    state.copy(loading = false, refreshing = false, report = result.data, devices = known)
                }
                is NetworkResult.Error -> _state.update { it.copy(loading = false, refreshing = false, error = result.message) }
            }
        }
    }

    fun selectPeriod(period: MetricPeriod) {
        if (period == _state.value.period) return
        _state.update { it.copy(period = period) }
        load()
    }

    fun selectDevice(deviceId: Long?) {
        if (deviceId == _state.value.deviceId) return
        _state.update { it.copy(deviceId = deviceId) }
        load()
    }
}
