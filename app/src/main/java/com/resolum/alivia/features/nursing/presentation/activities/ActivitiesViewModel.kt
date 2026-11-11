package com.resolum.alivia.features.nursing.presentation.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.usecases.CompleteActivityUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetActivitiesUseCase
import com.resolum.alivia.features.nursing.domain.usecases.NameAgendaPeopleUseCase
import com.resolum.alivia.features.profiles.domain.usecases.GetMyHouseholdUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

enum class ActivityGroupKind(val title: String) { TODAY("Hoy"), TOMORROW("Mañana"), UPCOMING("Pasado mañana"), PAST("Ayer") }

enum class ActivitiesView { LIST, CALENDAR }

data class ActivityGroup(val kind: ActivityGroupKind, val activities: List<CareActivity>)

data class ActivitiesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val today: LocalDate = Formats.today(),
    val activities: List<CareActivity> = emptyList(),
    val view: ActivitiesView = ActivitiesView.LIST,
    val month: YearMonth = YearMonth.from(Formats.today()),
    val monthActivities: List<CareActivity> = emptyList(),
    val monthLoading: Boolean = false,
    val selectedDay: LocalDate = Formats.today(),
    val selected: CareActivity? = null,
    val completing: Boolean = false,
    val completeError: String? = null,
    /** Only the principal relative creates activities; the rest only follow them. */
    val canCreate: Boolean = false,
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

    val daysWithActivities: Set<LocalDate> get() = monthActivities.map { it.date }.toSet()

    val selectedDayActivities: List<CareActivity>
        get() = monthActivities.filter { it.date == selectedDay }.sortedBy { it.startTime }

    private fun kindOf(date: LocalDate) = when {
        date == today -> ActivityGroupKind.TODAY
        date == today.plusDays(1) -> ActivityGroupKind.TOMORROW
        date.isAfter(today) -> ActivityGroupKind.UPCOMING
        else -> ActivityGroupKind.PAST
    }
}

/** The backend records a completion only while the occurrence is running, between its start and end. */
fun CareActivity.canComplete(now: LocalDateTime): Boolean {
    if (status != CareActivityStatus.SCHEDULED && status != CareActivityStatus.IN_PROGRESS) return false
    val start = LocalDateTime.of(date, startTime)
    val end = endTime?.let { LocalDateTime.of(if (it.isBefore(startTime)) date.plusDays(1) else date, it) }
    return !now.isBefore(start) && (end == null || !now.isAfter(end))
}

class ActivitiesViewModel(
    private val getActivities: GetActivitiesUseCase,
    private val completeActivity: CompleteActivityUseCase,
    private val namePeople: NameAgendaPeopleUseCase,
    private val getHousehold: GetMyHouseholdUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ActivitiesUiState())
    val state: StateFlow<ActivitiesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var monthJob: Job? = null

    /** Loads the agenda again; the list stays visible meanwhile, and a running load is reused unless pulling. */
    fun load(refresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!refresh) return
            loadJob?.cancel()
        }
        val today = Formats.today()
        _state.update { it.copy(loading = it.activities.isEmpty(), refreshing = refresh, error = null, today = today) }
        loadJob = viewModelScope.launch {
            (getHousehold() as? NetworkResult.Success)?.data?.let { home -> _state.update { it.copy(canCreate = home.canPlanActivities) } }
            when (val result = getActivities(today)) {
                is NetworkResult.Success -> {
                    val named = namePeople(result.data)
                    _state.update { it.copy(loading = false, refreshing = false, activities = named) }
                }
                is NetworkResult.Error -> _state.update { it.copy(loading = false, refreshing = false, error = result.message) }
            }
        }
        if (_state.value.view == ActivitiesView.CALENDAR) loadMonth()
    }

    fun selectView(view: ActivitiesView) {
        _state.update { it.copy(view = view) }
        if (view == ActivitiesView.CALENDAR) loadMonth()
    }

    fun moveMonth(delta: Long) {
        _state.update {
            val month = it.month.plusMonths(delta)
            it.copy(month = month, selectedDay = if (month == YearMonth.from(it.today)) it.today else month.atDay(1))
        }
        loadMonth()
    }

    fun selectDay(day: LocalDate) = _state.update { it.copy(selectedDay = day) }

    private fun loadMonth() {
        monthJob?.cancel()
        val month = _state.value.month
        _state.update { it.copy(monthLoading = true) }
        monthJob = viewModelScope.launch {
            when (val result = getActivities.between(month.atDay(1), month.atEndOfMonth())) {
                is NetworkResult.Success -> {
                    val named = namePeople(result.data)
                    _state.update { it.copy(monthLoading = false, monthActivities = named) }
                }
                is NetworkResult.Error -> _state.update { it.copy(monthLoading = false, error = result.message) }
            }
        }
    }

    fun open(activity: CareActivity?) = _state.update { it.copy(selected = activity, completeError = null) }

    fun complete() {
        val activity = _state.value.selected ?: return
        if (_state.value.completing) return
        _state.update { it.copy(completing = true, completeError = null) }
        viewModelScope.launch {
            when (val result = completeActivity(activity)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(completing = false, selected = null) }
                    load(refresh = true)
                }
                is NetworkResult.Error -> _state.update { it.copy(completing = false, completeError = result.message) }
            }
        }
    }
}
