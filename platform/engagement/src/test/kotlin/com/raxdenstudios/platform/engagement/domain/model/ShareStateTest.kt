package com.raxdenstudios.platform.engagement.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ShareStateTest {

    @Test
    fun `Given no shared timestamp When hasShared is read Then it is false`() {
        assertFalse(ShareState(sharedAt = null).hasShared)
    }

    @Test
    fun `Given a shared timestamp When hasShared is read Then it is true`() {
        assertTrue(ShareState(sharedAt = 1L).hasShared)
    }
}
