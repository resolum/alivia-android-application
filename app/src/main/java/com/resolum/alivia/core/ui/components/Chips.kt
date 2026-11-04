package com.resolum.alivia.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Small rounded label, optionally with a colored dot, used for statuses and service types. */
@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier, dot: Color? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot != null) Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Text(text, color = content, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(6.dp).clip(CircleShape).background(color))
}
