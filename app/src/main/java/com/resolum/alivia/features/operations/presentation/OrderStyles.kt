package com.resolum.alivia.features.operations.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.resolum.alivia.core.ui.components.Pill
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.DangerSoft
import com.resolum.alivia.core.ui.theme.EvaluationColor
import com.resolum.alivia.core.ui.theme.IncidentColor
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.InstallationColor
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.core.ui.theme.Warning
import com.resolum.alivia.core.ui.theme.WarningSoft
import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.IncidentStatus
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import com.resolum.alivia.features.operations.domain.models.OrderProgress
import com.resolum.alivia.features.operations.domain.models.ServiceType

val ServiceType.label: String
    get() = when (this) {
        ServiceType.EVALUATION -> "Revisión"
        ServiceType.INSTALLATION -> "Instalación"
        ServiceType.MAINTENANCE -> "Incidencia"
    }

val ServiceType.longLabel: String
    get() = when (this) {
        ServiceType.EVALUATION -> "Evaluación técnica"
        ServiceType.INSTALLATION -> "Instalación de dispositivos"
        ServiceType.MAINTENANCE -> "Incidencia técnica"
    }

val ServiceType.color: Color
    get() = when (this) {
        ServiceType.EVALUATION -> EvaluationColor
        ServiceType.INSTALLATION -> InstallationColor
        ServiceType.MAINTENANCE -> IncidentColor
    }

val ServiceType.icon: ImageVector
    get() = when (this) {
        ServiceType.EVALUATION -> Icons.Outlined.TaskAlt
        ServiceType.INSTALLATION -> Icons.Outlined.Build
        ServiceType.MAINTENANCE -> Icons.Outlined.ReportProblem
    }

val OrderProgress.label: String
    get() = when (this) {
        OrderProgress.SCHEDULED -> "Programada"
        OrderProgress.IN_PROGRESS -> "En proceso"
        OrderProgress.COMPLETED -> "Completada"
        OrderProgress.CANCELED -> "Cancelada"
    }

val DeviceType.label: String
    get() = when (this) {
        DeviceType.MICROPHONE -> "Micrófono"
        DeviceType.ACTUATOR -> "Actuador"
        DeviceType.SENSOR -> "Sensor"
        DeviceType.HUB -> "Hub central"
    }

@Composable
fun ProgressPill(progress: OrderProgress, modifier: Modifier = Modifier) {
    when (progress) {
        OrderProgress.SCHEDULED -> Pill(progress.label, NavySoft, Navy, modifier)
        OrderProgress.IN_PROGRESS -> Pill(progress.label, WarningSoft, Navy, modifier, dot = Warning)
        OrderProgress.COMPLETED -> Pill(progress.label, TealSoft, TealDark, modifier)
        OrderProgress.CANCELED -> Pill(progress.label, SurfaceMuted, InkMuted, modifier)
    }
}

/** The incident stage for maintenance orders (Registrada, En evaluación…), the order progress otherwise. */
@Composable
fun OrderStatusPill(order: TechnicalOrder, modifier: Modifier = Modifier) {
    val status = order.incident?.status
    if (order.serviceType != ServiceType.MAINTENANCE || status == null) return ProgressPill(order.progress, modifier)
    when (status) {
        IncidentStatus.OPEN, IncidentStatus.IN_EVALUATION -> Pill(status.label, WarningSoft, Navy, modifier, dot = Warning)
        IncidentStatus.SCHEDULED -> Pill(status.label, NavySoft, Navy, modifier)
        IncidentStatus.IN_PROGRESS -> Pill(status.label, WarningSoft, Navy, modifier, dot = Warning)
        IncidentStatus.RESOLVED -> Pill(status.label, TealSoft, TealDark, modifier)
        IncidentStatus.CANCELED -> Pill(status.label, SurfaceMuted, InkMuted, modifier)
    }
}

@Composable
fun TypePill(type: ServiceType, modifier: Modifier = Modifier) {
    when (type) {
        ServiceType.MAINTENANCE -> Pill(type.label, DangerSoft, Danger, modifier, dot = Danger)
        ServiceType.INSTALLATION -> Pill(type.label, TealSoft, TealDark, modifier, dot = InstallationColor)
        ServiceType.EVALUATION -> Pill(type.label, NavySoft, Navy, modifier, dot = Navy)
    }
}
