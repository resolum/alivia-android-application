package com.resolum.alivia.features.nursing.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.toArgb
import com.resolum.alivia.MainActivity
import com.resolum.alivia.R
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.features.notifications.push.PushNotificationChannels
import com.resolum.alivia.features.nursing.di.ActivityModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Shows a planned reminder or dose alert when its alarm goes off, unless it stopped being relevant. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val reminder = AgendaReminders.decode(intent.getStringExtra(EXTRA_REMINDER)) ?: return
        // An alarm delayed by doze or by a phone that was off can arrive late: past its moment it is not shown.
        if (System.currentTimeMillis() > reminder.relevantUntil) return

        when (reminder.kind) {
            ReminderKind.REMINDER -> show(context, reminder)
            ReminderKind.MEDICATION_ALERT -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Only a dose that is still unconfirmed raises the alert; without connection it is raised anyway.
                        val stillPending = reminder.administrationId?.let { id ->
                            withTimeoutOrNull(CHECK_TIMEOUT_MS) { ActivityModule.isDosePending(id) }
                        }
                        if (stillPending != false) show(context, reminder)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun show(context: Context, reminder: Reminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val alert = reminder.kind == ReminderKind.MEDICATION_ALERT
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, if (alert) PushNotificationChannels.ALERTS else PushNotificationChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            // The sub text and color tell a reminder (something coming up) from an alert (something that went wrong).
            .setSubText(if (alert) "Alerta" else "Recordatorio")
            .setColor((if (alert) Danger else TealDark).toArgb())
            .setContentTitle(reminder.title)
            .setContentText(reminder.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.body))
            .setCategory(if (alert) NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (alert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setTimeoutAfter((reminder.relevantUntil - System.currentTimeMillis()).coerceAtLeast(1))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(reminder.key.hashCode(), notification)
    }

    companion object {
        const val ACTION = "com.resolum.alivia.action.AGENDA_REMINDER"
        const val EXTRA_REMINDER = "reminder"
        private const val CHECK_TIMEOUT_MS = 8_000L
    }
}

/** Puts the agenda alarms back after the phone restarts or the app is updated. */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AgendaReminders.restore()
        }
    }
}
