package com.raxdenstudios.platform.device.initializer

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.startup.Initializer
import com.raxdenstudios.commons.android.ext.registerActivityLifecycleCallbacksOnApplication
import com.raxdenstudios.commons.android.util.ActivityLifecycleCallbacksAdapter
import com.raxdenstudios.platform.device.inapp.InAppReview
import com.raxdenstudios.platform.device.inapp.InAppShare
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * androidx.startup initializer that attaches the engagement prompts ([InAppReview], [InAppShare])
 * to every created [ComponentActivity] accepted by the [EngagementPromptsActivityFilter] bound in
 * Koin ([EngagementPromptsActivityFilter.All] when none is bound). Both must be attached before the
 * activity is STARTED, which `onActivityCreated` guarantees.
 *
 * ## Koin
 *
 * [create] only registers the activity lifecycle callbacks; [InAppReview], [InAppShare] and the
 * filter are resolved from Koin the first time an activity is created, which happens after every
 * initializer and `Application.onCreate` ran. So [dependencies] is empty and this works whoever
 * starts Koin, as long as the app starts it (with `DeviceDILibrary` loaded) in its own initializer
 * or in `Application.onCreate`, before the first activity.
 *
 * The filter is read once, on the first created [ComponentActivity] that reaches it, and kept for
 * the rest of the process: a filter bound later or a Koin reload is not picked up. If Koin is not
 * started at that moment the lookup throws, and it is retried on the next created activity.
 *
 * ## Setup
 *
 * Opt-in: the library manifest does not declare it. Enable it from the app manifest, inside the
 * androidx.startup provider:
 *
 * ```xml
 * <provider
 *     android:name="androidx.startup.InitializationProvider"
 *     android:authorities="${applicationId}.androidx-startup"
 *     android:exported="false"
 *     tools:node="merge">
 *     <meta-data
 *         android:name="com.raxdenstudios.platform.device.initializer.EngagementPromptsInitializer"
 *         android:value="androidx.startup" />
 * </provider>
 * ```
 *
 * App Startup gives no order between initializers that do not depend on each other, so the order
 * in which these callbacks and the app's own activity callbacks run is not defined.
 */
class EngagementPromptsInitializer :
    Initializer<Unit>,
    KoinComponent {

    private val inAppReview: InAppReview by inject()
    private val inAppShare: InAppShare by inject()
    private val activityFilter: EngagementPromptsActivityFilter by lazy {
        getKoin().getOrNull<EngagementPromptsActivityFilter>() ?: EngagementPromptsActivityFilter.All
    }

    override fun create(
        context: Context,
    ) {
        context.registerActivityLifecycleCallbacksOnApplication(EngagementPromptsCallbacks())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private inner class EngagementPromptsCallbacks : ActivityLifecycleCallbacksAdapter() {

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) {
            if (activity !is ComponentActivity || !activityFilter.accepts(activity)) return
            inAppReview.attach(activity)
            inAppShare.attach(activity)
        }
    }
}
