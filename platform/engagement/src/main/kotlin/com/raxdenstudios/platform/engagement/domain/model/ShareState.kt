package com.raxdenstudios.platform.engagement.domain.model

/**
 * Share prompt progress, part of [EngagementData].
 *
 * @property declines times the user answered the share prompt with "Not now" (back press or tap
 *   outside count too) or accepted it and then closed the share sheet without picking a target.
 *   Once it reaches [EngagementPolicy.maxShareDeclines], the share prompt is never shown again.
 * @property lastPromptAt epoch millis of the last time the share prompt was shown, or `null` if
 *   never shown; used to enforce [EngagementPolicy.shareCooldown] and
 *   [EngagementPolicy.minPromptsGap].
 * @property sharedAt epoch millis when the user picked a share target from the share prompt, or
 *   `null` if never.
 */
data class ShareState(
    val declines: Int = 0,
    val lastPromptAt: Long? = null,
    val sharedAt: Long? = null,
) {

    /** `true` once the user picked a share target; the share prompt is then never shown again. */
    val hasShared: Boolean
        get() = sharedAt != null
}
