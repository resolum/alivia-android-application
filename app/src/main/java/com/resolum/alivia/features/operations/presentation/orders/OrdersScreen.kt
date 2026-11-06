package com.resolum.alivia.features.operations.presentation.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.Dot
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.Pill
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.operations.di.OperationsModule
import com.resolum.alivia.features.operations.domain.models.ServiceType
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.presentation.OrderStatusPill
import com.resolum.alivia.features.operations.presentation.color
import com.resolum.alivia.features.operations.presentation.icon
import com.resolum.alivia.features.operations.presentation.label
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOpenOrder: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: OrdersViewModel = viewModel { OrdersViewModel(OperationsModule.getMyOrders()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Reload each time the screen is shown: entering the tab, coming back from an order or back to the app.
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Órdenes", subtitle = state.summary, onOpenProfile = onOpenProfile)
        ViewSwitch(state.view, viewModel::selectView, Modifier.padding(horizontal = 16.dp))
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
                if (state.view == OrdersView.CALENDAR) {
                    item { WeekCalendar(state, viewModel::selectDay) }
                }
                item { Filters(state, viewModel::selectFilter) }
                state.error?.let { message ->
                    item {
                        ErrorBanner(message)
                        TextButton(onClick = { viewModel.load() }) { Text("Reintentar") }
                    }
                }
                if (state.loading) {
                    item { Loading() }
                } else if (state.view == OrdersView.LIST) {
                    val groups = groupsOf(state.filtered, state.today)
                    if (groups.isEmpty() && state.error == null) item { EmptyOrders() }
                    groups.forEach { group ->
                        item(key = "h-${group.title}") { GroupHeader(group.title, group.orders.size) }
                        items(group.orders, key = { it.id }) { order ->
                            OrderCard(order, isNext = order.id == state.nextOrderId, onClick = { onOpenOrder(order.id) })
                        }
                    }
                } else {
                    val dayOrders = state.filtered.filter { it.date == state.selectedDay }.sortedBy { it.startTime }
                    item { GroupHeader(Formats.day(state.selectedDay, state.today), dayOrders.size) }
                    if (dayOrders.isEmpty()) item { EmptyOrders("No tienes órdenes este día") }
                    items(dayOrders, key = { it.id }) { order ->
                        OrderCard(order, isNext = order.id == state.nextOrderId, onClick = { onOpenOrder(order.id) })
                    }
                }
            }
        }
    }
}

private data class OrderGroup(val title: String, val orders: List<TechnicalOrder>)

/** Today, tomorrow and each later day in order; past days at the end, the latest first. */
private fun groupsOf(orders: List<TechnicalOrder>, today: LocalDate): List<OrderGroup> {
    val byDate = orders.groupBy { it.date }
    val upcoming = byDate.keys.filter { !it.isBefore(today) }.sorted()
    val past = byDate.keys.filter { it.isBefore(today) }.sortedDescending()
    return (upcoming + past).map { date ->
        val title = when {
            date == today -> "Hoy · ${Formats.day(date, today).substringAfter(", ")}"
            date == today.plusDays(1) -> "Mañana · ${Formats.day(date, today).substringAfter(", ")}"
            date.isAfter(today) -> "Próximas · ${Formats.day(date, today)}"
            else -> "Anteriores · ${Formats.day(date, today)}"
        }
        OrderGroup(title, byDate.getValue(date).sortedBy { it.startTime })
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String?, onOpenProfile: () -> Unit, overline: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (overline != null) Text(overline.uppercase(), style = MaterialTheme.typography.labelSmall, color = InkMuted)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Navy)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        IconButton(
            onClick = onOpenProfile,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Navy),
        ) {
            Icon(Icons.Outlined.Person, contentDescription = "Perfil", tint = Color.White)
        }
    }
}

@Composable
private fun ViewSwitch(view: OrdersView, onSelect: (OrdersView) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NavySoft).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            Triple(OrdersView.LIST, "Lista", Icons.AutoMirrored.Outlined.List),
            Triple(OrdersView.CALENDAR, "Calendario", Icons.Outlined.CalendarMonth),
        ).forEach { (option, label, icon) ->
            val selected = option == view
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Surface else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, color = Navy, style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Filters(state: OrdersUiState, onSelect: (OrderFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(OrderFilter.entries) { filter ->
            val selected = filter == state.filter
            val count = state.orders.count { filter.type == null || it.serviceType == filter.type }
            FilterChip(
                selected = selected,
                onClick = { onSelect(filter) },
                label = { Text(if (filter == OrderFilter.ALL) "${filter.label} ($count)" else filter.label) },
                leadingIcon = filter.type?.let { type -> { Dot(type.color, Modifier.size(8.dp)) } },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Navy,
                    selectedLabelColor = Color.White,
                    containerColor = Surface,
                    labelColor = Ink,
                ),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Outline),
            )
        }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Ink, modifier = Modifier.weight(1f))
        Text(if (count == 1) "1 orden" else "$count órdenes", style = MaterialTheme.typography.bodySmall, color = InkMuted)
    }
}

@Composable
fun OrderCard(order: TechnicalOrder, isNext: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(order.serviceType.color))
            Column(Modifier.weight(1f).padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNext) Pill("SIGUIENTE", TealSoft, TealDark)
                    Spacer(Modifier.weight(1f))
                    OrderStatusPill(order)
                }
                Text(order.homeName, style = MaterialTheme.typography.titleMedium, color = Navy,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(order.serviceType.icon, contentDescription = null, tint = order.serviceType.color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${order.serviceType.label} · ${Formats.window(order.startTime, order.endTime)}",
                        style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
                order.directions?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, contentDescription = null, tint = InkMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = InkMuted,
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 10.dp))
        }
    }
}

@Composable
private fun WeekCalendar(state: OrdersUiState, onSelectDay: (LocalDate) -> Unit) {
    val monday = state.selectedDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Formats.monthYear(state.selectedDay), style = MaterialTheme.typography.titleLarge, color = Navy)
                Spacer(Modifier.width(8.dp))
                Pill("Semana ${monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}", TealSoft, TealDark)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onSelectDay(state.selectedDay.minusWeeks(1)) }) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Semana anterior", tint = Navy)
                }
                IconButton(onClick = { onSelectDay(state.selectedDay.plusWeeks(1)) }) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Semana siguiente", tint = Navy)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                (0L..6L).map { monday.plusDays(it) }.forEach { day ->
                    val selected = day == state.selectedDay
                    val types = state.orders.filter { it.date == day }.map { it.serviceType }.distinct()
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) NavySoft else Color.Transparent)
                            .clickable { onSelectDay(day) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            if (day == state.today) "HOY" else day.dayOfWeek.getDisplayName(TextStyle.SHORT, Formats.locale)
                                .uppercase().trimEnd('.'),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day == state.today) Navy else InkMuted,
                        )
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(if (selected) Navy else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${day.dayOfMonth}", color = if (selected) Color.White else Ink, style = MaterialTheme.typography.titleSmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
                            types.forEach { Dot(it.color) }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceMuted).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf(ServiceType.MAINTENANCE, ServiceType.INSTALLATION, ServiceType.EVALUATION).forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(type.color, Modifier.size(8.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(type.label, style = MaterialTheme.typography.bodySmall, color = Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOrders(text: String = "No tienes órdenes asignadas") {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = Navy, modifier = Modifier.size(32.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = Navy)
        Text("Cualquier nueva asignación aparecerá aquí.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
    }
}
