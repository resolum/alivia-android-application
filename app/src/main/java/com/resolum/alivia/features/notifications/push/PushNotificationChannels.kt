package com.resolum.alivia.features.notifications.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Notification channels; the ids match the ones the backend sends (FirebasePushNotificationSender). */
object PushNotificationChannels {
    const val NOTIFICATIONS = "alivia_notifications"
    const val ALERTS = "alivia_alerts"

    /** Local and backend reminders of the agenda, kept apart from alerts so they are never mistaken for one. */
    const val REMINDERS = "alivia_reminders"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(NOTIFICATIONS, "Notificaciones", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Órdenes, actividades y avisos de Alivia"
                },
                NotificationChannel(REMINDERS, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Turnos, rutinas y medicamentos que se acercan en tu agenda"
                },
                NotificationChannel(ALERTS, "Alertas", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Fallas de dispositivos, pedidos de ayuda y otras alertas urgentes"
                },
            ),
        )
    }
}
