package com.raxdenstudios.platform.ui.motion

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class AppearShineTest {

    @Test
    fun `Given progress zero When shineBandStart Then band sits one band before the element`() {
        assertThat(shineBandStart(width = 100f, bandWidth = 20f, progress = 0f)).isEqualTo(-20f)
    }

    @Test
    fun `Given progress one When shineBandStart Then band sits one band after the element`() {
        assertThat(shineBandStart(width = 100f, bandWidth = 20f, progress = 1f)).isEqualTo(120f)
    }

    @Test
    fun `Given progress half When shineBandStart Then band is centered on the sweep`() {
        assertThat(shineBandStart(width = 100f, bandWidth = 20f, progress = 0.5f)).isEqualTo(50f)
    }
}
