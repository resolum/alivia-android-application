package com.resolum.alivia.features.iam

import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.models.SignIn
import com.resolum.alivia.features.iam.domain.models.UserRole
import com.resolum.alivia.features.iam.domain.repositories.AuthRepository
import com.resolum.alivia.features.iam.domain.usecases.SignInUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInRulesTest {
    @Test
    fun onlyTechniciansAndCaregiversUseTheMobileApp() {
        assertTrue(UserRole.from("TECHNICIAN").canUseMobileApp)
        assertTrue(UserRole.from("CAREGIVER").canUseMobileApp)
        assertFalse(UserRole.from("SUBSCRIPTION_MANAGER").canUseMobileApp)
        assertFalse(UserRole.from("ADMINISTRATOR").canUseMobileApp)
        assertEquals(UserRole.UNKNOWN, UserRole.from("OTHER"))
    }

    @Test
    fun checksTheCredentialsBeforeCallingTheBackend() = runBlocking {
        var calls = 0
        val repository = object : AuthRepository {
            override suspend fun signIn(credentials: SignIn): NetworkResult<Session> {
                calls++
                return NetworkResult.Success(Session("id", credentials.email, UserRole.TECHNICIAN))
            }
            override suspend fun restoreSession(): Session? = null
            override suspend fun signOut() = Unit
        }
        val signIn = SignInUseCase(repository)
        assertTrue(signIn("", "x") is NetworkResult.Error)
        assertTrue(signIn("tecnico", "x") is NetworkResult.Error)
        assertEquals(0, calls)
        val ok = signIn(" tecnico@alivia.pe ", "secret")
        assertEquals("tecnico@alivia.pe", (ok as NetworkResult.Success).data.email)
    }
}
