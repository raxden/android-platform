package com.raxdenstudios.platform.notification.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class NotificationTopicTest {

    @Test
    fun `Given a positive offset When utcOffset Then suffix is p and the hours`() {
        assertThat(NotificationTopic.utcOffset("daily_", 5).value).isEqualTo("daily_p5")
    }

    @Test
    fun `Given a negative offset When utcOffset Then suffix is m and the absolute hours`() {
        assertThat(NotificationTopic.utcOffset("daily_", -3).value).isEqualTo("daily_m3")
    }

    @Test
    fun `Given a zero offset When utcOffset Then suffix is 0`() {
        assertThat(NotificationTopic.utcOffset("daily_", 0).value).isEqualTo("daily_0")
    }

    @Test
    fun `Given an offset above the maximum When utcOffset Then it is clamped to 14`() {
        assertThat(NotificationTopic.utcOffset("d", 20).value).isEqualTo("dp14")
        assertThat(NotificationTopic.utcOffset("d", 14).value).isEqualTo("dp14")
    }

    @Test
    fun `Given an offset below the minimum When utcOffset Then it is clamped to -12`() {
        assertThat(NotificationTopic.utcOffset("d", -20).value).isEqualTo("dm12")
        assertThat(NotificationTopic.utcOffset("d", -12).value).isEqualTo("dm12")
    }

    @Test
    fun `Given an empty prefix When utcOffset Then only the suffix is used`() {
        assertThat(NotificationTopic.utcOffset("", 1).value).isEqualTo("p1")
    }
}
