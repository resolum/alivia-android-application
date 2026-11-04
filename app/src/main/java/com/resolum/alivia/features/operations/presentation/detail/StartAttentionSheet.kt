package com.resolum.alivia.features.operations.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.resolum.alivia.core.ui.components.Dot
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.presentation.color
import com.resolum.alivia.features.operations.presentation.longLabel

/** Confirms the start of the visit before the order moves to In progress, as the design asks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartAttentionSheet(
    order: TechnicalOrder,
    starting: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!starting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(NavySoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Build, contentDescription = null, tint = Navy, modifier = Modifier.size(30.dp))
            }
            Text("¿Iniciar atención?", style = MaterialTheme.typography.headlineSmall, color = Navy)
            Text("Confirma el inicio de la visita técnica presencial", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceMuted),
                border = BorderStroke(1.dp, Outline),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SummaryRow("Código de orden") { Text(order.code, style = MaterialTheme.typography.titleSmall, color = Ink) }
                    HorizontalDivider(color = Outline)
                    SummaryRow("Tipo de servicio") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Dot(order.serviceType.color, Modifier.size(8.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(order.serviceType.longLabel, style = MaterialTheme.typography.titleSmall, color = order.serviceType.color)
                        }
                    }
                    HorizontalDivider(color = Outline)
                    SummaryRow("Hogar beneficiario") { Text(order.homeName, style = MaterialTheme.typography.titleSmall, color = Ink) }
                    HorizontalDivider(color = Outline)
                    SummaryRow("Horario programado") {
                        val today = order.date == Formats.today()
                        Text(Formats.window(order.startTime, order.endTime) + if (today) " (Hoy)" else "",
                            style = MaterialTheme.typography.titleSmall, color = Ink)
                    }
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = InkMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    buildAnnotatedString {
                        append("La orden cambiará al estado ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Ink)) { append("En proceso") }
                        append(" y se registrará la hora exacta de inicio para trazabilidad asistencial.")
                    },
                    style = MaterialTheme.typography.bodySmall, color = InkMuted,
                )
            }
            if (error != null) ErrorBanner(error)
            PrimaryButton("Iniciar atención", onClick = onConfirm, icon = Icons.Filled.PlayArrow, busy = starting, container = Navy)
            Button(
                onClick = onDismiss,
                enabled = !starting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceMuted, contentColor = Navy),
            ) { Text("Cancelar", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkMuted, modifier = Modifier.weight(1f))
        value()
    }
}
