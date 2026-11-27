package com.resolum.alivia.features.notifications.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.resolum.alivia.MainActivity
import com.resolum.alivia.R
import com.resolum.alivia.features.notifications.di.NotificationsModule
import com.resolum.alivia.features.notifications.domain.PushMessage
import com.resolum.alivia.features.nursing.di.ActivityModule
import com.resolum.alivia.features.nursing.reminders.AgendaReminders
import com.resolum.alivia.features.shared.di.SharedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives Firebase Cloud Messaging events. With the app in the background Android shows the
 * notification by itself; in the foreground it arrives here and is shown the same way.
 */
class AliviaMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            // Only a signed-in account can own the device; otherwise the token is sent at the next sign-in.
            if (SharedModule.tokenDataStore.restore() != null) {
                NotificationsModule.registerPushDevice()(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val push = PushMessage.from(message.notification?.title, message.notification?.body, message.data)
        if (push.touchesAgenda) {
            // A reassignment changes who gets the reminders: refresh the open agenda and the local alarms.
            AgendaReminders.notifyAgendaChanged()
            scope.launch { runCatching { ActivityModule.syncReminders() } }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            this,
            when {
                push.isAlert -> PushNotificationChannels.ALERTS
                push.isReminder -> PushNotificationChannels.REMINDERS
                else -> PushNotificationChannels.NOTIFICATIONS
            },
        )
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setSubText(if (push.isAlert) "Alerta" else if (push.isReminder) "Recordatorio" else null)
            .setContentTitle(push.title)
            .setContentText(push.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(push.body))
            .setPriority(if (push.isAlert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(this).notify((push.notificationId ?: System.currentTimeMillis()).toInt(), notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
