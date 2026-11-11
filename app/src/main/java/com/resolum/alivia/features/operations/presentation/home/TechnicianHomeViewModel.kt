package com.resolum.alivia.features.operations.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.operations.domain.models.OrderProgress
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.models.WorkingDay
import com.resolum.alivia.features.operations.domain.usecases.GetMyOrdersUseCase
import com.resolum.alivia.features.operations.domain.usecases.GetMyScheduleUseCase
import com.resolum.alivia.features.profiles.domain.usecases.GetProfileUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TechnicianHomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val today: LocalDate = Formats.today(),
    val firstName: String? = null,
    val todayShifts: List<WorkingDay.Shift> = emptyList(),
    val orders: List<TechnicalOrder> = emptyList(),
) {
    /** Orders of today, in the order the technician attends them. */
    val todayOrders: List<TechnicalOrder>
        get() = orders.filter { it.date == today && it.progress != OrderProgress.CANCELED }.sortedBy { it.startTime }

    /** The last orders the technician closed, the most recent first. */
    val latestCompleted: List<TechnicalOrder>
        get() = orders.filter { it.progress == OrderProgress.COMPLETED }
            .sortedWith(compareByDescending<TechnicalOrder> { it.completedAt }.thenByDescending { it.date })
            .take(LATEST)

    private companion object {
        const val LATEST = 3
    }
}

/** Technician's start screen: greeting, today's working hours, today's orders and the last ones closed. */
class TechnicianHomeViewModel(
    private val getMyOrders: GetMyOrdersUseCase,
    private val getMySchedule: GetMyScheduleUseCase,
    private val getProfile: GetProfileUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(TechnicianHomeUiState())
    val state: StateFlow<TechnicianHomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun load(refresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!refresh) return
            loadJob?.cancel()
        }
        val today = Formats.today()
        _state.update { it.copy(loading = it.orders.isEmpty() && it.error == null, refreshing = refresh, error = null, today = today) }
        loadJob = viewModelScope.launch {
            val orders = async { getMyOrders() }
            val schedule = async { getMySchedule(today) }
            val profile = async { getProfile() }
            // The greeting and the working hours are extras: only the orders decide whether the screen failed.
            val name = (profile.await() as? NetworkResult.Success)?.data?.firstName?.ifBlank { null }
            val shifts = (schedule.await() as? NetworkResult.Success)?.data?.days?.firstOrNull { it.date == today }?.shifts
            when (val result = orders.await()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, refreshing = false, orders = result.data,
                        firstName = name ?: it.firstName, todayShifts = shifts ?: it.todayShifts)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, refreshing = false, error = result.message,
                        firstName = name ?: it.firstName, todayShifts = shifts ?: it.todayShifts)
                }
            }
        }
    }
}
