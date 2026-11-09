package com.raxdenstudios.platform.device

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.raxdenstudios.commons.android.ActivityHolder
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.core.util.IntentFactoryImpl
import com.raxdenstudios.platform.core.util.IntentType
import timber.log.Timber

/**
 * Google Play in-app review.
 *
 * The review dialog can only be shown a limited number of times per user (a quota controlled
 * by Play) and Play never reports whether it was actually displayed, so [Answer.Success] does
 * not guarantee the dialog appeared. The [ReviewInfo] needed to launch is prefetched when an
 * activity is attached, as Google recommends.
 *
 * ## Setup
 *
 * Bind the running activities once, e.g. in `Application.ActivityLifecycleCallbacks`:
 *
 * ```kotlin
 * override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
 *     if (activity !is ComponentActivity) return
 *     inAppReview.attach(activity)
 * }
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * safeLaunch {
 *     inAppReview.launch().onFailure {
 *         // The flow could not even start; offer the manual route instead.
 *         context.startActivity(intentFactory.build(IntentType.PlayStore))
 *     }
 *     // Do not treat Success as "the user saw the dialog" — Play quota may have skipped it.
 * }
 * ```
 */
interface InAppReview : DefaultLifecycleObserver {

    /**
     * Binds this helper to [activity] and follows its lifecycle so a [ReviewInfo] is prefetched
     * on create, as Google recommends. The activity is held weakly, so no detach is required.
     */
    fun attach(activity: ComponentActivity)

    /**
     * Launches the Google Play in-app review flow on the attached activity.
     *
     * Play never reports whether the dialog was actually displayed (it may be skipped due to
     * quota), so [Answer.Success] only means the flow was invoked. [Answer.Failure] means it could
     * not be invoked at all (no activity attached or request failure); callers may use it to fall
     * back to opening the Play Store page.
     */
    suspend fun launch(): Answer<Unit, Failure>
}

/**
 * Debug builds are not installed through Play, so the review flow cannot run there; this
 * variant goes straight to the Play Store page so the entry point can still be exercised.
 */
internal class DebugInAppReview(
    private val context: Context,
) : InAppReview {

    private val intentFactory = IntentFactoryImpl(context.applicationContext)

    override fun attach(activity: ComponentActivity) = Unit

    override suspend fun launch(): Answer<Unit, Failure> =
        coRunCatching {
            Timber.d("Debug mode: Opening Play Store directly")
            context.startActivity(intentFactory.build(IntentType.PlayStore))
        }.thenFailure { error ->
            Failure.Unknown("Play Store could not be opened: ${error.message}")
        }
}

internal class ReleaseInAppReview(
    context: Context,
) : InAppReview {

    private val activityHolder = ActivityHolder()
    private val manager = ReviewManagerFactory.create(context.applicationContext)

    private var reviewInfo: ReviewInfo? = null

    override fun attach(activity: ComponentActivity) {
        if (activityHolder.activity === activity) return

        activityHolder.attach(activity)
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        prefetchReviewInfo()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (activityHolder.activity === owner) detach()
    }

    private fun detach() {
        activityHolder.activity?.let { activity ->
            activity.lifecycle.removeObserver(this)
            activityHolder.detach(activity)
        }
    }

    override suspend fun launch(): Answer<Unit, Failure> {
        val activity = activityHolder.activity
            ?: return Answer.Failure(Failure.ResourceNotFound("No activity attached"))
        return launch(activity)
    }

    private suspend fun launch(activity: ComponentActivity): Answer<Unit, Failure> =
        coRunCatching { manager.launchReview(activity, consumeReviewInfo()) }
            .thenFailure { error ->
                Timber.e(error, "In-app review flow could not be launched")
                Failure.Unknown("In-app review flow could not be launched: ${error.message}")
            }

    /**
     * ReviewInfo is single-use and short-lived: returns the cached one (or requests a new one)
     * and drops it so the next launch starts from a fresh request.
     */
    private suspend fun consumeReviewInfo(): ReviewInfo {
        val info = reviewInfo ?: manager.requestReview()
        reviewInfo = null
        return info
    }

    /**
     * Requesting a [ReviewInfo] takes noticeable time, so it is fetched ahead of [launch] on
     * activity create (Google's recommendation) and cached until consumed. The guard avoids a
     * second request when several activities are attached in sequence.
     */
    private fun prefetchReviewInfo() {
        if (reviewInfo != null) return
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) reviewInfo = task.result
        }
    }
}
