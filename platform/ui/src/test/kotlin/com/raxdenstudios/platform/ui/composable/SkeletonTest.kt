package com.raxdenstudios.platform.ui.composable

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class SkeletonTest {

    @Test
    fun `Given height equal to two line heights When textLines Then returns 2`() {
        assertThat(Skeleton.textLines(height = 40f, lineHeight = 20f)).isEqualTo(2)
    }

    @Test
    fun `Given ratio 1_4 When textLines Then rounds down to 1`() {
        assertThat(Skeleton.textLines(height = 28f, lineHeight = 20f)).isEqualTo(1)
    }

    @Test
    fun `Given ratio 1_6 When textLines Then rounds up to 2`() {
        assertThat(Skeleton.textLines(height = 32f, lineHeight = 20f)).isEqualTo(2)
    }

    @Test
    fun `Given zero line height When textLines Then returns 1`() {
        assertThat(Skeleton.textLines(height = 100f, lineHeight = 0f)).isEqualTo(1)
    }

    @Test
    fun `Given negative line height When textLines Then returns 1`() {
        assertThat(Skeleton.textLines(height = 100f, lineHeight = -5f)).isEqualTo(1)
    }

    @Test
    fun `Given zero height When textLines Then returns 1`() {
        assertThat(Skeleton.textLines(height = 0f, lineHeight = 20f)).isEqualTo(1)
    }

    @Test
    fun `Given huge ratio When textLines Then caps at 100`() {
        assertThat(Skeleton.textLines(height = 100_000f, lineHeight = 1f)).isEqualTo(100)
    }
}
