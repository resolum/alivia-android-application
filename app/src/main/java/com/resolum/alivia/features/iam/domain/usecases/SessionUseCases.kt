package com.resolum.alivia.features.iam.domain.usecases

import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.repositories.AuthRepository

class RestoreSessionUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Session? = repository.restoreSession()
}

class SignOutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}
