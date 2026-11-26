package com.raxdenstudios.platform.ui.motion

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class PressScaleTest {

    private val press = PressInteraction.Press(Offset.Zero)

    @Test
    fun `Given pressed and motion allowed When pressScaleTarget Then returns pressed scale`() {
        assertThat(pressScaleTarget(isPressed = true, isReducedMotion = false, pressedScale = 0.9f))
            .isEqualTo(0.9f)
    }

    @Test
    fun `Given not pressed When pressScaleTarget Then returns rest scale`() {
        assertThat(pressScaleTarget(isPressed = false, isReducedMotion = false, pressedScale = 0.9f))
            .isEqualTo(1f)
    }

    @Test
    fun `Given pressed and reduced motion When pressScaleTarget Then returns rest scale`() {
        assertThat(pressScaleTarget(isPressed = true, isReducedMotion = true, pressedScale = 0.9f))
            .isEqualTo(1f)
    }

    @Test
    fun `Given press interaction When trackPress Then adds it`() {
        val presses = mutableListOf<PressInteraction.Press>()

        trackPress(presses, press)

        assertThat(presses).containsExactly(press)
    }

    @Test
    fun `Given tracked press When release Then removes it`() {
        val presses = mutableListOf(press)

        trackPress(presses, PressInteraction.Release(press))

        assertThat(presses).isEmpty()
    }

    @Test
    fun `Given tracked press When cancel Then removes it`() {
        val presses = mutableListOf(press)

        trackPress(presses, PressInteraction.Cancel(press))

        assertThat(presses).isEmpty()
    }

    @Test
    fun `Given tracked press When unrelated interaction Then leaves presses untouched`() {
        val presses = mutableListOf(press)

        trackPress(presses, DragInteraction.Start())

        assertThat(presses).containsExactly(press)
    }
}
