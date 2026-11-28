package com.resolum.alivia.features.tracking.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.DangerSoft
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.core.ui.theme.Warning
import com.resolum.alivia.core.ui.theme.WarningSoft
import com.resolum.alivia.features.nursing.di.ActivityModule
import com.resolum.alivia.features.operations.presentation.orders.ScreenHeader
import com.resolum.alivia.features.tracking.di.HomeHistoryModule
import com.resolum.alivia.features.tracking.domain.models.HistoryCategory
import com.resolum.alivia.features.tracking.domain.models.HistoryDay
import com.resolum.alivia.features.tracking.domain.models.HistoryEntry
import com.resolum.alivia.features.tracking.domain.models.HistoryTone
import com.resolum.alivia.features.tracking.domain.models.dayFor
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What happened in the home: the actions of the voice commands, the alerts and the connection, live. */
@Composable
fun HistoryScreen(
    onOpenNotifications: () -> Unit,
    viewModel: HistoryViewModel = viewModel {
        HistoryViewModel(ActivityModule.getAssistedPersons(), HomeHistoryModule.getHistory(), HomeHistoryModule.getDevices(),
            HomeHistoryModule.follow())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.start()
        onPauseOrDispose { viewModel.stop() }
    }

    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    val entries = state.entries
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Historial",
            subtitle = state.person?.let { "Hogar de ${it.firstName}" },
            onOpenProfile = onOpenNotifications,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterOption("Todos", state.filter == null) { viewModel.select(null) }
            HistoryCategory.entries.forEach { category ->
                FilterOption(category.label, state.filter == category) { viewModel.select(category) }
            }
            if (state.live) Pill("En vivo", TealSoft, TealDark, dot = TealDark)
            else if (!state.loading && state.person != null) Pill("Reconectando", SurfaceMuted, InkMuted, dot = InkMuted)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            state.error?.let { item { ErrorBanner(it) } }
            when {
                state.loading -> item { Loading() }
                state.noPerson -> item { EmptyHistory("Todavía no hay una persona asistida en tu red de cuidado") }
                entries.isEmpty() && state.error == null -> item {
                    EmptyHistory(if (state.filter == null) "Aún no hay acciones registradas" else "No hay eventos con este filtro")
                }
                else -> entries.groupBy { it.event.dayFor(today, zone) }.toSortedMap().forEach { (day, rows) ->
                    item(key = "d-$day") { SectionLabel(day.label.uppercase(), Modifier.padding(top = 4.dp)) }
                    items(rows, key = { "${it.event.type}-${it.event.id}" }) { entry -> HistoryRow(entry, day, zone) }
                }
            }
        }
    }
}

@Composable
private fun FilterOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Navy, selectedLabelColor = Surface,
            containerColor = Surface, labelColor = Navy),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Outline,
            selectedBorderColor = Navy),
    )
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-PE"))
private val DATE_TIME = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-PE"))

@Composable
private fun HistoryRow(entry: HistoryEntry, day: HistoryDay, zone: ZoneId) {
    val (container, tint) = entry.tone.colors()
    AliviaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
                Icon(entry.icon(), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleSmall, color = Navy, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
            val at = entry.event.occurredAt.atZone(zone)
            Text((if (day == HistoryDay.EARLIER) DATE_TIME else TIME).format(at), style = MaterialTheme.typography.labelSmall,
                color = InkMuted)
        }
    }
}

private fun HistoryTone.colors(): Pair<Color, Color> = when (this) {
    HistoryTone.SUCCESS -> TealSoft to TealDark
    HistoryTone.WARNING -> WarningSoft to Warning
    HistoryTone.DANGER -> DangerSoft to Danger
    HistoryTone.NEUTRAL -> NavySoft to Navy
}

private fun HistoryEntry.icon(): ImageVector = when (event.type) {
    "COMMAND_REQUESTED" -> Icons.Outlined.RecordVoiceOver
    "HELP_REQUESTED" -> Icons.Outlined.NotificationImportant
    "LOW_BATTERY" -> Icons.Outlined.BatteryAlert
    "DEVICE_FAILURE", "COMMAND_REJECTED" -> Icons.Outlined.ErrorOutline
    "EDGE_DISCONNECTED", "DEVICE_DISCONNECTED" -> Icons.Outlined.WifiOff
    "EDGE_RECONNECTED", "DEVICE_RECOVERED" -> Icons.Outlined.Wifi
    else -> when (device?.type) {
        "DOOR" -> Icons.Outlined.DoorFront
        "WINDOW", "CURTAIN" -> Icons.Outlined.Window
        "LIGHT" -> Icons.Outlined.Lightbulb
        "MICROPHONE" -> Icons.Outlined.Mic
        else -> Icons.Outlined.Sensors
    }
}

@Composable
private fun EmptyHistory(text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)).background(NavySoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.History, contentDescription = null, tint = Navy, modifier = Modifier.size(30.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = Navy, textAlign = TextAlign.Center)
        Text("Cada orden por voz y cada alerta del hogar aparecerá aquí al instante.", style = MaterialTheme.typography.bodyMedium,
            color = InkMuted, textAlign = TextAlign.Center)
    }
}

