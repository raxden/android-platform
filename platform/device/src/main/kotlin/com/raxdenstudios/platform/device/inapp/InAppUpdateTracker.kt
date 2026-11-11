package com.raxdenstudios.platform.device.inapp

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

/**
 * Firebase Analytics reporting for [InAppUpdate], injected into [InAppUpdateImpl] (the debug
 * variant does not track). Every method logs one `in_app_update_*` event; parameters are
 * documented on each method.
 */
internal interface InAppUpdateTracker {

    /**
     * Fires `in_app_update_checked` each time [InAppUpdate.check] successfully queries Play.
     * Params: `available` (sent as the string `"true"`/`"false"`), `version_code` of the pending
     * update, `priority` (0..5 from Play Console) and `staleness_days` — the latter omitted when
     * Play does not report it.
     */
    fun checked(available: Boolean, versionCode: Long, priority: Long, stalenessDays: Long?)

    /**
     * Fires `in_app_update_launched` whenever [InAppUpdate.launch] returns an outcome. The
     * [result] is sent snake_case as the `result` param: `started`, `not_available`,
     * `already_in_progress` or `not_allowed` — see [InAppUpdateLaunchResult]. The user's
     * decision inside Play's UI is reported separately via [accepted], [declined] or
     * [flowFailed].
     */
    fun launched(result: InAppUpdateLaunchResult)

    /**
     * Fires `in_app_update_failed` when [InAppUpdate.check] or [InAppUpdate.launch] returns a
     * failure. The [reason] is sent lowercase as the `reason` param.
     */
    fun failed(reason: Reason)

    /** Fires `in_app_update_accepted` when Play's update UI returns `Activity.RESULT_OK`. */
    fun accepted()

    /** Fires `in_app_update_declined` when the user cancels Play's update UI (`RESULT_CANCELED`). */
    fun declined()

    /**
     * Fires `in_app_update_flow_failed` when Play's update UI finishes with a result code other
     * than OK or CANCELED, i.e. the flow ended in error.
     */
    fun flowFailed()

    /**
     * Fires `in_app_update_resumed` when a flow Play reports as
     * `DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS` is relaunched on activity resume, so an
     * interrupted update continues instead of stalling.
     */
    fun resumed()

    /**
     * Why a check or launch could not complete; fixed codes reported as the `reason` param of
     * `in_app_update_failed`.
     */
    enum class Reason {

        /** No activity is attached to [InAppUpdate]. */
        NO_ACTIVITY,

        /** The Play request (check or launch) failed. */
        ERROR,
    }
}

/** [InAppUpdateTracker] backed by [FirebaseAnalytics]; reason codes are sent lowercase. */
internal class InAppUpdateTrackerImpl(private val firebaseAnalytics: FirebaseAnalytics) : InAppUpdateTracker {

    override fun checked(available: Boolean, versionCode: Long, priority: Long, stalenessDays: Long?) {
        firebaseAnalytics.logEvent(IN_APP_UPDATE_CHECKED) {
            param("available", available.toString())
            param("version_code", versionCode)
            param("priority", priority)
            stalenessDays?.let { param("staleness_days", it) }
        }
    }

    override fun launched(result: InAppUpdateLaunchResult) {
        firebaseAnalytics.logEvent(IN_APP_UPDATE_LAUNCHED) {
            param("result", result.toTrackingName())
        }
    }

    override fun failed(reason: InAppUpdateTracker.Reason) {
        firebaseAnalytics.logEvent(IN_APP_UPDATE_FAILED) {
            param("reason", reason.name.lowercase())
        }
    }

    override fun accepted() {
        firebaseAnalytics.logEvent(IN_APP_UPDATE_ACCEPTED) {}
    }

    override fun declined() {
        firebaseAnalytics.logEvent(IN_APP_UPDATE_DECLINED) {}
    }

    override fun flowFailed() {
        firebaseAnalytics.logEvent(IN_APP_UPDATE_FLOW_FAILED) {}
    }

    override fun resumed() {
        firebaseAnalytics.logEvent(IN_APP_UPDATE_RESUMED) {}
    }

    private fun InAppUpdateLaunchResult.toTrackingName(): String = when (this) {
        InAppUpdateLaunchResult.Started -> "started"
        InAppUpdateLaunchResult.NotAvailable -> "not_available"
        InAppUpdateLaunchResult.AlreadyInProgress -> "already_in_progress"
        InAppUpdateLaunchResult.NotAllowed -> "not_allowed"
    }

    private companion object {
        const val IN_APP_UPDATE_CHECKED = "in_app_update_checked"
        const val IN_APP_UPDATE_LAUNCHED = "in_app_update_launched"
        const val IN_APP_UPDATE_FAILED = "in_app_update_failed"
        const val IN_APP_UPDATE_ACCEPTED = "in_app_update_accepted"
        const val IN_APP_UPDATE_DECLINED = "in_app_update_declined"
        const val IN_APP_UPDATE_FLOW_FAILED = "in_app_update_flow_failed"
        const val IN_APP_UPDATE_RESUMED = "in_app_update_resumed"
    }
}
