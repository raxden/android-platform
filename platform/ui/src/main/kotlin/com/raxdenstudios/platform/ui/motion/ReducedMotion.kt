package com.raxdenstudios.platform.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

private const val DEFAULT_ANIMATOR_DURATION_SCALE = 1f

/**
 * Whether the system turned animations off (`Settings.Global.ANIMATOR_DURATION_SCALE` is 0). Screens should then
 * keep only short fades and skip decorative motion. Previews always animate.
 *
 * The setting is read once and remembered for this composition (and context); it is not observed. A change made
 * in the system settings while the screen is shown applies the next time the composable enters composition, such
 * as after navigating away and back or a configuration change.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    return remember(context, isPreview) { isPreview.not() && isReducedMotion(context) }
}

/**
 * [rememberReducedMotion] for code outside composition, such as modifier nodes or navigation transitions. It reads
 * the setting at the moment it is called (a node reads it on attach or per press) and does not observe it.
 */
fun isReducedMotion(
    context: Context,
): Boolean = isAnimatorDisabled(
    animatorDurationScale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        DEFAULT_ANIMATOR_DURATION_SCALE,
    ),
)

/** True when the animator duration scale set by the system means "animations off". */
internal fun isAnimatorDisabled(
    animatorDurationScale: Float,
): Boolean = animatorDurationScale == 0f
