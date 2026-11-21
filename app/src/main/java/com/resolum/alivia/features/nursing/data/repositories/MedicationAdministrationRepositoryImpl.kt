package com.resolum.alivia.features.nursing.data.repositories

import com.resolum.alivia.features.nursing.data.datasources.remote.mappers.MedicationAdministrationMapper
import com.resolum.alivia.features.nursing.data.datasources.remote.services.MedicationAdministrationDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.MedicationAdministrationService
import com.resolum.alivia.features.nursing.data.datasources.remote.services.RecordMedicationOutcomeRequestDto
import com.resolum.alivia.features.nursing.domain.models.MedicationAdministration
import com.resolum.alivia.features.nursing.domain.models.MedicationResult
import com.resolum.alivia.features.nursing.domain.repositories.MedicationAdministrationRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import java.time.LocalDateTime

class MedicationAdministrationRepositoryImpl(private val service: MedicationAdministrationService) :
    BaseRepository(), MedicationAdministrationRepository {

    override suspend fun find(id: String) = mapped(safeCall { service.find(id) })

    override suspend fun record(id: String, result: MedicationResult, reason: String?, occurredAt: LocalDateTime) =
        mapped(safeCall { service.record(id, RecordMedicationOutcomeRequestDto(result.name, reason, occurredAt.toString())) })

    private fun mapped(result: NetworkResult<MedicationAdministrationDto>): NetworkResult<MedicationAdministration> = when (result) {
        is NetworkResult.Error -> result
        is NetworkResult.Success -> MedicationAdministrationMapper.toDomain(result.data)?.let { NetworkResult.Success(it) }
            ?: NetworkResult.Error("El servidor devolvió una administración incompleta.")
    }
}
