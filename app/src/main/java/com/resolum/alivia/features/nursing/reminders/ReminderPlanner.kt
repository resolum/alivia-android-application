package com.resolum.alivia.features.nursing.reminders

import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.MedicationAdministration
import java.time.LocalDateTime
import java.time.ZoneId

/** A reminder tells about something coming up; an alert tells about something that went wrong. */
enum class ReminderKind { REMINDER, MEDICATION_ALERT }

/** A local notification planned for one agenda occurrence. Times are epoch millis so it can be stored as is. */
data class Reminder(
    val key: String,
    val activityKey: String,
    val kind: ReminderKind,
    val title: String,
    val body: String,
    val triggerAt: Long,
    /** Past this moment the notification is no longer useful and is dropped instead of shown. */
    val relevantUntil: Long,
    val activityTitle: String,
    val administrationId: String? = null,
)

/** How the caregiver's agenda changed since the last sync, by occurrence title. */
data class AgendaChange(val removed: List<String> = emptyList(), val added: List<String> = emptyList()) {
    val isEmpty: Boolean get() = removed.isEmpty() && added.isEmpty()
}

object ReminderPlanner {
    /** Minutes before the start when the reminder is shown. */
    const val LEAD_MINUTES = 15L

    /** Minutes after the window of a dose when, still unconfirmed, it raises an alert. */
    const val ALERT_GRACE_MINUTES = 1L

    private val OPEN = setOf(CareActivityStatus.SCHEDULED, CareActivityStatus.IN_PROGRESS)

    /** Reminders of the open occurrences the account is responsible for that are still ahead. */
    fun plan(activities: List<CareActivity>, now: LocalDateTime, zone: ZoneId = Formats.zone): List<Reminder> =
        activities.filter { it.mine && it.status in OPEN }.flatMap { activity ->
            val start = LocalDateTime.of(activity.date, activity.startTime)
            val end = endOf(activity)
            buildList {
                val remindAt = start.minusMinutes(LEAD_MINUTES)
                if (remindAt.isAfter(now)) add(reminder(activity, remindAt, end, zone))
                val assignmentId = activity.assignmentId
                if (activity.type == CareActivityType.MEDICATION && assignmentId != null) {
                    val alertAt = end.plusMinutes(ALERT_GRACE_MINUTES)
                    if (alertAt.isAfter(now)) add(alert(activity, assignmentId, alertAt, zone))
                }
            }
        }.distinctBy { it.key }

    /**
     * Compares the reminders planned at the last sync with the agenda now. An occurrence that had a reminder and is no
     * longer the account's (reassigned or removed) counts as removed; a new upcoming one as added. Without a previous
     * sync nothing is reported, so the first load does not announce the whole agenda.
     */
    fun changes(previous: List<Reminder>?, activities: List<CareActivity>, now: LocalDateTime, zone: ZoneId = Formats.zone): AgendaChange {
        if (previous == null) return AgendaChange()
        val byKey = activities.associateBy { it.id }
        val nowMillis = now.atZone(zone).toInstant().toEpochMilli()
        val removed = previous.filter { it.kind == ReminderKind.REMINDER && it.relevantUntil > nowMillis }
            .filter { reminder -> byKey[reminder.activityKey].let { it == null || !it.mine } }
            .map { it.activityTitle }
        val known = previous.map { it.activityKey }.toSet()
        val added = plan(activities, now, zone).filter { it.kind == ReminderKind.REMINDER && it.activityKey !in known }
            .map { it.activityTitle }
        return AgendaChange(removed.distinct(), added.distinct())
    }

    private fun endOf(activity: CareActivity): LocalDateTime {
        val start = LocalDateTime.of(activity.date, activity.startTime)
        return activity.endTime?.let { LocalDateTime.of(if (it.isBefore(activity.startTime)) activity.date.plusDays(1) else activity.date, it) }
            ?: start.plusMinutes(MedicationAdministration.DEFAULT_WINDOW_MINUTES)
    }

    private fun reminder(activity: CareActivity, at: LocalDateTime, end: LocalDateTime, zone: ZoneId): Reminder {
        val person = activity.assistedPerson?.let { " · para $it" }.orEmpty()
        val (title, body) = if (activity.type == CareActivityType.MEDICATION) {
            // Names the medication and time only: the reminder never suggests changing the prescription.
            "Recordatorio de medicamento" to "${activity.title} a las ${Formats.time(activity.startTime)}$person"
        } else {
            val period = activity.endTime?.let { Formats.window(activity.startTime, it) } ?: Formats.time(activity.startTime)
            "Recordatorio de actividad" to "${activity.title} · $period$person"
        }
        return Reminder(
            key = "${activity.id}#reminder",
            activityKey = activity.id,
            kind = ReminderKind.REMINDER,
            title = title,
            body = body,
            triggerAt = at.millis(zone),
            relevantUntil = end.millis(zone),
            activityTitle = activity.title,
        )
    }

    private fun alert(activity: CareActivity, assignmentId: Long, at: LocalDateTime, zone: ZoneId): Reminder {
        val person = activity.assistedPerson?.let { " para $it" }.orEmpty()
        return Reminder(
            key = "${activity.id}#alert",
            activityKey = activity.id,
            kind = ReminderKind.MEDICATION_ALERT,
            title = "Medicamento sin confirmar",
            body = "${activity.title} de las ${Formats.time(activity.startTime)}$person venció sin registro. Registra si se administró o se omitió.",
            triggerAt = at.millis(zone),
            relevantUntil = at.plusHours(12).millis(zone),
            activityTitle = activity.title,
            administrationId = MedicationAdministration.idOf(assignmentId, activity.date),
        )
    }

    private fun LocalDateTime.millis(zone: ZoneId) = atZone(zone).toInstant().toEpochMilli()
}
