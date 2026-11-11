package com.resolum.alivia.features.operations.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.operations.di.OperationsModule
import com.resolum.alivia.features.operations.domain.models.WorkingDay
import com.resolum.alivia.features.operations.presentation.orders.ScreenHeader

/** The technician's assigned working days of this week and the next. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onOpenProfile: () -> Unit,
    viewModel: ScheduleViewModel = viewModel { ScheduleViewModel(OperationsModule.getMySchedule()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Horario", subtitle = "Próximas 2 semanas", onOpenProfile = onOpenProfile)
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.load(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { Summary(state) }
                val error = state.error
                when {
                    error != null -> item {
                        ErrorBanner(error)
                        TextButton(onClick = { viewModel.load() }) { Text("Reintentar") }
                    }
                    state.loading -> item { Loading() }
                    state.days.isEmpty() -> item {
                        AliviaCard {
                            Text("No tienes turnos asignados para estas dos semanas.",
                                style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                        }
                    }
                    else -> {
                        item { SectionLabel("Turnos por día", Modifier.padding(top = 4.dp)) }
                        items(state.days, key = { it.date.toString() }) { day -> DayCard(day, state) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(state: ScheduleUiState) {
    AliviaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.EditCalendar, contentDescription = null, tint = Navy)
            }
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Jornada asignada", style = MaterialTheme.typography.titleMedium, color = Navy)
                val range = if (state.from != null && state.to != null) {
                    "entre ${Formats.shortDate(state.from)} y ${Formats.shortDate(state.to)}"
                } else "en las próximas 2 semanas"
                val count = state.days.size
                Text("$count ${if (count == 1) "día laborable" else "días laborables"} $range",
                    style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        }
    }
}

@Composable
private fun DayCard(day: WorkingDay, state: ScheduleUiState) {
    AliviaCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                Modifier.size(width = 48.dp, height = 54.dp).clip(RoundedCornerShape(13.dp)).background(TealSoft),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(Formats.shortWeekday(day.date), style = MaterialTheme.typography.labelSmall, color = TealDark)
                Text("${day.date.dayOfMonth}", style = MaterialTheme.typography.titleLarge, color = Navy)
            }
            Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Formats.day(day.date, state.today), style = MaterialTheme.typography.titleMedium, color = Ink)
                if (day.shifts.isEmpty()) {
                    Text("Sin bloques asignados", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                }
                day.shifts.forEach { shift ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = InkMuted, modifier = Modifier.size(18.dp))
                        Text(Formats.window(shift.start, shift.end), style = MaterialTheme.typography.bodyMedium, color = Ink)
                    }
                }
            }
        }
    }
}
