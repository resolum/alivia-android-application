package com.resolum.alivia.features.devices.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavyButton
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.devices.di.DevicesModule
import com.resolum.alivia.features.devices.domain.models.HomeDevice
import com.resolum.alivia.features.devices.domain.models.HomeDeviceType
import com.resolum.alivia.features.operations.presentation.orders.ScreenHeader

val HomeDeviceType.label: String
    get() = when (this) {
        HomeDeviceType.HUB -> "Hub central"
        HomeDeviceType.MICROPHONE -> "Cubo de voz"
        HomeDeviceType.ACTUATOR -> "Actuador"
        HomeDeviceType.SENSOR -> "Sensor"
        HomeDeviceType.OTHER -> "Dispositivo"
    }

private val HomeDeviceType.groupLabel: String
    get() = when (this) {
        HomeDeviceType.HUB -> "Hub central"
        HomeDeviceType.MICROPHONE -> "Cubos de voz"
        HomeDeviceType.ACTUATOR -> "Luces y actuadores"
        HomeDeviceType.SENSOR -> "Sensores"
        HomeDeviceType.OTHER -> "Otros"
    }

val HomeDeviceType.icon: ImageVector
    get() = when (this) {
        HomeDeviceType.HUB -> Icons.Outlined.DeviceHub
        HomeDeviceType.MICROPHONE -> Icons.Outlined.Mic
        HomeDeviceType.ACTUATOR -> Icons.Outlined.Lightbulb
        HomeDeviceType.SENSOR -> Icons.Outlined.Sensors
        HomeDeviceType.OTHER -> Icons.Outlined.Devices
    }

/** Devices installed in the caregiver's home, grouped by kind; each one opens its detail. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onOpenNotifications: () -> Unit,
    viewModel: DevicesViewModel = viewModel { DevicesViewModel(DevicesModule.getHomeDevices()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    val devices = state.home?.devices.orEmpty()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Dispositivos",
            subtitle = if (state.loaded) (if (devices.size == 1) "1 equipo instalado" else "${devices.size} equipos instalados") else null,
            onOpenProfile = onOpenNotifications,
        )
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
                when {
                    state.loading -> item { Loading() }
                    state.loaded && devices.isEmpty() -> item { EmptyDevices() }
                    else -> {
                        item { Summary(devices, state.home?.address) }
                        devices.groupBy { it.type }.toSortedMap().forEach { (type, group) ->
                            item(key = "h-$type") {
                                SectionLabel("${type.groupLabel} · ${group.size}", Modifier.padding(top = 4.dp))
                            }
                            items(group, key = { it.macAddress }) { device -> DeviceCard(device) { viewModel.select(device) } }
                        }
                    }
                }
            }
        }
    }

    state.selected?.let { device -> DeviceDetailSheet(device, state.home?.address, onDismiss = { viewModel.select(null) }) }
}

@Composable
private fun Summary(devices: List<HomeDevice>, address: String?) {
    AliviaCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Monitoreo activo", TealSoft, TealDark, dot = TealDark)
            address?.let {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = InkMuted, modifier = Modifier.size(14.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple(devices.size, "Instalados", NavySoft),
                Triple(devices.count { it.type == HomeDeviceType.MICROPHONE }, "Cubos de voz", SurfaceMuted),
                Triple(devices.count { it.type == HomeDeviceType.SENSOR }, "Sensores", SurfaceMuted),
            ).forEach { (count, label, color) ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(color).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("$count", style = MaterialTheme.typography.titleLarge, color = Navy)
                    Text(label, style = MaterialTheme.typography.bodySmall, color = InkMuted, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: HomeDevice, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
                Icon(device.type.icon, contentDescription = null, tint = Navy, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(device.alias, style = MaterialTheme.typography.titleSmall, color = Navy, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Instalado", TealSoft, TealDark, dot = TealDark)
                    Text(device.type.label, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = InkMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDetailSheet(device: HomeDevice, address: String?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(NavySoft), contentAlignment = Alignment.Center) {
                Icon(device.type.icon, contentDescription = null, tint = Navy, modifier = Modifier.size(30.dp))
            }
            Text(device.alias, style = MaterialTheme.typography.titleLarge, color = Navy, textAlign = TextAlign.Center)
            Text(device.type.label, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            Pill("Instalado", TealSoft, TealDark, dot = TealDark)
            AliviaCard(Modifier.padding(top = 8.dp)) {
                SectionLabel("Información esencial")
                DetailRow("Tipo", device.type.label)
                HorizontalDivider(color = Outline)
                DetailRow("Dirección MAC", device.macAddress)
                address?.let {
                    HorizontalDivider(color = Outline)
                    DetailRow("Hogar", it)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Ink, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun EmptyDevices() {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Sensors, contentDescription = null, tint = Navy, modifier = Modifier.size(30.dp))
        }
        Text("Tu hogar todavía no tiene dispositivos instalados", style = MaterialTheme.typography.titleMedium, color = Navy,
            textAlign = TextAlign.Center)
        Text("Aparecerán aquí cuando el técnico termine la instalación.", style = MaterialTheme.typography.bodyMedium,
            color = InkMuted, textAlign = TextAlign.Center)
    }
}
