package com.raxdenstudios.platform.notification

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic
import org.junit.Test

internal class NotificationStorageTest {

    @Test
    fun `Given the storage When reading constants Then keys keep their persisted values`() {
        assertThat(NotificationStorage.PREFERENCES_NAME).isEqualTo("platform_notification")
        assertThat(NotificationStorage.PERMISSION_REQUESTED_KEY).isEqualTo("permission_requested")
        assertThat(NotificationStorage.DAILY_TOPIC_KEY).isEqualTo("daily_topic")
    }

    @Test
    fun `Given a topic When building the legacy key Then it is prefixed with the topic value`() {
        val key = NotificationStorage.legacyTopicRemovedKey(NotificationTopic("radio_p1"))

        assertThat(key).isEqualTo("legacy_topic_removed_radio_p1")
    }
}
