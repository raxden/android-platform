package com.raxdenstudios.platform.notification.data.datasource

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.notification.NotificationStorage
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class NotificationTopicLocalDataSourceImplTest {

    private val preferences: SharedPreferences = RuntimeEnvironment.getApplication()
        .getSharedPreferences(NotificationStorage.PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val dataSource = NotificationTopicLocalDataSourceImpl(preferences)
    private val topic = NotificationTopic("daily_p1")

    @Test
    fun `Given nothing stored When getTopic Then null`() = runTest {
        assertThat(dataSource.getTopic()).isEqualTo(Answer.Success(null))
    }

    @Test
    fun `Given a saved topic When getTopic Then returns it stored under the daily key`() = runTest {
        assertThat(dataSource.saveTopic(topic)).isEqualTo(Answer.Success(Unit))

        assertThat(dataSource.getTopic()).isEqualTo(Answer.Success(topic))
        assertThat(preferences.getString("daily_topic", null)).isEqualTo("daily_p1")
    }

    @Test
    fun `Given nothing stored When isLegacyTopicRemoved Then false`() = runTest {
        assertThat(dataSource.isLegacyTopicRemoved(topic)).isEqualTo(Answer.Success(false))
    }

    @Test
    fun `Given a marked legacy topic When isLegacyTopicRemoved Then true only for that topic`() = runTest {
        assertThat(dataSource.markLegacyTopicRemoved(topic)).isEqualTo(Answer.Success(Unit))

        assertThat(dataSource.isLegacyTopicRemoved(topic)).isEqualTo(Answer.Success(true))
        assertThat(dataSource.isLegacyTopicRemoved(NotificationTopic("other"))).isEqualTo(Answer.Success(false))
        assertThat(preferences.getBoolean("legacy_topic_removed_daily_p1", false)).isTrue()
    }

    @Test
    fun `Given preferences throw When reading or writing Then returns failure`() = runTest {
        val broken: SharedPreferences = mockk()
        every { broken.getString(any(), any()) } throws IllegalStateException("boom")
        every { broken.getBoolean(any(), any()) } throws IllegalStateException("boom")
        every { broken.edit() } throws IllegalStateException("boom")
        val brokenDataSource = NotificationTopicLocalDataSourceImpl(broken)

        assertThat(brokenDataSource.getTopic()).isInstanceOf(Answer.Failure::class.java)
        assertThat(brokenDataSource.saveTopic(topic)).isInstanceOf(Answer.Failure::class.java)
        assertThat(brokenDataSource.isLegacyTopicRemoved(topic)).isInstanceOf(Answer.Failure::class.java)
        assertThat(brokenDataSource.markLegacyTopicRemoved(topic)).isInstanceOf(Answer.Failure::class.java)
    }
}
