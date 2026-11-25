package com.raxdenstudios.platform.engagement.domain.model

/**
 * Occurrence counts of an origin that justify an engagement prompt: every count in [early] and
 * then every multiple of [step] (`step`, `2 * step`, `3 * step`…).
 *
 * ```kotlin
 * Milestones(early = setOf(3), step = 10)    // 3, 10, 20, 30…
 * Milestones(early = setOf(1, 3), step = 5)  // 1, 3, 5, 10, 15…
 * ```
 *
 * Used by `MilestoneEngagementEligibility` to declare one cadence per origin.
 *
 * @property early milestones reached before the repeating cadence starts; they may also be
 *   multiples of [step], which is harmless.
 * @property step repeating cadence; must be positive.
 */
data class Milestones(
    val early: Set<Int>,
    val step: Int,
) {

    init {
        require(step > 0) { "step must be positive, was $step" }
    }

    /** Returns `true` when [count] is one of the [early] milestones or a positive multiple of [step]. */
    fun isMilestone(
        count: Int,
    ): Boolean = count in early || (count >= step && count % step == 0)
}
