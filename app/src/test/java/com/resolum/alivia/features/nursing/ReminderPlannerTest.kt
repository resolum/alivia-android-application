package com.resolum.alivia.features.nursing

import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.CarePriority
import com.resolum.alivia.features.nursing.reminders.ReminderKind
import com.resolum.alivia.features.nursing.reminders.ReminderPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val zone = ZoneId.of("America/Lima")
    private val date = LocalDate.of(2026, 11, 21)
    private val now = LocalDateTime.of(date, LocalTime.of(7, 0))

    private fun activity(
        id: String, title: String, start: LocalTime, end: LocalTime? = start.plusMinutes(30),
        type: CareActivityType = CareActivityType.HYGIENE, mine: Boolean = true,
        status: CareActivityStatus = CareActivityStatus.SCHEDULED,
    ) = CareActivity(
        id = id, activityId = 1, assignmentId = id.length.toLong(), title = title, type = type, assistedPerson = "Rosa",
        responsible = "Tú", date = date, startTime = start, endTime = end, status = status, recurring = false,
        priority = CarePriority.ROUTINE, mine = mine,
    )

    private fun millis(time: LocalTime) = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun remindsAheadOfTheStartAndNamesPeriodAndPerson() {
        val plan = ReminderPlanner.plan(listOf(activity("a", "Baño asistido", LocalTime.of(9, 0))), now, zone)
        val reminder = plan.single()
        assertEquals(ReminderKind.REMINDER, reminder.kind)
        assertEquals(millis(LocalTime.of(8, 45)), reminder.triggerAt)
        assertEquals(millis(LocalTime.of(9, 30)), reminder.relevantUntil)
        assertTrue(reminder.body.contains("Baño asistido"))
        assertTrue(reminder.body.contains("para Rosa"))
        assertTrue(reminder.body.contains("–"))
    }

    @Test
    fun aDoseGetsAReminderWithoutAdviceAndAnAlertWhenItsWindowEnds() {
        val dose = activity("dose", "Enalapril 10 mg", LocalTime.of(8, 0), type = CareActivityType.MEDICATION)
        val plan = ReminderPlanner.plan(listOf(dose), now, zone)
        val reminder = plan.first { it.kind == ReminderKind.REMINDER }
        assertEquals("Recordatorio de medicamento", reminder.title)
        assertTrue(reminder.body.startsWith("Enalapril 10 mg a las"))
        listOf("dosis", "aument", "reduc", "cambi", "suspend").forEach { assertFalse(reminder.body.lowercase().contains(it)) }

        val alert = plan.first { it.kind == ReminderKind.MEDICATION_ALERT }
        assertEquals(millis(LocalTime.of(8, 31)), alert.triggerAt)
        assertEquals("4-20261121", alert.administrationId)
    }

    @Test
    fun skipsOthersClosedAndPastOccurrences() {
        val plan = ReminderPlanner.plan(
            listOf(
                activity("other", "De otro", LocalTime.of(9, 0), mine = false),
                activity("done", "Hecha", LocalTime.of(9, 0), status = CareActivityStatus.COMPLETED),
                activity("soon", "Ya casi", LocalTime.of(7, 10)),
            ),
            now, zone,
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun aReassignmentRemovesTheReminderAndIsReported() {
        val bath = activity("bath", "Baño", LocalTime.of(10, 0))
        val walk = activity("walk", "Paseo", LocalTime.of(11, 0))
        val previous = ReminderPlanner.plan(listOf(bath, walk), now, zone)

        val agendaNow = listOf(bath.copy(mine = false, responsible = "Ana"), walk, activity("meal", "Almuerzo", LocalTime.of(12, 0)))
        val change = ReminderPlanner.changes(previous, agendaNow, now, zone)
        assertEquals(listOf("Baño"), change.removed)
        assertEquals(listOf("Almuerzo"), change.added)
        assertTrue(ReminderPlanner.plan(agendaNow, now, zone).none { it.activityKey == "bath" })
    }

    @Test
    fun theFirstSyncAndACompletionAreNotReportedAsChanges() {
        val bath = activity("bath", "Baño", LocalTime.of(10, 0))
        assertTrue(ReminderPlanner.changes(null, listOf(bath), now, zone).isEmpty)

        val previous = ReminderPlanner.plan(listOf(bath), now, zone)
        val completed = bath.copy(status = CareActivityStatus.COMPLETED)
        assertTrue(ReminderPlanner.changes(previous, listOf(completed), now, zone).isEmpty)
    }
}
