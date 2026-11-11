package com.resolum.alivia.features.iam

import com.resolum.alivia.features.iam.data.datasources.remote.services.ForgotPasswordRequestDto
import com.resolum.alivia.features.iam.data.datasources.remote.services.PasswordResetService
import com.resolum.alivia.features.iam.data.datasources.remote.services.ResetPasswordRequestDto
import com.resolum.alivia.features.iam.data.repositories.PasswordResetRepositoryImpl
import com.resolum.alivia.features.iam.domain.usecases.PasswordRules
import com.resolum.alivia.features.iam.domain.usecases.ResetPasswordUseCase
import com.resolum.alivia.features.iam.domain.usecases.SendPasswordResetCodeUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class PasswordResetTest {
    private class FakeService : PasswordResetService {
        var code = 202
        val sent = mutableListOf<Any>()
        private fun reply(): Response<Unit> =
            if (code < 300) Response.success(code, Unit) else Response.error(code, "{}".toResponseBody("application/json".toMediaType()))

        override suspend fun forgotPassword(body: ForgotPasswordRequestDto): Response<Unit> {
            sent += body
            return reply()
        }

        override suspend fun resetPassword(body: ResetPasswordRequestDto): Response<Unit> {
            sent += body
            return reply()
        }
    }

    @Test
    fun checksThePasswordRules() {
        assertEquals(listOf(false, false, false, false), PasswordRules.checks("").map { it.passed })
        assertEquals(listOf(true, true, true, false), PasswordRules.checks("Nueva2026").map { it.passed })
        assertTrue(PasswordRules.isValid("Nueva2026!"))
        assertFalse(PasswordRules.isValid("nueva2026!"))
        assertFalse(PasswordRules.isValid("Aa1!" + "x".repeat(80)))
    }

    @Test
    fun sendsTheCodeOnlyForAValidEmail() = runBlocking {
        val service = FakeService()
        val send = SendPasswordResetCodeUseCase(PasswordResetRepositoryImpl(service))
        assertEquals("Escribe un correo electrónico válido.", (send("ana@") as NetworkResult.Error).message)
        assertTrue(service.sent.isEmpty())
        assertEquals(NetworkResult.Success(Unit), send(" ana@alivia.test "))
        assertEquals(ForgotPasswordRequestDto("ana@alivia.test"), service.sent.last())
        service.code = 500
        assertEquals(500, (send("ana@alivia.test") as NetworkResult.Error).code)
    }

    @Test
    fun resetsWithACodeAndAStrongPassword() = runBlocking {
        val service = FakeService().apply { code = 204 }
        val reset = ResetPasswordUseCase(PasswordResetRepositoryImpl(service))
        fun message(result: NetworkResult<Unit>) = (result as NetworkResult.Error).message
        assertEquals("Ingresa los 6 dígitos del código.", message(reset("a@b.co", "12a456", "Nueva2026!", "Nueva2026!")))
        assertEquals("Ingresa los 6 dígitos del código.", message(reset("a@b.co", "123", "Nueva2026!", "Nueva2026!")))
        assertEquals("La contraseña no cumple los requisitos de seguridad.", message(reset("a@b.co", "123456", "nueva", "nueva")))
        assertEquals("Las contraseñas no coinciden.", message(reset("a@b.co", "123456", "Nueva2026!", "Nueva2026?")))
        assertTrue(service.sent.isEmpty())

        assertEquals(NetworkResult.Success(Unit), reset(" a@b.co ", "123456", "Nueva2026!", "Nueva2026!"))
        assertEquals(ResetPasswordRequestDto("a@b.co", "123456", "Nueva2026!"), service.sent.last())
        service.code = 400
        assertEquals(ResetPasswordUseCase.INVALID_CODE, message(reset("a@b.co", "123456", "Nueva2026!", "Nueva2026!")))
        service.code = 503
        assertEquals(503, (reset("a@b.co", "123456", "Nueva2026!", "Nueva2026!") as NetworkResult.Error).code)
    }
}
