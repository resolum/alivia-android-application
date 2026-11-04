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
) {
    /** Name of the home in the lists; the backend keeps who receives the visit, not a home name. */
    val homeName: String get() = contactName?.let { "Hogar de $it" } ?: "Hogar de la solicitud"

    val isOpen: Boolean get() = progress == OrderProgress.SCHEDULED || progress == OrderProgress.IN_PROGRESS
}
