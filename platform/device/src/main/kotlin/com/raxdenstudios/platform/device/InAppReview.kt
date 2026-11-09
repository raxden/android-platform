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
     * not be invoked at all (no activity attached, debuggable build or request failure); callers
     * may use it to fall back to opening the Play Store page.
     */
    suspend fun launch(): Answer<Unit, Failure>
}

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
        coRunCatching {
            // ReviewInfo is single-use and short-lived: consume the cached one (or request a new
            // one) and drop it so the next launch starts from a fresh request.
            val info = reviewInfo ?: manager.requestReview()
            reviewInfo = null
            manager.launchReview(activity, info)
        }.thenFailure { error ->
            Timber.e(error, "In-app review flow could not be launched")
            Failure.Unknown("In-app review flow could not be launched: ${error.message}")
        }

    private fun prefetchReviewInfo() {
        if (reviewInfo != null) return
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) reviewInfo = task.result
        }
    }
}
