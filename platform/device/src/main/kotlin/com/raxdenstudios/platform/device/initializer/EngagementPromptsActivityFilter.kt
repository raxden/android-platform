package com.raxdenstudios.platform.device.initializer

import android.app.Activity

/**
 * Decides which activities [EngagementPromptsInitializer] attaches the engagement prompts to.
 *
 * Optional: when no binding is found in Koin, [All] is used and every created
 * [androidx.activity.ComponentActivity] gets the prompts. Bind one from the app to keep the prompts
 * off screens where they never make sense (for example a full-screen alarm shown over the lock
 * screen). A milestone reached while such a screen is in front stays pending in the engagement
 * engine and is shown on a later screen that does host the prompts.
 *
 * ## Usage
 *
 * ```kotlin
 * module {
 *     single { EngagementPromptsActivityFilter { activity -> activity !is AlarmRingingActivity } }
 * }
 * ```
 */
fun interface EngagementPromptsActivityFilter {

    /**
     * Whether the engagement prompts are attached to [activity].
     *
     * The filter instance is read from Koin once, by [EngagementPromptsInitializer]; this method
     * runs for every created [androidx.activity.ComponentActivity].
     */
    fun accepts(
        activity: Activity,
    ): Boolean

    companion object {

        /** Accepts every activity; the default when the app binds no filter. */
        val All: EngagementPromptsActivityFilter = EngagementPromptsActivityFilter { true }
    }
}
