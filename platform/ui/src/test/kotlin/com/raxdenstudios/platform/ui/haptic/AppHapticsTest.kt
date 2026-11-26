package com.raxdenstudios.platform.ui.haptic

import android.view.HapticFeedbackConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class AppHapticsTest {

    @Test
    fun `Given TAP When toFeedbackConstant Then is VIRTUAL_KEY on every api`() {
        listOf(29, 30, 33, 34).forEach {
            assertThat(HapticEvent.TAP.toFeedbackConstant(it)).isEqualTo(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    @Test
    fun `Given SOFT_TAP When toFeedbackConstant Then is CLOCK_TICK on every api`() {
        listOf(29, 30, 33, 34).forEach {
            assertThat(HapticEvent.SOFT_TAP.toFeedbackConstant(it)).isEqualTo(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    @Test
    fun `Given TICK When toFeedbackConstant Then is CLOCK_TICK below 34`() {
        listOf(29, 30, 33).forEach {
            assertThat(HapticEvent.TICK.toFeedbackConstant(it)).isEqualTo(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    @Test
    fun `Given TICK When toFeedbackConstant at api 34 Then is SEGMENT_FREQUENT_TICK`() {
        assertThat(HapticEvent.TICK.toFeedbackConstant(34))
            .isEqualTo(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    }

    @Test
    fun `Given CONFIRM When toFeedbackConstant at api 29 Then is KEYBOARD_TAP`() {
        assertThat(HapticEvent.CONFIRM.toFeedbackConstant(29)).isEqualTo(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    @Test
    fun `Given CONFIRM When toFeedbackConstant from api 30 Then is CONFIRM`() {
        listOf(30, 33, 34).forEach {
            assertThat(HapticEvent.CONFIRM.toFeedbackConstant(it)).isEqualTo(HapticFeedbackConstants.CONFIRM)
        }
    }

    @Test
    fun `Given ERROR When toFeedbackConstant at api 29 Then is LONG_PRESS`() {
        assertThat(HapticEvent.ERROR.toFeedbackConstant(29)).isEqualTo(HapticFeedbackConstants.LONG_PRESS)
    }

    @Test
    fun `Given ERROR When toFeedbackConstant from api 30 Then is REJECT`() {
        listOf(30, 33, 34).forEach {
            assertThat(HapticEvent.ERROR.toFeedbackConstant(it)).isEqualTo(HapticFeedbackConstants.REJECT)
        }
    }

    @Test
    fun `Given REJECT When toFeedbackConstant at api 29 Then is VIRTUAL_KEY`() {
        assertThat(HapticEvent.REJECT.toFeedbackConstant(29)).isEqualTo(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    @Test
    fun `Given REJECT When toFeedbackConstant from api 30 Then is REJECT`() {
        listOf(30, 33, 34).forEach {
            assertThat(HapticEvent.REJECT.toFeedbackConstant(it)).isEqualTo(HapticFeedbackConstants.REJECT)
        }
    }
}
