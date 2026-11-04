package com.resolum.alivia.features.shared

import com.resolum.alivia.features.shared.data.datasources.datastore.SessionTokenStore
import com.resolum.alivia.features.shared.data.datasources.datastore.StoredSession
import com.resolum.alivia.features.shared.data.datasources.remote.AuthInterceptor
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.datasources.remote.TokenRefreshAuthenticator
import com.resolum.alivia.features.shared.data.datasources.remote.map
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

/** In-memory session for the network tests. */
class FakeSessionStore(var stored: StoredSession? = null) : SessionTokenStore {
    var cleared = 0
    override val accessToken: String? get() = stored?.accessToken
    override val session: StoredSession? get() = stored
    override suspend fun restore(): StoredSession? = stored
    override suspend fun save(session: StoredSession) { stored = session }
    override suspend fun clear() { stored = null; cleared++ }
}

fun httpError(code: Int, body: String = ""): HttpException =
    HttpException(retrofit2.Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

class SessionNetworkTest {
    private val session = StoredSession("access-1", "refresh-1", "acc-1", "elena@alivia.test", "TECHNICIAN")

    private class Repository : BaseRepository() {
        suspend fun <T> call(block: suspend () -> T) = safeCall(block)
    }

    @Test
    fun turnsBackendFailuresIntoSpanishMessages() = runBlocking {
        val repository = Repository()
        assertEquals(NetworkResult.Success(1), repository.call { 1 })
        assertEquals("Tu sesión terminó. Vuelve a ingresar.", (repository.call { throw httpError(401) } as NetworkResult.Error).message)
        assertEquals("Solo el técnico asignado.", (repository.call { throw httpError(403, """{"detail":"Solo el técnico asignado."}""") } as NetworkResult.Error).message)
        assertEquals("No tienes permiso para esta acción.", (repository.call { throw httpError(403, "no json") } as NetworkResult.Error).message)
        assertEquals("No encontramos la orden.", (repository.call { throw httpError(404) } as NetworkResult.Error).message)
        assertEquals(503, (repository.call { throw httpError(503) } as NetworkResult.Error).code)
        assertEquals("Ya existe.", (repository.call { throw httpError(409, """{"detail":"Ya existe."}""") } as NetworkResult.Error).message)
        assertEquals("No se pudo completar la acción.", (repository.call { throw httpError(400) } as NetworkResult.Error).message)
        assertTrue((repository.call { throw IOException("offline") } as NetworkResult.Error).message.startsWith("No pudimos conectar"))
        assertEquals(NetworkResult.Success(2), NetworkResult.Success(1).map { it + 1 })
        val error = NetworkResult.Error("x", 500)
        assertEquals(error, error.map { 1 })
    }

    @Test
    fun addsTheTokenExceptToTheAuthCalls() {
        val interceptor = AuthInterceptor(FakeSessionStore(session))
        val orders = FakeChain(Request.Builder().url("http://localhost/api/v1/technical-orders/mine").build())
        interceptor.intercept(orders)
        assertEquals("Bearer access-1", orders.proceeded?.header("Authorization"))

        val signIn = FakeChain(Request.Builder().url("http://localhost/api/v1/auth/sign-in").build())
        interceptor.intercept(signIn)
        assertNull(signIn.proceeded?.header("Authorization"))

        val signedOut = FakeChain(Request.Builder().url("http://localhost/api/v1/technical-orders/mine").build())
        AuthInterceptor(FakeSessionStore()).intercept(signedOut)
        assertNull(signedOut.proceeded?.header("Authorization"))
    }

    @Test
    fun renewsTheSessionOnceAndClosesItWhenTheRenewalFails() {
        val tokens = FakeSessionStore(session)
        var expired = 0
        val renewed = TokenRefreshAuthenticator(tokens, { expired++ }) { it.copy(accessToken = "access-2", refreshToken = "refresh-2") }

        val retry = renewed.authenticate(null, unauthorized("/api/v1/technical-orders/mine", "access-1"))
        assertEquals("Bearer access-2", retry?.header("Authorization"))
        assertEquals("refresh-2", tokens.stored?.refreshToken)

        // Another call already renewed it: it retries with the current token without renewing again.
        val waited = renewed.authenticate(null, unauthorized("/api/v1/technical-orders/mine", "access-1"))
        assertEquals("Bearer access-2", waited?.header("Authorization"))

        assertNull(renewed.authenticate(null, unauthorized("/api/v1/auth/refresh", "access-2")))
        assertNull(renewed.authenticate(null, unauthorized("/api/v1/technical-orders/mine", "access-2", prior = true)))

        val failing = TokenRefreshAuthenticator(tokens, { expired++ }) { null }
        assertNull(failing.authenticate(null, unauthorized("/api/v1/technical-orders/mine", "access-2")))
        assertEquals(1, expired)
        assertEquals(1, tokens.cleared)
        assertNull(failing.authenticate(null, unauthorized("/api/v1/technical-orders/mine", "access-2")))
        assertTrue(TokenRefreshAuthenticator("http://localhost/", FakeSessionStore(), {}).authenticate(
            null, unauthorized("/api/v1/technical-orders/mine", null)) == null)
    }

    private fun unauthorized(path: String, token: String?, prior: Boolean = false): Response {
        val request = Request.Builder().url("http://localhost$path").apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        fun response() = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized")
        return response().apply { if (prior) priorResponse(response().build()) }.build()
    }

    private class FakeChain(private val request: Request) : Interceptor.Chain {
        var proceeded: Request? = null
        override fun request(): Request = request
        override fun proceed(request: Request): Response {
            proceeded = request
            return Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()
        }
        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = this
        override fun readTimeoutMillis() = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit) = this
        override fun writeTimeoutMillis() = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = this
    }
}
