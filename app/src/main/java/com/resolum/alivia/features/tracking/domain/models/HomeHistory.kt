package com.resolum.alivia.features.tracking.domain.models

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What kind of filter of the history an event belongs to. */
enum class HistoryCategory(val label: String) { ACTIONS("Acciones"), ALERTS("Alertas"), CONNECTION("Conexión") }

/** How the row is drawn: what went well, what needs a look, what is serious. */
enum class HistoryTone { NEUTRAL, SUCCESS, WARNING, DANGER }

/** The device an event is about, as the home status names it. */
data class HistoryDevice(val id: Long, val alias: String, val type: String?)

/** One thing that happened in the home, as `/events/recent` and the live channel give it. */
data class HistoryEvent(
    val id: String,
    val type: String,
    val severity: String?,
    val result: String?,
    val action: String?,
    val deviceId: Long?,
    val occurredAt: Instant,
    val batteryPct: Int?,
    val detail: String?,
    val commandId: String?,
) {
    /** The text the history shows for it, with the name of its device when the home has it. */
    fun describe(device: HistoryDevice?): HistoryEntry {
        val name = device?.alias ?: "Dispositivo"
        val verb = verbOf(action)
        return when (type) {
            "COMMAND_REQUESTED" -> HistoryEntry(this, "Comando de voz reconocido",
                listOfNotNull(verb?.replaceFirstChar { it.uppercase() }, device?.alias).joinToString(" · ").ifEmpty { "Por comando de voz" },
                HistoryCategory.ACTIONS, HistoryTone.NEUTRAL, device)
            "COMMAND_RESULT" -> if (result == "SUCCESS") {
                HistoryEntry(this, "$name ${participleOf(action)}", detail ?: "Por comando de voz", HistoryCategory.ACTIONS,
                    HistoryTone.SUCCESS, device)
            } else {
                HistoryEntry(this, "No se pudo ${verb ?: "accionar"} ${name.lowercaseFirst()}",
                    if (result == "FAILED_TIMEOUT") "El actuador no respondió a tiempo" else "El actuador informó una falla",
                    HistoryCategory.ACTIONS, HistoryTone.WARNING, device)
            }
            "COMMAND_REJECTED" -> HistoryEntry(this, "Orden no ejecutada",
                if (result == "REJECTED_LOW_CONFIDENCE") "No se entendió la orden con seguridad" else (detail ?: "El dispositivo no está disponible"),
                HistoryCategory.ACTIONS, HistoryTone.WARNING, device)
            "DEVICE_STATE_CHANGED" -> HistoryEntry(this, "$name cambió de estado", detail ?: "Informado por el equipo",
                HistoryCategory.ACTIONS, HistoryTone.NEUTRAL, device)
            "LOW_BATTERY" -> HistoryEntry(this, "Batería baja · $name", batteryPct?.let { "Queda $it %" } ?: "Conviene cargarlo",
                HistoryCategory.ALERTS, HistoryTone.WARNING, device)
            "DEVICE_FAILURE" -> HistoryEntry(this, "Falla en $name", detail ?: "Se avisó a la familia", HistoryCategory.ALERTS,
                HistoryTone.DANGER, device)
            "HELP_REQUESTED" -> HistoryEntry(this, "Pedido de ayuda", "Se avisó a la familia", HistoryCategory.ALERTS,
                HistoryTone.DANGER, device)
            "DEVICE_RECOVERED" -> HistoryEntry(this, "$name volvió a responder", "Conexión restablecida",
                HistoryCategory.CONNECTION, HistoryTone.SUCCESS, device)
            "DEVICE_DISCONNECTED" -> HistoryEntry(this, "$name sin conexión", "Dejó de reportar", HistoryCategory.CONNECTION,
                HistoryTone.WARNING, device)
            "EDGE_DISCONNECTED" -> HistoryEntry(this, "Se perdió la conexión del hogar", "El equipo central dejó de reportar",
                HistoryCategory.CONNECTION, HistoryTone.DANGER, device)
            "EDGE_RECONNECTED" -> HistoryEntry(this, "Conexión del hogar restablecida", "El equipo central volvió a reportar",
                HistoryCategory.CONNECTION, HistoryTone.SUCCESS, device)
            else -> HistoryEntry(this, detail ?: "Evento del hogar", name, HistoryCategory.ACTIONS, HistoryTone.NEUTRAL, device)
        }
    }

    private companion object {
        fun verbOf(action: String?) = when (action) {
            "OPEN" -> "abrir"; "CLOSE" -> "cerrar"; "ON" -> "encender"; "OFF" -> "apagar"; else -> null
        }

        // Puerta, ventana, cortina y luz: todos femeninos.
        fun participleOf(action: String?) = when (action) {
            "OPEN" -> "abierta"; "CLOSE" -> "cerrada"; "ON" -> "encendida"; "OFF" -> "apagada"; else -> "accionada"
        }

        fun String.lowercaseFirst() = replaceFirstChar { it.lowercase() }
    }
}

/** A row of the history, ready to draw. */
data class HistoryEntry(
    val event: HistoryEvent,
    val title: String,
    val subtitle: String,
    val category: HistoryCategory,
    val tone: HistoryTone,
    val device: HistoryDevice?,
)

/** The groups of the history: today, yesterday and before. */
enum class HistoryDay(val label: String) { TODAY("Hoy"), YESTERDAY("Ayer"), EARLIER("Anteriores") }

fun HistoryEvent.dayFor(today: LocalDate, zone: ZoneId): HistoryDay {
    val date = occurredAt.atZone(zone).toLocalDate()
    return when {
        date == today -> HistoryDay.TODAY
        date == today.minusDays(1) -> HistoryDay.YESTERDAY
        else -> HistoryDay.EARLIER
    }
}

/** Adds the live events to what was loaded, without repeating one, newest first. */
fun mergeHistory(current: List<HistoryEvent>, incoming: List<HistoryEvent>, max: Int = 200): List<HistoryEvent> =
    (incoming + current).distinctBy { it.id to it.type }.sortedByDescending { it.occurredAt }.take(max)

/** The live channel of the history: whether it is open, and each event it brings. */
sealed interface LiveHistoryMessage {
    data object Connected : LiveHistoryMessage
    data class Event(val event: HistoryEvent) : LiveHistoryMessage
}
