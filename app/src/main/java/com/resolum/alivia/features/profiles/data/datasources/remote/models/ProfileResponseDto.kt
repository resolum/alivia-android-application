package com.resolum.alivia.features.profiles.data.datasources.remote.models

/** Profile as `/api/v1/profiles/me` returns it. */
data class ProfileResponseDto(
    val emailAddress: String? = null,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val dni: String? = null,
    val birthDate: String? = null,
)
