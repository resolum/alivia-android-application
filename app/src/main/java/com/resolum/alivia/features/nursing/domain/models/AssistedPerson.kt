package com.resolum.alivia.features.nursing.domain.models

import java.time.LocalDate
import java.time.Period

/** Person the caregiver looks after, as `/api/v1/assisted-persons` returns them. */
data class AssistedPerson(
    val id: Long,
    val householdId: Long?,
    val firstName: String,
    val lastName: String,
    val dni: String?,
    val birthDate: LocalDate?,
    val phoneNumber: String?,
    val kinship: String?,
    val mobilityLevel: String?,
    val emergencyContactName: String?,
    val emergencyContactPhone: String?,
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
    val initials: String get() = listOf(firstName, lastName).mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
    fun age(today: LocalDate): Int? = birthDate?.let { Period.between(it, today).years }

    /** How the assisted person relates to the principal relative ("Madre o padre" for PARENT). */
    val kinshipLabel: String?
        get() = when (kinship) {
            "PARENT" -> "Madre o padre"
            "GRANDPARENT" -> "Abuela o abuelo"
            "AUNT_UNCLE" -> "Tía o tío"
            "SPOUSE" -> "Cónyuge"
            "SIBLING" -> "Hermana o hermano"
            "OTHER" -> "Otro"
            else -> null
        }

    val mobilityLabel: String?
        get() = when (mobilityLevel) {
            "PARTIAL" -> "Movilidad parcial"
            "VERY_LIMITED" -> "Movilidad muy limitada"
            "NO_LIMB_MOBILITY" -> "Sin movilidad de extremidades"
            else -> null
        }
}

enum class CareRole(val label: String) { PRINCIPAL("Principal"), CAREGIVER("Cuidador"), GUEST("Invitado") }

/** A member of the home's care network; the backend only gives the account and the role. */
data class CareNetworkMember(val userId: Long, val role: CareRole)
