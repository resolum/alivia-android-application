package com.resolum.alivia.features.nursing.domain.repositories

import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.LocalDate

interface ActivityRepository {
    suspend fun agenda(from: LocalDate, to: LocalDate): NetworkResult<List<CareActivity>>
}
