package com.resolum.alivia.features.nursing.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.resolum.alivia.core.ui.theme.Danger
import com.resolum.alivia.core.ui.theme.DangerSoft
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.SurfaceMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.core.ui.theme.Warning
import com.resolum.alivia.core.ui.theme.WarningSoft
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.CareActivityType

val CareActivityType.icon: ImageVector
    get() = when (this) {
        CareActivityType.HYGIENE -> Icons.Outlined.CleanHands
        CareActivityType.MEDICATION -> Icons.Outlined.Medication
        CareActivityType.APPOINTMENT -> Icons.Outlined.LocalHospital
        CareActivityType.THERAPY -> Icons.Outlined.SelfImprovement
        CareActivityType.MEAL -> Icons.Outlined.Restaurant
        CareActivityType.BATH -> Icons.Outlined.Bathtub
        @Suppress("DEPRECATION")
        CareActivityType.EXERCISE -> Icons.Outlined.DirectionsWalk
        CareActivityType.OTHER -> Icons.Outlined.Checklist
    }

val CareActivityType.label: String
    get() = when (this) {
        CareActivityType.HYGIENE -> "Higiene"
        CareActivityType.MEDICATION -> "Medicamento"
        CareActivityType.APPOINTMENT -> "Cita médica"
        CareActivityType.THERAPY -> "Terapia"
        CareActivityType.MEAL -> "Alimentación"
        CareActivityType.BATH -> "Baño"
        CareActivityType.EXERCISE -> "Ejercicio"
        CareActivityType.OTHER -> "Otra"
    }

val CareActivityStatus.label: String
    get() = when (this) {
        CareActivityStatus.SCHEDULED -> "Programada"
        CareActivityStatus.IN_PROGRESS -> "En curso"
        CareActivityStatus.COMPLETED -> "Completada"
        CareActivityStatus.CANCELLED -> "Cancelada"
        CareActivityStatus.MISSED -> "Vencida"
        CareActivityStatus.OMITTED -> "Omitida"
    }

val CareActivityStatus.container: Color
    get() = when (this) {
        CareActivityStatus.SCHEDULED -> NavySoft
        CareActivityStatus.IN_PROGRESS -> WarningSoft
        CareActivityStatus.COMPLETED -> TealSoft
        CareActivityStatus.CANCELLED, CareActivityStatus.OMITTED -> SurfaceMuted
        CareActivityStatus.MISSED -> DangerSoft
    }

val CareActivityStatus.content: Color
    get() = when (this) {
        CareActivityStatus.SCHEDULED -> Navy
        CareActivityStatus.IN_PROGRESS -> Warning
        CareActivityStatus.COMPLETED -> TealDark
        CareActivityStatus.CANCELLED, CareActivityStatus.OMITTED -> InkMuted
        CareActivityStatus.MISSED -> Danger
    }
