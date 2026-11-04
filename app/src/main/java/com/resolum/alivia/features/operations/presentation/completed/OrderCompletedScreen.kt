package com.resolum.alivia.features.operations.presentation.completed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.resolum.alivia.features.operations.domain.models.ServiceType
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.Pill
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.DangerSoft
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.Teal
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.operations.di.OperationsModule
import com.resolum.alivia.features.operations.domain.models.EvaluationResult
import com.resolum.alivia.features.operations.domain.models.InstallationResult
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.presentation.detail.OrderDetailViewModel
import com.resolum.alivia.features.operations.presentation.longLabel

@Composable
fun OrderCompletedScreen(
    orderId: Long,
    onBackToOrders: () -> Unit,
    onOpenSummary: (Long) -> Unit,
    onOpenInstallation: (Long) -> Unit,
    viewModel: OrderDetailViewModel = viewModel(key = "completed-$orderId") {
        OrderDetailViewModel(orderId, OperationsModule.getOrder(), OperationsModule.startOrder())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBackToOrders)
    val order = state.order

    Column(
        Modifier.fillMaxSize().background(Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            state.loading -> Loading()
            order == null -> ErrorBanner(state.error ?: "No encontramos la orden.")
            else -> {
                Spacer(Modifier.height(24.dp))
                Box(Modifier.size(96.dp).clip(CircleShape).background(TealSoft), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Teal), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(34.dp))
                    }
                }
                Text("Orden completada", style = MaterialTheme.typography.headlineSmall, color = Navy)
                Text("La información se registró correctamente", style = MaterialTheme.typography.bodyMedium, color = Ink)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow(Icons.Outlined.Tag, "Código de orden") {
                        Text(order.code, style = MaterialTheme.typography.titleMedium, color = Navy)
                    }
                    SummaryRow(Icons.Outlined.WorkOutline, "Tipo de orden") { Pill(order.serviceType.longLabel, NavySoft, Navy) }
                    SummaryRow(Icons.Outlined.Verified, "Resultado final") { ResultPill(order) }
                    order.completedAt?.let { completedAt ->
                        SummaryRow(Icons.Outlined.AccessTime, "Finalización") {
                            Text(Formats.moment(completedAt), style = MaterialTheme.typography.titleSmall, color = Ink)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NavySoft).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = TealDark)
                    Spacer(Modifier.width(10.dp))
                    Text(messageOf(order), style = MaterialTheme.typography.bodySmall, color = Ink)
                }
                if (order.awaitsFamilyConfirmation) {
                    AwaitingFamilyConfirmation(order, onOpenInstallation)
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Volver a órdenes", onClick = onBackToOrders, container = Navy)
                TextButton(onClick = { onOpenSummary(order.id) }) { Text("Ver resumen", color = Navy) }
            }
        }
    }
}

/** A viable evaluation waits for the family to confirm the configuration on the web before the installation. */
private val TechnicalOrder.awaitsFamilyConfirmation: Boolean
    get() = serviceType == ServiceType.EVALUATION &&
        (evaluationResult == EvaluationResult.VIABLE || evaluationResult == EvaluationResult.VIABLE_WITH_ADAPTATION)

/**
 * Asks the backend every few seconds, while the screen is shown, whether the family already confirmed: then the
 * installation order of the same request exists and the technician can start it.
 */
@Composable
private fun AwaitingFamilyConfirmation(order: TechnicalOrder, onOpenInstallation: (Long) -> Unit) {
    val findInstallation = remember { OperationsModule.findInstallationOfRequest() }
    var installation by remember { mutableStateOf<TechnicalOrder?>(null) }
    LaunchedEffect(order.draftId) {
        while (installation == null) {
            val result = findInstallation(order.draftId)
            if (result is NetworkResult.Success) installation = result.data
            if (installation == null) delay(5_000)
        }
    }
    val found = installation
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (found == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = TealDark, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Esperando confirmación de la familia", style = MaterialTheme.typography.titleSmall, color = Navy)
            }
            Text(
                "La familia revisa y confirma la configuración desde la web. Cuando lo haga, aquí y en Órdenes aparecerá la orden de instalación.",
                style = MaterialTheme.typography.bodySmall, color = Ink,
            )
        } else {
            Text("La familia confirmó la configuración", style = MaterialTheme.typography.titleSmall, color = Navy)
            Text("Ya puedes continuar con la instalación (${found.code}).", style = MaterialTheme.typography.bodySmall, color = Ink)
            PrimaryButton("Iniciar instalación", onClick = { onOpenInstallation(found.id) }, container = TealDark)
        }
    }
}

private fun messageOf(order: TechnicalOrder): String = when {
    order.evaluationResult == EvaluationResult.NOT_VIABLE ->
        "La solicitud se cerró y se liberó el pago retenido de la familia."
    order.installationResult == InstallationResult.APPROVED ->
        if (order.installedDevices.isEmpty())
            "El hogar quedó en servicio. Los dispositivos podrán registrarse posteriormente desde la aplicación web."
        else
            "El hogar quedó en servicio con ${order.installedDevices.size} dispositivos. La orden está disponible en tu historial."
    else -> "La orden se ha sincronizado y estará disponible en tu historial."
}

@Composable
private fun ResultPill(order: TechnicalOrder) {
    when {
        order.evaluationResult == EvaluationResult.NOT_VIABLE -> Pill("No viable", DangerSoft, Danger, dot = Danger)
        order.evaluationResult == EvaluationResult.VIABLE_WITH_ADAPTATION -> Pill("Viable con adaptación", TealSoft, TealDark, dot = TealDark)
        order.evaluationResult == EvaluationResult.VIABLE -> Pill("Viable", TealSoft, TealDark, dot = TealDark)
        order.installationResult == InstallationResult.APPROVED -> Pill("Instalada", TealSoft, TealDark, dot = TealDark)
        order.installationResult == InstallationResult.REJECTED -> Pill("Rechazada", DangerSoft, Danger, dot = Danger)
        else -> Pill("Resuelta", TealSoft, TealDark, dot = TealDark)
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String, value: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = InkMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        value()
    }
}

