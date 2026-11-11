package com.resolum.alivia.features.profiles.domain.models

/** The home the signed-in caregiver belongs to, and their role in it (PRINCIPAL, CAREGIVER or GUEST). */
data class Household(val id: Long, val alias: String, val address: String?, val myRole: String? = null) {
    /** Only the principal relative plans activities; caregivers and guests follow their schedule. */
    val canPlanActivities: Boolean get() = myRole == "PRINCIPAL"
}
