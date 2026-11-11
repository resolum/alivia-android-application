package com.resolum.alivia.features.operations.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavyButton
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.operations.di.OperationsModule
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.presentation.ProgressPill
import com.resolum.alivia.features.operations.presentation.longLabel
import com.resolum.alivia.features.operations.presentation.orders.OrderCard
import com.resolum.alivia.features.operations.presentation.orders.ScreenHeader
import com.resolum.alivia.features.profiles.di.ProfileModule
import java.time.LocalTime

/** Start screen of the technician: who is working, today's orders and the last ones closed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianHomeScreen(
    onOpenOrder: (Long) -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: TechnicianHomeViewModel = viewModel {
        TechnicianHomeViewModel(OperationsModule.getMyOrders(), OperationsModule.getMySchedule(), ProfileModule.getProfile())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Inicio", subtitle = null, onOpenProfile = onOpenNotifications, overline = "Técnico en campo · Alivia")
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.load(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { Greeting(state) }
                item { WorkdayCard(state) }
                val error = state.error
                when {
                    state.loading -> item { Loading() }
                    error != null -> item {
                        ErrorBanner(error)
                        TextButton(onClick = { viewModel.load() }) { Text("Reintentar", color = NavyButton) }
                    }
                    else -> {
                        val today = state.todayOrders
                        if (today.isEmpty()) {
                            item { NoOrdersToday(onOpenSchedule) }
                        } else {
                            item { SectionLabel("Órdenes de hoy", Modifier.padding(top = 4.dp)) }
                            items(today, key = { "t-${it.id}" }) { order ->
                                OrderCard(order, isNext = false, onClick = { onOpenOrder(order.id) })
                            }
                        }
                        item { LatestHeader(onOpenOrders) }
                        val latest = state.latestCompleted
                        if (latest.isEmpty()) {
                            item {
                                AliviaCard {
                                    Text("Aún no tienes órdenes finalizadas.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                                }
                            }
                        }
                        items(latest, key = { "c-${it.id}" }) { order -> CompletedOrderCard(order) { onOpenOrder(order.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(state: TechnicianHomeUiState) {
    Column(Modifier.padding(top = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(Formats.longDay(state.today), style = MaterialTheme.typography.bodySmall, color = InkMuted)
        Text(state.firstName?.let { "Hola, $it" } ?: "Hola", style = MaterialTheme.typography.headlineMedium, color = Navy)
    }
}

@Composable
private fun WorkdayCard(state: TechnicianHomeUiState) {
    val now = LocalTime.now(Formats.zone)
    val shifts = state.todayShifts
    val badge = when {
        shifts.any { !now.isBefore(it.start) && now.isBefore(it.end) } -> WorkdayBadge("Jornada activa", TealSoft, TealDark)
        shifts.isNotEmpty() -> WorkdayBadge("Jornada de hoy", NavySoft, Navy)
        else -> WorkdayBadge("Sin jornada hoy", SurfaceMuted, InkMuted)
    }
    val count = state.todayOrders.size
    AliviaCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(badge.label, badge.container, badge.content, dot = badge.content)
            Text(
                shifts.joinToString(" · ") { unbroken(Formats.window(it.start, it.end)) }.ifEmpty { "—" },
                style = MaterialTheme.typography.bodySmall, color = Ink, modifier = Modifier.weight(1f),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Pill(if (count == 1) "1 orden hoy" else "$count órdenes hoy", NavySoft, Navy)
        }
    }
}

/** Keeps "8:00 a. m." together so a window only breaks around the dash. */
private fun unbroken(window: String) = window.split(" – ").joinToString(" – ") { it.replace(' ', '\u00A0') }

private data class WorkdayBadge(val label: String, val container: Color, val content: Color)

@Composable
private fun NoOrdersToday(onOpenSchedule: () -> Unit) {
    AliviaCard {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = Navy)
            }
            Text("No tienes órdenes asignadas para hoy", style = MaterialTheme.typography.titleLarge, color = Navy,
                textAlign = TextAlign.Center)
            Text("Cualquier nueva asignación o cambio en tu jornada será notificado por la central de Alivia.",
                style = MaterialTheme.typography.bodyMedium, color = InkMuted, textAlign = TextAlign.Center)
            Button(
                onClick = onOpenSchedule,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = NavySoft, contentColor = Navy),
                modifier = Modifier.padding(top = 6.dp).height(48.dp),
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Ver mi horario", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun LatestHeader(onOpenOrders: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Últimas órdenes", style = MaterialTheme.typography.titleLarge, color = Navy)
            Text("Servicios finalizados recientemente", style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        TextButton(onClick = onOpenOrders) {
            Text("Ver historial", style = MaterialTheme.typography.labelMedium, color = Navy)
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CompletedOrderCard(order: TechnicalOrder, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val closed = order.completedAt?.let { Formats.moment(it) } ?: Formats.day(order.date)
                Text("#${order.code} · $closed", style = MaterialTheme.typography.bodySmall, color = InkMuted,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                ProgressPill(order.progress)
            }
            Text(order.serviceType.longLabel, style = MaterialTheme.typography.titleMedium, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = InkMuted, modifier = Modifier.size(16.dp))
                Text(order.homeName, style = MaterialTheme.typography.bodySmall, color = Ink, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = InkMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
