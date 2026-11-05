package com.resolum.alivia.features.operations.presentation.detail

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.Teal
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.features.operations.di.OperationsModule
import com.resolum.alivia.features.operations.domain.models.IncidentAction
import com.resolum.alivia.features.operations.domain.models.IncidentSummary
import com.resolum.alivia.features.operations.domain.models.OrderProgress
import com.resolum.alivia.features.operations.domain.models.ServiceType
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.presentation.OrderStatusPill
import com.resolum.alivia.features.operations.presentation.TypePill
import com.resolum.alivia.features.operations.presentation.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit,
    onOpenEvaluation: (Long) -> Unit,
    onOpenInstallation: (Long) -> Unit,
    onOpenCompleted: (Long) -> Unit,
    onOpenResolve: (Long) -> Unit = {},
    viewModel: OrderDetailViewModel = viewModel(key = "order-$orderId") {
        OrderDetailViewModel(orderId, OperationsModule.getOrder(), OperationsModule.startOrder(), OperationsModule.advanceIncident())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order
    val continueWith: (TechnicalOrder) -> Unit = { started ->
        when (started.serviceType) {
            ServiceType.EVALUATION -> onOpenEvaluation(started.id)
            ServiceType.INSTALLATION -> onOpenInstallation(started.id)
            ServiceType.MAINTENANCE -> onOpenResolve(started.id)
        }
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        DetailTopBar(title = "Detalle de orden", subtitle = order?.code, onBack = onBack)
        Box(Modifier.weight(1f)) {
            when {
                state.loading -> Loading()
                order == null -> Column(Modifier.padding(16.dp)) {
                    ErrorBanner(state.error ?: "No encontramos la orden.")
                    TextButton(onClick = viewModel::load) { Text("Reintentar") }
                }
                else -> OrderBody(order)
            }
        }
        if (order != null) {
            Column(Modifier.background(Surface).navigationBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                state.advanceError?.let { ErrorBanner(it, Modifier.padding(bottom = 8.dp)) }
                when {
                    order.incidentAction == IncidentAction.EVALUATE -> {
                        PrimaryButton("Marcar como evaluada", onClick = viewModel::advance, icon = Icons.AutoMirrored.Outlined.FactCheck,
                            busy = state.advancing, container = Navy)
                        Text("Revisaste el reporte de la familia: la incidencia pasa a En evaluación",
                            style = MaterialTheme.typography.bodySmall, color = InkMuted, modifier = Modifier.padding(top = 8.dp))
                    }
                    order.incidentAction == IncidentAction.CONFIRM_VISIT -> {
                        PrimaryButton("Confirmar visita", onClick = viewModel::advance, icon = Icons.Outlined.EventAvailable,
                            busy = state.advancing, container = Navy)
                        Text("La familia verá la visita programada para ${Formats.day(order.date)} · ${Formats.window(order.startTime, order.endTime)}",
                            style = MaterialTheme.typography.bodySmall, color = InkMuted, modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center)
                    }
                    order.progress == OrderProgress.SCHEDULED -> {
                        PrimaryButton("Iniciar atención", onClick = viewModel::askToStart, icon = Icons.Outlined.PlayCircleOutline,
                            container = Navy)
                        Text("Al pulsar, confirmarás el inicio antes de cambiar a En proceso",
                            style = MaterialTheme.typography.bodySmall, color = InkMuted, modifier = Modifier.padding(top = 8.dp))
                    }
                    order.progress == OrderProgress.IN_PROGRESS && order.serviceType == ServiceType.EVALUATION ->
                        PrimaryButton("Registrar resultado de la evaluación", onClick = { onOpenEvaluation(order.id) },
                            icon = Icons.Outlined.TaskAlt, container = Navy)
                    order.progress == OrderProgress.IN_PROGRESS && order.serviceType == ServiceType.INSTALLATION ->
                        PrimaryButton("Continuar instalación", onClick = { onOpenInstallation(order.id) },
                            icon = Icons.Outlined.Build, container = Navy)
                    order.awaitsResolution ->
                        PrimaryButton("Registrar resolución", onClick = { onOpenResolve(order.id) },
                            icon = Icons.Outlined.TaskAlt, container = Navy)
                    order.progress == OrderProgress.COMPLETED ->
                        PrimaryButton("Ver resumen", onClick = { onOpenCompleted(order.id) }, icon = Icons.Outlined.TaskAlt, container = Navy)
                    else -> Text("La orden fue cancelada.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                }
            }
        }
    }

    if (state.confirmingStart && order != null) {
        StartAttentionSheet(
            order = order,
            starting = state.starting,
            error = state.startError,
            onConfirm = { viewModel.start(continueWith) },
            onDismiss = viewModel::cancelStart,
        )
    }
}

@Composable
fun DetailTopBar(title: String, subtitle: String?, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().background(Surface).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = Ink) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Navy)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        trailing()
    }
}

@Composable
private fun OrderBody(order: TechnicalOrder) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypePill(order.serviceType)
            OrderStatusPill(order)
        }
        Text(order.homeName, style = MaterialTheme.typography.headlineSmall, color = Navy)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = InkMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("${Formats.day(order.date)} · ${Formats.window(order.startTime, order.endTime)}",
                style = MaterialTheme.typography.bodyMedium, color = Ink)
        }

        SectionLabel("Visita", Modifier.padding(top = 8.dp))
        AliviaCard {
            InfoRow(Icons.Outlined.Person, "Responsable", order.contactName ?: "La familia no indicó quién recibe la visita") {
                if (order.contactPhone != null) {
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${order.contactPhone}".toUri())) },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = TealDark),
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Llamar")
                    }
                }
            }
            HorizontalDivider(color = Outline)
            InfoRow(Icons.Outlined.Place, "Referencia de acceso", order.directions ?: "Sin indicaciones registradas") {
                if (order.directions != null) {
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, ("geo:0,0?q=" + Uri.encode(order.directions)).toUri()))
                    }) {
                        Text("Abrir en Maps", color = Navy)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = Navy, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        SectionLabel("Solicitud", Modifier.padding(top = 8.dp))
        AliviaCard {
            when (order.serviceType) {
                ServiceType.EVALUATION -> {
                    Text("Evaluación técnica de compatibilidad", style = MaterialTheme.typography.titleMedium, color = Navy)
                    Text("Revisa en el hogar que la configuración contratada pueda instalarse. Si el hogar es viable, " +
                        "la familia confirma la configuración en la web y se abre la orden de instalación; si no, la solicitud " +
                        "se cierra y se libera el pago de la familia.",
                        style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
                ServiceType.INSTALLATION -> {
                    Text("Instalación de dispositivos", style = MaterialTheme.typography.titleMedium, color = Navy)
                    Text(if (order.installedDevices.isEmpty()) "Aún no hay dispositivos registrados."
                    else "${order.installedDevices.size} dispositivos registrados.",
                        style = MaterialTheme.typography.bodyMedium, color = Ink)
                    order.installedDevices.forEach { device ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Memory, contentDescription = null, tint = TealDark, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${device.alias} · ${device.type.label}", style = MaterialTheme.typography.bodyMedium, color = Ink,
                                modifier = Modifier.weight(1f))
                            Text(device.macAddress, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        }
                    }
                }
                ServiceType.MAINTENANCE -> IncidentDetails(order)
            }
            order.completionNotes?.let {
                HorizontalDivider(color = Outline)
                InfoRow(Icons.Outlined.Info, "Notas del cierre", it)
            }
        }
    }
}

@Composable
private fun IncidentDetails(order: TechnicalOrder) {
    val incident = order.incident
    SectionLabel("Problema reportado", color = MaterialTheme.colorScheme.error)
    if (incident == null) {
        Text("Incidencia #${order.incidentId}", style = MaterialTheme.typography.titleMedium, color = Navy)
        Text("Revisa el dispositivo reportado por la familia.", style = MaterialTheme.typography.bodyMedium, color = Ink)
        return
    }
    IncidentContent(incident)
}

/** The incident as the family reported it: code, problem, device and their description. */
@Composable
fun IncidentCard(incident: IncidentSummary) {
    AliviaCard {
        SectionLabel("Problema reportado", color = MaterialTheme.colorScheme.error)
        IncidentContent(incident)
    }
}

@Composable
private fun IncidentContent(incident: IncidentSummary) {
    Text("${incident.code} · ${incident.problem.label}", style = MaterialTheme.typography.titleMedium, color = Navy)
    incident.device?.let { device ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Memory, contentDescription = null, tint = TealDark, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("${device.alias} · ${device.type.label}", style = MaterialTheme.typography.bodyMedium, color = Ink,
                modifier = Modifier.weight(1f))
            Text(device.macAddress, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
    }
    if (incident.description.isNotBlank()) {
        Text("“${incident.description}”", style = MaterialTheme.typography.bodyMedium, color = Ink)
    }
    incident.occurredAt?.let {
        Text("Ocurrió ${Formats.moment(it)}", style = MaterialTheme.typography.bodySmall, color = InkMuted)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, action: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = Ink)
        }
        action()
    }
}
