package com.resolum.alivia.features.iam.domain.usecases

import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.models.SignIn
import com.resolum.alivia.features.iam.domain.repositories.AuthRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

class SignInUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): NetworkResult<Session> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || password.isEmpty()) return NetworkResult.Error("Ingresa tu correo y tu contraseña.")
        if (!EMAIL.matches(cleanEmail)) return NetworkResult.Error("Ingresa un correo válido.")
        return repository.signIn(SignIn(cleanEmail, password))
    }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
