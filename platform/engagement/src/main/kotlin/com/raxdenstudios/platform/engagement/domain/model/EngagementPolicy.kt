package com.raxdenstudios.platform.engagement.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Rate limits applied by `EngagementUseCase` to the in-app review and the share prompt.
 *
 * The defaults suit most apps. `EngagementDILibrary` binds it as a Koin `factory`, so an app can
 * override it from one of its own modules (loaded after `EngagementDILibrary`), e.g. building it
 * from Remote Config:
 *
 * ```kotlin
 * factory { EngagementPolicy(shareCooldown = remoteConfig.getLong("share_cooldown_days").days) }
 * ```
 *
 * `EngagementUseCase` is a singleton: the policy is resolved once, when it is first created.
 * Overriding relies on Koin definition override, so it does not work in apps that start Koin with
 * `allowOverride(false)`.
 *
 * Durations are compared against epoch millis, so they are truncated to whole milliseconds; a zero
 * or negative cooldown, gap or install age disables that limit, while a zero or negative
 * [pendingPromptTtl] disables pending prompts (they expire immediately).
 *
 * @property maxReviewAttempts in-app review launches after which the review is never requested
 *   again; `0` disables the review.
 * @property reviewCooldown minimum time between two in-app review launches.
 * @property maxShareDeclines share prompt answers that count as a decline ("Not now", or accepted
 *   and then the share sheet closed without picking a target) after which the share prompt is
 *   never shown again; `0` disables the share prompt.
 * @property shareCooldown minimum time between two share prompts.
 * @property minPromptsGap minimum time between a review launch and a share prompt, in either
 *   order, so the user is never asked twice within this window.
 * @property pendingPromptTtl how long a reached milestone that could not show anything stays
 *   pending; once it expires the next call is eligible only on its own milestone again.
 * @property minInstallAge time since the app was first installed before any prompt is shown;
 *   milestones reached earlier are counted but neither shown nor kept pending.
 */
data class EngagementPolicy(
    val maxReviewAttempts: Int = DEFAULT_MAX_REVIEW_ATTEMPTS,
    val reviewCooldown: Duration = 90.days,
    val maxShareDeclines: Int = DEFAULT_MAX_SHARE_DECLINES,
    val shareCooldown: Duration = 30.days,
    val minPromptsGap: Duration = 14.days,
    val pendingPromptTtl: Duration = 48.hours,
    val minInstallAge: Duration = 3.days,
) {

    private companion object {
        const val DEFAULT_MAX_REVIEW_ATTEMPTS = 3
        const val DEFAULT_MAX_SHARE_DECLINES = 2
    }
}
