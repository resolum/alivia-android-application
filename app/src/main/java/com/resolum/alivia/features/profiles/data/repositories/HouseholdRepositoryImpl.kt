package com.resolum.alivia.features.profiles.data.repositories

import com.resolum.alivia.features.profiles.data.datasources.remote.services.HouseholdService
import com.resolum.alivia.features.profiles.domain.models.Household
import com.resolum.alivia.features.profiles.domain.repositories.HouseholdRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.repositories.BaseRepository

class HouseholdRepositoryImpl(private val service: HouseholdService) : BaseRepository(), HouseholdRepository {
    override suspend fun mine(): NetworkResult<Household?> = when (val result = safeCall { service.mine() }) {
        is NetworkResult.Success -> NetworkResult.Success(
            result.data.id?.let { id ->
                Household(id, result.data.alias?.trim()?.ifEmpty { null } ?: "Mi hogar", result.data.formattedAddress?.trim()?.ifEmpty { null })
            },
        )
        is NetworkResult.Error -> if (result.code == 404) NetworkResult.Success(null) else result
    }
}
