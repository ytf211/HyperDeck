package com.hyperdeck.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

sealed interface UiTransitionRequest {
    val origin: Offset
    val overlayColor: androidx.compose.ui.graphics.Color
    val token: Long

    class Theme(
        override val token: Long,
        override val origin: Offset,
        override val overlayColor: androidx.compose.ui.graphics.Color,
        val applyChange: suspend () -> Unit
    ) : UiTransitionRequest

    class Language(
        override val token: Long,
        override val origin: Offset,
        override val overlayColor: androidx.compose.ui.graphics.Color,
        val applyChange: suspend () -> Unit
    ) : UiTransitionRequest
}

class UiTransitionManager {

    var activeRequest: UiTransitionRequest? by mutableStateOf(null)
        private set

    private var nextToken = 0L

    fun startThemeTransition(
        origin: Offset,
        overlayColor: androidx.compose.ui.graphics.Color,
        applyChange: suspend () -> Unit
    ) {
        activeRequest = UiTransitionRequest.Theme(
            token = ++nextToken,
            origin = origin,
            overlayColor = overlayColor,
            applyChange = applyChange
        )
    }

    fun startLanguageTransition(
        origin: Offset,
        overlayColor: androidx.compose.ui.graphics.Color,
        applyChange: suspend () -> Unit
    ) {
        activeRequest = UiTransitionRequest.Language(
            token = ++nextToken,
            origin = origin,
            overlayColor = overlayColor,
            applyChange = applyChange
        )
    }

    fun clear(token: Long) {
        if (activeRequest?.token == token) {
            activeRequest = null
        }
    }
}
