package com.resolum.alivia.features.notifications.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Notification channels; the ids match the ones the backend sends (FirebasePushNotificationSender). */
object PushNotificationChannels {
    const val NOTIFICATIONS = "alivia_notifications"
    const val ALERTS = "alivia_alerts"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(NOTIFICATIONS, "Notificaciones", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Órdenes, actividades y avisos de Alivia"
                },
                NotificationChannel(ALERTS, "Alertas", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Fallas de dispositivos, pedidos de ayuda y otras alertas urgentes"
                },
            ),
        )
    }
}
