package com.resolum.alivia.features.nursing.presentation.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.VerifiedUser
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
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavyButton
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.features.nursing.di.ActivityModule
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.presentation.container
import com.resolum.alivia.features.nursing.presentation.content
import com.resolum.alivia.features.nursing.presentation.icon
import com.resolum.alivia.features.nursing.presentation.label
import com.resolum.alivia.features.operations.presentation.orders.ScreenHeader

/** The caregiver's care agenda, from yesterday to the day after tomorrow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    onOpenNotifications: () -> Unit,
    viewModel: ActivitiesViewModel = viewModel { ActivitiesViewModel(ActivityModule.getActivities()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Actividades", subtitle = state.summary, onOpenProfile = onOpenNotifications)
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.load(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                state.error?.let { message ->
                    item {
                        ErrorBanner(message)
                        TextButton(onClick = { viewModel.load() }) { Text("Reintentar", color = NavyButton) }
                    }
                }
                if (state.loading) {
                    item { Loading() }
                } else {
                    val groups = state.groups
                    if (groups.isEmpty() && state.error == null) item { EmptyActivities() }
                    groups.forEach { group ->
                        item(key = "h-${group.kind}") { GroupHeader(group, state) }
                        items(group.activities, key = { "${group.kind}-${it.id}" }) { ActivityCard(it) }
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
        if (date != null) {
            Text("  ${Formats.longDay(date)}", style = MaterialTheme.typography.bodySmall, color = InkMuted,
                modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.weight(1f))
        val count = group.activities.size
        Pill(if (count == 1) "1 tarea" else "$count tareas", NavySoft, Navy)
    }
}

@Composable
private fun ActivityCard(activity: CareActivity) {
    AliviaCard {
        Row(verticalAlignment = Alignment.Top) {
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
