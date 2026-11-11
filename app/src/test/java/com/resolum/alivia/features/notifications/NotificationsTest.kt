package com.resolum.alivia.features.notifications

import com.resolum.alivia.features.notifications.data.MarkReadRequestDto
import com.resolum.alivia.features.notifications.data.NotificationDto
import com.resolum.alivia.features.notifications.data.NotificationRepositoryImpl
import com.resolum.alivia.features.notifications.data.NotificationService
import com.resolum.alivia.features.notifications.domain.GetNotificationsUseCase
import com.resolum.alivia.features.notifications.domain.MarkNotificationsReadUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.time.LocalDateTime

class NotificationsTest {
    private class FakeService : NotificationService {
        var failWith: Exception? = null
        var readCode = 204
        val marked = mutableListOf<List<Long>>()
        override suspend fun mine(): List<NotificationDto> {
            failWith?.let { throw it }
            return listOf(
                NotificationDto(1, "ASSIGNMENT_CREATED", "Nueva orden", " ", "READ", "2026-11-10T08:15:00"),
                NotificationDto(2, "ACTUATOR_FAILURE", " ", "Falla en la puerta", "UNREAD", "2026-11-11T13:15:00Z"),
                NotificationDto(3, null, "Sin fecha", null, "UNREAD", "mañana"),
                NotificationDto(null, null, "Sin id"),
            )
        }

        override suspend fun markRead(body: MarkReadRequestDto): Response<Unit> {
            marked += body.ids
            return if (readCode < 300) Response.success(204, Unit)
            else Response.error(readCode, "{}".toResponseBody("application/json".toMediaType()))
        }
    }

    @Test
    fun mapsAndSortsTheNotifications() = runBlocking {
        val notifications = (GetNotificationsUseCase(NotificationRepositoryImpl(FakeService()))() as NetworkResult.Success).data
        assertEquals(listOf(2L, 1L, 3L), notifications.map { it.id })
        val alert = notifications[0]
        assertEquals("Notificación", alert.title)
        assertEquals("Falla en la puerta", alert.body)
        assertFalse(alert.read)
        assertTrue(alert.isAlert)
        assertEquals(LocalDateTime.of(2026, 11, 11, 8, 15), alert.createdAt)
        val info = notifications[1]
        assertTrue(info.read)
        assertNull(info.body)
        assertFalse(info.isAlert)
        assertEquals(LocalDateTime.of(2026, 11, 10, 8, 15), info.createdAt)
        assertNull(notifications[2].createdAt)
    }

    @Test
    fun marksAsReadOnlyWhenThereIsSomething() = runBlocking {
        val service = FakeService()
        val markRead = MarkNotificationsReadUseCase(NotificationRepositoryImpl(service))
        assertEquals(NetworkResult.Success(Unit), markRead(emptyList()))
        assertTrue(service.marked.isEmpty())
        assertEquals(NetworkResult.Success(Unit), markRead(listOf(2, 3)))
        assertEquals(listOf(2L, 3L), service.marked.last())
        service.readCode = 500
        assertEquals(500, (markRead(listOf(2)) as NetworkResult.Error).code)
        service.failWith = httpError(401)
        assertTrue(GetNotificationsUseCase(NotificationRepositoryImpl(service))() is NetworkResult.Error)
    }
}
