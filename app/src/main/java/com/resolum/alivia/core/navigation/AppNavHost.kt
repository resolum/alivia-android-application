package com.resolum.alivia.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.resolum.alivia.features.iam.di.AuthModule
import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.presentation.LoginScreen
import com.resolum.alivia.features.iam.presentation.SplashScreen
import com.resolum.alivia.features.operations.presentation.completed.OrderCompletedScreen
import com.resolum.alivia.features.operations.presentation.detail.OrderDetailScreen
import com.resolum.alivia.features.operations.presentation.evaluation.EvaluationNext
import com.resolum.alivia.features.operations.presentation.evaluation.EvaluationScreen
import com.resolum.alivia.features.operations.presentation.installation.InstallationScreen
import com.resolum.alivia.features.operations.presentation.maintenance.ResolveIncidentScreen
import com.resolum.alivia.features.shared.di.SharedModule
import com.resolum.alivia.features.shared.presentation.MainScreen
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val ORDER = "order/{orderId}"
    const val EVALUATION = "order/{orderId}/evaluation"
    const val INSTALLATION = "order/{orderId}/installation"
    const val COMPLETED = "order/{orderId}/completed"
    const val RESOLVE = "order/{orderId}/resolve"

    fun order(id: Long) = "order/$id"
    fun evaluation(id: Long) = "order/$id/evaluation"
    fun installation(id: Long) = "order/$id/installation"
    fun completed(id: Long) = "order/$id/completed"
    fun resolve(id: Long) = "order/$id/resolve"
}

private val orderArgument = listOf(navArgument("orderId") { type = NavType.LongType })

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    var session by remember { mutableStateOf<Session?>(null) }
    val scope = rememberCoroutineScope()

    fun goToLogin() {
        session = null
        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
    }

    // The refresh token expired: back to the sign-in screen.
    LaunchedEffect(Unit) {
        SharedModule.sessionExpired.collect { goToLogin() }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen { restored ->
                session = restored
                navController.navigate(if (restored != null) Routes.MAIN else Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }
        composable(Routes.LOGIN) {
            LoginScreen(onSignedIn = { signedIn ->
                session = signedIn
                navController.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.MAIN) {
            val current = session
            if (current == null) {
                LaunchedEffect(Unit) { goToLogin() }
            } else {
                MainScreen(
                    session = current,
                    onOpenOrder = { navController.navigate(Routes.order(it)) },
                    onSignOut = { scope.launch { AuthModule.signOutUseCase()(); goToLogin() } },
                )
            }
        }
        composable(Routes.ORDER, arguments = orderArgument) { entry ->
            val orderId = entry.arguments?.getLong("orderId") ?: return@composable
            OrderDetailScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() },
                onOpenEvaluation = { navController.navigate(Routes.evaluation(it)) },
                onOpenInstallation = { navController.navigate(Routes.installation(it)) },
                onOpenCompleted = { navController.navigate(Routes.completed(it)) },
                onOpenResolve = { navController.navigate(Routes.resolve(it)) },
            )
        }
        composable(Routes.EVALUATION, arguments = orderArgument) { entry ->
            val orderId = entry.arguments?.getLong("orderId") ?: return@composable
            EvaluationScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() },
                onNext = { next ->
                    val route = when (next) {
                        is EvaluationNext.Completed -> Routes.completed(next.orderId)
                    }
                    navController.navigate(route) { popUpTo(Routes.MAIN) }
                },
            )
        }
        composable(Routes.INSTALLATION, arguments = orderArgument) { entry ->
            val orderId = entry.arguments?.getLong("orderId") ?: return@composable
            InstallationScreen(
                orderId = orderId,
                onExit = { navController.popBackStack() },
                onCompleted = { navController.navigate(Routes.completed(it)) { popUpTo(Routes.MAIN) } },
            )
        }
        composable(Routes.RESOLVE, arguments = orderArgument) { entry ->
            val orderId = entry.arguments?.getLong("orderId") ?: return@composable
            ResolveIncidentScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() },
                onResolved = { navController.navigate(Routes.completed(it)) { popUpTo(Routes.MAIN) } },
            )
        }
        composable(Routes.COMPLETED, arguments = orderArgument) { entry ->
            val orderId = entry.arguments?.getLong("orderId") ?: return@composable
            OrderCompletedScreen(
                orderId = orderId,
                onBackToOrders = { navController.popBackStack(Routes.MAIN, inclusive = false) },
                onOpenSummary = { navController.navigate(Routes.order(it)) { popUpTo(Routes.MAIN) } },
                onOpenInstallation = { navController.navigate(Routes.order(it)) { popUpTo(Routes.MAIN) } },
            )
        }
    }
}
