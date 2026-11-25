package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.platform.engagement.domain.model.Milestones

/**
 * Decides whether an origin occurrence count reaches a milestone that justifies prompting the
 * user to engage with the app. [DefaultEngagementEligibility] ships a generic set of thresholds
 * and apps can override it via DI, usually with a [MilestoneEngagementEligibility] declaring one
 * [Milestones] cadence per origin.
 */
interface EngagementEligibility {

    /**
     * Returns `true` when [count] reaches a milestone that justifies prompting.
     *
     * @param origin the tracked origin, as passed to [EngagementUseCase.Params.origin].
     * @param count occurrences already recorded for [origin], including the current call.
     */
    fun isEligible(
        origin: String,
        count: Int,
    ): Boolean
}

/**
 * Applies the same generic milestone thresholds to any origin: eligible on the third occurrence
 * and then every tenth occurrence afterwards.
 */
internal class DefaultEngagementEligibility : EngagementEligibility {

    override fun isEligible(
        origin: String,
        count: Int,
    ): Boolean = milestones.isMilestone(count)

    private companion object {
        const val FIRST_MILESTONE = 3
        const val STEP = 10

        val milestones = Milestones(
            early = setOf(FIRST_MILESTONE),
            step = STEP,
        )
    }
}
