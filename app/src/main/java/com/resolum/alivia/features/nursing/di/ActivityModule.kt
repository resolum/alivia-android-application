package com.resolum.alivia.features.nursing.di

import com.resolum.alivia.features.nursing.data.datasources.remote.ActivityFacadeService
import com.resolum.alivia.features.nursing.data.datasources.remote.services.ActivityService
import com.resolum.alivia.features.nursing.data.datasources.remote.services.CareNetworkService
import com.resolum.alivia.features.nursing.data.repositories.ActivityRepositoryImpl
import com.resolum.alivia.features.nursing.data.repositories.CareNetworkRepositoryImpl
import com.resolum.alivia.features.nursing.domain.repositories.CareNetworkRepository
import com.resolum.alivia.features.nursing.domain.usecases.CompleteActivityUseCase
import com.resolum.alivia.features.nursing.domain.usecases.CreateCareActivityUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetAssistedPersonsUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetCaregiversUseCase
import com.resolum.alivia.features.nursing.domain.usecases.NameAgendaPeopleUseCase
import com.resolum.alivia.features.nursing.domain.usecases.GetCareNetworkUseCase
import com.resolum.alivia.features.nursing.domain.repositories.ActivityRepository
import com.resolum.alivia.features.nursing.domain.usecases.GetActivitiesUseCase
import com.resolum.alivia.features.nursing.data.datasources.remote.services.MedicationAdministrationService
import com.resolum.alivia.features.nursing.data.repositories.MedicationAdministrationRepositoryImpl
import com.resolum.alivia.features.nursing.domain.repositories.MedicationAdministrationRepository
import com.resolum.alivia.features.nursing.domain.usecases.GetMedicationAdministrationUseCase
import com.resolum.alivia.features.nursing.domain.usecases.RecordMedicationOutcomeUseCase
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.shared.di.SharedModule
import com.resolum.alivia.features.nursing.reminders.AgendaChange
import com.resolum.alivia.features.nursing.reminders.AgendaReminders
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.LocalDateTime

object ActivityModule {
    private val repository: ActivityRepository by lazy {
        ActivityRepositoryImpl(ActivityFacadeService(SharedModule.retrofit.create(ActivityService::class.java)))
    }

    private val careNetworkRepository: CareNetworkRepository by lazy {
        CareNetworkRepositoryImpl(SharedModule.retrofit.create(CareNetworkService::class.java))
    }

    private val medicationRepository: MedicationAdministrationRepository by lazy {
        MedicationAdministrationRepositoryImpl(SharedModule.retrofit.create(MedicationAdministrationService::class.java))
    }

    fun getActivities() = GetActivitiesUseCase(repository)
    fun getAssistedPersons() = GetAssistedPersonsUseCase(careNetworkRepository)
    fun getCareNetwork() = GetCareNetworkUseCase(careNetworkRepository)
    fun getCaregivers() = GetCaregiversUseCase(careNetworkRepository)
    fun createActivity() = CreateCareActivityUseCase(careNetworkRepository)
    fun completeActivity() = CompleteActivityUseCase(careNetworkRepository)
    fun nameAgendaPeople() = NameAgendaPeopleUseCase(careNetworkRepository)
    fun getMedicationAdministration() = GetMedicationAdministrationUseCase(medicationRepository)
    fun recordMedicationOutcome() = RecordMedicationOutcomeUseCase(medicationRepository) { LocalDateTime.now(Formats.zone) }

    /** Whether the dose still has no result; null when it could not be checked. */
    suspend fun isDosePending(administrationId: String): Boolean? =
        (medicationRepository.find(administrationId) as? NetworkResult.Success)?.data?.pending

    /** Loads the agenda and refreshes the local reminders, for when the agenda screen is not open. */
    suspend fun syncReminders(): AgendaChange? {
        val result = getActivities()(Formats.today()) as? NetworkResult.Success ?: return null
        return AgendaReminders.sync(nameAgendaPeople()(result.data))
    }
}
