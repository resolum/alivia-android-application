package com.resolum.alivia.features.operations.presentation.evaluation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.DangerSoft
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.InstallationColor
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.operations.di.OperationsModule
import com.resolum.alivia.features.operations.domain.models.EvaluationResult
import com.resolum.alivia.features.operations.presentation.detail.DetailTopBar

@Composable
fun EvaluationScreen(
    orderId: Long,
    onBack: () -> Unit,
    onNext: (EvaluationNext) -> Unit,
    viewModel: EvaluationViewModel = viewModel(key = "evaluation-$orderId") {
        EvaluationViewModel(orderId, OperationsModule.getOrder(), OperationsModule.recordEvaluation())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order

    Column(Modifier.fillMaxSize().background(Background).imePadding()) {
        DetailTopBar(title = "Resultado de la evaluación", subtitle = order?.let { "${it.code} · ${it.homeName}" }, onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                order == null && state.loadError != null -> ErrorBanner(state.loadError!!)
                order == null -> Loading()
                else -> {
                    Text("¿El hogar puede recibir la configuración contratada?", style = MaterialTheme.typography.titleMedium, color = Navy)
                    ResultOption(
                        EvaluationResult.VIABLE, state.result, Icons.Outlined.CheckCircle, InstallationColor, TealSoft,
                        "Viable", "El hogar cumple. La familia confirma la configuración en la web y luego se abre la instalación.", viewModel::select,
                    )
                    ResultOption(
                        EvaluationResult.NOT_VIABLE, state.result, Icons.Outlined.Block, Danger, DangerSoft,
                        "No viable", "Se cierra la solicitud y se devuelve a la familia el monto que pagó.", viewModel::select,
                    )
                    SectionLabel("Notas", Modifier.padding(top = 8.dp))
                    AliviaCard {
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = viewModel::updateNotes,
                            placeholder = { Text("Observaciones de la visita") },
                            supportingText = {
                                Text("${state.notes.length}/300")
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Outline, focusedBorderColor = Navy),
                        )
                    }
                    state.error?.let { ErrorBanner(it) }
                }
            }
        }
        if (order != null) {
            Box(Modifier.background(Surface).navigationBarsPadding().padding(16.dp)) {
                val notViable = state.result == EvaluationResult.NOT_VIABLE
                PrimaryButton(
                    text = when (state.result) {
                        null -> "Elige un resultado"
                        EvaluationResult.NOT_VIABLE -> "Rechazar y devolver el pago"
                        else -> "Confirmar y continuar con la instalación"
                    },
                    onClick = { viewModel.submit(onNext) },
                    enabled = state.result != null,
                    busy = state.saving,
                    container = if (notViable) Danger else Navy,
                )
            }
        }
    }

    if (state.confirmingRejection) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRejection,
            title = { Text("¿Marcar el hogar como no viable?") },
            text = {
                Text("La solicitud se cerrará y se liberará el pago retenido de la familia. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.submit(onNext) }) { Text("Sí, rechazar", color = Danger) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissRejection) { Text("Cancelar") } },
            containerColor = Surface,
        )
    }
}

@Composable
private fun ResultOption(
    value: EvaluationResult,
    selected: EvaluationResult?,
    icon: ImageVector,
    tint: Color,
    soft: Color,
    title: String,
    description: String,
    onSelect: (EvaluationResult) -> Unit,
) {
    val isSelected = value == selected
    Card(
        onClick = { onSelect(value) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) soft else Surface),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) tint else Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Ink)
                Text(description, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
            RadioButton(selected = isSelected, onClick = { onSelect(value) },
                colors = RadioButtonDefaults.colors(selectedColor = tint))
        }
    }
}
