package com.resolum.alivia.features.iam.domain.models

/** Roles of the Alivia accounts, as the backend names them. */
enum class UserRole {
    ADMINISTRATOR, CAREGIVER, TECHNICIAN, SUBSCRIPTION_MANAGER, ASSISTED_PERSON, UNKNOWN;

    /** The mobile app is for the technicians in the field and for the families (caregivers). */
    val canUseMobileApp: Boolean get() = this == TECHNICIAN || this == CAREGIVER

    companion object {
        fun from(value: String?): UserRole = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

/** Credentials typed in the sign-in screen. */
data class SignIn(val email: String, val password: String)

/** Signed-in account. */
data class Session(val accountId: String, val email: String, val role: UserRole)
