package com.raxdenstudios.platform.engagement.domain

/** Outcome of an [EngagementUseCase] call. */
sealed interface EngagementResult {

    /** Nothing was shown: not a milestone, already engaged, or another flow in progress. */
    data object None : EngagementResult

    /** The in-app review flow was launched and the app is now marked as rated. */
    data object ReviewLaunched : EngagementResult

    /**
     * The review flow could not be launched. The app is still not rated, so the caller may offer
     * the Play Store page manually. The share prompt, if eligible, is attempted in the same call.
     */
    data object ReviewUnavailable : EngagementResult

    /** The user picked a share target and the app is now marked as shared. */
    data object ShareChosen : EngagementResult

    /** The share prompt was declined, dismissed or could not be shown; it is retried later. */
    data object ShareNotChosen : EngagementResult
}
