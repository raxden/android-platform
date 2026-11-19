package com.raxdenstudios.platform.device.inapp

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.raxdenstudios.commons.android.ActivityHolder

internal const val NO_RESUMED_ACTIVITY = "No resumed activity attached"

/**
 * The attached activity only while it is in the foreground: one alive but stopped in the
 * background (behind a notification action, the lock screen, background playback) counts as
 * none, so the prompt is never launched where the user cannot see it.
 */
internal fun ActivityHolder.resumedActivity(): ComponentActivity? = activity
    ?.takeIf { it.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
