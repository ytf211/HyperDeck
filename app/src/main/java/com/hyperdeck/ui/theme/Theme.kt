package com.hyperdeck.ui.theme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
fun RevealTransitionHost(
    manager: UiTransitionManager,
    activityToken: Int,
    content: @Composable () -> Unit
) {
    val request = manager.activeRequest

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (request != null && activityToken >= request.minActivityToken) {
            NativeCircularRevealOverlay(
                request = request,
                onFinished = { manager.clear(request.token) }
            )
        }
    }
}

@Composable
private fun NativeCircularRevealOverlay(
    request: UiTransitionRequest,
    onFinished: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                val imageView = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.FIT_XY
                    setImageBitmap(request.screenshot)
                }
                addView(imageView)
            }
        },
        update = { container ->
            val imageView = container.getChildAt(0) as ImageView
            imageView.setImageBitmap(request.screenshot)

            if (container.getTag(request.token.hashCode()) == true) {
                return@AndroidView
            }

            container.setTag(request.token.hashCode(), true)
            container.post {
                val centerX = request.origin.x.coerceIn(0f, container.width.toFloat()).toInt()
                val centerY = request.origin.y.coerceIn(0f, container.height.toFloat()).toInt()
                val finalRadius = listOf(
                    hypot(centerX.toDouble(), centerY.toDouble()),
                    hypot((container.width - centerX).toDouble(), centerY.toDouble()),
                    hypot(centerX.toDouble(), (container.height - centerY).toDouble()),
                    hypot(
                        (container.width - centerX).toDouble(),
                        (container.height - centerY).toDouble()
                    )
                ).maxOrNull()?.toFloat() ?: 0f

                val animator = ViewAnimationUtils.createCircularReveal(
                    imageView,
                    centerX,
                    centerY,
                    finalRadius,
                    0f
                )
                animator.duration = 520L
                animator.interpolator = FastOutSlowInInterpolator()
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        imageView.visibility = View.INVISIBLE
                        onFinished()
                    }
                })
                animator.start()
            }
        }
    )
}
