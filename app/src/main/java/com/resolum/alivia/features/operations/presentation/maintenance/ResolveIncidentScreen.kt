package com.resolum.alivia.features.operations.presentation.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.features.operations.di.OperationsModule
import com.resolum.alivia.features.operations.domain.usecases.ResolveIncidentUseCase
import com.resolum.alivia.features.operations.presentation.detail.DetailTopBar
import com.resolum.alivia.features.operations.presentation.detail.IncidentCard

@Composable
fun ResolveIncidentScreen(
    orderId: Long,
    onBack: () -> Unit,
    onResolved: (Long) -> Unit,
    viewModel: ResolveIncidentViewModel = viewModel(key = "resolve-$orderId") {
        ResolveIncidentViewModel(orderId, OperationsModule.getOrder(), OperationsModule.resolveIncident())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order

    Column(Modifier.fillMaxSize().background(Background).imePadding()) {
        DetailTopBar(
            title = "Resolver incidencia",
            subtitle = order?.let { listOfNotNull(it.incident?.code ?: it.code, it.homeName).joinToString(" · ") },
            onBack = onBack,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                order == null && state.loadError != null -> ErrorBanner(state.loadError!!)
                order == null -> Loading()
                else -> {
                    order.incident?.let { IncidentCard(it) }
                    SectionLabel("Diagnóstico", Modifier.padding(top = 8.dp))
                    AliviaCard {
                        ResolutionField(
                            value = state.diagnosis,
                            onChange = viewModel::updateDiagnosis,
                            placeholder = "¿Qué encontraste en el dispositivo?",
                        )
                    }
                    SectionLabel("Trabajo realizado", Modifier.padding(top = 8.dp))
                    AliviaCard {
                        ResolutionField(
                            value = state.workPerformed,
                            onChange = viewModel::updateWorkPerformed,
                            placeholder = "Cambio de pila, recalibración, prueba de funcionamiento…",
                        )
                    }
                    Text(
                        "Al confirmar, la orden se cierra y la familia verá la incidencia como resuelta en la web.",
                        style = MaterialTheme.typography.bodySmall, color = InkMuted,
                    )
                    state.error?.let { ErrorBanner(it) }
                }
            }
        }
        if (order != null) {
            Box(Modifier.background(Surface).navigationBarsPadding().padding(16.dp)) {
                PrimaryButton(
                    text = "Marcar incidencia como resuelta",
                    onClick = { viewModel.submit(onResolved) },
                    enabled = state.canSubmit,
                    busy = state.saving,
                    icon = Icons.Outlined.TaskAlt,
                    container = TealDark,
                )
            }
        }
    }
}

@Composable
private fun ResolutionField(value: String, onChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        supportingText = { Text("${value.length}/${ResolveIncidentUseCase.MAX_RESOLUTION}", color = InkMuted) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Outline, focusedBorderColor = Navy),
    )
}
