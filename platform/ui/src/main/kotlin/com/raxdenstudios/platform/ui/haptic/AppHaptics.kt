package com.raxdenstudios.platform.ui.haptic

import android.annotation.SuppressLint
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Single entry point for haptics. Each method names a kind of feedback and picks the closest
 * [HapticFeedbackConstants] for the device: `CONFIRM`/`REJECT` from API 30, `SEGMENT_FREQUENT_TICK` from API 34 and
 * older constants below. The system "touch feedback" setting is respected; [isEnabled] lets an app switch haptics off.
 *
 * ## Usage
 *
 * Inside composition, get the instance bound to the current view with [rememberAppHaptics]:
 *
 * ```kotlin
 * val haptics = rememberAppHaptics()
 * Button(onClick = {
 *     haptics.confirm()
 *     onSave()
 * }) { /* ... */ }
 * ```
 *
 * Outside composition, construct it with the view that should perform the feedback: `AppHaptics(view)`.
 */
@Stable
class AppHaptics(
    private val view: View,
    private val isEnabled: Boolean = true,
) {

    /** A light tap: a button pressed, a value placed or changed. */
    fun tap() = perform(HapticEvent.TAP)

    /** A very soft tap: a value removed. */
    fun softTap() = perform(HapticEvent.SOFT_TAP)

    /** One tick per step of a continuous gesture, such as a drag over several cells. */
    fun tick() = perform(HapticEvent.TICK)

    /** Something went right: a step completed, a reward earned. */
    fun confirm() = perform(HapticEvent.CONFIRM)

    /** Something went wrong: a rule broken, a mistake pointed out. */
    fun error() = perform(HapticEvent.ERROR)

    /** The action is not allowed: a locked element was touched. */
    fun reject() = perform(HapticEvent.REJECT)

    private fun perform(
        event: HapticEvent,
    ) {
        if (isEnabled) view.performHapticFeedback(event.toFeedbackConstant(Build.VERSION.SDK_INT))
    }
}

/** [AppHaptics] bound to the current view. */
@Composable
fun rememberAppHaptics(
    isEnabled: Boolean = true,
): AppHaptics {
    val view = LocalView.current
    return remember(view, isEnabled) { AppHaptics(view, isEnabled) }
}

/** The kinds of feedback [AppHaptics] can perform. */
internal enum class HapticEvent {
    TAP,
    SOFT_TAP,
    TICK,
    CONFIRM,
    ERROR,
    REJECT,
}

/** The [HapticFeedbackConstants] value for this event on a device running [sdkInt]. */
@SuppressLint("InlinedApi") // Every constant newer than minSdk is guarded by the sdkInt checks below.
internal fun HapticEvent.toFeedbackConstant(
    sdkInt: Int,
): Int = when (this) {
    HapticEvent.TAP -> HapticFeedbackConstants.VIRTUAL_KEY

    HapticEvent.SOFT_TAP -> HapticFeedbackConstants.CLOCK_TICK

    HapticEvent.TICK -> if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }

    HapticEvent.CONFIRM -> if (sdkInt >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.KEYBOARD_TAP
    }

    HapticEvent.ERROR -> if (sdkInt >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.REJECT
    } else {
        HapticFeedbackConstants.LONG_PRESS
    }

    HapticEvent.REJECT -> if (sdkInt >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.REJECT
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }
}
