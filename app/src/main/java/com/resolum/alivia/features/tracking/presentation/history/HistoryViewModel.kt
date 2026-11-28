package com.resolum.alivia.features.tracking.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.usecases.GetAssistedPersonsUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.tracking.domain.models.HistoryCategory
import com.resolum.alivia.features.tracking.domain.models.HistoryDevice
import com.resolum.alivia.features.tracking.domain.models.HistoryEvent
import com.resolum.alivia.features.tracking.domain.models.LiveHistoryMessage
import com.resolum.alivia.features.tracking.domain.models.mergeHistory
import com.resolum.alivia.features.tracking.domain.usecases.FollowHomeHistoryUseCase
import com.resolum.alivia.features.tracking.domain.usecases.GetHistoryDevicesUseCase
import com.resolum.alivia.features.tracking.domain.usecases.GetHomeHistoryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val person: AssistedPerson? = null,
    val noPerson: Boolean = false,
    val events: List<HistoryEvent> = emptyList(),
    val devices: Map<Long, HistoryDevice> = emptyMap(),
    val filter: HistoryCategory? = null,
    val live: Boolean = false,
) {
    /** The rows to show, with the filter applied. */
    val entries get() = events.map { it.describe(it.deviceId?.let(devices::get)) }.filter { filter == null || it.category == filter }
}

/**
 * The history of the home: loads the latest events over REST and then listens to the live channel, so each action the
 * voice edge reports appears at once. When the channel drops it reloads (nothing is missed) and connects again.
 */
class HistoryViewModel(
    private val getPersons: GetAssistedPersonsUseCase,
    private val getHistory: GetHomeHistoryUseCase,
    private val getDevices: GetHistoryDevicesUseCase,
    private val follow: FollowHomeHistoryUseCase,
    private val retryMinMs: Long = 2_000,
    private val retryMaxMs: Long = 30_000,
) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private var job: Job? = null

    /** Starts loading and listening; called while the screen is visible. */
    fun start() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val person = _state.value.person ?: findPerson() ?: return@launch
            var wait = retryMinMs
            while (true) {
                if (reload(person.id)) {
                    try {
                        follow(person.id).collect { message ->
                            when (message) {
                                LiveHistoryMessage.Connected -> {
                                    wait = retryMinMs
                                    _state.update { it.copy(live = true) }
                                }
                                is LiveHistoryMessage.Event -> onLive(person.id, message.event)
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // the channel dropped: reload and connect again
                    }
                }
                _state.update { it.copy(live = false) }
                delay(wait)
                wait = (wait * 2).coerceAtMost(retryMaxMs)
            }
        }
    }

    /** Stops listening while the screen is not visible. */
    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(live = false) }
    }

    fun select(filter: HistoryCategory?) = _state.update { it.copy(filter = filter) }

    private suspend fun findPerson(): AssistedPerson? = when (val result = getPersons()) {
        is NetworkResult.Success -> result.data.firstOrNull().also { person ->
            _state.update { it.copy(person = person, noPerson = person == null, loading = person != null) }
        }
        is NetworkResult.Error -> {
            _state.update { it.copy(loading = false, error = result.message) }
            null
        }
    }

    private suspend fun reload(personId: Long): Boolean {
        if (_state.value.devices.isEmpty()) loadDevices(personId)
        return when (val result = getHistory(personId)) {
            is NetworkResult.Success -> {
                _state.update { it.copy(loading = false, error = null, events = mergeHistory(it.events, result.data)) }
                true
            }
            is NetworkResult.Error -> {
                _state.update { it.copy(loading = false, error = result.message) }
                false
            }
        }
    }

    private suspend fun loadDevices(personId: Long) {
        (getDevices(personId) as? NetworkResult.Success)?.let { result ->
            _state.update { it.copy(devices = result.data.associateBy(HistoryDevice::id)) }
        }
    }

    private suspend fun onLive(personId: Long, event: HistoryEvent) {
        _state.update { it.copy(events = mergeHistory(it.events, listOf(event))) }
        // A device installed after the screen opened is named on its first event.
        if (event.deviceId != null && event.deviceId !in _state.value.devices) loadDevices(personId)
    }
}
