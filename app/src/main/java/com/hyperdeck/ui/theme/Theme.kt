package com.hyperdeck.ui.theme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.hypot

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
fun AttachNativeRevealTransition(
    hostView: View,
    manager: UiTransitionManager,
    activityToken: Int
) {
    val request = manager.activeRequest

    LaunchedEffect(request?.token, activityToken) {
        val transition = manager.consumeForActivity(activityToken) ?: return@LaunchedEffect
        hostView.post {
            playNativeReveal(
                hostView = hostView,
                request = transition,
                onFinished = { manager.clear(transition.token) }
            )
        }
    }
}

private fun playNativeReveal(
    hostView: View,
    request: UiTransitionRequest,
    onFinished: () -> Unit
) {
    val overlayHost = hostView.rootView as? ViewGroup
    if (overlayHost == null || overlayHost.width == 0 || overlayHost.height == 0) {
        onFinished()
        return
    }

    val overlay = overlayHost.overlay
    val overlayView = when (request) {
        is ThemeTransitionRequest -> View(hostView.context).apply {
            setBackgroundColor(request.overlayColor)
        }

        is LanguageTransitionRequest -> ImageView(hostView.context).apply {
            setImageBitmap(request.screenshot)
            scaleType = ImageView.ScaleType.FIT_XY
        }
    }

    overlayView.layout(0, 0, overlayHost.width, overlayHost.height)
    overlay.add(overlayView)

    val centerX = request.origin.x.coerceIn(0f, overlayHost.width.toFloat()).toInt()
    val centerY = request.origin.y.coerceIn(0f, overlayHost.height.toFloat()).toInt()
    val startRadius = listOf(
        hypot(centerX.toDouble(), centerY.toDouble()),
        hypot((overlayHost.width - centerX).toDouble(), centerY.toDouble()),
        hypot(centerX.toDouble(), (overlayHost.height - centerY).toDouble()),
        hypot(
            (overlayHost.width - centerX).toDouble(),
            (overlayHost.height - centerY).toDouble()
        )
    ).maxOrNull()?.toFloat() ?: 0f

    val animator = ViewAnimationUtils.createCircularReveal(
        overlayView,
        centerX,
        centerY,
        startRadius,
        0f
    )
    animator.duration = if (request is LanguageTransitionRequest) 360L else 420L
    animator.interpolator = FastOutSlowInInterpolator()
    animator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            overlay.remove(overlayView)
            onFinished()
        }
    })
    animator.start()
}

fun Color.toOverlayArgb(): Int = toArgb()
