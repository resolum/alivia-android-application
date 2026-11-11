package com.resolum.alivia.features.nursing.presentation.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.usecases.GetActivitiesUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ActivityGroupKind(val title: String) { TODAY("Hoy"), TOMORROW("Mañana"), UPCOMING("Pasado mañana"), PAST("Ayer") }

data class ActivityGroup(val kind: ActivityGroupKind, val activities: List<CareActivity>)

data class ActivitiesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val today: LocalDate = Formats.today(),
    val activities: List<CareActivity> = emptyList(),
) {
    /** Hoy, Mañana, Pasado mañana and Ayer, skipping the empty ones; Ayer runs from the most recent. */
    val groups: List<ActivityGroup>
        get() {
            val byKind = activities.groupBy { kindOf(it.date) }
            return ActivityGroupKind.entries.mapNotNull { kind ->
                val sorted = byKind[kind]?.sortedWith(compareBy({ it.date }, { it.startTime })) ?: return@mapNotNull null
                ActivityGroup(kind, if (kind == ActivityGroupKind.PAST) sorted.reversed() else sorted)
            }
        }

    val summary: String
        get() = activities.count { it.date == today }.let { if (it == 1) "1 actividad hoy" else "$it actividades hoy" }

    private fun kindOf(date: LocalDate) = when {
        date == today -> ActivityGroupKind.TODAY
        date == today.plusDays(1) -> ActivityGroupKind.TOMORROW
        date.isAfter(today) -> ActivityGroupKind.UPCOMING
        else -> ActivityGroupKind.PAST
    }
}

class ActivitiesViewModel(private val getActivities: GetActivitiesUseCase) : ViewModel() {
    private val _state = MutableStateFlow(ActivitiesUiState())
    val state: StateFlow<ActivitiesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Loads the agenda again; the list stays visible meanwhile, and a running load is reused unless pulling. */
    fun load(refresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!refresh) return
            loadJob?.cancel()
        }
        val today = Formats.today()
        _state.update { it.copy(loading = it.activities.isEmpty(), refreshing = refresh, error = null, today = today) }
        loadJob = viewModelScope.launch {
            when (val result = getActivities(today)) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, refreshing = false, activities = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(loading = false, refreshing = false, error = result.message) }
            }
        }
    }
}
