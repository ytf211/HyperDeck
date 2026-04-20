package com.hyperdeck.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset

data class UiTransitionRequest(
    val token: Long,
    val screenshot: Bitmap,
    val origin: Offset,
    val minActivityToken: Int
)

class UiTransitionManager {

    private var nextTransitionToken = 0L
    private var nextActivityToken = 0

    var currentActivityToken: Int = 0
        private set

    var activeRequest: UiTransitionRequest? = null
        private set

    fun registerActivity(): Int {
        currentActivityToken = ++nextActivityToken
        return currentActivityToken
    }

    fun startThemeTransition(origin: Offset, screenshot: Bitmap) {
        replaceRequest(
            UiTransitionRequest(
                token = ++nextTransitionToken,
                screenshot = screenshot,
                origin = origin,
                minActivityToken = currentActivityToken
            )
        )
    }

    fun startLanguageTransition(origin: Offset, screenshot: Bitmap) {
        replaceRequest(
            UiTransitionRequest(
                token = ++nextTransitionToken,
                screenshot = screenshot,
                origin = origin,
                minActivityToken = currentActivityToken + 1
            )
        )
    }

    fun clear(token: Long) {
        val request = activeRequest
        if (request?.token == token) {
            request.screenshot.recycleSafely()
            activeRequest = null
        }
    }

    private fun replaceRequest(newRequest: UiTransitionRequest) {
        activeRequest?.screenshot.recycleSafely()
        activeRequest = newRequest
    }

    private fun Bitmap.recycleSafely() {
        if (!isRecycled) {
            recycle()
        }
    }
}
