package com.resolum.alivia.features.shared.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.Teal
import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.models.UserRole
import com.resolum.alivia.features.operations.presentation.orders.OrdersScreen
import com.resolum.alivia.features.operations.presentation.orders.ScreenHeader

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Outlined.Home),
    ORDERS("Órdenes", Icons.AutoMirrored.Outlined.Assignment),
    SCHEDULE("Horario", Icons.Outlined.CalendarMonth),
    PROFILE("Perfil", Icons.Outlined.AccountCircle),
}

/**
 * Main navigation of the app. For now only Órdenes works; Inicio, Horario and Perfil are placeholders, and
 * Perfil lets the account sign out.
 */
@Composable
fun MainScreen(session: Session, onOpenOrder: (Long) -> Unit, onSignOut: () -> Unit) {
    val technician = session.role == UserRole.TECHNICIAN
    var tab by rememberSaveable { mutableStateOf(if (technician) MainTab.ORDERS else MainTab.HOME) }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Navy,
                            selectedTextColor = Navy,
                            indicatorColor = Teal,
                            unselectedIconColor = InkMuted,
                            unselectedTextColor = InkMuted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                MainTab.ORDERS -> if (technician) {
                    OrdersScreen(onOpenOrder = onOpenOrder, onOpenProfile = { tab = MainTab.PROFILE })
                } else {
                    ComingSoon(Icons.AutoMirrored.Outlined.Assignment, "Órdenes", "Aquí verás las visitas técnicas de tu hogar.")
                }
                MainTab.HOME -> {
                    ScreenHeader(title = "Inicio", subtitle = null, onOpenProfile = { tab = MainTab.PROFILE },
                        overline = if (technician) "Técnico en campo · Alivia" else "Alivia")
                    ComingSoon(Icons.Outlined.Home, "Muy pronto", "Esta sección estará disponible en una próxima versión.")
                }
                MainTab.SCHEDULE -> {
                    ScreenHeader(title = "Horario", subtitle = null, onOpenProfile = { tab = MainTab.PROFILE })
                    ComingSoon(Icons.Outlined.CalendarMonth, "Muy pronto", "Esta sección estará disponible en una próxima versión.")
                }
                MainTab.PROFILE -> {
                    ScreenHeader(title = "Perfil", subtitle = session.email, onOpenProfile = {})
                    ComingSoon(Icons.Outlined.AccountCircle, session.email,
                        if (technician) "Cuenta de técnico" else "Cuenta de cuidador", Modifier.weight(1f))
                    OutlinedButton(onClick = onSignOut, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                        Text("  Cerrar sesión", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

