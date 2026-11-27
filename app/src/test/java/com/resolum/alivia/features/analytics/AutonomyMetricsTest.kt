package com.resolum.alivia.features.analytics

import com.google.gson.Gson
import com.resolum.alivia.features.analytics.data.datasources.remote.MetricFacadeService
import com.resolum.alivia.features.analytics.data.datasources.remote.models.MetricResponseDto
import com.resolum.alivia.features.analytics.data.datasources.remote.services.MetricService
import com.resolum.alivia.features.analytics.data.repositories.MetricRepositoryImpl
import com.resolum.alivia.features.analytics.domain.models.ActionResult
import com.resolum.alivia.features.analytics.domain.models.ActionSource
import com.resolum.alivia.features.analytics.domain.models.AutonomyAction
import com.resolum.alivia.features.analytics.domain.models.ControlledDeviceType
import com.resolum.alivia.features.analytics.domain.models.MetricPeriod
import com.resolum.alivia.features.analytics.domain.usecases.GetMetricsUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.httpError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AutonomyMetricsTest {
    private val zone = ZoneId.of("America/Lima")
    private val today = LocalDate.of(2026, 11, 21)

    // What a backend could send, audio reference included: the app must not keep it.
    private val json = """
        {
          "lastUpdatedAt": "2026-11-21T15:00:00Z",
          "edgeOnline": false,
          "pendingEvents": true,
          "devices": [{"id": 1, "name": "Luz sala", "type": "LIGHT"}, {"id": 2, "name": "Puerta", "type": "DOOR"}],
          "actions": [
            {"eventId": "e1", "deviceId": 1, "deviceName": "Luz sala", "deviceType": "LIGHT", "intent": "Encender luz",
             "source": "VOICE", "result": "SUCCESS", "occurredAt": "2026-11-21T08:00:00", "audioUrl": "https://x/a.wav"},
            {"eventId": "e1", "deviceId": 1, "deviceName": "Luz sala", "deviceType": "LIGHT", "intent": "Encender luz",
             "source": "VOICE", "result": "SUCCESS", "occurredAt": "2026-11-21T08:00:00", "executedLocally": true},
            {"eventId": "e2", "deviceId": 2, "deviceName": "Puerta", "deviceType": "DOOR", "intent": "Abrir puerta",
             "source": "VOICE", "result": "LOW_CONFIDENCE", "occurredAt": "2026-11-20T21:00:00"},
            {"eventId": "e3", "deviceId": 2, "deviceName": "Puerta", "deviceType": "door", "intent": "Cerrar puerta",
             "source": "APP", "result": "NO_CONFIRMATION", "occurredAt": "2026-11-19T21:00:00", "executedLocally": true},
            {"eventId": "e4", "deviceId": 1, "deviceType": "LIGHT", "result": "FAILED", "occurredAt": "2026-11-01T10:00:00"},
            {"eventId": "", "result": "SUCCESS", "occurredAt": "2026-11-21T09:00:00"}
          ]
        }
    """.trimIndent()

    private class FakeService(private val body: String?, private val fail: Exception? = null) : MetricService {
        val asked = mutableListOf<Triple<String, String, Long?>>()
        override suspend fun autonomy(from: String, to: String, deviceId: Long?): MetricResponseDto {
            asked += Triple(from, to, deviceId)
            fail?.let { throw it }
            return Gson().fromJson(body, MetricResponseDto::class.java)
        }
    }

    private fun useCase(service: MetricService) = GetMetricsUseCase(MetricRepositoryImpl(MetricFacadeService(service), zone))

    @Test
    fun summarizesTheWeekCountingEachEventOnce() = runBlocking {
        val service = FakeService(json)
        val report = (useCase(service)(MetricPeriod.WEEK, null, today) as NetworkResult.Success).data
        assertEquals(Triple("2026-11-15", "2026-11-21", null), service.asked.single())

        assertEquals(listOf("e1", "e2", "e3"), report.actions.map { it.eventId })
        assertEquals(3, report.summary.requested)
        assertEquals(1, report.summary.executed)
        assertEquals(1, report.summary.count(ActionResult.LOW_CONFIDENCE))
        assertEquals(1, report.summary.count(ActionResult.NO_CONFIRMATION))
        assertEquals(0, report.summary.count(ActionResult.FAILED))
        assertEquals(2, report.summary.byType[ControlledDeviceType.DOOR]?.requested)
        assertEquals(0, report.summary.byType[ControlledDeviceType.DOOR]?.executed)
        assertEquals(1, report.summary.executedLocally)
    }

    @Test
    fun filtersByDeviceAndPeriod() = runBlocking {
        val service = FakeService(json)
        val report = (useCase(service)(MetricPeriod.TODAY, 2, today) as NetworkResult.Success).data
        assertEquals(Triple("2026-11-21", "2026-11-21", 2L), service.asked.single())
        assertTrue(report.actions.isEmpty())

        val month = (useCase(FakeService(json))(MetricPeriod.MONTH, 1, today) as NetworkResult.Success).data
        assertEquals(listOf("e1", "e4"), month.actions.map { it.eventId })
    }

    @Test
    fun keepsVoiceAsIntentOnlyAndWarnsWhenTheEdgeIsOffline() = runBlocking {
        val report = (useCase(FakeService(json))(MetricPeriod.WEEK, null, today) as NetworkResult.Success).data
        val voice = report.actions.first()
        assertEquals("Encender luz", voice.intent)
        assertEquals(ActionSource.VOICE, voice.source)
        // The domain model has no field that could hold the recording.
        assertFalse(AutonomyAction::class.java.declaredFields.any { it.name.contains("audio", ignoreCase = true) })

        assertTrue(report.metrics.mayBeIncomplete)
        assertFalse(report.metrics.edgeOnline)
        assertEquals(LocalDateTime.of(2026, 11, 21, 10, 0), report.metrics.lastUpdatedAt)
    }

    @Test
    fun saysWhenTheMetricsAreNotAvailable() = runBlocking {
        val result = useCase(FakeService(null, httpError(404)))(MetricPeriod.WEEK, null, today) as NetworkResult.Error
        assertEquals(MetricRepositoryImpl.NOT_AVAILABLE, result.message)
    }
}
