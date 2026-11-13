package com.raxdenstudios.platform.engagement.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class DefaultEngagementEligibilityTest {

    private val eligibility = DefaultEngagementEligibility()

    @Test
    fun `Given third occurrence of any origin When checked Then eligible`() {
        assertTrue(eligibility.isEligible("app_opened", 3))
        assertTrue(eligibility.isEligible("favourite_added", 3))
    }

    @Test
    fun `Given occurrences below first milestone When checked Then not eligible`() {
        assertFalse(eligibility.isEligible("app_opened", 0))
        assertFalse(eligibility.isEligible("app_opened", 1))
        assertFalse(eligibility.isEligible("favourite_added", 2))
    }

    @Test
    fun `Given occurrences between first and step milestones When checked Then not eligible`() {
        assertFalse(eligibility.isEligible("app_opened", 4))
        assertFalse(eligibility.isEligible("favourite_added", 9))
    }

    @Test
    fun `Given occurrences on ten step milestone When checked Then eligible`() {
        assertTrue(eligibility.isEligible("app_opened", 10))
        assertTrue(eligibility.isEligible("favourite_added", 20))
        assertTrue(eligibility.isEligible("some_other_origin", 30))
    }

    @Test
    fun `Given occurrences off ten step milestone When checked Then not eligible`() {
        assertFalse(eligibility.isEligible("app_opened", 11))
        assertFalse(eligibility.isEligible("app_opened", 15))
        assertFalse(eligibility.isEligible("favourite_added", 25))
    }

    @Test
    fun `Given arbitrary origin When checked Then same generic milestones apply`() {
        assertFalse(eligibility.isEligible("some_random_origin", 2))
        assertTrue(eligibility.isEligible("some_random_origin", 3))
        assertFalse(eligibility.isEligible("some_random_origin", 7))
        assertTrue(eligibility.isEligible("some_random_origin", 40))
        assertFalse(eligibility.isEligible("some_random_origin", 41))
    }
}
