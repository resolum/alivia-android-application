package com.resolum.alivia.features.profiles.domain.usecases

import com.resolum.alivia.features.profiles.domain.models.Profile
import com.resolum.alivia.features.profiles.domain.models.ProfileChanges
import com.resolum.alivia.features.profiles.domain.repositories.ProfileRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

/** Sends only what changed. Names cannot be empty and the phone is digits only (E.164 without the plus). */
class UpdateProfileUseCase(private val repository: ProfileRepository) {

    suspend operator fun invoke(current: Profile, firstName: String, lastName: String, phone: String): NetworkResult<Profile> {
        val first = firstName.trim()
        val last = lastName.trim()
        val digits = phone.filter { !it.isWhitespace() && it != '+' }
        when {
            first.isEmpty() -> return NetworkResult.Error("Escribe tus nombres.")
            last.isEmpty() -> return NetworkResult.Error("Escribe tus apellidos.")
            !digits.all { it in '0'..'9' } -> return NetworkResult.Error("El teléfono solo puede tener números.")
            digits.isEmpty() && current.phoneNumber != null -> return NetworkResult.Error("El teléfono no puede quedar vacío.")
        }
        val changes = ProfileChanges(
            firstName = first.takeIf { it != current.firstName },
            lastName = last.takeIf { it != current.lastName },
            phoneNumber = digits.takeIf { it.isNotEmpty() && it != current.phoneNumber },
        )
        if (changes.isEmpty) return NetworkResult.Success(current)
        return send(changes)
    }

    suspend operator fun invoke(photo: ByteArray): NetworkResult<Profile> = send(ProfileChanges(photo = photo))

    private suspend fun send(changes: ProfileChanges): NetworkResult<Profile> = when (val result = repository.update(changes)) {
        is NetworkResult.Error -> if (result.code == 400) NetworkResult.Error(INVALID_DATA, 400) else result
        is NetworkResult.Success -> result
    }

    companion object {
        const val INVALID_DATA = "Revisa tus datos: hay información que no es válida."
    }
}
