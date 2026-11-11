package com.resolum.alivia.features.profiles.data.datasources.remote.mappers

import com.resolum.alivia.features.profiles.data.datasources.remote.models.ProfileResponseDto
import com.resolum.alivia.features.profiles.domain.models.Profile
import java.time.LocalDate

object ProfileMapper {
    fun toDomain(dto: ProfileResponseDto) = Profile(
        firstName = dto.firstName.orEmpty(),
        lastName = dto.lastName.orEmpty(),
        email = dto.emailAddress.orEmpty(),
        phoneNumber = nonBlank(dto.phoneNumber),
        dni = nonBlank(dto.dni),
        birthDate = dto.birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        photoUrl = nonBlank(dto.photoUrl),
    )

    private fun nonBlank(value: String?) = value?.trim()?.ifEmpty { null }
}
