package com.resolum.alivia.features.nursing.domain.usecases

import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.repositories.CareNetworkRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class GetAssistedPersonsUseCase(private val repository: CareNetworkRepository) {
    suspend operator fun invoke() = repository.assistedPersons()
}

class GetCareNetworkUseCase(private val repository: CareNetworkRepository) {
    suspend operator fun invoke(householdId: Long) = repository.members(householdId)
}

class GetCaregiversUseCase(private val repository: CareNetworkRepository) {
    suspend operator fun invoke(person: AssistedPerson) = repository.caregivers(person.id, person.householdId)
}

/** Validates the form, then creates the activity with its schedule and responsible caregiver. */
class CreateCareActivityUseCase(private val repository: CareNetworkRepository) {
    suspend operator fun invoke(
        person: AssistedPerson?,
        type: CareActivityType?,
        title: String,
        description: String,
        caregiverId: Long?,
        date: LocalDate,
        days: Set<DayOfWeek>,
        start: LocalTime,
        end: LocalTime,
        today: LocalDate,
    ): NetworkResult<Long> {
        val cleanTitle = title.trim()
        val cleanDescription = description.trim().ifEmpty { null }
        return when {
            person == null -> NetworkResult.Error("No tienes una persona asistida asignada.")
            type == null -> NetworkResult.Error("Elige el tipo de actividad.")
            cleanTitle.isEmpty() -> NetworkResult.Error("Escribe el nombre de la actividad.")
            cleanTitle.length > 120 -> NetworkResult.Error("El nombre admite hasta 120 caracteres.")
            (cleanDescription?.length ?: 0) > 500 -> NetworkResult.Error("La instrucción admite hasta 500 caracteres.")
            days.isEmpty() && date.isBefore(today) -> NetworkResult.Error("La fecha no puede ser anterior a hoy.")
            end == start -> NetworkResult.Error("La hora de inicio y de fin no pueden ser iguales.")
            caregiverId == null -> NetworkResult.Error("Elige al responsable de la actividad.")
            else -> when (val result = repository.createActivity(person, type, cleanTitle, cleanDescription, caregiverId, date, days, start, end)) {
                is NetworkResult.Error -> if (result.code == 409) {
                    NetworkResult.Error("El responsable ya tiene otra actividad en ese horario.", 409)
                } else result
                is NetworkResult.Success -> result
            }
        }
    }
}

/** Records the occurrence as completed; the backend accepts it only between its start and end. */
class CompleteActivityUseCase(private val repository: CareNetworkRepository) {
    suspend operator fun invoke(activity: CareActivity, notes: String = ""): NetworkResult<Unit> {
        val activityId = activity.activityId
        val assignmentId = activity.assignmentId
        if (activityId == null || assignmentId == null) return NetworkResult.Error("Esta actividad no se puede marcar desde la app.")
        return when (val result = repository.complete(activityId, assignmentId, activity.date, notes.trim().ifEmpty { null })) {
            is NetworkResult.Error -> if (result.code == 400) {
                NetworkResult.Error("Solo puedes marcarla mientras está en curso, entre su hora de inicio y de fin.", 400)
            } else result
            is NetworkResult.Success -> result
        }
    }
}

/**
 * Names who each occurrence is for and who is responsible, since the agenda leaves the names empty: "Tú" for the
 * signed-in caregiver, the other caregiver's name when the home lists it, and the assisted person's name.
 */
class NameAgendaPeopleUseCase(private val repository: CareNetworkRepository) {
    suspend operator fun invoke(activities: List<CareActivity>): List<CareActivity> {
        val people = (repository.assistedPersons() as? NetworkResult.Success)?.data.orEmpty()
        val persons = people.mapNotNull { person -> person.personProfileId?.let { it to person.fullName } }.toMap()
        val caregivers = people.flatMap { person ->
            (repository.caregivers(person.id, person.householdId) as? NetworkResult.Success)?.data.orEmpty()
        }.associateBy { it.id }
        return activities.map { activity ->
            activity.copy(
                assistedPerson = activity.assistedPerson ?: activity.assistedPersonProfileId?.let(persons::get),
                responsible = activity.responsible ?: activity.responsibleCaregiverId?.let { id ->
                    val caregiver = caregivers[id]
                    when {
                        caregiver?.isMe == true -> "Tú"
                        caregiver?.fullName != null -> caregiver.fullName
                        else -> "Otro cuidador"
                    }
                },
            )
        }
    }
}
