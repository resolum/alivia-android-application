package com.resolum.alivia.features.analytics.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.Pill
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.DangerSoft
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Motion
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavyButton
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.core.ui.theme.Warning
import com.resolum.alivia.core.ui.theme.WarningSoft
import com.resolum.alivia.features.analytics.di.MetricModule
import com.resolum.alivia.features.analytics.domain.models.ActionResult
import com.resolum.alivia.features.analytics.domain.models.ActionSource
import com.resolum.alivia.features.analytics.domain.models.AutonomyAction
import com.resolum.alivia.features.analytics.domain.models.AutonomyMetrics
import com.resolum.alivia.features.analytics.domain.models.AutonomySummary
import com.resolum.alivia.features.analytics.domain.models.ControlledDeviceType
import com.resolum.alivia.features.analytics.domain.models.MetricPeriod
import com.resolum.alivia.features.operations.presentation.orders.NotificationsButton
import java.time.LocalDateTime

/** How often the assisted person managed lights, doors and windows by themselves, and where they struggled. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutonomyScreen(
    onOpenNotifications: () -> Unit,
    viewModel: AutonomyViewModel = viewModel { AutonomyViewModel(MetricModule.getMetrics()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Autonomía", style = MaterialTheme.typography.headlineSmall, color = Navy)
                Text("Acciones que pidió la persona asistida", style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
            NotificationsButton(onClick = onOpenNotifications)
        }
        PeriodSwitch(state.period, viewModel::selectPeriod, Modifier.padding(horizontal = 16.dp))
        if (state.devices.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                item(key = "all") { DeviceChip("Todos", state.deviceId == null) { viewModel.selectDevice(null) } }
                items(state.devices, key = { it.id }) { device ->
                    DeviceChip(device.name, state.deviceId == device.id) { viewModel.selectDevice(device.id) }
                }
            }
        }
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.load(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                val report = state.report
                state.error?.let { message ->
                    item(key = "error") {
                        ErrorBanner(message)
                        TextButton(onClick = { viewModel.load() }) { Text("Reintentar", color = NavyButton) }
                    }
                }
                when {
                    state.loading -> item(key = "loading") { Loading() }
                    report == null -> Unit
                    else -> {
                        item(key = "sync") { SyncStatus(report.metrics) }
                        if (report.actions.isEmpty()) {
                            item(key = "empty") { EmptyMetrics(state.period) }
                        } else {
                            item(key = "summary") { SummaryCard(report.summary) }
                            item(key = "types") { DeviceTypesCard(report.summary) }
                            item(key = "list-title") {
                                Text("Acciones", style = MaterialTheme.typography.titleMedium, color = Navy,
                                    modifier = Modifier.padding(top = 4.dp))
                            }
                            items(report.actions, key = { it.eventId }) { ActionRow(it) }
                            item(key = "privacy") {
                                Text(
                                    "Los comandos de voz se muestran sin audio: solo la intención, el dispositivo, el resultado y el momento.",
                                    style = MaterialTheme.typography.bodySmall, color = InkMuted, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSwitch(period: MetricPeriod, onSelect: (MetricPeriod) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NavySoft).padding(4.dp)) {
        MetricPeriod.entries.forEach { item ->
            val selected = item == period
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (selected) Surface else Color.Transparent)
                    .clickable { onSelect(item) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(item.label, style = MaterialTheme.typography.labelLarge, color = Navy) }
        }
    }
}

@Composable
private fun DeviceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Navy, selectedLabelColor = Surface),
    )
}

/** When the Edge is offline or still sending events, the numbers may be short: say so next to the last update. */
@Composable
private fun SyncStatus(metrics: AutonomyMetrics) {
    val updated = metrics.lastUpdatedAt?.let { "Última actualización: ${moment(it)}" } ?: "Sin actualizaciones registradas"
    if (metrics.mayBeIncomplete) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WarningSoft).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(if (metrics.edgeOnline) Icons.Outlined.Sync else Icons.Outlined.CloudOff, contentDescription = null, tint = Warning,
                modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (metrics.edgeOnline) "Hay eventos por sincronizar" else "El Edge de la vivienda está sin conexión",
                    style = MaterialTheme.typography.titleSmall, color = Ink)
                Text("$updated. Puede haber acciones que aún no aparecen aquí.", style = MaterialTheme.typography.bodySmall, color = Ink)
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Sync, contentDescription = null, tint = InkMuted, modifier = Modifier.size(14.dp))
            Text(updated, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
    }
}

private val RESULT_ORDER = listOf(ActionResult.SUCCESS, ActionResult.FAILED, ActionResult.LOW_CONFIDENCE, ActionResult.NO_CONFIRMATION)

private val ActionResult.label: String
    get() = when (this) {
        ActionResult.SUCCESS -> "Exitosas"
        ActionResult.FAILED -> "Fallidas"
        ActionResult.LOW_CONFIDENCE -> "Rechazadas por baja confianza"
        ActionResult.NO_CONFIRMATION -> "Sin confirmación del actuador"
    }

private val ActionResult.single: String
    get() = when (this) {
        ActionResult.SUCCESS -> "Exitosa"
        ActionResult.FAILED -> "Fallida"
        ActionResult.LOW_CONFIDENCE -> "Baja confianza"
        ActionResult.NO_CONFIRMATION -> "Sin confirmar"
    }

private val ActionResult.color: Color
    get() = when (this) {
        ActionResult.SUCCESS -> TealDark
        ActionResult.FAILED -> Danger
        ActionResult.LOW_CONFIDENCE -> Warning
        ActionResult.NO_CONFIRMATION -> InkMuted
    }

private val ActionResult.soft: Color
    get() = when (this) {
        ActionResult.SUCCESS -> TealSoft
        ActionResult.FAILED -> DangerSoft
        ActionResult.LOW_CONFIDENCE -> WarningSoft
        ActionResult.NO_CONFIRMATION -> SurfaceMuted
    }

/** Tabular figures, so counts do not shift width when they change. */
private const val tabular = "tnum"

@Composable
private fun SummaryCard(summary: AutonomySummary) {
    AliviaCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${summary.executed}", style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = tabular), color = Navy)
            Text(" de ${summary.requested}", style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = tabular),
                color = InkMuted, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(if (summary.requested == 1) "acción solicitada se ejecutó" else "acciones solicitadas se ejecutaron",
            style = MaterialTheme.typography.bodyMedium, color = Ink)
        ResultBar(summary)
        RESULT_ORDER.forEach { result ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(result.color))
                Text(result.label, style = MaterialTheme.typography.bodyMedium, color = Ink,
                    modifier = Modifier.weight(1f).padding(start = 10.dp))
                Text("${summary.count(result)}", style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = tabular), color = Navy)
            }
        }
        if (summary.executedLocally > 0) {
            HorizontalDivider(color = Outline)
            Text(
                if (summary.executedLocally == 1) "1 acción se ejecutó en la vivienda sin Internet y se sincronizó después."
                else "${summary.executedLocally} acciones se ejecutaron en la vivienda sin Internet y se sincronizaron después.",
                style = MaterialTheme.typography.bodySmall, color = InkMuted,
            )
        }
    }
}

/** Share of each result. Widths ease to their new value when the filter changes, so the shift is readable. */
@Composable
private fun ResultBar(summary: AutonomySummary) {
    val total = summary.requested.coerceAtLeast(1).toFloat()
    val description = RESULT_ORDER.joinToString { "${it.label}: ${summary.count(it)}" }
    Row(
        Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(SurfaceMuted)
            .semantics { contentDescription = description },
    ) {
        RESULT_ORDER.forEach { result ->
            val share by animateFloatAsState(summary.count(result) / total, Motion.move(), label = "share-$result")
            if (share > 0.001f) Box(Modifier.weight(share).fillMaxSize().background(result.color))
        }
    }
}

private val ControlledDeviceType.label: String
    get() = when (this) {
        ControlledDeviceType.LIGHT -> "Luces"
        ControlledDeviceType.DOOR -> "Puertas"
        ControlledDeviceType.WINDOW -> "Ventanas"
        ControlledDeviceType.OTHER -> "Otros"
    }

private val ControlledDeviceType.icon: ImageVector
    get() = when (this) {
        ControlledDeviceType.LIGHT -> Icons.Outlined.Lightbulb
        ControlledDeviceType.DOOR -> Icons.Outlined.DoorFront
        ControlledDeviceType.WINDOW -> Icons.Outlined.Window
        ControlledDeviceType.OTHER -> Icons.Outlined.Sensors
    }

@Composable
private fun DeviceTypesCard(summary: AutonomySummary) {
    AliviaCard {
        Text("Por tipo de dispositivo", style = MaterialTheme.typography.titleSmall, color = Navy)
        val types = ControlledDeviceType.entries.filter { it != ControlledDeviceType.OTHER || summary.byType.containsKey(it) }
        types.forEach { type ->
            val count = summary.byType[type]
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(NavySoft), contentAlignment = Alignment.Center) {
                    Icon(type.icon, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                }
                Text(type.label, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f).padding(start = 12.dp))
                Text(
                    if (count == null) "Sin acciones" else "${count.executed} de ${count.requested}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = tabular),
                    color = if (count == null) InkMuted else Navy,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(action: AutonomyAction) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(action.result.soft), contentAlignment = Alignment.Center) {
            Icon(
                if (action.source == ActionSource.VOICE) Icons.Outlined.MicNone else Icons.Outlined.TouchApp,
                contentDescription = if (action.source == ActionSource.VOICE) "Comando de voz" else "Desde la app",
                tint = action.result.color, modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(action.intent, style = MaterialTheme.typography.titleSmall, color = Navy, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${action.deviceName} · ${moment(action.occurredAt)}", style = MaterialTheme.typography.bodySmall, color = InkMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (action.executedLocally) {
                Text("Ejecutada localmente", style = MaterialTheme.typography.labelSmall, color = TealDark)
            }
        }
        Spacer(Modifier.width(8.dp))
        Pill(action.result.single, action.result.soft, action.result.color)
    }
}

@Composable
private fun EmptyMetrics(period: MetricPeriod) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Insights, contentDescription = null, tint = Navy, modifier = Modifier.size(30.dp))
        }
        Text("Sin acciones en este periodo", style = MaterialTheme.typography.titleMedium, color = Navy)
        Text(
            if (period == MetricPeriod.TODAY) "Hoy todavía no se pidió ninguna acción sobre luces, puertas o ventanas."
            else "Prueba con un periodo más largo u otro dispositivo.",
            style = MaterialTheme.typography.bodySmall, color = InkMuted, textAlign = TextAlign.Center,
        )
    }
}

private fun moment(value: LocalDateTime): String = "${Formats.day(value.toLocalDate())}, ${Formats.time(value.toLocalTime())}"
