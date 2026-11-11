package com.resolum.alivia.features.nursing.data.datasources.remote.models

/** Activity of `/api/v1/care-agenda` as the backend returns it. */
data class ActivityResponseDto(
    val activity: ActivityDto? = null,
    val assignmentId: Long? = null,
    val assistedPerson: AssistedPersonDto? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val responsible: ResponsibleDto? = null,
    val recurring: Boolean? = null,
    val status: String? = null,
    val priority: String? = null,
) {
    data class ActivityDto(val id: Long? = null, val title: String? = null, val type: String? = null)
    data class AssistedPersonDto(val id: Long? = null, val name: String? = null)
    data class ResponsibleDto(val type: String? = null, val id: Long? = null, val name: String? = null, val email: String? = null)
}
