package com.raxdenstudios.platform.engagement.domain

/** Outcome of an [EngagementUseCase] call. */
sealed interface EngagementResult {

    /**
     * Nothing was shown: no milestone reached and no prompt pending (or it expired), both prompts
     * blocked by the [com.raxdenstudios.platform.engagement.domain.model.EngagementPolicy]
     * (app installed too recently, rated/shared, caps, cooldowns or the gap between prompts), or
     * another flow in progress (a reached milestone is then kept pending for a later call).
     */
    data object None : EngagementResult

    /**
     * The in-app review flow was launched and the attempt recorded; it is requested again after
     * its cooldown, up to its attempts cap. Play does not report whether the dialog was shown.
     */
    data object ReviewLaunched : EngagementResult

    /**
     * The review flow could not be launched (e.g. no foreground activity) and no share target was
     * picked in the fallback share prompt. The caller may offer the Play Store page manually and,
     * if it does, report it through [MarkAppAsRatedUseCase]. When the share prompt could not be
     * shown either, the prompt is kept pending for the next call.
     */
    data object ReviewUnavailable : EngagementResult

    /** The user picked a share target and the app is now marked as shared. */
    data object ShareChosen : EngagementResult

    /**
     * The share prompt was declined, or accepted and then the share sheet was closed without
     * picking a target; both count as a decline, and the prompt is retried on a later milestone
     * once its cooldown elapses unless it has been declined too many times. Or it could not be
     * shown, and is kept pending for the next call.
     */
    data object ShareNotChosen : EngagementResult
}
