package com.raxdenstudios.platform.ui.composable

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class BottomNavigationBarTest {

    @Test
    fun `Given visible When collapsedHeight Then returns measured height`() {
        assertThat(collapsedHeight(visible = true, measuredHeight = 56)).isEqualTo(56)
    }

    @Test
    fun `Given not visible When collapsedHeight Then returns zero`() {
        assertThat(collapsedHeight(visible = false, measuredHeight = 56)).isEqualTo(0)
    }

    @Test
    fun `Given reduced motion When motionAware Then returns reduced`() {
        assertThat(motionAware(isReducedMotion = true, normal = "normal", reduced = "reduced"))
            .isEqualTo("reduced")
    }

    @Test
    fun `Given animations on When motionAware Then returns normal`() {
        assertThat(motionAware(isReducedMotion = false, normal = "normal", reduced = "reduced"))
            .isEqualTo("normal")
    }
}
