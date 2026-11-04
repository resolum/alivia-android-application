package com.resolum.alivia.features.operations.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.operations.domain.models.ServiceType
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.usecases.GetMyOrdersUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class OrdersView { LIST, CALENDAR }

enum class OrderFilter(val label: String, val type: ServiceType?) {
    ALL("Todas", null),
    EVALUATIONS("Revisiones", ServiceType.EVALUATION),
    INSTALLATIONS("Instalaciones", ServiceType.INSTALLATION),
    INCIDENTS("Incidencias", ServiceType.MAINTENANCE),
}

data class OrdersUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val orders: List<TechnicalOrder> = emptyList(),
    val view: OrdersView = OrdersView.LIST,
    val filter: OrderFilter = OrderFilter.ALL,
    val today: LocalDate = Formats.today(),
    val selectedDay: LocalDate = Formats.today(),
) {
    val filtered: List<TechnicalOrder>
        get() = orders.filter { filter.type == null || it.serviceType == filter.type }

    /** The order the technician attends next: the first open one from today on. */
    val nextOrderId: Long?
        get() = orders.filter { it.isOpen && !it.date.isBefore(today) }
            .minWithOrNull(compareBy({ it.date }, { it.startTime }))?.id

    val summary: String
        get() {
            val open = orders.filter { it.isOpen }
            val todayCount = open.count { it.date == today }
            val tomorrowCount = open.count { it.date == today.plusDays(1) }
            val later = open.count { it.date.isAfter(today.plusDays(1)) }
            return "$todayCount hoy · $tomorrowCount mañana · $later ${if (later == 1) "próxima" else "próximas"}"
        }
}

class OrdersViewModel(private val getMyOrders: GetMyOrdersUseCase) : ViewModel() {
    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /**
     * Loads the orders again. The screen calls it every time it is shown (entering the tab, coming back from an
     * order or to the app), so new assignments appear without pulling. The current list stays visible meanwhile,
     * and a load already running is reused unless the technician pulls to refresh.
     */
    fun load(refresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!refresh) return
            loadJob?.cancel()
        }
        _state.update { it.copy(loading = it.orders.isEmpty(), refreshing = refresh, error = null, today = Formats.today()) }
        loadJob = viewModelScope.launch {
            when (val result = getMyOrders()) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, refreshing = false, orders = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(loading = false, refreshing = false, error = result.message) }
            }
        }
    }

    fun selectView(view: OrdersView) = _state.update { it.copy(view = view) }
    fun selectFilter(filter: OrderFilter) = _state.update { it.copy(filter = filter) }
    fun selectDay(day: LocalDate) = _state.update { it.copy(selectedDay = day) }
}
