package com.raxdenstudios.platform.engagement.domain.model

/**
 * Persisted engagement progress, stored in the module's own Room database
 * (`platform_engagement.db`).
 *
 * Older versions stored it as JSON in the `platform_engagement` `SharedPreferences` file; that
 * payload is moved into the database on first access. Payloads written before [reviewAttempts]
 * existed used [hasRated] to mean "the in-app review was launched once"; they are migrated to
 * `reviewAttempts = 1`, `hasRated = false`.
 *
 * @property hasRated `true` once the user explicitly went to rate the app (e.g. opened the Play
 *   Store page from a settings entry, reported through `MarkAppAsRatedUseCase`). From then on the
 *   in-app review is never requested again.
 * @property hasShared `true` once the user has picked a share target. From then on the share
 *   prompt is never shown again.
 * @property counts recorded occurrences per `EngagementUseCase.Params.origin`; each origin
 *   follows its own milestone progression.
 * @property shareDeclines times the user answered the share prompt with "Not now" (back press or
 *   tap outside count too); closing the share sheet after accepting does not. Once it reaches
 *   the cap, the share prompt is never shown again.
 * @property lastSharePromptAt epoch millis of the last time the share prompt was shown, or `null`
 *   if never shown; used to enforce a cooldown between prompts.
 * @property reviewAttempts times the in-app review flow was launched (Play never reports whether
 *   the dialog was actually displayed). Once it reaches the cap, the review is never requested
 *   again.
 * @property lastReviewAt epoch millis of the last in-app review launch, or `null` if never
 *   launched; used to enforce a cooldown between review requests.
 * @property pendingPrompt `true` when a milestone was reached but no prompt could be shown
 *   (another flow was in progress, or neither the review nor the share prompt could launch). The
 *   next engagement call is then eligible regardless of its milestone; it is cleared once a
 *   review is launched or the share prompt is shown, or when neither prompt is allowed anymore.
 */
data class EngagementData(
    val hasRated: Boolean = false,
    val hasShared: Boolean = false,
    val counts: Map<String, Int> = emptyMap(),
    val shareDeclines: Int = 0,
    val lastSharePromptAt: Long? = null,
    val reviewAttempts: Int = 0,
    val lastReviewAt: Long? = null,
    val pendingPrompt: Boolean = false,
)
