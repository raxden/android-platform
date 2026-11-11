package com.raxdenstudios.platform.device.inapp

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

/**
 * Firebase Analytics reporting for [InAppShare], injected into [InAppShareImpl]. Every method
 * logs one `in_app_share_*` event; parameters are documented on each method.
 */
internal interface InAppShareTracker {

    /**
     * Fires `in_app_share_prompt_shown` when the native modal asking whether to share the app is
     * displayed, before the system share sheet. No params.
     */
    fun promptShown()

    /**
     * Fires `in_app_share_prompt_accepted` when the user accepts the modal and the system share
     * sheet is about to open. No params.
     */
    fun promptAccepted()

    /**
     * Fires `in_app_share_prompt_declined` when the user rejects or dismisses the modal without
     * opening the system share sheet. No params.
     */
    fun promptDeclined()

    /**
     * Fires `in_app_share_target_chosen` when the user picks a destination app in the share
     * sheet. [packageName] is sent as the `package_name` param and omitted entirely when the
     * system did not report the chosen component. Like [InAppShareResult.TargetChosen], this
     * only means a target was picked — not that the content was sent.
     */
    fun targetChosen(packageName: String?)

    /**
     * Fires `in_app_share_dismissed` when the user closes the share sheet without picking a
     * target (including when the chosen-component broadcast never arrives).
     */
    fun dismissed()

    /**
     * Fires `in_app_share_failed` when [InAppShare.launch] returns a failure. The [reason] is
     * sent lowercase as the `reason` param.
     */
    fun failed(reason: Reason)

    /**
     * Why the share sheet could not be shown; fixed codes reported as the `reason` param of
     * `in_app_share_failed`.
     */
    enum class Reason {

        /** No activity (and therefore no result launcher) is attached to [InAppShare]. */
        NO_ACTIVITY,

        /** A previous share is still in flight. */
        ALREADY_IN_PROGRESS,

        /** The system rejected the chooser launch or it threw. */
        ERROR,
    }
}

/** [InAppShareTracker] backed by [FirebaseAnalytics]; reason codes are sent lowercase. */
internal class InAppShareTrackerImpl(private val firebaseAnalytics: FirebaseAnalytics) : InAppShareTracker {

    override fun promptShown() {
        firebaseAnalytics.logEvent(IN_APP_SHARE_PROMPT_SHOWN) {}
    }

    override fun promptAccepted() {
        firebaseAnalytics.logEvent(IN_APP_SHARE_PROMPT_ACCEPTED) {}
    }

    override fun promptDeclined() {
        firebaseAnalytics.logEvent(IN_APP_SHARE_PROMPT_DECLINED) {}
    }

    override fun targetChosen(packageName: String?) {
        firebaseAnalytics.logEvent(IN_APP_SHARE_TARGET_CHOSEN) {
            packageName?.let { param("package_name", it) }
        }
    }

    override fun dismissed() {
        firebaseAnalytics.logEvent(IN_APP_SHARE_DISMISSED) {}
    }

    override fun failed(reason: InAppShareTracker.Reason) {
        firebaseAnalytics.logEvent(IN_APP_SHARE_FAILED) {
            param("reason", reason.name.lowercase())
        }
    }

    private companion object {
        const val IN_APP_SHARE_PROMPT_SHOWN = "in_app_share_prompt_shown"
        const val IN_APP_SHARE_PROMPT_ACCEPTED = "in_app_share_prompt_accepted"
        const val IN_APP_SHARE_PROMPT_DECLINED = "in_app_share_prompt_declined"
        const val IN_APP_SHARE_TARGET_CHOSEN = "in_app_share_target_chosen"
        const val IN_APP_SHARE_DISMISSED = "in_app_share_dismissed"
        const val IN_APP_SHARE_FAILED = "in_app_share_failed"
    }
}
