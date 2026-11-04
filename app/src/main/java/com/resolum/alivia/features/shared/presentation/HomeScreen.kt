package com.resolum.alivia.features.shared.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavySoft

/** Placeholder for the sections that are not built yet. */
@Composable
fun ComingSoon(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(80.dp).clip(CircleShape).background(NavySoft), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(36.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = Navy, textAlign = TextAlign.Center)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = InkMuted, textAlign = TextAlign.Center)
        }
    }
}
