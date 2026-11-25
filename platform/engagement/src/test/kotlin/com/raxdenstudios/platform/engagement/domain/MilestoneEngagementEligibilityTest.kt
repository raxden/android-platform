package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.platform.engagement.domain.model.Milestones
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class MilestoneEngagementEligibilityTest {

    private val eligibility = MilestoneEngagementEligibility(
        milestones = mapOf(
            "favourite_added" to Milestones(early = setOf(3), step = 10),
            "long_listening_session" to Milestones(early = setOf(1, 3), step = 5),
        ),
    )

    @Test
    fun `Given origin with its own cadence When checked Then eligible only on its milestones`() {
        assertTrue(eligibility.isEligible("favourite_added", 3))
        assertTrue(eligibility.isEligible("favourite_added", 10))
        assertTrue(eligibility.isEligible("favourite_added", 20))
        assertFalse(eligibility.isEligible("favourite_added", 1))
        assertFalse(eligibility.isEligible("favourite_added", 5))
    }

    @Test
    fun `Given independent origins When same count is checked Then each follows its own cadence`() {
        assertTrue(eligibility.isEligible("long_listening_session", 1))
        assertFalse(eligibility.isEligible("favourite_added", 1))
        assertTrue(eligibility.isEligible("long_listening_session", 5))
        assertFalse(eligibility.isEligible("favourite_added", 5))
    }

    @Test
    fun `Given unknown origin When checked Then never eligible`() {
        assertFalse(eligibility.isEligible("app_foreground", 1))
        assertFalse(eligibility.isEligible("app_foreground", 3))
        assertFalse(eligibility.isEligible("app_foreground", 10))
    }

    @Test
    fun `Given empty map When checked Then never eligible`() {
        val empty = MilestoneEngagementEligibility(milestones = emptyMap())

        assertFalse(empty.isEligible("favourite_added", 3))
        assertFalse(empty.isEligible("favourite_added", 10))
    }
}
