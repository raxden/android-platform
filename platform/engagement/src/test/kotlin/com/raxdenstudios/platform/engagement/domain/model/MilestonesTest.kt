package com.raxdenstudios.platform.engagement.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal class MilestonesTest {

    @Test
    fun `Given early count When checked Then is milestone`() {
        val milestones = Milestones(early = setOf(1, 3), step = 10)

        assertTrue(milestones.isMilestone(1))
        assertTrue(milestones.isMilestone(3))
    }

    @Test
    fun `Given multiples of step When checked Then are milestones`() {
        val milestones = Milestones(early = setOf(3), step = 10)

        assertTrue(milestones.isMilestone(10))
        assertTrue(milestones.isMilestone(20))
        assertTrue(milestones.isMilestone(30))
    }

    @Test
    fun `Given counts that are neither early nor multiples When checked Then are not milestones`() {
        val milestones = Milestones(early = setOf(3), step = 10)

        assertFalse(milestones.isMilestone(2))
        assertFalse(milestones.isMilestone(4))
        assertFalse(milestones.isMilestone(9))
        assertFalse(milestones.isMilestone(11))
        assertFalse(milestones.isMilestone(25))
    }

    @Test
    fun `Given zero When checked Then is not a milestone`() {
        assertFalse(Milestones(early = emptySet(), step = 5).isMilestone(0))
    }

    @Test
    fun `Given negative counts When checked Then are not milestones`() {
        val milestones = Milestones(early = setOf(3), step = 5)

        assertFalse(milestones.isMilestone(-1))
        assertFalse(milestones.isMilestone(-5))
        assertFalse(milestones.isMilestone(-10))
    }

    @Test
    fun `Given non positive step When created Then throws`() {
        assertThrows(IllegalArgumentException::class.java) { Milestones(early = setOf(1), step = 0) }
        assertThrows(IllegalArgumentException::class.java) { Milestones(early = setOf(1), step = -5) }
    }

    @Test
    fun `Given early 7 and step 30 When checked Then 7 30 and 60 are milestones and 14 is not`() {
        val milestones = Milestones(early = setOf(7), step = 30)

        assertTrue(milestones.isMilestone(7))
        assertTrue(milestones.isMilestone(30))
        assertTrue(milestones.isMilestone(60))
        assertFalse(milestones.isMilestone(14))
    }

    @Test
    fun `Given early count that is also a multiple When checked Then is a milestone`() {
        assertTrue(Milestones(early = setOf(10), step = 10).isMilestone(10))
    }

    @Test
    fun `Given step of one When checked Then every positive count is a milestone`() {
        val milestones = Milestones(early = emptySet(), step = 1)

        assertTrue((1..5).all(milestones::isMilestone))
    }

    @Test
    fun `Given step with default values When read Then exposes them`() {
        val milestones = Milestones(early = setOf(2), step = 4)

        assertEquals(setOf(2), milestones.early)
        assertEquals(4, milestones.step)
    }
}
