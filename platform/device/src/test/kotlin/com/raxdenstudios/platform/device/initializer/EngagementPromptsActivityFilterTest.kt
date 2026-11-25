package com.raxdenstudios.platform.device.initializer

import android.app.Activity
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

internal class EngagementPromptsActivityFilterTest {

    @Test
    fun `Given the All filter When any activity is checked Then it is accepted`() {
        assertTrue(EngagementPromptsActivityFilter.All.accepts(mockk<Activity>()))
    }
}
