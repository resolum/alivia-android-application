package com.resolum.alivia.features.iam.domain.usecases

import com.resolum.alivia.features.iam.domain.repositories.PasswordResetRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

/** The backend password rules, checked as the user types. */
object PasswordRules {
    const val SPECIAL = "@\$!%*?&#"

    data class Check(val label: String, val passed: Boolean)

    fun checks(password: String) = listOf(
        Check("8 caracteres como mínimo", password.codePointCount(0, password.length) >= 8),
        Check("Al menos una mayúscula y una minúscula", password.any { it.isUpperCase() } && password.any { it.isLowerCase() }),
        Check("Al menos un número", password.any { it.isDigit() }),
        Check("Al menos un símbolo ($SPECIAL)", password.any { it in SPECIAL }),
    )

    fun isValid(password: String) = checks(password).all { it.passed } && password.toByteArray().size <= 72
}

/** Asks for a 6-digit code; the backend answers the same whether or not the email has an account. */
class SendPasswordResetCodeUseCase(private val repository: PasswordResetRepository) {
    suspend operator fun invoke(email: String): NetworkResult<Unit> {
        val clean = email.trim()
        if (!EMAIL.matches(clean)) return NetworkResult.Error("Escribe un correo electrónico válido.")
        return repository.sendCode(clean)
    }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

/** Sets the new password with the emailed code. A 400 means a wrong, expired or exhausted code. */
class ResetPasswordUseCase(private val repository: PasswordResetRepository) {
    suspend operator fun invoke(email: String, code: String, password: String, confirmation: String): NetworkResult<Unit> {
        if (code.length != 6 || !code.all { it.isDigit() }) return NetworkResult.Error("Ingresa los 6 dígitos del código.")
        if (!PasswordRules.isValid(password)) return NetworkResult.Error("La contraseña no cumple los requisitos de seguridad.")
        if (password != confirmation) return NetworkResult.Error("Las contraseñas no coinciden.")
        return when (val result = repository.reset(email.trim(), code, password)) {
            is NetworkResult.Error -> if (result.code == 400) NetworkResult.Error(INVALID_CODE, 400) else result
            is NetworkResult.Success -> result
        }
    }

    companion object {
        const val INVALID_CODE = "El código no es válido o ya venció. Revísalo o pide uno nuevo."
    }
}
