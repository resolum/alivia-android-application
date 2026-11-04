package com.resolum.alivia.features.iam

import com.resolum.alivia.features.iam.data.datasources.remote.AuthFacadeService
import com.resolum.alivia.features.iam.data.datasources.remote.mappers.SignInMapper
import com.resolum.alivia.features.iam.data.datasources.remote.models.AccountDto
import com.resolum.alivia.features.iam.data.datasources.remote.models.SignInRequestDto
import com.resolum.alivia.features.iam.data.datasources.remote.models.SignInResponseDto
import com.resolum.alivia.features.iam.data.datasources.remote.services.AuthService
import com.resolum.alivia.features.iam.data.repositories.AuthRepositoryImpl
import com.resolum.alivia.features.iam.domain.models.SignIn
import com.resolum.alivia.features.iam.domain.models.UserRole
import com.resolum.alivia.features.iam.domain.usecases.RestoreSessionUseCase
import com.resolum.alivia.features.iam.domain.usecases.SignOutUseCase
import com.resolum.alivia.features.shared.FakeSessionStore
import com.resolum.alivia.features.shared.data.datasources.datastore.StoredSession
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryTest {
    private fun response(role: String?, access: String? = "a", user: Boolean = true) = SignInResponseDto(
        access, "r", "Bearer", 900, if (user) AccountDto("acc-7", "elena@alivia.test", role, "ACTIVE", 7) else null,
    )

    private class FakeAuthService(private val reply: () -> SignInResponseDto) : AuthService {
        var lastRequest: SignInRequestDto? = null
        override suspend fun signIn(request: SignInRequestDto): SignInResponseDto {
            lastRequest = request
            return reply()
        }
    }

    @Test
    fun keepsTheSessionOnlyForTheRolesOfTheApp() = runBlocking {
        val tokens = FakeSessionStore()
        val service = FakeAuthService { response("TECHNICIAN") }
        val technician = AuthRepositoryImpl(AuthFacadeService(service), tokens).signIn(SignIn("elena@alivia.test", "secret"))
        assertEquals(UserRole.TECHNICIAN, (technician as NetworkResult.Success).data.role)
        assertEquals("elena@alivia.test", service.lastRequest?.email)
        assertEquals("a", tokens.stored?.accessToken)

        val managerTokens = FakeSessionStore()
        val manager = AuthRepositoryImpl(AuthFacadeService(FakeAuthService { response("SUBSCRIPTION_MANAGER") }), managerTokens)
            .signIn(SignIn("diego@alivia.test", "secret"))
        assertEquals(AuthRepositoryImpl.NOT_ALLOWED, (manager as NetworkResult.Error).message)
        assertEquals(403, manager.code)
        assertNull(managerTokens.stored)

        val broken = AuthRepositoryImpl(AuthFacadeService(FakeAuthService { response("TECHNICIAN", access = null) }), FakeSessionStore())
            .signIn(SignIn("a@b.pe", "x"))
        assertEquals("La respuesta del servidor no es válida.", (broken as NetworkResult.Error).message)
        assertNull(SignInMapper.toStoredSession(response("TECHNICIAN", user = false)))
    }

    @Test
    fun explainsWrongCredentialsAndOtherFailures() = runBlocking {
        val wrong = AuthRepositoryImpl(AuthFacadeService(FakeAuthService { throw httpError(401) }), FakeSessionStore())
            .signIn(SignIn("a@b.pe", "x"))
        assertEquals("Correo o contraseña incorrectos.", (wrong as NetworkResult.Error).message)
        val down = AuthRepositoryImpl(AuthFacadeService(FakeAuthService { throw httpError(503) }), FakeSessionStore())
            .signIn(SignIn("a@b.pe", "x"))
        assertEquals(503, (down as NetworkResult.Error).code)
    }

    @Test
    fun restoresOnlyValidSessionsAndSignsOut() = runBlocking {
        val tokens = FakeSessionStore(StoredSession("a", "r", "acc-7", "elena@alivia.test", "CAREGIVER"))
        val repository = AuthRepositoryImpl(AuthFacadeService(FakeAuthService { response("CAREGIVER") }), tokens)
        assertEquals(UserRole.CAREGIVER, RestoreSessionUseCase(repository)()?.role)
        SignOutUseCase(repository)()
        assertNull(tokens.stored)
        assertNull(RestoreSessionUseCase(repository)())

        val unknown = FakeSessionStore(StoredSession("a", "r", "acc-1", "x@y.pe", "OTHER"))
        assertNull(AuthRepositoryImpl(AuthFacadeService(FakeAuthService { response("OTHER") }), unknown).restoreSession())
        assertEquals(1, unknown.cleared)
        val manager = FakeSessionStore(StoredSession("a", "r", "acc-1", "x@y.pe", "ADMINISTRATOR"))
        assertNull(AuthRepositoryImpl(AuthFacadeService(FakeAuthService { response("ADMINISTRATOR") }), manager).restoreSession())
    }
}
