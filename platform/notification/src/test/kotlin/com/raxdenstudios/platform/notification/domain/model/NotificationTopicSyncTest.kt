package com.raxdenstudios.platform.notification.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class NotificationTopicSyncTest {

    private val topic = NotificationTopic("p1")
    private val other = NotificationTopic("p2")

    @Test
    fun `Given same topic and no legacy removed When hasChanges Then false`() {
        val sync = NotificationTopicSync(topic, topic, emptySet())

        assertThat(sync.hasChanges).isFalse()
    }

    @Test
    fun `Given no previous topic When hasChanges Then true`() {
        val sync = NotificationTopicSync(topic, null, emptySet())

        assertThat(sync.hasChanges).isTrue()
    }

    @Test
    fun `Given a different previous topic When hasChanges Then true`() {
        val sync = NotificationTopicSync(topic, other, emptySet())

        assertThat(sync.hasChanges).isTrue()
    }

    @Test
    fun `Given same topic and removed legacy topics When hasChanges Then true`() {
        val sync = NotificationTopicSync(topic, topic, setOf(other))

        assertThat(sync.hasChanges).isTrue()
    }
}
