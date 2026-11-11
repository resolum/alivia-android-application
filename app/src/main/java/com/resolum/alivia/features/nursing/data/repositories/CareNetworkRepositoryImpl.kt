package com.resolum.alivia.features.nursing.data.repositories

import com.resolum.alivia.features.nursing.data.datasources.remote.services.CareNetworkService
import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.models.CareNetworkMember
import com.resolum.alivia.features.nursing.domain.models.CareRole
import com.resolum.alivia.features.nursing.domain.repositories.CareNetworkRepository
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import java.time.LocalDate

class CareNetworkRepositoryImpl(private val service: CareNetworkService) : BaseRepository(), CareNetworkRepository {
    override suspend fun assistedPersons() = safeCall {
        service.assistedPersons().mapNotNull { dto ->
            AssistedPerson(
                id = dto.id ?: return@mapNotNull null,
                householdId = dto.householdId,
                firstName = dto.firstName?.trim().orEmpty(),
                lastName = dto.lastName?.trim().orEmpty(),
                dni = dto.dni?.trim()?.ifEmpty { null },
                birthDate = dto.birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                phoneNumber = dto.phoneNumber?.trim()?.ifEmpty { null },
                kinship = dto.kinship,
                mobilityLevel = dto.mobilityLevel,
                emergencyContactName = dto.emergencyContactName?.trim()?.ifEmpty { null },
                emergencyContactPhone = dto.emergencyContactPhone?.trim()?.ifEmpty { null },
            )
        }
    }

    override suspend fun members(householdId: Long) = safeCall {
        service.members(householdId)
            .filter { it.status == null || it.status == "ACTIVE" }
            .mapNotNull { dto ->
                val role = CareRole.entries.firstOrNull { it.name == dto.responsibility } ?: return@mapNotNull null
                CareNetworkMember(dto.userId ?: return@mapNotNull null, role)
            }
            .sortedBy { it.role.ordinal }
    }
}
