package com.resolum.alivia.features.profiles.domain.models

/** The home the signed-in caregiver belongs to. */
data class Household(val id: Long, val alias: String, val address: String?)
