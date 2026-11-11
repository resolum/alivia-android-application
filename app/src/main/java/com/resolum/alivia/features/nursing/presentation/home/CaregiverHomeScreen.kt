package com.resolum.alivia.features.nursing.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.nursing.di.ActivityModule
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.presentation.container
import com.resolum.alivia.features.nursing.presentation.content
import com.resolum.alivia.features.nursing.presentation.label
import com.resolum.alivia.features.operations.presentation.orders.NotificationsButton
import com.resolum.alivia.features.profiles.di.ProfileModule
import com.resolum.alivia.features.profiles.domain.models.Household

/** Start screen of the caregiver: a summary of the day and the state of the home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverHomeScreen(
    onOpenActivities: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: CaregiverHomeViewModel = viewModel {
        CaregiverHomeViewModel(ActivityModule.getActivities(), ProfileModule.getProfile(), ProfileModule.getMyHousehold(),
            ActivityModule.getAssistedPersons(), ActivityModule.getCareNetwork())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<HomeSheet?>(null) }

    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { viewModel.load(refresh = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.firstName?.let { "Hola, $it" } ?: "Hola", style = MaterialTheme.typography.headlineMedium, color = Navy)
                    Text("¡Bienvenido a Alivia Cuidados! Tu resumen y el estado del hogar para hoy.",
                        style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
                NotificationsButton(onClick = onOpenNotifications)
            }
            val error = state.error
            when {
                state.loading -> Loading()
                else -> {
                    if (error != null) {
                        ErrorBanner(error)
                        TextButton(onClick = { viewModel.load() }) { Text("Reintentar", color = NavyButton) }
                    }
                    NextActivityCard(state.nextActivity, state, onOpenActivities)
                    CareNetworkCard(state.network, state.assistedPerson, onOpen = { sheet = HomeSheet.NETWORK })
                    AssistedPersonCard(state.assistedPerson, state.household, state.today, onOpen = { sheet = HomeSheet.PERSON })
                }
            }
        }
    }

    when (sheet) {
        HomeSheet.NETWORK -> CareNetworkSheet(state.network, state.assistedPerson, onDismiss = { sheet = null })
        HomeSheet.PERSON -> state.assistedPerson?.let {
            AssistedPersonSheet(it, state.household, state.today, onDismiss = { sheet = null })
        }
        null -> Unit
    }
}

@Composable
private fun CardTitle(icon: ImageVector, title: String, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        }
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Ink, modifier = Modifier.padding(start = 10.dp))
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onClick) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = Navy)
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun NextActivityCard(activity: CareActivity?, state: CaregiverHomeUiState, onOpenActivities: () -> Unit) {
    AliviaCard {
        CardTitle(Icons.Outlined.Schedule, "Próxima actividad") {
            if (activity != null) Pill(activity.status.label, activity.status.container, activity.status.content)
        }
        if (activity == null) {
            Text("No tienes actividades pendientes", style = MaterialTheme.typography.titleMedium, color = Navy)
            Text(if (state.todayCount == 0) "No hay actividades programadas para hoy." else "Completaste las actividades de hoy.",
                style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        } else {
            Text(activity.title, style = MaterialTheme.typography.titleLarge, color = Navy)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Navy, modifier = Modifier.size(16.dp))
                val day = Formats.day(activity.date, state.today).substringBefore(",")
                Text("$day · ${Formats.time(activity.startTime)}", style = MaterialTheme.typography.bodyMedium, color = Ink)
            }
            activity.responsible?.let { responsible ->
                Text(
                    buildAnnotatedString {
                        append("A cargo de: ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(responsible) }
                    },
                    style = MaterialTheme.typography.bodyMedium, color = Ink,
                )
            }
        }
        LinkRow("Ver actividades", onOpenActivities)
    }
}

