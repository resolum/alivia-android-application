package com.resolum.alivia.features.analytics.domain.models

import java.time.LocalDate
import java.time.LocalDateTime

enum class ControlledDeviceType { LIGHT, DOOR, WINDOW, OTHER }

/** How an action requested by the assisted person ended. */
enum class ActionResult { SUCCESS, FAILED, LOW_CONFIDENCE, NO_CONFIRMATION }

enum class ActionSource { VOICE, APP, OTHER }

/**
 * One action the assisted person asked for. A voice command is kept only as its intent: the captured audio is
 * never part of this model, so it cannot be shown or played.
 */
data class AutonomyAction(
    val eventId: String,
    val deviceId: Long?,
    val deviceName: String,
    val deviceType: ControlledDeviceType,
    val intent: String,
    val source: ActionSource,
    val result: ActionResult,
    val occurredAt: LocalDateTime,
    /** Run by the home's Edge while it had no Internet, and synchronized later. */
    val executedLocally: Boolean,
)

data class AutonomyDevice(val id: Long, val name: String, val type: ControlledDeviceType)

/** What the backend knows about the home's actions, and how fresh it is. */
data class AutonomyMetrics(
    val actions: List<AutonomyAction>,
    val devices: List<AutonomyDevice>,
    val lastUpdatedAt: LocalDateTime?,
    val edgeOnline: Boolean,
    val pendingEvents: Boolean,
) {
    /** Results may be missing events: the Edge is offline or still has events to send. */
    val mayBeIncomplete: Boolean get() = !edgeOnline || pendingEvents
}

enum class MetricPeriod(val label: String, val days: Long) {
    TODAY("Hoy", 1), WEEK("7 días", 7), MONTH("30 días", 30);

    fun from(today: LocalDate): LocalDate = today.minusDays(days - 1)
}

data class DeviceTypeCount(val requested: Int, val executed: Int)

data class AutonomySummary(
    val requested: Int,
    val executed: Int,
    val byResult: Map<ActionResult, Int>,
    val byType: Map<ControlledDeviceType, DeviceTypeCount>,
    val executedLocally: Int,
) {
    fun count(result: ActionResult): Int = byResult[result] ?: 0
}

object AutonomyCalculator {
    /** An event synchronized twice (offline retry) counts once. */
    fun distinct(actions: List<AutonomyAction>): List<AutonomyAction> = actions.distinctBy { it.eventId }

    fun filter(actions: List<AutonomyAction>, deviceId: Long?, from: LocalDate, to: LocalDate): List<AutonomyAction> =
        actions.filter { action ->
            val day = action.occurredAt.toLocalDate()
            (deviceId == null || action.deviceId == deviceId) && !day.isBefore(from) && !day.isAfter(to)
        }

    fun summarize(actions: List<AutonomyAction>): AutonomySummary = AutonomySummary(
        requested = actions.size,
        executed = actions.count { it.result == ActionResult.SUCCESS },
        byResult = actions.groupingBy { it.result }.eachCount(),
        byType = actions.groupBy { it.deviceType }.mapValues { (_, list) ->
            DeviceTypeCount(list.size, list.count { it.result == ActionResult.SUCCESS })
        },
        executedLocally = actions.count { it.executedLocally },
    )
}
