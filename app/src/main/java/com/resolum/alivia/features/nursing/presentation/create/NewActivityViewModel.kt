package com.resolum.alivia.features.nursing.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.CareCaregiver
import com.resolum.alivia.features.nursing.domain.usecases.CreateCareActivityUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetAssistedPersonsUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetCaregiversUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class NewActivityStep { ACTIVITY, SCHEDULE }

data class NewActivityUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val step: NewActivityStep = NewActivityStep.ACTIVITY,
    val person: AssistedPerson? = null,
    val caregivers: List<CareCaregiver> = emptyList(),
    val type: CareActivityType? = null,
    val title: String = "",
    val description: String = "",
    val date: LocalDate = Formats.today(),
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(9, 30),
    val weekly: Boolean = false,
    val days: Set<DayOfWeek> = emptySet(),
    val caregiverId: Long? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val canContinue: Boolean get() = person != null && type != null && title.isNotBlank()
    val canSave: Boolean get() = canContinue && blockedReason == null && !saving

    /** An end earlier than the start means the activity ends the next day, as the backend allows. */
    val endsNextDay: Boolean get() = end.isBefore(start)

    /** Why "Confirmar programación" is disabled, shown above the button. */
    val blockedReason: String?
        get() = when {
            caregiverId == null -> "Elige al responsable de la actividad."
            end == start -> "La hora de inicio y de fin no pueden ser iguales."
            weekly && days.isEmpty() -> "Elige al menos un día de la semana."
            else -> null
        }
}

/** Two steps: what the activity is, then when it happens and who is responsible. */
class NewActivityViewModel(
    private val getAssistedPersons: GetAssistedPersonsUseCase,
    private val getCaregivers: GetCaregiversUseCase,
    private val createActivity: CreateCareActivityUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(NewActivityUiState())
    val state: StateFlow<NewActivityUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val persons = getAssistedPersons()) {
                is NetworkResult.Error -> _state.update { it.copy(loading = false, error = persons.message) }
                is NetworkResult.Success -> {
                    val person = persons.data.firstOrNull()
                    val caregivers = person?.let { (getCaregivers(it) as? NetworkResult.Success)?.data }.orEmpty()
                    _state.update {
                        it.copy(loading = false, person = person, caregivers = caregivers,
                            caregiverId = it.caregiverId ?: caregivers.firstOrNull { c -> c.isMe }?.id ?: caregivers.firstOrNull()?.id)
                    }
                }
            }
        }
    }

    fun selectType(type: CareActivityType) = _state.update {
        it.copy(type = type, title = if (it.title.isBlank() || it.title == it.type?.let(::suggestion)) suggestion(type) else it.title, error = null)
    }

    fun onTitle(value: String) = _state.update { it.copy(title = value.take(120), error = null) }
    fun onDescription(value: String) = _state.update { it.copy(description = value.take(500), error = null) }
    fun onDate(value: LocalDate) = _state.update { it.copy(date = value, error = null) }

    /** A start equal to the end pushes the end 30 minutes after it; an earlier end means it ends the next day. */
    fun onStart(value: LocalTime) = _state.update {
        it.copy(start = value, end = if (it.end == value) value.plusMinutes(30) else it.end, error = null)
    }

    fun onEnd(value: LocalTime) = _state.update { it.copy(end = value, error = null) }

    fun setWeekly(weekly: Boolean) = _state.update {
        it.copy(weekly = weekly, days = if (weekly && it.days.isEmpty()) setOf(it.date.dayOfWeek) else it.days, error = null)
    }

    fun toggleDay(day: DayOfWeek) = _state.update { it.copy(days = if (day in it.days) it.days - day else it.days + day, error = null) }
    fun selectCaregiver(id: Long) = _state.update { it.copy(caregiverId = id, error = null) }

    fun next() {
        if (_state.value.canContinue) _state.update { it.copy(step = NewActivityStep.SCHEDULE, error = null) }
        else _state.update { it.copy(error = "Elige el tipo y escribe el nombre de la actividad.") }
    }

    /** Back arrow: to the first step, or false when the screen should close. */
    fun back(): Boolean {
        if (_state.value.step == NewActivityStep.ACTIVITY) return false
        _state.update { it.copy(step = NewActivityStep.ACTIVITY, error = null) }
        return true
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = createActivity(
                person = s.person, type = s.type, title = s.title, description = s.description, caregiverId = s.caregiverId,
                date = s.date, days = if (s.weekly) s.days else emptySet(), start = s.start, end = s.end, today = Formats.today(),
            )
            when (result) {
                is NetworkResult.Success -> _state.update { it.copy(saving = false, saved = true) }
                is NetworkResult.Error -> _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    private fun suggestion(type: CareActivityType) = when (type) {
        CareActivityType.HYGIENE -> "Aseo personal"
        CareActivityType.MEDICATION -> "Medicación"
        CareActivityType.APPOINTMENT -> "Cita médica"
        CareActivityType.THERAPY -> "Sesión de terapia"
        CareActivityType.MEAL -> "Alimentación"
        CareActivityType.BATH -> "Baño asistido"
        CareActivityType.EXERCISE -> "Paseo asistido"
        CareActivityType.OTHER -> ""
    }
}
