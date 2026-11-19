package com.raxdenstudios.platform.engagement.domain.model

/**
 * Persisted engagement progress, stored in the module's own Room database
 * (`platform_engagement.db`).
 *
 * @property counts recorded occurrences per `EngagementUseCase.Params.origin`; each origin
 *   follows its own milestone progression.
 * @property review progress of the in-app review prompt.
 * @property share progress of the share prompt.
 * @property pendingPromptAt epoch millis when a milestone was reached but no prompt could be shown
 *   (another flow was in progress, or neither the review nor the share prompt could launch), or
 *   `null` if none is pending. While it is younger than [EngagementPolicy.pendingPromptTtl] the
 *   next engagement call is eligible regardless of its milestone; an expired one is ignored and
 *   cleared on the next engagement call. It is also cleared once a review is launched or the
 *   share prompt is shown, or when neither prompt is allowed anymore.
 */
data class EngagementData(
    val counts: Map<String, Int> = emptyMap(),
    val review: ReviewState = ReviewState(),
    val share: ShareState = ShareState(),
    val pendingPromptAt: Long? = null,
)
