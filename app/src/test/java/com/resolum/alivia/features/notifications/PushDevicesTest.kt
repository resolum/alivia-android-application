package com.resolum.alivia.features.notifications

import com.resolum.alivia.features.notifications.data.PushDeviceRepositoryImpl
import com.resolum.alivia.features.notifications.data.PushDeviceService
import com.resolum.alivia.features.notifications.data.RegisterPushDeviceRequestDto
import com.resolum.alivia.features.notifications.domain.PushMessage
import com.resolum.alivia.features.notifications.domain.PushTokenProvider
import com.resolum.alivia.features.notifications.domain.RegisterPushDeviceUseCase
import com.resolum.alivia.features.notifications.domain.UnregisterPushDeviceUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class PushDevicesTest {
    private class FakeTokens(private val token: String?) : PushTokenProvider {
        override suspend fun currentToken() = token
    }

    private class FakeService : PushDeviceService {
        var code = 201
        var failWith: Exception? = null
        val registered = mutableListOf<RegisterPushDeviceRequestDto>()
        val unregistered = mutableListOf<String>()

        private fun reply(): Response<Unit> {
            failWith?.let { throw it }
            return if (code < 300) Response.success(code, Unit)
            else Response.error(code, "{}".toResponseBody("application/json".toMediaType()))
        }

        override suspend fun register(body: RegisterPushDeviceRequestDto): Response<Unit> {
            registered += body
            return reply()
        }

        override suspend fun unregister(token: String): Response<Unit> {
            unregistered += token
            return reply()
        }
    }

    @Test
    fun registersTheCurrentTokenAsAnAndroidDevice() = runBlocking {
        val service = FakeService()
        val result = RegisterPushDeviceUseCase(FakeTokens("fcm-1"), PushDeviceRepositoryImpl(service))()
        assertEquals(NetworkResult.Success(Unit), result)
        assertEquals(listOf(RegisterPushDeviceRequestDto("fcm-1", "ANDROID")), service.registered)
    }

    @Test
    fun prefersTheNewTokenThatFirebaseGave() = runBlocking {
        val service = FakeService()
        RegisterPushDeviceUseCase(FakeTokens("old"), PushDeviceRepositoryImpl(service))("new")
        RegisterPushDeviceUseCase(FakeTokens("current"), PushDeviceRepositoryImpl(service))(" ")
        assertEquals(listOf("new", "current"), service.registered.map { it.token })
    }

    @Test
    fun doesNotCallTheBackendWithoutAToken() = runBlocking {
        val service = FakeService()
        val result = RegisterPushDeviceUseCase(FakeTokens(null), PushDeviceRepositoryImpl(service))()
        assertEquals(NetworkResult.Error(RegisterPushDeviceUseCase.NO_TOKEN), result)
        assertTrue(service.registered.isEmpty())
    }

    @Test
    fun reportsWhenTheBackendRejectsTheToken() = runBlocking {
        val service = FakeService().apply { code = 400 }
        val result = RegisterPushDeviceUseCase(FakeTokens("fcm"), PushDeviceRepositoryImpl(service))()
        assertEquals(400, (result as NetworkResult.Error).code)
    }

    @Test
    fun unregistersTheCurrentToken() = runBlocking {
        val service = FakeService().apply { code = 204 }
        val result = UnregisterPushDeviceUseCase(FakeTokens("fcm-2"), PushDeviceRepositoryImpl(service))()
        assertEquals(NetworkResult.Success(Unit), result)
        assertEquals(listOf("fcm-2"), service.unregistered)
    }

    @Test
    fun aTokenTheBackendDoesNotKnowIsAlreadyUnregistered() = runBlocking {
        val service = FakeService().apply { code = 404 }
        val result = UnregisterPushDeviceUseCase(FakeTokens("gone"), PushDeviceRepositoryImpl(service))()
        assertEquals(NetworkResult.Success(Unit), result)
    }

    @Test
    fun unregisterFailsOnOtherErrorsAndSkipsWithoutAToken() = runBlocking {
        val failing = FakeService().apply { code = 500 }
        assertEquals(500, (UnregisterPushDeviceUseCase(FakeTokens("t"), PushDeviceRepositoryImpl(failing))() as NetworkResult.Error).code)

        val offline = FakeService().apply { failWith = IOException("offline") }
        assertTrue(UnregisterPushDeviceUseCase(FakeTokens("t"), PushDeviceRepositoryImpl(offline))() is NetworkResult.Error)

        val untouched = FakeService()
        assertEquals(NetworkResult.Success(Unit), UnregisterPushDeviceUseCase(FakeTokens(""), PushDeviceRepositoryImpl(untouched))())
        assertTrue(untouched.unregistered.isEmpty())
    }

    @Test
    fun readsThePushMessageOfTheBackend() {
        val alert = PushMessage.from(
            "Falla en la puerta", " La cerradura no responde ",
            mapOf("notificationId" to "42", "type" to "ACTUATOR_FAILURE", "sourceId" to "7"),
        )
        assertEquals(PushMessage(42, "Falla en la puerta", "La cerradura no responde", isAlert = true), alert)

        val dataOnly = PushMessage.from(null, null, mapOf("title" to "Nueva orden", "body" to "Instalación", "notificationId" to "x"))
        assertEquals("Nueva orden", dataOnly.title)
        assertEquals("Instalación", dataOnly.body)
        assertNull(dataOnly.notificationId)
        assertFalse(dataOnly.isAlert)

        val empty = PushMessage.from(" ", "", emptyMap())
        assertEquals("Notificación", empty.title)
        assertNull(empty.body)
    }
}
