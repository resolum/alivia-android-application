package com.resolum.alivia.features.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.DangerSoft
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavyButton
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.notifications.di.NotificationsModule
import com.resolum.alivia.features.notifications.domain.AppNotification

/** Notification center opened from the bell of the headers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel {
        NotificationsViewModel(NotificationsModule.getNotifications(), NotificationsModule.markRead())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.load() }

    Column(Modifier.fillMaxSize().background(Background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = Navy) }
            Text("Notificaciones", style = MaterialTheme.typography.titleLarge, color = Navy, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::markAllRead, enabled = state.notifications.any { !it.read }) {
                Text("Marcar todas", style = MaterialTheme.typography.labelMedium, color = NavyButton)
            }
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { viewModel.select(filter) },
                    label = { Text("${filter.label} (${state.count(filter)})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Navy, selectedLabelColor = Surface, containerColor = NavySoft, labelColor = Navy,
                    ),
                    border = null,
                )
            }
        }
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.load(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                state.error?.let { message ->
                    item {
                        ErrorBanner(message)
                        TextButton(onClick = { viewModel.load() }) { Text("Reintentar", color = NavyButton) }
                    }
                }
                val visible = state.visible
                when {
                    state.loading -> item { Loading() }
                    visible.isEmpty() && state.error == null -> item { EmptyNotifications(state.filter) }
                    else -> {
                        val (today, earlier) = visible.partition { it.createdAt?.toLocalDate() == state.today }
                        if (today.isNotEmpty()) {
                            item { SectionLabel("Hoy") }
                            item { NotificationGroup(today, viewModel::open) }
                        }
                        if (earlier.isNotEmpty()) {
                            item { SectionLabel("Anteriores", Modifier.padding(top = 6.dp)) }
                            item { NotificationGroup(earlier, viewModel::open) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationGroup(items: List<AppNotification>, onOpen: (AppNotification) -> Unit) {
    AliviaCard {
        items.forEach { NotificationRow(it, onOpen) }
    }
}

@Composable
private fun NotificationRow(item: AppNotification, onOpen: (AppNotification) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onOpen(item) }, verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(if (item.isAlert) DangerSoft else TealSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (item.isAlert) Icons.Outlined.Warning else Icons.Outlined.Info, contentDescription = null,
                tint = if (item.isAlert) Danger else TealDark, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = Navy)
            item.body?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Ink) }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item.createdAt?.let { Text(Formats.time(it.toLocalTime()), style = MaterialTheme.typography.labelSmall, color = InkMuted) }
            if (!item.read) Box(Modifier.size(8.dp).clip(CircleShape).background(if (item.isAlert) Danger else TealDark))
        }
    }
}

@Composable
private fun EmptyNotifications(filter: NotificationFilter) {
    Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = Navy, modifier = Modifier.size(32.dp))
        }
        Text(
            when (filter) {
                NotificationFilter.ALL -> "No tienes notificaciones"
                NotificationFilter.UNREAD -> "Estás al día"
                NotificationFilter.TODAY -> "No hay notificaciones de hoy"
            },
            style = MaterialTheme.typography.titleMedium, color = Navy, textAlign = TextAlign.Center,
        )
        Text("Aquí verás los avisos del hogar, las asignaciones y los cambios de tu cuenta.",
            style = MaterialTheme.typography.bodyMedium, color = InkMuted, textAlign = TextAlign.Center)
    }
}
