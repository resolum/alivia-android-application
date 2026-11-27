package com.resolum.alivia.features.nursing.presentation.activities

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.Pill
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.DangerSoft
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Motion
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavyButton
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.core.ui.theme.Warning
import com.resolum.alivia.core.ui.theme.WarningSoft
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.MedicationAdministration
import com.resolum.alivia.features.nursing.domain.models.MedicationResult
import java.time.LocalDateTime

private val OMISSION_REASONS = listOf("Se negó a tomarlo", "No había medicamento", "Indicación del médico")

private sealed interface DosePhase {
    data object Loading : DosePhase
    data class Pending(val administration: MedicationAdministration) : DosePhase
    data class Closed(val administration: MedicationAdministration) : DosePhase
    data class Unavailable(val message: String?) : DosePhase
}

/**
 * Confirm or omit one dose. A dose with a definitive result only shows that result: it cannot be confirmed twice.
 */
@Composable
fun MedicationRecordSection(
    activity: CareActivity,
    state: MedicationRecordState,
    canRecord: Boolean,
    onConfirm: () -> Unit,
    onOmit: (String) -> Unit,
) {
    val administration = state.administration
    val phase = when {
        state.loading -> DosePhase.Loading
        administration == null -> DosePhase.Unavailable(state.error)
        administration.pending -> DosePhase.Pending(administration)
        else -> DosePhase.Closed(administration)
    }
    // The result replaces the actions in place: a short fade and settle, so the change reads as "saved", not as a new screen.
    AnimatedContent(
        targetState = phase,
        contentKey = { it::class },
        transitionSpec = { (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.96f)) togetherWith fadeOut(Motion.exit()) },
        label = "dose",
    ) { current ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (current) {
                DosePhase.Loading -> Loading(Modifier.height(96.dp))
                is DosePhase.Unavailable -> {
                    ErrorBanner(current.message ?: "No pudimos cargar esta administración.")
                }
                is DosePhase.Closed -> {
                    if (state.duplicate) {
                        Notice(Icons.Outlined.Info, "Esta dosis ya tenía un resultado. No se registró de nuevo.", NavySoft, Navy)
                    }
                    ResultCard(current.administration, activity)
                    if (state.justRecorded) {
                        Text("Guardado en el historial de la dosis.", style = MaterialTheme.typography.bodySmall, color = InkMuted,
                            modifier = Modifier.fillMaxWidth())
                    }
                }
                is DosePhase.Pending -> PendingActions(current.administration, state, canRecord, onConfirm, onOmit)
            }
        }
    }
}

@Composable
private fun PendingActions(
    administration: MedicationAdministration,
    state: MedicationRecordState,
    canRecord: Boolean,
    onConfirm: () -> Unit,
    onOmit: (String) -> Unit,
) {
    var omitting by rememberSaveable { mutableStateOf(false) }
    var reason by rememberSaveable { mutableStateOf("") }
    val now = LocalDateTime.now(Formats.zone)

    if (administration.isLate(now)) {
        Notice(
            Icons.Outlined.Schedule,
            "Fuera del horario previsto (terminó a las ${Formats.time(administration.windowEnd.toLocalTime())}). " +
                "Se guardará la hora real y quedará como registro tardío.",
            WarningSoft, Ink,
        )
    }
    state.error?.let { ErrorBanner(it) }
    if (!canRecord || state.forbidden) {
        Notice(Icons.Outlined.Lock, "Solo la persona responsable o el familiar a cargo puede registrar esta dosis.", SurfaceMuted, InkMuted)
        return
    }

    PrimaryButton(
        "Confirmar administración",
        onClick = onConfirm,
        icon = Icons.Outlined.CheckCircle,
        enabled = !omitting,
        busy = state.saving && !omitting,
        container = Navy,
    )
    if (!omitting) {
        OutlinedButton(
            onClick = { omitting = true },
            enabled = !state.saving,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Outline),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("No se administró", color = Navy, style = MaterialTheme.typography.labelLarge) }
    }
    AnimatedVisibility(
        visible = omitting,
        enter = expandVertically(Motion.enter()) + fadeIn(Motion.enter()),
        exit = shrinkVertically(Motion.exit()) + fadeOut(Motion.exit()),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("¿Por qué no se administró?", style = MaterialTheme.typography.titleSmall, color = Navy)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OMISSION_REASONS.forEach { option ->
                    FilterChip(
                        selected = reason == option,
                        onClick = { reason = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavySoft, selectedLabelColor = Navy),
                    )
                }
            }
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it.take(MAX_REASON) },
                label = { Text("Motivo") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Navy, focusedLabelColor = Navy),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                "Guardar omisión",
                onClick = { onOmit(reason) },
                icon = Icons.Outlined.Block,
                enabled = reason.isNotBlank(),
                busy = state.saving,
                container = NavyButton,
            )
            OutlinedButton(
                onClick = { omitting = false },
                enabled = !state.saving,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Outline),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Volver", color = InkMuted) }
        }
    }
}

@Composable
private fun ResultCard(administration: MedicationAdministration, activity: CareActivity) {
    val outcome = administration.outcome
    val result = outcome?.result ?: MedicationResult.MISSED
    val (icon, title, tint, container) = when (result) {
        MedicationResult.ADMINISTERED -> Quad(Icons.Outlined.CheckCircle, "Administrada", TealDark, TealSoft)
        MedicationResult.OMITTED -> Quad(Icons.Outlined.Block, "No se administró", InkMuted, SurfaceMuted)
        MedicationResult.MISSED -> Quad(Icons.Outlined.ErrorOutline, "Vencida sin confirmar", Danger, DangerSoft)
    }
    AliviaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = Navy,
                modifier = Modifier.weight(1f).padding(start = 12.dp))
            if (administration.recordedLate) Pill("Tardía", WarningSoft, Warning)
        }
        HorizontalDivider(color = Outline)
        ResultLine("Horario programado", activity.endTime?.let { Formats.window(administration.startTime, it) }
            ?: Formats.time(administration.startTime))
        outcome?.occurredAt?.let {
            ResultLine("Registrada", "${Formats.day(it.toLocalDate())} · ${Formats.time(it.toLocalTime())}")
        }
        val who = outcome?.recordedBy ?: administration.responsible ?: activity.responsible
        if (result != MedicationResult.MISSED) who?.let { ResultLine("Responsable", it) }
        outcome?.reason?.let { ResultLine("Motivo", it) }
        if (result == MedicationResult.MISSED) {
            Text("La ventana de atención terminó sin registro y se generó una alerta.", style = MaterialTheme.typography.bodySmall,
                color = InkMuted)
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = InkMuted, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(0.58f))
    }
}

@Composable
private fun Notice(icon: ImageVector, text: String, container: Color, content: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(container).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = content)
    }
}

private data class Quad(val icon: ImageVector, val title: String, val tint: Color, val container: Color)

private const val MAX_REASON = 240
