package com.raxdenstudios.platform.device.inapp

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

/**
 * Firebase Analytics reporting for [InAppReview], injected into [ReleaseInAppReview] (the debug
 * variant does not track). Every method logs one `in_app_review_*` event; parameters are
 * documented on each method.
 */
internal interface InAppReviewTracker {

    /**
     * Fires `in_app_review_launched` when [InAppReview.launch] succeeds — Play's review flow was
     * invoked. As with [InAppReview] itself, this does not guarantee the dialog was displayed
     * (Play may skip it due to quota).
     */
    fun launched()

    /**
     * Fires `in_app_review_failed` when [InAppReview.launch] returns a failure. The [reason] is
     * sent lowercase as the `reason` param.
     */
    fun failed(reason: Reason)

    /**
     * Why the review flow could not be launched; fixed codes reported as the `reason` param of
     * `in_app_review_failed`.
     */
    enum class Reason {

        /** No activity is attached to [InAppReview]. */
        NO_ACTIVITY,

        /** The Play review request itself failed. */
        ERROR,
    }
}

/** [InAppReviewTracker] backed by [FirebaseAnalytics]; reason codes are sent lowercase. */
internal class InAppReviewTrackerImpl(
    private val firebaseAnalytics: FirebaseAnalytics,
) : InAppReviewTracker {

    override fun launched() {
        firebaseAnalytics.logEvent(IN_APP_REVIEW_LAUNCHED) {}
    }

    override fun failed(reason: InAppReviewTracker.Reason) {
        firebaseAnalytics.logEvent(IN_APP_REVIEW_FAILED) {
            param("reason", reason.name.lowercase())
        }
    }

    private companion object {
        const val IN_APP_REVIEW_LAUNCHED = "in_app_review_launched"
        const val IN_APP_REVIEW_FAILED = "in_app_review_failed"
    }
}
