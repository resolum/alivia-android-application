package com.resolum.alivia.features.nursing.domain.repositories

import com.resolum.alivia.features.nursing.domain.models.MedicationAdministration
import com.resolum.alivia.features.nursing.domain.models.MedicationResult
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.LocalDateTime

interface MedicationAdministrationRepository {
    suspend fun find(id: String): NetworkResult<MedicationAdministration>
    suspend fun record(id: String, result: MedicationResult, reason: String?, occurredAt: LocalDateTime): NetworkResult<MedicationAdministration>
}
