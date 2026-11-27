package com.raxdenstudios.platform.featureflag.di

import com.raxdenstudios.platform.featureflag.domain.model.TestFeatureFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FeatureFlagDILibraryTest {

    @Test
    fun `Given a negative minimumFetchInterval When constructed Then throws IllegalArgumentException`() {
        try {
            FeatureFlagDILibrary(
                flags = TestFeatureFlag.entries,
                minimumFetchInterval = (-1).seconds,
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("minimumFetchInterval must not be negative"))
        }
    }

    @Test
    fun `Given a zero minimumFetchInterval When constructed Then builds the modules`() {
        val library = FeatureFlagDILibrary(
            flags = TestFeatureFlag.entries,
            minimumFetchInterval = Duration.ZERO,
        )

        assertEquals(2, library.getModules().size)
    }

    @Test
    fun `Given a positive minimumFetchInterval When constructed Then builds the modules`() {
        val library = FeatureFlagDILibrary(
            flags = TestFeatureFlag.entries,
            minimumFetchInterval = 1.seconds,
        )

        assertEquals(2, library.getModules().size)
    }
}
