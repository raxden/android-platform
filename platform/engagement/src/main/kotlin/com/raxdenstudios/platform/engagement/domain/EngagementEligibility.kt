package com.raxdenstudios.platform.engagement.domain

/**
 * Decides whether an origin occurrence count reaches a milestone that justifies prompting the
 * user to engage with the app. [DefaultEngagementEligibility] ships a generic set of thresholds
 * and apps can override it with their own implementation via DI.
 */
interface EngagementEligibility {

    /**
     * Returns `true` when [count] reaches a milestone that justifies prompting.
     *
     * @param origin the tracked origin, as passed to [EngagementUseCase.Params.origin].
     * @param count occurrences already recorded for [origin], including the current call.
     */
    fun isEligible(origin: String, count: Int): Boolean
}

/**
 * Applies the same generic milestone thresholds to any origin: eligible on the third occurrence
 * and then every tenth occurrence afterwards.
 */
internal class DefaultEngagementEligibility : EngagementEligibility {

    override fun isEligible(origin: String, count: Int): Boolean =
        count == FIRST_MILESTONE || (count >= STEP && count % STEP == 0)

    private companion object {
        const val FIRST_MILESTONE = 3
        const val STEP = 10
    }
}
