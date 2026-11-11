package com.resolum.alivia.features.profiles.domain.models

import java.time.LocalDate

/** Personal profile of the signed-in account. Email, DNI and birth date are read-only. */
data class Profile(
    val firstName: String,
    val lastName: String,
    val email: String,
    /** E.164 without the plus sign, as the backend keeps it. */
    val phoneNumber: String?,
    val dni: String?,
    val birthDate: LocalDate?,
    val photoUrl: String?,
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    val initials: String get() = listOf(firstName, lastName).mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
}

/** Fields the user changed; the ones left null stay as they are. */
data class ProfileChanges(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val photo: ByteArray? = null,
) {
    val isEmpty: Boolean get() = firstName == null && lastName == null && phoneNumber == null && photo == null
}
