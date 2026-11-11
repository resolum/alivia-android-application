package com.resolum.alivia.features.nursing.domain.models

import java.time.LocalDate
import java.time.LocalTime

enum class CareActivityType { HYGIENE, MEDICATION, APPOINTMENT, THERAPY, MEAL, BATH, EXERCISE, OTHER }

enum class CareActivityStatus { SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, MISSED, OMITTED }

enum class CarePriority { HIGH, MEDIUM, LOW, ROUTINE }

/** One activity of the care agenda assigned to a day, as the caregiver sees it in the list. */
data class CareActivity(
    val id: String,
    /** Id of the care activity, needed to record its completion. */
    val activityId: Long?,
    val assignmentId: Long?,
    val title: String,
    val type: CareActivityType,
    /** Null when the backend does not send the names. */
    val assistedPerson: String?,
    val responsible: String?,
    /** Profile id of the assisted person and caregiver id of the responsible, to name them when the backend does not. */
    val assistedPersonProfileId: Long? = null,
    val responsibleCaregiverId: Long? = null,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime?,
    val status: CareActivityStatus,
    val recurring: Boolean,
    val priority: CarePriority,
)
