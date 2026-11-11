package com.resolum.alivia.features.profiles.domain.usecases

import com.resolum.alivia.features.profiles.domain.models.Profile
import com.resolum.alivia.features.profiles.domain.repositories.ProfileRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

class GetProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(): NetworkResult<Profile> = when (val result = repository.me()) {
        is NetworkResult.Error -> if (result.code == 404) NetworkResult.Error("Tu cuenta todavía no tiene un perfil.", 404) else result
        is NetworkResult.Success -> result
    }
}
