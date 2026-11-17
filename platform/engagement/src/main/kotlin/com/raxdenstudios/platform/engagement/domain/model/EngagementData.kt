package com.raxdenstudios.platform.engagement.domain.model

import kotlinx.serialization.Serializable

/**
 * Persisted engagement progress, serialized to the `platform_engagement` `SharedPreferences`
 * file under the `engagement_data` key. Unknown JSON keys are ignored on decode, so payloads
 * written by other versions still load.
 *
 * @property hasRated `true` once the in-app review flow has been launched (not necessarily
 *   completed by the user). From then on, eligible milestones trigger the share prompt instead.
 * @property hasShared `true` once the user has picked a share target. From then on, calls only
 *   count occurrences and no further prompt is shown.
 * @property counts recorded occurrences per `EngagementUseCase.Params.origin`; each origin
 *   follows its own milestone progression.
 * @property shareDeclines times the user answered the share prompt with "Not now" (back press or
 *   tap outside count too); closing the share sheet after accepting does not. Once it reaches the cap, the share prompt is never shown again.
 * @property lastSharePromptAt epoch millis of the last time the share prompt was shown, or `null`
 *   if never shown; used to enforce a cooldown between prompts.
 */
@Serializable
data class EngagementData(
    val hasRated: Boolean = false,
    val hasShared: Boolean = false,
    val counts: Map<String, Int> = emptyMap(),
    val shareDeclines: Int = 0,
    val lastSharePromptAt: Long? = null,
)
