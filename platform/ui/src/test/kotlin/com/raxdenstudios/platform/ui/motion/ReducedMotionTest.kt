package com.raxdenstudios.platform.ui.motion

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class ReducedMotionTest {

    @Test
    fun `Given scale zero When isAnimatorDisabled Then returns true`() {
        assertThat(isAnimatorDisabled(animatorDurationScale = 0f)).isTrue()
    }

    @Test
    fun `Given default scale When isAnimatorDisabled Then returns false`() {
        assertThat(isAnimatorDisabled(animatorDurationScale = 1f)).isFalse()
    }

    @Test
    fun `Given fractional scale When isAnimatorDisabled Then returns false`() {
        assertThat(isAnimatorDisabled(animatorDurationScale = 0.5f)).isFalse()
    }
}
