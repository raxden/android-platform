package com.raxdenstudios.platform.engagement.domain.model

/**
 * In-app review progress, part of [EngagementData].
 *
 * @property attempts times the in-app review flow was launched (Play never reports whether the
 *   dialog was actually displayed). Once it reaches [EngagementPolicy.maxReviewAttempts], the
 *   review is never requested again.
 * @property lastAt epoch millis of the last in-app review launch, or `null` if never launched;
 *   used to enforce [EngagementPolicy.reviewCooldown] and [EngagementPolicy.minPromptsGap].
 * @property ratedAt epoch millis of the last time the user explicitly went to rate the app (e.g.
 *   opened the Play Store page from a settings entry, reported through `MarkAppAsRatedUseCase`),
 *   or `null` if never.
 */
data class ReviewState(
    val attempts: Int = 0,
    val lastAt: Long? = null,
    val ratedAt: Long? = null,
) {

    /** `true` once the user explicitly went to rate the app; the in-app review is then never requested again. */
    val hasRated: Boolean
        get() = ratedAt != null
}
