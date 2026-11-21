package com.resolum.alivia.features.nursing.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDateTime

/**
 * Keeps one local alarm per planned reminder. Alarms live on the phone, so a reminder still shows without
 * connection; the planned list is stored to reschedule it after a reboot and to tell what changed in the agenda.
 */
object AgendaReminders {
    private const val PREFS = "agenda_reminders"
    private const val KEY_PLANNED = "planned"
    private val gson = Gson()
    private val listType = object : TypeToken<List<Reminder>>() {}.type

    private lateinit var appContext: Context

    private val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when something outside the agenda screen (a push) says the agenda may have changed. */
    val agendaChanged: SharedFlow<Unit> = changed.asSharedFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun notifyAgendaChanged() {
        changed.tryEmit(Unit)
    }

    /** Replaces the alarms with the ones of [activities] and returns what changed since the previous sync. */
    @Synchronized
    fun sync(activities: List<CareActivity>, now: LocalDateTime = LocalDateTime.now(Formats.zone)): AgendaChange {
        val previous = stored()
        val change = ReminderPlanner.changes(previous, activities, now)
        val planned = ReminderPlanner.plan(activities, now)
        val keep = planned.map { it.key }.toSet()
        previous.orEmpty().filter { it.key !in keep }.forEach(::cancel)
        planned.forEach(::schedule)
        store(planned)
        return change
    }

    /** Drops the alert of a dose once its result is recorded. */
    @Synchronized
    fun dismissAlert(activityKey: String) {
        val planned = stored() ?: return
        val (gone, left) = planned.partition { it.activityKey == activityKey && it.kind == ReminderKind.MEDICATION_ALERT }
        gone.forEach(::cancel)
        if (gone.isNotEmpty()) store(left)
    }

    /** Alarms are erased when the phone restarts; this puts back the ones still ahead. */
    fun restore() {
        val now = System.currentTimeMillis()
        stored().orEmpty().filter { it.relevantUntil > now }.forEach(::schedule)
    }

    /** On sign-out: the next account must not get this one's reminders. */
    @Synchronized
    fun clear() {
        stored().orEmpty().forEach(::cancel)
        prefs().edit().remove(KEY_PLANNED).apply()
    }

    private fun schedule(reminder: Reminder) {
        val manager = appContext.getSystemService(AlarmManager::class.java) ?: return
        // Inexact on purpose: no exact-alarm permission is needed and a few minutes of drift is fine for a reminder.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pendingIntent(reminder))
    }

    private fun cancel(reminder: Reminder) {
        appContext.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(reminder))
    }

    private fun pendingIntent(reminder: Reminder): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        reminder.key.hashCode(),
        Intent(appContext, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION)
            .putExtra(ReminderReceiver.EXTRA_REMINDER, gson.toJson(reminder)),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun decode(json: String?): Reminder? = json?.let { runCatching { gson.fromJson(it, Reminder::class.java) }.getOrNull() }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Null before the first sync, so the first load does not report the whole agenda as new. */
    private fun stored(): List<Reminder>? = prefs().getString(KEY_PLANNED, null)
        ?.let { runCatching { gson.fromJson<List<Reminder>>(it, listType) }.getOrNull() }

    private fun store(reminders: List<Reminder>) {
        prefs().edit().putString(KEY_PLANNED, gson.toJson(reminders)).apply()
    }
}
