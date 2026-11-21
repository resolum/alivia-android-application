package com.resolum.alivia.features.analytics

import com.resolum.alivia.features.analytics.data.datasources.remote.mappers.MetricMapper
import com.resolum.alivia.features.analytics.data.datasources.remote.models.MetricResponseDto
import com.resolum.alivia.features.analytics.domain.models.ActionResult
import com.resolum.alivia.features.analytics.domain.models.ActionSource
import com.resolum.alivia.features.analytics.domain.models.AutonomyCalculator
import com.resolum.alivia.features.analytics.domain.models.ControlledDeviceType
import com.resolum.alivia.features.analytics.domain.models.MetricPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MetricEdgeCasesTest {
    private val zone = ZoneId.of("America/Lima")

    @Test
    fun anEmptyAnswerIsAnUpToDateHomeWithoutActions() {
        val metrics = MetricMapper.toDomain(MetricResponseDto(), zone)
        assertTrue(metrics.actions.isEmpty())
        assertTrue(metrics.devices.isEmpty())
        assertNull(metrics.lastUpdatedAt)
        assertTrue(metrics.edgeOnline)
        assertFalse(metrics.pendingEvents)
        assertFalse(metrics.mayBeIncomplete)
    }

    @Test
    fun fillsMissingNamesAndUnknownValues() {
        val metrics = MetricMapper.toDomain(
            MetricResponseDto(
                lastUpdatedAt = "bad",
                edgeOnline = true,
                pendingEvents = false,
                devices = listOf(MetricResponseDto.DeviceDto(null, "Sin id"), MetricResponseDto.DeviceDto(4, " ", "fridge")),
                actions = listOf(
                    MetricResponseDto.ActionDto(eventId = "a", deviceName = " ", intent = " ", source = "robot", result = "?",
                        deviceType = null, occurredAt = "2026-11-21T08:00:00-05:00"),
                    MetricResponseDto.ActionDto(eventId = "b", occurredAt = " "),
                    MetricResponseDto.ActionDto(eventId = null, occurredAt = "2026-11-21T08:00:00"),
                ),
            ),
            zone,
        )
        assertNull(metrics.lastUpdatedAt)
        assertEquals("Dispositivo 4", metrics.devices.single().name)
        assertEquals(ControlledDeviceType.OTHER, metrics.devices.single().type)
        val action = metrics.actions.single()
        assertEquals("Dispositivo", action.deviceName)
        assertEquals("Acción", action.intent)
        assertEquals(ActionSource.OTHER, action.source)
        assertEquals(ActionResult.NO_CONFIRMATION, action.result)
        assertEquals(8, action.occurredAt.hour)
        assertFalse(action.executedLocally)
    }

    @Test
    fun periodsEndToday() {
        val today = LocalDate.of(2026, 11, 21)
        assertEquals(today, MetricPeriod.TODAY.from(today))
        assertEquals(LocalDate.of(2026, 10, 23), MetricPeriod.MONTH.from(today))
        val summary = AutonomyCalculator.summarize(emptyList())
        assertEquals(0, summary.count(ActionResult.FAILED))
        assertTrue(summary.byType.isEmpty())
    }
}
