package com.hyperdeck.ui.theme

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

sealed interface UiTransitionRequest {
    val token: Long
    val origin: Offset
    val minActivityToken: Int
}

data class ThemeTransitionRequest(
    override val token: Long,
    override val origin: Offset,
    override val minActivityToken: Int,
    val overlayColor: Int
) : UiTransitionRequest

data class LanguageTransitionRequest(
    override val token: Long,
    override val origin: Offset,
    override val minActivityToken: Int,
    val screenshot: Bitmap
) : UiTransitionRequest

class UiTransitionManager {

    private var nextTransitionToken = 0L
    private var nextActivityToken = 0
    private val startedByActivityToken = mutableMapOf<Long, Int>()

    var currentActivityToken: Int = 0
        private set

    var activeRequest: UiTransitionRequest? by mutableStateOf(null)
        private set

    fun registerActivity(): Int {
        currentActivityToken = ++nextActivityToken
        return currentActivityToken
    }

    fun startThemeTransition(origin: Offset, overlayColor: Int) {
        replaceRequest(
            ThemeTransitionRequest(
                token = ++nextTransitionToken,
                origin = origin,
                minActivityToken = currentActivityToken,
                overlayColor = overlayColor
            )
        )
    }

    fun startLanguageTransition(origin: Offset, screenshot: Bitmap) {
        replaceRequest(
            LanguageTransitionRequest(
                token = ++nextTransitionToken,
                origin = origin,
                minActivityToken = currentActivityToken + 1,
                screenshot = screenshot
            )
        )
    }

    fun consumeForActivity(activityToken: Int): UiTransitionRequest? {
        val request = activeRequest ?: return null
        if (activityToken < request.minActivityToken) return null
        if (startedByActivityToken[request.token] == activityToken) return null
        startedByActivityToken[request.token] = activityToken
        return request
    }

    fun clear(token: Long) {
        val request = activeRequest
        if (request?.token == token) {
            if (request is LanguageTransitionRequest && !request.screenshot.isRecycled) {
                request.screenshot.recycle()
            }
            activeRequest = null
        }
        startedByActivityToken.remove(token)
    }

    private fun replaceRequest(newRequest: UiTransitionRequest) {
        activeRequest?.let { previous ->
            if (previous is LanguageTransitionRequest && !previous.screenshot.isRecycled) {
                previous.screenshot.recycle()
            }
            startedByActivityToken.remove(previous.token)
        }
        activeRequest = newRequest
    }
}
