package com.resolum.alivia.features.iam.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resolum.alivia.core.ui.components.AliviaLogo
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.features.iam.di.AuthModule
import com.resolum.alivia.features.iam.domain.models.Session
import kotlinx.coroutines.delay

/** Restores the saved session and decides where the app starts. */
@Composable
fun SplashScreen(onResolved: (Session?) -> Unit) {
    LaunchedEffect(Unit) {
        val session = AuthModule.restoreSessionUseCase()()
        delay(600)
        onResolved(session)
    }
    Box(Modifier.fillMaxSize().background(Background).systemBarsPadding()) {
        AliviaLogo(Modifier.align(Alignment.Center), markSize = 40.dp, textSize = MaterialTheme.typography.headlineMedium.fontSize)
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), color = TealDark, strokeWidth = 2.dp)
            Text("Iniciando sistema...", style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
    }
}
