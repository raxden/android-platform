package com.raxdenstudios.platform.engagement.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ReviewStateTest {

    @Test
    fun `Given no rated timestamp When hasRated is read Then it is false`() {
        assertFalse(ReviewState(ratedAt = null).hasRated)
    }

    @Test
    fun `Given a rated timestamp When hasRated is read Then it is true`() {
        assertTrue(ReviewState(ratedAt = 1L).hasRated)
    }
}
