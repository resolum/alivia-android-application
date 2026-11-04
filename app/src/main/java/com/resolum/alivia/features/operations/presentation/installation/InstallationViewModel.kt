package com.resolum.alivia.features.operations.presentation.installation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.InstallationResult
import com.resolum.alivia.features.operations.domain.models.MacAddress
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.usecases.CompleteInstallationUseCase
import com.resolum.alivia.features.operations.domain.usecases.GetOrderUseCase
import com.resolum.alivia.features.operations.domain.usecases.RegisterDeviceUseCase
import com.resolum.alivia.features.operations.domain.usecases.RemoveDeviceUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The four steps of an installation at the home. */
enum class InstallationStep(val number: Int, val title: String) {
    HOME(1, "Hogar"),
    DEVICES(2, "Dispositivos"),
    TESTS(3, "Pruebas"),
    CLOSING(4, "Cierre"),
}

data class DeviceForm(val alias: String = "", val macAddress: String = "", val type: DeviceType? = null) {
    val macComplete: Boolean get() = macAddress.length == 17
    val macValid: Boolean get() = MacAddress.isValid(macAddress)
}

data class InstallationUiState(
    val order: TechnicalOrder? = null,
    val loadError: String? = null,
    val step: InstallationStep = InstallationStep.HOME,
    val form: DeviceForm = DeviceForm(),
    val tested: Set<String> = emptySet(),
    val result: InstallationResult? = null,
    val notes: String = "",
    val busy: Boolean = false,
    val error: String? = null,
) {
    val duplicateMac: Boolean get() = order?.installedDevices?.any { it.macAddress == form.macAddress } == true
    val allTested: Boolean get() = order?.installedDevices?.all { it.macAddress in tested } == true
}

class InstallationViewModel(
    private val orderId: Long,
    private val getOrder: GetOrderUseCase,
    private val registerDevice: RegisterDeviceUseCase,
    private val removeDevice: RemoveDeviceUseCase,
    private val completeInstallation: CompleteInstallationUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(InstallationUiState())
    val state: StateFlow<InstallationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = getOrder(orderId)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(order = result.data, step = if (result.data.installedDevices.isEmpty()) InstallationStep.HOME else InstallationStep.DEVICES)
                }
                is NetworkResult.Error -> _state.update { it.copy(loadError = result.message) }
            }
        }
    }

    fun goTo(step: InstallationStep) = _state.update { it.copy(step = step, error = null) }

    /** Steps back; returns false on the first step so the screen can leave. */
    fun back(): Boolean {
        val previous = InstallationStep.entries.getOrNull(_state.value.step.ordinal - 1) ?: return false
        goTo(previous)
        return true
    }

    fun updateAlias(alias: String) = _state.update { it.copy(form = it.form.copy(alias = alias.take(60)), error = null) }
    fun updateMac(mac: String) = _state.update { it.copy(form = it.form.copy(macAddress = MacAddress.format(mac)), error = null) }
    fun clearMac() = _state.update { it.copy(form = it.form.copy(macAddress = "")) }
    fun updateType(type: DeviceType) = _state.update { it.copy(form = it.form.copy(type = type), error = null) }
    fun toggleTested(mac: String) = _state.update { it.copy(tested = if (mac in it.tested) it.tested - mac else it.tested + mac) }
    fun selectResult(result: InstallationResult) = _state.update { it.copy(result = result, error = null) }
    fun updateNotes(notes: String) = _state.update { it.copy(notes = notes.take(300), error = null) }

    fun register() {
        val current = _state.value
        val order = current.order ?: return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = registerDevice(order, current.form.alias, current.form.macAddress, current.form.type)) {
                is NetworkResult.Success -> _state.update { it.copy(busy = false, order = result.data, form = DeviceForm()) }
                is NetworkResult.Error -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    fun remove(macAddress: String) {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = removeDevice(orderId, macAddress)) {
                is NetworkResult.Success -> _state.update { it.copy(busy = false, order = result.data, tested = it.tested - macAddress) }
                is NetworkResult.Error -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    fun finish(onCompleted: (Long) -> Unit) {
        val current = _state.value
        val order = current.order ?: return
        val result = current.result ?: return _state.update { it.copy(error = "Elige el resultado de la instalación.") }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val closed = completeInstallation(order, result, current.notes)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(busy = false, order = closed.data) }
                    onCompleted(closed.data.id)
                }
                is NetworkResult.Error -> _state.update { it.copy(busy = false, error = closed.message) }
            }
        }
    }
}
