package com.raxdenstudios.platform.engagement.domain

/**
 * Decides whether an origin occurrence count reaches a milestone that justifies prompting the
 * user to engage with the app. [DefaultEngagementEligibility] ships a generic set of thresholds
 * and apps can override it with their own implementation via DI.
 */
interface EngagementEligibility {

    fun isEligible(origin: String, count: Int): Boolean
}

/**
 * Applies the same generic milestone thresholds to any origin: eligible on the first occurrence
 * and then every fixed number of occurrences afterwards.
 */
internal class DefaultEngagementEligibility : EngagementEligibility {

    override fun isEligible(origin: String, count: Int): Boolean =
        count == FIRST_OCCURRENCE || (count >= STEP_START && count % STEP == 0)

    private companion object {
        const val FIRST_OCCURRENCE = 3
        const val STEP_START = 10
        const val STEP = 10
    }
}
