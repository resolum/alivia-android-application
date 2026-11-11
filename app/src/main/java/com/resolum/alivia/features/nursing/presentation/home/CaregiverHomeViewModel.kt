package com.resolum.alivia.features.nursing.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.CareNetworkMember
import com.resolum.alivia.features.nursing.domain.usecases.GetActivitiesUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetAssistedPersonsUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetCareNetworkUseCase
import com.resolum.alivia.features.profiles.domain.models.Household
import com.resolum.alivia.features.profiles.domain.usecases.GetMyHouseholdUseCase
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
import java.time.LocalDateTime

data class CaregiverHomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val today: LocalDate = Formats.today(),
    val firstName: String? = null,
    val household: Household? = null,
    val activities: List<CareActivity> = emptyList(),
    val assistedPerson: AssistedPerson? = null,
    val network: List<CareNetworkMember> = emptyList(),
    val now: LocalDateTime = LocalDateTime.now(Formats.zone),
) {
    /** The first pending activity from now on. */
    val nextActivity: CareActivity?
        get() = activities
            .filter { it.status == CareActivityStatus.SCHEDULED || it.status == CareActivityStatus.IN_PROGRESS }
            .filter { !LocalDateTime.of(it.date, it.endTime ?: it.startTime).isBefore(now) }
            .minWithOrNull(compareBy({ it.date }, { it.startTime }))

    val todayCount: Int get() = activities.count { it.date == today }

}

/** Caregiver's start screen: greeting, next activity, care network and the assisted person. */
class CaregiverHomeViewModel(
    private val getActivities: GetActivitiesUseCase,
    private val getProfile: GetProfileUseCase,
    private val getHousehold: GetMyHouseholdUseCase,
    private val getAssistedPersons: GetAssistedPersonsUseCase,
    private val getCareNetwork: GetCareNetworkUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(CaregiverHomeUiState())
    val state: StateFlow<CaregiverHomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun load(refresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!refresh) return
            loadJob?.cancel()
        }
        val today = Formats.today()
        _state.update {
            it.copy(loading = it.activities.isEmpty() && it.household == null, refreshing = refresh, error = null,
                today = today, now = LocalDateTime.now(Formats.zone))
        }
        loadJob = viewModelScope.launch {
            val activities = async { getActivities(today) }
            val profile = async { getProfile() }
            val household = async { getHousehold() }
            val persons = async { getAssistedPersons() }
            val name = (profile.await() as? NetworkResult.Success)?.data?.firstName?.ifBlank { null }
            val home = (household.await() as? NetworkResult.Success)?.data
            val person = (persons.await() as? NetworkResult.Success)?.data?.firstOrNull()
            val network = (person?.householdId ?: home?.id)?.let { id -> (getCareNetwork(id) as? NetworkResult.Success)?.data }
            _state.update {
                it.copy(assistedPerson = person ?: it.assistedPerson, network = network ?: it.network)
            }
            when (val result = activities.await()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(loading = false, refreshing = false, activities = result.data, firstName = name ?: it.firstName,
                        household = home ?: it.household)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, refreshing = false, error = result.message, firstName = name ?: it.firstName,
                        household = home ?: it.household)
                }
            }
        }
    }
}
