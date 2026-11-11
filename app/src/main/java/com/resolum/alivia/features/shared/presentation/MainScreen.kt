package com.resolum.alivia.features.shared.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.Teal
import com.resolum.alivia.features.devices.presentation.DevicesScreen
import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.models.UserRole
import com.resolum.alivia.features.nursing.presentation.activities.ActivitiesScreen
import com.resolum.alivia.features.nursing.presentation.home.CaregiverHomeScreen
import com.resolum.alivia.features.operations.presentation.home.TechnicianHomeScreen
import com.resolum.alivia.features.operations.presentation.orders.OrdersScreen
import com.resolum.alivia.features.operations.presentation.orders.ScreenHeader
import com.resolum.alivia.features.operations.presentation.schedule.ScheduleScreen
import com.resolum.alivia.features.profiles.presentation.ProfileScreen

@Suppress("DEPRECATION")
enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Outlined.Home),
    ORDERS("Órdenes", Icons.AutoMirrored.Outlined.Assignment),
    SCHEDULE("Horario", Icons.Outlined.CalendarMonth),
    ACTIVITIES("Actividades", Icons.Outlined.EventNote),
    DEVICES("Dispositivos", Icons.Outlined.Sensors),
    HISTORY("Historial", Icons.Outlined.History),
    PROFILE("Perfil", Icons.Outlined.AccountCircle);

    companion object {
        /** Tabs of the bottom bar for each role of the mobile app. */
        fun tabsFor(role: UserRole): List<MainTab> = if (role == UserRole.TECHNICIAN) {
            listOf(HOME, ORDERS, SCHEDULE, PROFILE)
        } else {
            listOf(HOME, ACTIVITIES, DEVICES, HISTORY, PROFILE)
        }
    }
}

/** Main navigation of the app, adapted to the role of the signed-in account. */
@Composable
fun MainScreen(session: Session, onOpenOrder: (Long) -> Unit, onOpenNotifications: () -> Unit, onSignOut: () -> Unit) {
    val technician = session.role == UserRole.TECHNICIAN
    val tabs = MainTab.tabsFor(session.role)
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val openNotifications = onOpenNotifications

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                tabs.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible) },
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
                MainTab.HOME -> if (technician) {
                    TechnicianHomeScreen(
                        onOpenOrder = onOpenOrder,
                        onOpenSchedule = { tab = MainTab.SCHEDULE },
                        onOpenOrders = { tab = MainTab.ORDERS },
                        onOpenNotifications = openNotifications,
                    )
                } else {
                    CaregiverHomeScreen(onOpenActivities = { tab = MainTab.ACTIVITIES }, onOpenNotifications = openNotifications)
                }
                MainTab.ORDERS -> OrdersScreen(onOpenOrder = onOpenOrder, onOpenProfile = openNotifications)
                MainTab.SCHEDULE -> ScheduleScreen(onOpenProfile = openNotifications)
                MainTab.ACTIVITIES -> ActivitiesScreen(onOpenNotifications = openNotifications)
                MainTab.DEVICES -> DevicesScreen(onOpenNotifications = openNotifications)
                MainTab.HISTORY -> {
                    ScreenHeader(title = "Historial", subtitle = null, onOpenProfile = openNotifications)
                    ComingSoon(Icons.Outlined.History, "Muy pronto",
                        "Aquí verás los eventos del hogar: comandos de voz, actividades y alertas.")
                }
                MainTab.PROFILE -> ProfileScreen(session = session, onSignOut = onSignOut)
            }
        }
    }
}
