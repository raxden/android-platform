package com.raxdenstudios.platform.ui.composable

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class PressFeedbackTest {

    @Test
    fun `Given scale 1 When scalesOnPress Then returns false`() {
        assertThat(scalesOnPress(pressedScale = 1f)).isFalse()
    }

    @Test
    fun `Given scale below 1 When scalesOnPress Then returns true`() {
        assertThat(scalesOnPress(pressedScale = 0.9f)).isTrue()
    }

    @Test
    fun `Given None When read Then has no scale and no haptics`() {
        assertThat(PressFeedback.None.buttonPressedScale).isEqualTo(1f)
        assertThat(PressFeedback.None.iconButtonPressedScale).isEqualTo(1f)
        assertThat(PressFeedback.None.hapticsEnabled).isFalse()
    }

    @Test
    fun `Given default constructor When compared with None Then they are equal`() {
        assertThat(PressFeedback()).isEqualTo(PressFeedback.None)
    }

    @Test
    fun `Given equal instances When hashCode Then is the same`() {
        val first = PressFeedback(0.9f, 0.8f, true)
        val second = PressFeedback(0.9f, 0.8f, true)

        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `Given instances differing in one field When compared Then are not equal`() {
        val base = PressFeedback(0.9f, 0.8f, true)

        assertThat(base).isNotEqualTo(PressFeedback(0.5f, 0.8f, true))
        assertThat(base).isNotEqualTo(PressFeedback(0.9f, 0.5f, true))
        assertThat(base).isNotEqualTo(PressFeedback(0.9f, 0.8f, false))
    }

    @Test
    fun `Given instance When toString Then contains field values`() {
        assertThat(PressFeedback(0.9f, 0.8f, true).toString()).isEqualTo(
            "PressFeedback(buttonPressedScale=0.9, iconButtonPressedScale=0.8, hapticsEnabled=true)",
        )
    }
}
