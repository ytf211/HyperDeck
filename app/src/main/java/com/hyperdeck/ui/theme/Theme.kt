package com.hyperdeck.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.AnimationVector1D
import kotlinx.coroutines.delay

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
)

fun resolveHyperDeckColorScheme(
    context: android.content.Context,
    darkTheme: Boolean,
    dynamicColor: Boolean
): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
}

@Composable
fun HyperDeckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = resolveHyperDeckColorScheme(context, darkTheme, dynamicColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun RevealTransitionHost(
    manager: UiTransitionManager,
    content: @Composable () -> Unit
) {
    val request = manager.activeRequest
    val radius = remember(request?.token) { Animatable(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (request != null) {
            RevealOverlay(
                request = request,
                radius = radius,
                onFinished = { manager.clear(request.token) }
            )
        }
    }
}

private suspend fun UiTransitionRequest.apply() {
    when (this) {
        is UiTransitionRequest.Language -> applyChange()
        is UiTransitionRequest.Theme -> applyChange()
    }
}

@Composable
private fun RevealOverlay(
    request: UiTransitionRequest,
    radius: Animatable<Float, AnimationVector1D>,
    onFinished: () -> Unit
) {
    val animationSpec = remember {
        tween<Float>(durationMillis = 520, easing = FastOutSlowInEasing)
    }
    val holdDelay = if (request is UiTransitionRequest.Language) 160L else 90L
    var canvasSize by remember(request.token) { mutableStateOf(Size.Zero) }

    LaunchedEffect(request.token, canvasSize) {
        if (canvasSize == Size.Zero) return@LaunchedEffect
        val targetRadius = maxRevealRadius(request.origin, canvasSize)
        radius.snapTo(0f)
        radius.animateTo(targetRadius, animationSpec)
        request.apply()
        delay(holdDelay)
        onFinished()
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        canvasSize = size
        drawCircle(
            color = request.overlayColor,
            radius = radius.value,
            center = Offset(
                x = request.origin.x.coerceIn(0f, size.width),
                y = request.origin.y.coerceIn(0f, size.height)
            )
        )
    }
}

private fun maxRevealRadius(origin: Offset, size: Size): Float {
    if (size == Size.Zero) return 0f
    val corners = listOf(
        Offset.Zero,
        Offset(size.width, 0f),
        Offset(0f, size.height),
        Offset(size.width, size.height)
    )
    return corners.maxOf { corner ->
        kotlin.math.hypot((corner.x - origin.x).toDouble(), (corner.y - origin.y).toDouble()).toFloat()
    }
}
