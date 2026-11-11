package com.resolum.alivia.features.profiles.domain.repositories

import com.resolum.alivia.features.profiles.domain.models.Household
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

interface HouseholdRepository {
    /** The caregiver's home, or null when the account is not linked to one yet. */
    suspend fun mine(): NetworkResult<Household?>
}
