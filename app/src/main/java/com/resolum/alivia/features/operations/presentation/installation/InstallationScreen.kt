package com.resolum.alivia.features.operations.presentation.installation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SettingsInputComponent
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.Pill
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.DangerSoft
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.InstallationColor
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.operations.di.OperationsModule
import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.InstallationResult
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.presentation.label

@Composable
fun InstallationScreen(
    orderId: Long,
    onExit: () -> Unit,
    onCompleted: (Long) -> Unit,
    viewModel: InstallationViewModel = viewModel(key = "installation-$orderId") {
        InstallationViewModel(
            orderId, OperationsModule.getOrder(), OperationsModule.registerDevice(),
            OperationsModule.removeDevice(), OperationsModule.completeInstallation(),
        )
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order
    BackHandler { if (!viewModel.back()) onExit() }

    Column(Modifier.fillMaxSize().background(Background).imePadding()) {
        Row(
            Modifier.fillMaxWidth().background(Surface).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (!viewModel.back()) onExit() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = Ink)
            }
            Column(Modifier.weight(1f)) {
                Text(titleOf(state.step), style = MaterialTheme.typography.titleLarge, color = Navy)
                Text("Paso ${state.step.number} · Instalación", style = MaterialTheme.typography.bodySmall, color = TealDark)
            }
            IconButton(onClick = onExit) { Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Ink) }
        }
        if (order == null) {
            if (state.loadError != null) ErrorBanner(state.loadError!!, Modifier.padding(16.dp)) else Loading()
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepProgress(state.step, order.code)
            when (state.step) {
                InstallationStep.HOME -> HomeStep(order)
                InstallationStep.DEVICES -> DevicesStep(state, viewModel)
                InstallationStep.TESTS -> TestsStep(state, viewModel::toggleTested)
                InstallationStep.CLOSING -> ClosingStep(state, viewModel)
            }
            state.error?.let { ErrorBanner(it) }
        }
        Column(
            Modifier.background(Surface).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (state.step) {
                InstallationStep.HOME -> PrimaryButton("Confirmar hogar y continuar", onClick = { viewModel.goTo(InstallationStep.DEVICES) },
                    container = Navy)
                InstallationStep.DEVICES -> {
                    PrimaryButton(
                        "Registrar dispositivo", onClick = viewModel::register, icon = Icons.Outlined.AddCircleOutline,
                        enabled = state.form.alias.isNotBlank() && state.form.macValid && !state.duplicateMac && state.form.type != null,
                        busy = state.busy, container = Navy,
                    )
                    SecondaryButton(
                        if (order.installedDevices.isEmpty()) "Omitir por ahora y continuar" else "Continuar a pruebas",
                        enabled = true,
                    ) { viewModel.goTo(InstallationStep.TESTS) }
                }
                InstallationStep.TESTS -> PrimaryButton(
                    if (state.allTested) "Continuar al cierre" else "Marca cada dispositivo probado",
                    onClick = { viewModel.goTo(InstallationStep.CLOSING) }, enabled = state.allTested, container = Navy,
                )
                InstallationStep.CLOSING -> PrimaryButton(
                    "Finalizar instalación", onClick = { viewModel.finish(onCompleted) },
                    enabled = state.result != null, busy = state.busy,
                    container = if (state.result == InstallationResult.REJECTED) Danger else Navy,
                )
            }
        }
    }
}

private fun titleOf(step: InstallationStep) = when (step) {
    InstallationStep.HOME -> "Verificar hogar"
    InstallationStep.DEVICES -> "Registrar dispositivos"
    InstallationStep.TESTS -> "Probar dispositivos"
    InstallationStep.CLOSING -> "Cerrar instalación"
}

@Composable
private fun StepProgress(step: InstallationStep, code: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySoft),
        border = BorderStroke(1.dp, Outline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = TealDark, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Paso ${step.number} de 4: ${step.title}", style = MaterialTheme.typography.titleSmall, color = TealDark,
                    modifier = Modifier.weight(1f))
                Pill(code, Navy, Color.White)
            }
            LinearProgressIndicator(
                progress = { step.number / 4f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = InstallationColor,
                trackColor = Surface,
                drawStopIndicator = {},
            )
            Row {
                Text("Hardware IoT", style = MaterialTheme.typography.bodySmall, color = Ink, modifier = Modifier.weight(1f))
                Text("${step.number * 25}% completado", style = MaterialTheme.typography.bodySmall, color = TealDark)
            }
        }
    }
}

@Composable
private fun HomeCard(order: TechnicalOrder, verified: Boolean) {
    AliviaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Home, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Hogar asignado", style = MaterialTheme.typography.titleSmall, color = Ink, modifier = Modifier.weight(1f))
            if (verified) Pill("Verificado", TealSoft, TealDark)
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceMuted).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(NavySoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = Navy)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(order.homeName, style = MaterialTheme.typography.titleMedium, color = Navy)
                Text(order.directions ?: "Sin indicaciones registradas", style = MaterialTheme.typography.bodySmall, color = Ink)
            }
        }
    }
}

@Composable
private fun HomeStep(order: TechnicalOrder) {
    HomeCard(order, verified = false)
    AliviaCard {
        Text("Antes de instalar", style = MaterialTheme.typography.titleSmall, color = Navy)
        listOf(
            "Confirma con la familia los ambientes donde irán los dispositivos.",
            "Verifica que haya red Wi-Fi y tomacorrientes cerca de cada punto.",
            "Ten a mano la etiqueta con la dirección MAC de cada dispositivo.",
        ).forEach { line ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = TealDark, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(line, style = MaterialTheme.typography.bodyMedium, color = Ink)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicesStep(state: InstallationUiState, viewModel: InstallationViewModel) {
    val order = state.order ?: return
    HomeCard(order, verified = true)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = InkMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Todos los campos marcados con (*) son obligatorios", style = MaterialTheme.typography.bodySmall, color = InkMuted)
    }
    AliviaCard {
        FieldTitle("1. Alias del dispositivo", "Identificador único")
        FilledField(
            value = state.form.alias,
            onValueChange = viewModel::updateAlias,
            placeholder = "Micrófono Cabecera",
            icon = Icons.AutoMirrored.Outlined.Label,
        )
        Text("Asigna un nombre descriptivo para la ubicación física del paciente.", style = MaterialTheme.typography.bodySmall, color = Ink)
    }
    AliviaCard {
        FieldTitle("2. Dirección MAC", null)
        FilledField(
            value = state.form.macAddress,
            onValueChange = viewModel::updateMac,
            placeholder = "00:1A:2B:3C:4D:5E",
            icon = Icons.Outlined.Router,
            monospace = true,
            capitalization = KeyboardCapitalization.Characters,
            trailing = if (state.form.macAddress.isNotEmpty()) {
                { IconButton(onClick = viewModel::clearMac) { Icon(Icons.Outlined.Cancel, contentDescription = "Borrar", tint = Ink) } }
            } else null,
        )
        if (state.form.macComplete && state.form.macValid) {
            Notice(Icons.Outlined.Verified, TealDark, NavySoft, "Formato válido de 12 dígitos",
                "Hexadecimales en mayúsculas separados por pares con dos puntos (:).")
            if (state.duplicateMac) {
                Notice(Icons.Outlined.WarningAmber, Danger, DangerSoft, "Este dispositivo ya está registrado en la orden actual.", null)
            } else {
                Notice(Icons.Outlined.CheckCircle, TealDark, TealSoft, "Verificación: no se detectan duplicidades en la orden actual.", null)
            }
        } else {
            Text("Escribe los 12 dígitos; los dos puntos se agregan solos.", style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
    }
    AliviaCard {
        FieldTitle("3. Tipo de dispositivo", null)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            TextField(
                value = state.form.type?.label ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Elige el tipo") },
                leadingIcon = { Icon(iconOf(state.form.type), contentDescription = null, tint = TealDark) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                colors = filledColors(),
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DeviceType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        leadingIcon = { Icon(iconOf(type), contentDescription = null, tint = TealDark) },
                        onClick = { viewModel.updateType(type); expanded = false },
                    )
                }
            }
        }
        Text("Categoría del módulo de asistencia para mapeo en el hub central.", style = MaterialTheme.typography.bodySmall, color = Ink)
    }
    if (order.installedDevices.isNotEmpty()) {
        AliviaCard {
            Text("Registrados en esta orden (${order.installedDevices.size})", style = MaterialTheme.typography.titleSmall, color = Navy)
            order.installedDevices.forEachIndexed { index, device ->
                if (index > 0) HorizontalDivider(color = Outline)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconOf(device.type), contentDescription = null, tint = TealDark)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.alias, style = MaterialTheme.typography.bodyLarge, color = Ink)
                        Text("${device.type.label} · ${device.macAddress}", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    }
                    IconButton(onClick = { viewModel.remove(device.macAddress) }, enabled = !state.busy) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Quitar ${device.alias}", tint = Danger)
                    }
                }
            }
        }
    }
}

@Composable
private fun TestsStep(state: InstallationUiState, onToggle: (String) -> Unit) {
    val order = state.order ?: return
    AliviaCard {
        Text("Prueba cada dispositivo", style = MaterialTheme.typography.titleSmall, color = Navy)
        Text("Verifica que encienda, se conecte al hub y responda antes de marcarlo.", style = MaterialTheme.typography.bodyMedium, color = Ink)
        order.installedDevices.forEach { device ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceMuted).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = device.macAddress in state.tested,
                    onCheckedChange = { onToggle(device.macAddress) },
                    colors = CheckboxDefaults.colors(checkedColor = InstallationColor),
                )
                Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                    Text(device.alias, style = MaterialTheme.typography.bodyLarge, color = Ink)
                    Text("${device.type.label} · ${device.macAddress}", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                }
                Icon(iconOf(device.type), contentDescription = null, tint = TealDark, modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}

@Composable
private fun ClosingStep(state: InstallationUiState, viewModel: InstallationViewModel) {
    val order = state.order ?: return
    AliviaCard {
        Text("Resultado de la instalación", style = MaterialTheme.typography.titleSmall, color = Navy)
        Text(
            if (order.installedDevices.isEmpty())
                "Sin dispositivos registrados. Podrás completar este paso posteriormente desde la aplicación web."
            else
                "${order.installedDevices.size} dispositivos registrados y probados.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
        )
        ResultRow("Aprobada", "El hogar queda en servicio y la familia ya puede usar Alivia.",
            state.result == InstallationResult.APPROVED, InstallationColor) { viewModel.selectResult(InstallationResult.APPROVED) }
        ResultRow("Rechazada", "La instalación no se pudo completar; la central reprogramará la visita.",
            state.result == InstallationResult.REJECTED, Danger) { viewModel.selectResult(InstallationResult.REJECTED) }
    }
    AliviaCard {
        Text("Notas", style = MaterialTheme.typography.titleSmall, color = Navy)
        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::updateNotes,
            placeholder = { Text("Observaciones del cierre (opcional)") },
            supportingText = { Text("${state.notes.length}/300") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Outline, focusedBorderColor = Navy),
        )
    }
}

@Composable
private fun ResultRow(title: String, description: String, selected: Boolean, tint: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) SurfaceMuted else Surface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) tint else Outline),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = tint))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Ink)
                Text(description, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        }
    }
}

@Composable
private fun FieldTitle(title: String, hint: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Ink)
        Text(" *", style = MaterialTheme.typography.titleSmall, color = Danger)
        Spacer(Modifier.weight(1f))
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = InkMuted)
    }
}

@Composable
private fun filledColors() = TextFieldDefaults.colors(
    focusedContainerColor = SurfaceMuted,
    unfocusedContainerColor = SurfaceMuted,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

@Composable
private fun FilledField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    monospace: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    trailing: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = InkMuted) },
        trailingIcon = trailing,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        textStyle = if (monospace) MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(12.dp),
        colors = filledColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Notice(icon: ImageVector, tint: Color, container: Color, title: String, body: String?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(container).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = if (tint == Danger) Danger else Ink)
            if (body != null) Text(body, style = MaterialTheme.typography.bodySmall, color = Ink)
        }
    }
}

@Composable
private fun SecondaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NavySoft, contentColor = Navy),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

private fun iconOf(type: DeviceType?): ImageVector = when (type) {
    DeviceType.MICROPHONE -> Icons.Outlined.Mic
    DeviceType.ACTUATOR -> Icons.Outlined.SettingsInputComponent
    DeviceType.SENSOR -> Icons.Outlined.Sensors
    DeviceType.HUB -> Icons.Outlined.Router
    null -> Icons.Outlined.Memory
}
