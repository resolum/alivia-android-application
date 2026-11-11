package com.resolum.alivia.features.nursing.domain.repositories

import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.models.CareNetworkMember
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

interface CareNetworkRepository {
    suspend fun assistedPersons(): NetworkResult<List<AssistedPerson>>
    suspend fun members(householdId: Long): NetworkResult<List<CareNetworkMember>>
}
