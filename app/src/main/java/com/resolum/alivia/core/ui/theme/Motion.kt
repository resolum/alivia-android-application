package com.resolum.alivia.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * Motion tokens. Everything that enters or answers a tap eases out (fast start, so the screen feels like it heard
 * the user); things that move on screen use ease-in-out. UI motion stays under 300 ms.
 */
object Motion {
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val EaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    const val FAST = 160
    const val BASE = 220
    const val EXIT = 140

    fun <T> enter(): FiniteAnimationSpec<T> = tween(BASE, easing = EaseOut)
    fun <T> exit(): FiniteAnimationSpec<T> = tween(EXIT, easing = EaseOut)
    fun <T> move(): FiniteAnimationSpec<T> = tween(BASE, easing = EaseInOut)
}
