package com.resolum.alivia.features.operations.domain.models

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class ServiceType { EVALUATION, INSTALLATION, MAINTENANCE }

/** Status the technician sees: a scheduled order is in progress once it was started at the home. */
enum class OrderProgress { SCHEDULED, IN_PROGRESS, COMPLETED, CANCELED }

enum class EvaluationResult { VIABLE, VIABLE_WITH_ADAPTATION, NOT_VIABLE }

enum class InstallationResult { APPROVED, REJECTED }

enum class DeviceType { MICROPHONE, ACTUATOR, SENSOR, HUB }

data class InstalledDevice(val alias: String, val macAddress: String, val type: DeviceType)

/** What the family reported from the web; unknown values from the backend read as OTHER. */
enum class IncidentProblem(val label: String) {
    BATTERY("Batería baja o agotada"),
    NO_RESPONSE("Sin respuesta"),
    FALSE_ALARM("Falsa alarma"),
    CONNECTIVITY("Problema de conexión"),
    PHYSICAL_DAMAGE("Daño físico"),
    OTHER("Otro problema"),
}

/** Incident stage as the family follows it on the web. */
enum class IncidentStatus(val label: String) {
    OPEN("Registrada"),
    IN_EVALUATION("En evaluación"),
    SCHEDULED("Visita programada"),
    IN_PROGRESS("En atención"),
    RESOLVED("Resuelta"),
    CANCELED("Cancelada"),
}

/** What the technician does next with the incident of a maintenance order, step by step. */
enum class IncidentAction { EVALUATE, CONFIRM_VISIT, START, RESOLVE }

/** The incident behind a maintenance order, with the device the family pointed at. */
data class IncidentSummary(
    val id: Long,
    val code: String,
    val status: IncidentStatus?,
    val problem: IncidentProblem,
    val description: String,
    val occurredAt: Instant?,
    val device: InstalledDevice?,
)

data class TechnicalOrder(
    val id: Long,
    val code: String,
    val draftId: String,
    val serviceType: ServiceType,
    val incidentId: Long?,
    val progress: OrderProgress,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val technicianName: String,
    val contactName: String?,
    val contactPhone: String?,
    val directions: String?,
    val evaluationResult: EvaluationResult?,
    val installationResult: InstallationResult?,
    val completionNotes: String?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val installedDevices: List<InstalledDevice>,
    val incident: IncidentSummary? = null,
) {
    /** Name of the home in the lists; the backend keeps who receives the visit, not a home name. */
    val homeName: String get() = contactName?.let { "Hogar de $it" } ?: "Hogar de la solicitud"

    val isOpen: Boolean get() = progress == OrderProgress.SCHEDULED || progress == OrderProgress.IN_PROGRESS

    /**
     * Next step of a maintenance order: evaluate the report, confirm the visit, start it at the home and resolve it.
     * Null for other orders, and once the order is closed.
     */
    val incidentAction: IncidentAction?
        get() = when {
            serviceType != ServiceType.MAINTENANCE -> null
            progress == OrderProgress.IN_PROGRESS -> IncidentAction.RESOLVE
            progress != OrderProgress.SCHEDULED -> null
            incident?.status == IncidentStatus.OPEN -> IncidentAction.EVALUATE
            incident?.status == IncidentStatus.IN_EVALUATION -> IncidentAction.CONFIRM_VISIT
            else -> IncidentAction.START
        }

    /** A maintenance order the technician already started: its incident is waiting for the resolution. */
    val awaitsResolution: Boolean get() = serviceType == ServiceType.MAINTENANCE && progress == OrderProgress.IN_PROGRESS
}
