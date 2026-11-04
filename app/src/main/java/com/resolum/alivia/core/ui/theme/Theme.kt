package com.resolum.alivia.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AliviaColorScheme = lightColorScheme(
    primary = NavyButton,
    onPrimary = Color.White,
    primaryContainer = NavySoft,
    onPrimaryContainer = Navy,
    secondary = TealDark,
    onSecondary = Color.White,
    secondaryContainer = Teal,
    onSecondaryContainer = Navy,
    background = Background,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkMuted,
    outline = Outline,
    outlineVariant = Outline,
    error = Danger,
    errorContainer = DangerSoft,
)

/** The app always uses the Alivia brand colors, so dynamic color and dark mode are not applied. */
@Composable
fun AliviaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AliviaColorScheme,
        typography = Typography,
        content = content
    )
}
