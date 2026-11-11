package com.resolum.alivia.features.nursing.presentation.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.nursing.di.ActivityModule
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.presentation.icon
import com.resolum.alivia.features.nursing.presentation.label
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.TextStyle

/** Creates a care activity for the assisted person and assigns it to the caregiver or another one. */
@Composable
fun NewActivityScreen(
    onClose: () -> Unit,
    viewModel: NewActivityViewModel = viewModel {
        NewActivityViewModel(ActivityModule.getAssistedPersons(), ActivityModule.getCaregivers(), ActivityModule.createActivity())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val goBack = { if (!viewModel.back()) onClose() }
    BackHandler(onBack = goBack)
    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    Column(Modifier.fillMaxSize().background(Background).systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = Navy) }
            Column(Modifier.weight(1f)) {
                Text(if (state.step == NewActivityStep.ACTIVITY) "Nueva actividad" else "Programación",
                    style = MaterialTheme.typography.titleLarge, color = Navy)
                Text("Paso ${state.step.ordinal + 1} de 2", style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        }
        StepTabs(state.step)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.loading) {
                Loading()
            } else if (state.step == NewActivityStep.ACTIVITY) {
                ActivityStep(state, viewModel)
            } else {
                ScheduleStep(state, viewModel)
            }
            state.error?.let { ErrorBanner(it) }
        }
        Column(Modifier.background(Surface).padding(16.dp)) {
            if (state.step == NewActivityStep.ACTIVITY) {
                PrimaryButton("Continuar", onClick = viewModel::next, icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    enabled = state.canContinue, container = Navy)
            } else {
                state.blockedReason?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                PrimaryButton("Confirmar programación", onClick = viewModel::save, icon = Icons.Outlined.Check,
                    enabled = state.canSave, busy = state.saving, container = Navy)
            }
        }
    }
}

@Composable
private fun StepTabs(step: NewActivityStep) {
    Row(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp)).background(NavySoft).padding(4.dp)) {
        listOf("1  Actividad", "2  Programación").forEachIndexed { index, label ->
            val selected = step.ordinal == index
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (selected) Navy else NavySoft).padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Surface else Navy)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Surface, focusedContainerColor = Surface, unfocusedBorderColor = Outline, focusedBorderColor = Navy,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityStep(state: NewActivityUiState, viewModel: NewActivityViewModel) {
    SectionLabel("Persona asistida")
    AliviaCard {
        val person = state.person
        if (person == null) {
            Text("No tienes una persona asistida asignada.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(TealSoft), contentAlignment = Alignment.Center) {
                    Text(person.initials, style = MaterialTheme.typography.titleSmall, color = Navy)
                }
                Text(person.fullName, style = MaterialTheme.typography.titleMedium, color = Navy, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
    SectionLabel("Tipo de actividad", Modifier.padding(top = 4.dp))
    // One swipeable row, as the design shows it.
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(CareActivityType.entries) { type ->
            FilterChip(
                selected = state.type == type,
                onClick = { viewModel.selectType(type) },
                label = { Text(type.label) },
                leadingIcon = { Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Navy, selectedLabelColor = Surface, selectedLeadingIconColor = Surface,
                    containerColor = Surface, labelColor = Navy, iconColor = Navy,
                ),
            )
        }
    }
    SectionLabel("Detalle", Modifier.padding(top = 4.dp))
    OutlinedTextField(
        value = state.title, onValueChange = viewModel::onTitle, label = { Text("Nombre") },
        placeholder = { Text("Ej. Medicación de la mañana") }, singleLine = true,
        shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.description, onValueChange = viewModel::onDescription, label = { Text("Instrucción (opcional)") },
        placeholder = { Text("Ej. Dejar un vaso de agua en la mesa junto a la pastilla") }, minLines = 3,
        supportingText = { Text("${state.description.length}/500") },
        shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduleStep(state: NewActivityUiState, viewModel: NewActivityViewModel) {
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf<Boolean?>(null) } // true = start, false = end

    AliviaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(TealSoft), contentAlignment = Alignment.Center) {
                Icon(state.type?.icon ?: Icons.Outlined.Schedule, contentDescription = null, tint = TealDark)
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text((state.type?.label ?: "").uppercase(), style = MaterialTheme.typography.labelSmall, color = TealDark)
                Text(state.title, style = MaterialTheme.typography.titleMedium, color = Navy)
                state.person?.let { Text(it.fullName, style = MaterialTheme.typography.bodySmall, color = InkMuted) }
            }
        }
    }
    SectionLabel("Fecha y hora", Modifier.padding(top = 4.dp))
    AliviaCard {
        if (!state.weekly) PickerRow(Icons.Outlined.CalendarToday, "Fecha", Formats.day(state.date)) { pickingDate = true }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { PickerRow(Icons.Outlined.Schedule, "Inicio", Formats.time(state.start)) { pickingTime = true } }
            Box(Modifier.weight(1f)) { PickerRow(Icons.Outlined.Schedule, "Fin", Formats.time(state.end)) { pickingTime = false } }
        }
        if (state.endsNextDay) {
            Text("Termina al día siguiente", style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
    }
    SectionLabel("Repetición", Modifier.padding(top = 4.dp))
    AliviaCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to "No se repite", true to "Cada semana").forEach { (weekly, label) ->
                FilterChip(
                    selected = state.weekly == weekly, onClick = { viewModel.setWeekly(weekly) }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Navy, selectedLabelColor = Surface, labelColor = Navy),
                )
            }
        }
        if (state.weekly) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val selected = day in state.days
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(if (selected) Navy else NavySoft).clickable { viewModel.toggleDay(day) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(day.getDisplayName(TextStyle.NARROW, Formats.locale).uppercase(), style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Surface else Navy)
                    }
                }
            }
        }
    }
    SectionLabel("Responsable", Modifier.padding(top = 4.dp))
    if (state.caregivers.isEmpty()) {
        AliviaCard { Text("No hay cuidadores asignados a esta persona.", style = MaterialTheme.typography.bodyMedium, color = InkMuted) }
    }
    var other = 0
    state.caregivers.forEach { caregiver ->
        val name = if (caregiver.isMe) "Tú" else caregiver.fullName ?: "Otro cuidador ${++other}"
        val selected = state.caregiverId == caregiver.id
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface)
                .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Navy else Outline), RoundedCornerShape(16.dp))
                .clickable { viewModel.selectCaregiver(caregiver.id) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (caregiver.isMe) TealSoft else NavySoft), contentAlignment = Alignment.Center) {
                Text(if (caregiver.isMe) "TÚ" else initialsOf(caregiver.fullName) ?: "C$other",
                    style = MaterialTheme.typography.labelLarge, color = Navy)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = Navy)
                Text(if (caregiver.isMe) caregiver.fullName ?: "Cuidador activo en turno" else "Cuidador de la red",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (caregiver.isMe) TealDark else InkMuted)
            }
            Icon(if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked, contentDescription = null,
                tint = if (selected) Navy else InkMuted)
        }
    }
    Spacer(Modifier.height(4.dp))

    if (pickingDate) {
        val today = Formats.today()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickingDate = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state = pickerState) }
    }
    pickingTime?.let { isStart ->
        val initial = if (isStart) state.start else state.end
        val timeState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickingTime = null },
            confirmButton = {
                TextButton(onClick = {
                    val value = LocalTime.of(timeState.hour, timeState.minute)
                    if (isStart) viewModel.onStart(value) else viewModel.onEnd(value)
                    pickingTime = null
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = null }) { Text("Cancelar") } },
            title = { Text(if (isStart) "Hora de inicio" else "Hora de fin") },
            text = { TimePicker(state = timeState) },
        )
    }
}

private fun initialsOf(name: String?) =
    name?.split(" ")?.filter { it.isNotBlank() }?.mapNotNull { it.firstOrNull() }?.take(2)?.joinToString("")?.uppercase()?.ifEmpty { null }

@Composable
private fun PickerRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = InkMuted, modifier = Modifier.size(18.dp))
        Column(Modifier.padding(start = 10.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            Text(value, style = MaterialTheme.typography.titleSmall, color = Ink)
        }
    }
}
