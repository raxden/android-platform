package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.raxdenstudios.platform.ui.haptic.rememberAppHaptics
import com.raxdenstudios.platform.ui.motion.pressScale

private const val NO_SCALE = 1f

/**
 * How [Button] and [IconButton] react to a touch. A scale of 1 keeps the element still; below 1 it shrinks to that
 * scale while pressed (see [pressScale]). With [hapticsEnabled] every click performs a light haptic tap.
 *
 * [None], the default of [LocalPressFeedback], adds no feedback. An app enables it once at its theme root:
 *
 * ```kotlin
 * CompositionLocalProvider(
 *     LocalPressFeedback provides PressFeedback(
 *         buttonPressedScale = 0.96f,
 *         iconButtonPressedScale = 0.88f,
 *         hapticsEnabled = true,
 *     ),
 * ) { content() }
 * ```
 *
 * Not a data class on purpose: fields can be added later without breaking binary compatibility (no `copy` nor
 * `componentN`).
 *
 * @property buttonPressedScale scale [Button] animates to while pressed; 1 leaves buttons still.
 * @property iconButtonPressedScale scale [IconButton] animates to while pressed; 1 leaves icon buttons still.
 * @property hapticsEnabled whether every click of [Button] and [IconButton] performs a light haptic tap.
 */
@Immutable
class PressFeedback(
    val buttonPressedScale: Float = NO_SCALE,
    val iconButtonPressedScale: Float = NO_SCALE,
    val hapticsEnabled: Boolean = false,
) {

    override fun equals(
        other: Any?,
    ): Boolean {
        if (this === other) return true
        if (other !is PressFeedback) return false
        return buttonPressedScale == other.buttonPressedScale &&
            iconButtonPressedScale == other.iconButtonPressedScale &&
            hapticsEnabled == other.hapticsEnabled
    }

    override fun hashCode(): Int {
        var result = buttonPressedScale.hashCode()
        result = HASH_MULTIPLIER * result + iconButtonPressedScale.hashCode()
        result = HASH_MULTIPLIER * result + hapticsEnabled.hashCode()
        return result
    }

    override fun toString(): String = "PressFeedback(" +
        "buttonPressedScale=$buttonPressedScale, " +
        "iconButtonPressedScale=$iconButtonPressedScale, " +
        "hapticsEnabled=$hapticsEnabled)"

    companion object {

        private const val HASH_MULTIPLIER = 31

        /** No scale and no haptics. */
        val None = PressFeedback()
    }
}

/**
 * The [PressFeedback] read by [Button] and [IconButton]; [PressFeedback.None] unless an app provides one. Provide it
 * once at the theme root, so every button of the app opts in without per-call parameters:
 *
 * ```kotlin
 * CompositionLocalProvider(
 *     LocalPressFeedback provides PressFeedback(buttonPressedScale = 0.96f, hapticsEnabled = true),
 * ) {
 *     AppNavHost()
 * }
 * ```
 */
val LocalPressFeedback = staticCompositionLocalOf { PressFeedback.None }

/** Whether [pressedScale] makes the element move at all. */
internal fun scalesOnPress(
    pressedScale: Float,
): Boolean = pressedScale != NO_SCALE

/** [pressScale] to [pressedScale], or nothing when the scale would not change the element. */
internal fun Modifier.pressFeedbackScale(
    interactionSource: InteractionSource,
    pressedScale: Float,
): Modifier = if (scalesOnPress(pressedScale)) pressScale(interactionSource, pressedScale) else this

/**
 * [onClick] preceded by a haptic tap when [LocalPressFeedback] enables haptics. The returned lambda keeps its identity
 * across recompositions and always calls the latest [onClick]; haptics are only looked up while enabled.
 */
@Composable
internal fun rememberPressFeedbackClick(
    onClick: () -> Unit,
): () -> Unit {
    val currentOnClick by rememberUpdatedState(onClick)
    val haptics = if (LocalPressFeedback.current.hapticsEnabled) rememberAppHaptics() else null
    val currentHaptics by rememberUpdatedState(haptics)
    return remember {
        {
            currentHaptics?.tap()
            currentOnClick()
        }
    }
}
