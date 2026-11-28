package com.resolum.alivia.features.tracking

import com.resolum.alivia.features.tracking.data.datasources.remote.services.HistoryEventDto
import com.resolum.alivia.features.tracking.data.repositories.toDomain
import com.resolum.alivia.features.tracking.domain.models.HistoryCategory
import com.resolum.alivia.features.tracking.domain.models.HistoryDay
import com.resolum.alivia.features.tracking.domain.models.HistoryDevice
import com.resolum.alivia.features.tracking.domain.models.HistoryEvent
import com.resolum.alivia.features.tracking.domain.models.HistoryTone
import com.resolum.alivia.features.tracking.domain.models.dayFor
import com.resolum.alivia.features.tracking.domain.models.mergeHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeHistoryTest {
    private val door = HistoryDevice(4, "Puerta principal", "DOOR")
    private val at = Instant.parse("2026-11-27T15:00:00Z")

    private fun event(type: String, result: String? = null, action: String? = "OPEN", id: String = "e1", at: Instant = this.at) =
        HistoryEvent(id, type, null, result, action, 4, at, null, "Por comando de voz", "c1")

    @Test
    fun a_voice_command_reads_as_in_the_design() {
        val requested = event("COMMAND_REQUESTED").describe(door)
        assertEquals("Comando de voz reconocido", requested.title)
        assertEquals("Abrir · Puerta principal", requested.subtitle)

        val done = event("COMMAND_RESULT", "SUCCESS").describe(door)
        assertEquals("Puerta principal abierta", done.title)
        assertEquals("Por comando de voz", done.subtitle)
        assertEquals(HistoryTone.SUCCESS, done.tone)
        assertEquals(HistoryCategory.ACTIONS, done.category)

        val light = event("COMMAND_RESULT", "SUCCESS", action = "ON").describe(HistoryDevice(5, "Luz del dormitorio", "LIGHT"))
        assertEquals("Luz del dormitorio encendida", light.title)
    }

    @Test
    fun failures_alerts_and_connection_have_their_own_group() {
        val timeout = event("COMMAND_RESULT", "FAILED_TIMEOUT").describe(door)
        assertEquals("No se pudo abrir puerta principal", timeout.title)
        assertEquals(HistoryTone.WARNING, timeout.tone)
        assertEquals(HistoryCategory.ALERTS, event("HELP_REQUESTED", action = null).describe(null).category)
        assertEquals(HistoryCategory.CONNECTION, event("EDGE_DISCONNECTED", action = null).describe(null).category)
        assertEquals("Dispositivo cambió de estado", event("DEVICE_STATE_CHANGED").describe(null).title)
    }

    @Test
    fun live_events_are_merged_once_and_newest_first() {
        val old = event("COMMAND_REQUESTED", id = "a", at = at.minusSeconds(60))
        val new = event("COMMAND_RESULT", "SUCCESS", id = "b")
        val merged = mergeHistory(listOf(old), listOf(new, old))
        assertEquals(listOf("b", "a"), merged.map { it.id })
    }

    @Test
    fun events_are_grouped_by_today_yesterday_and_before() {
        val zone = ZoneId.of("America/Lima")
        val today = LocalDate.of(2026, 11, 27)
        assertEquals(HistoryDay.TODAY, event("X").dayFor(today, zone))
        assertEquals(HistoryDay.YESTERDAY, event("X", at = Instant.parse("2026-11-26T20:00:00Z")).dayFor(today, zone))
        assertEquals(HistoryDay.EARLIER, event("X", at = Instant.parse("2026-11-20T20:00:00Z")).dayFor(today, zone))
    }

    @Test
    fun the_backend_event_is_read_and_one_without_its_basics_is_dropped() {
        val dto = HistoryEventDto(eventId = "e9", type = "COMMAND_RESULT", result = "SUCCESS", deviceId = 4,
            occurredAt = "2026-11-27T15:00:00.123Z", action = "CLOSE")
        val event = dto.toDomain()!!
        assertEquals(Instant.parse("2026-11-27T15:00:00.123Z"), event.occurredAt)
        assertEquals("CLOSE", event.action)
        assertNull(dto.copy(occurredAt = null).toDomain())
        assertNull(dto.copy(type = null).toDomain())
    }
}
