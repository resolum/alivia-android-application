package com.resolum.alivia.features.nursing.presentation.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavyButton
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.features.nursing.di.ActivityModule
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.presentation.container
import com.resolum.alivia.features.nursing.presentation.content
import com.resolum.alivia.features.nursing.presentation.icon
import com.resolum.alivia.features.nursing.presentation.label
import com.resolum.alivia.features.operations.presentation.orders.NotificationsButton
import com.resolum.alivia.features.profiles.di.ProfileModule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle

/** The caregiver's care agenda as a list (yesterday to the day after tomorrow) or a month calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    onOpenNotifications: () -> Unit,
    onNewActivity: () -> Unit,
    viewModel: ActivitiesViewModel = viewModel {
        ActivitiesViewModel(ActivityModule.getActivities(), ActivityModule.completeActivity(), ActivityModule.nameAgendaPeople(),
            ProfileModule.getMyHousehold())
    },
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
                Text("Actividades", style = MaterialTheme.typography.headlineSmall, color = Navy)
                Text(state.summary, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
            NotificationsButton(onClick = onOpenNotifications)
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ViewSwitch(state.view, viewModel::selectView, Modifier.weight(1f))
            if (state.canCreate) {
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onNewActivity,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(44.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Nueva", style = MaterialTheme.typography.labelLarge)
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
                state.error?.let { message ->
                    item {
                        ErrorBanner(message)
                        TextButton(onClick = { viewModel.load() }) { Text("Reintentar", color = NavyButton) }
                    }
                }
                if (state.view == ActivitiesView.CALENDAR) {
                    item { MonthCalendar(state, viewModel::moveMonth, viewModel::selectDay) }
                    item {
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(Formats.longDay(state.selectedDay), style = MaterialTheme.typography.titleMedium, color = Navy,
                                modifier = Modifier.weight(1f))
                            val count = state.selectedDayActivities.size
                            Pill(if (count == 1) "1 actividad" else "$count actividades", NavySoft, Navy)
                        }
                    }
                    if (state.monthLoading && state.monthActivities.isEmpty()) {
                        item { Loading() }
                    } else if (state.selectedDayActivities.isEmpty()) {
                        item {
                            AliviaCard {
                                Text("No hay actividades este día.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                            }
                        }
                    }
                    items(state.selectedDayActivities, key = { "c-${it.id}" }) { ActivityCard(it) { viewModel.open(it) } }
                } else if (state.loading) {
                    item { Loading() }
                } else {
                    val groups = state.groups
                    if (groups.isEmpty() && state.error == null) item { EmptyActivities() }
                    groups.forEach { group ->
                        item(key = "h-${group.kind}") { GroupHeader(group, state) }
                        items(group.activities, key = { "${group.kind}-${it.id}" }) { ActivityCard(it) { viewModel.open(it) } }
                    }
                }
            }
        }
    }

    state.selected?.let { activity ->
        ActivityDetailSheet(activity, state.completing, state.completeError, onComplete = viewModel::complete,
            onDismiss = { viewModel.open(null) })
    }
}

@Composable
private fun ViewSwitch(view: ActivitiesView, onSelect: (ActivitiesView) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(NavySoft).padding(4.dp)) {
        listOf(
            Triple(ActivitiesView.LIST, "Lista", Icons.AutoMirrored.Outlined.List),
            Triple(ActivitiesView.CALENDAR, "Calendario", Icons.Outlined.CalendarMonth),
        ).forEach { (item, label, icon) ->
            val selected = view == item
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (selected) Surface else Color.Transparent)
                    .clickable { onSelect(item) }.padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                Text(" $label", style = MaterialTheme.typography.labelLarge, color = Navy)
            }
        }
    }
}

@Composable
private fun MonthCalendar(state: ActivitiesUiState, onMove: (Long) -> Unit, onSelect: (LocalDate) -> Unit) {
    val locale = Formats.locale
    AliviaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Formats.monthYear(state.month.atDay(1)), style = MaterialTheme.typography.titleLarge, color = Navy,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { onMove(-1) }) { Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mes anterior", tint = Navy) }
            IconButton(onClick = { onMove(1) }) { Icon(Icons.Outlined.ChevronRight, contentDescription = "Mes siguiente", tint = Navy) }
        }
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(day.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.labelSmall, color = InkMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val first = state.month.atDay(1)
        val offset = first.dayOfWeek.value - 1
        val cells = (0 until ((offset + state.month.lengthOfMonth() + 6) / 7) * 7).map { index ->
            val day = index - offset + 1
            if (day in 1..state.month.lengthOfMonth()) state.month.atDay(day) else null
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val selected = date == state.selectedDay
                            val today = date == state.today
                            Column(
                                Modifier.fillMaxSize().clip(CircleShape)
                                    .background(if (selected) Navy else if (today) NavySoft else Color.Transparent)
                                    .clickable { onSelect(date) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("${date.dayOfMonth}", style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) Surface else Ink)
                                Box(Modifier.size(5.dp).clip(CircleShape).background(
                                    if (date in state.daysWithActivities) (if (selected) Surface else TealDark) else Color.Transparent,
                                ))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(group: ActivityGroup, state: ActivitiesUiState) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
        Text(group.kind.title, style = MaterialTheme.typography.titleLarge, color = Navy)
        val date = when (group.kind) {
            ActivityGroupKind.TODAY -> state.today
            ActivityGroupKind.TOMORROW -> state.today.plusDays(1)
            ActivityGroupKind.UPCOMING -> state.today.plusDays(2)
            ActivityGroupKind.PAST -> state.today.minusDays(1)
        }
        Text("  ${Formats.longDay(date)}", style = MaterialTheme.typography.bodySmall, color = InkMuted,
            modifier = Modifier.padding(bottom = 3.dp))
        Spacer(Modifier.weight(1f))
        val count = group.activities.size
        Pill(if (count == 1) "1 tarea" else "$count tareas", NavySoft, Navy)
    }
}

@Composable
private fun ActivityCard(activity: CareActivity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.width(56.dp)) {
                Text(Formats.time(activity.startTime), style = MaterialTheme.typography.labelMedium, color = Navy)
                if (activity.recurring) {
                    Icon(Icons.Outlined.Repeat, contentDescription = "Recurrente", tint = InkMuted,
                        modifier = Modifier.padding(top = 4.dp).size(14.dp))
                }
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
                Icon(activity.type.icon, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(activity.title, style = MaterialTheme.typography.titleMedium, color = Navy, modifier = Modifier.weight(1f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Pill(activity.status.label, activity.status.container, activity.status.content)
                }
                activity.assistedPerson?.let { Detail(Icons.Outlined.Person, it) }
                activity.responsible?.let { Detail(Icons.Outlined.VerifiedUser, "Resp: $it") }
            }
        }
    }
}

@Composable
private fun Detail(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = InkMuted, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityDetailSheet(
    activity: CareActivity,
    completing: Boolean,
    error: String?,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(activity.type.label, NavySoft, Navy)
                Pill(activity.status.label, activity.status.container, activity.status.content)
            }
            Text(activity.title, style = MaterialTheme.typography.headlineSmall, color = Navy)
            AliviaCard {
                DetailRow(Icons.Outlined.Schedule, "Fecha y horario",
                    "${Formats.day(activity.date)} · " + (activity.endTime?.let { Formats.window(activity.startTime, it) }
                        ?: Formats.time(activity.startTime)))
                HorizontalDivider(color = Outline)
                DetailRow(activity.type.icon, "Tipo de actividad", activity.type.label)
                HorizontalDivider(color = Outline)
                DetailRow(Icons.Outlined.Repeat, "Frecuencia", if (activity.recurring) "Se repite cada semana" else "No se repite")
                HorizontalDivider(color = Outline)
                DetailRow(Icons.Outlined.VerifiedUser, "Asignada a", activity.responsible ?: "Sin responsable asignado")
                activity.assistedPerson?.let {
                    HorizontalDivider(color = Outline)
                    DetailRow(Icons.Outlined.Person, "Persona asistida", it)
                }
            }
            error?.let { ErrorBanner(it) }
            val canComplete = activity.canComplete(LocalDateTime.now(Formats.zone))
            PrimaryButton(
                if (canComplete) "Marcar como completada" else "Disponible durante su horario",
                onClick = onComplete,
                icon = Icons.Outlined.CheckCircle,
                enabled = canComplete,
                busy = completing,
                container = Navy,
            )
            Text("Solo se puede marcar mientras la actividad está en curso.", style = MaterialTheme.typography.bodySmall,
                color = InkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(NavySoft), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            Text(value, style = MaterialTheme.typography.titleSmall, color = Ink)
        }
    }
}

@Composable
private fun EmptyActivities() {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
            @Suppress("DEPRECATION")
            Icon(Icons.Outlined.EventNote, contentDescription = null, tint = Navy, modifier = Modifier.size(30.dp))
        }
        Text("No hay actividades en estas fechas", style = MaterialTheme.typography.titleMedium, color = Navy, textAlign = TextAlign.Center)
    }
}
