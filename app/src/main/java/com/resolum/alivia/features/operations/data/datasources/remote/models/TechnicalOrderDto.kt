package com.resolum.alivia.features.operations.data.datasources.remote.models

/** Technical order as {@code /api/v1/technical-orders} returns it. */
data class TechnicalOrderDto(
    val id: Long?,
    val code: String?,
    val draftId: String?,
    val serviceType: String?,
    val incidentId: Long?,
    val status: String?,
    val date: String?,
    val startTime: String?,
    val endTime: String?,
    val technicianId: Long?,
    val technicianName: String?,
    val contactName: String?,
    val contactPhone: String?,
    val directions: String?,
    val evaluationResult: String?,
    val installationResult: String?,
    val completionNotes: String?,
    val startedAt: String?,
    val completedAt: String?,
    val installedDevices: List<InstalledDeviceDto>?,
)

data class InstalledDeviceDto(val alias: String?, val macAddress: String?, val type: String?)

data class EvaluationResultRequestDto(val result: String, val notes: String?)

data class InstallationResultRequestDto(val result: String, val notes: String?)

data class RegisterDeviceRequestDto(val alias: String, val macAddress: String, val type: String)
