package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.platform.engagement.domain.model.Milestones

/**
 * [EngagementEligibility] driven by a table of [Milestones] per origin. Origins missing from
 * [milestones] are never eligible, so an origin that only exists to flush a pending prompt (e.g.
 * `"app_foreground"`) just has to be left out of the table.
 *
 * ```kotlin
 * module {
 *     factory<EngagementEligibility> {
 *         MilestoneEngagementEligibility(
 *             milestones = mapOf(
 *                 "favourite_added" to Milestones(early = setOf(3), step = 10),
 *                 "long_listening_session" to Milestones(early = setOf(1, 3), step = 5),
 *             ),
 *         )
 *     }
 * }
 * ```
 *
 * @param milestones cadence of each eligible origin, keyed by [EngagementUseCase.Params.origin].
 */
class MilestoneEngagementEligibility(
    private val milestones: Map<String, Milestones>,
) : EngagementEligibility {

    override fun isEligible(
        origin: String,
        count: Int,
    ): Boolean = milestones[origin]?.isMilestone(count) ?: false
}
