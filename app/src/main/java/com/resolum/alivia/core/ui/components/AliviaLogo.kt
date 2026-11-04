package com.resolum.alivia.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resolum.alivia.core.ui.theme.Navy

/** Official Alivia person/heart mark next to the wordmark. */
@Composable
fun AliviaLogo(modifier: Modifier = Modifier, markSize: Dp = 28.dp, textSize: TextUnit = 28.sp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(markSize / 3f)) {
        Canvas(Modifier.size(markSize)) {
            val stroke = size.minDimension * 0.105f
            val teal = Color(0xFF29C3B5)
            val outline = Stroke(width = stroke, cap = StrokeCap.Round)

            drawCircle(
                color = teal,
                radius = size.minDimension * 0.17f,
                center = Offset(size.width * 0.5f, size.height * 0.2f),
                style = outline,
            )

            val left = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.35f)
                cubicTo(
                    size.width * 0.02f, size.height * 0.37f,
                    size.width * 0.02f, size.height * 0.64f,
                    size.width * 0.47f, size.height * 0.91f,
                )
            }
            drawPath(left, color = teal, style = outline)

            val right = Path().apply {
                moveTo(size.width * 0.72f, size.height * 0.35f)
                cubicTo(
                    size.width * 0.98f, size.height * 0.37f,
                    size.width * 0.98f, size.height * 0.64f,
                    size.width * 0.53f, size.height * 0.91f,
                )
            }
            drawPath(right, color = Navy, style = outline)
        }
        Text("Alivia", color = Navy, fontSize = textSize, fontWeight = FontWeight.Normal)
    }
}
