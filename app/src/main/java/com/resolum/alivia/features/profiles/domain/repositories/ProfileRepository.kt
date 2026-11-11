package com.resolum.alivia.features.profiles.domain.repositories

import com.resolum.alivia.features.profiles.domain.models.Profile
import com.resolum.alivia.features.profiles.domain.models.ProfileChanges
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

interface ProfileRepository {
    suspend fun me(): NetworkResult<Profile>
    suspend fun update(changes: ProfileChanges): NetworkResult<Profile>
}
