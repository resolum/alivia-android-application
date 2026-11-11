package com.resolum.alivia.features.devices.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.features.devices.domain.models.HomeDevice
import com.resolum.alivia.features.devices.domain.models.HomeDevices
import com.resolum.alivia.features.devices.domain.usecases.GetHomeDevicesUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevicesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val home: HomeDevices? = null,
    val selected: HomeDevice? = null,
)

class DevicesViewModel(private val getHomeDevices: GetHomeDevicesUseCase) : ViewModel() {
    private val _state = MutableStateFlow(DevicesUiState())
    val state: StateFlow<DevicesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun load(refresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!refresh) return
            loadJob?.cancel()
        }
        _state.update { it.copy(loading = !it.loaded, refreshing = refresh, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = getHomeDevices()) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, refreshing = false, loaded = true, home = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(loading = false, refreshing = false, error = result.message) }
            }
        }
    }

    fun select(device: HomeDevice?) = _state.update { it.copy(selected = device) }
}
