package com.resolum.alivia.features.profiles.data.datasources.remote.models

/** Home as `/api/v1/households/mine` returns it. */
data class HouseholdResponseDto(
    val id: Long? = null,
    val alias: String? = null,
    val formattedAddress: String? = null,
    /** For `/mine`, only the signed-in account's own membership. */
    val members: List<MemberDto>? = null,
) {
    data class MemberDto(val responsibility: String? = null, val status: String? = null)
}
