package com.raxdenstudios.platform.notification.data.repository

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.datasource.NotificationTopicLocalDataSource
import com.raxdenstudios.platform.notification.data.datasource.NotificationTopicRemoteDataSource
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class NotificationTopicRepositoryImplTest {

    private val remote: NotificationTopicRemoteDataSource = mockk()
    private val local: NotificationTopicLocalDataSource = mockk()
    private val repository = NotificationTopicRepositoryImpl(remote, local)
    private val topic = NotificationTopic("daily_p1")

    @Test
    fun `Given a topic When subscribe Then delegates to the remote data source`() = runTest {
        coEvery { remote.subscribe(topic) } returns Answer.Success(Unit)

        assertThat(repository.subscribe(topic)).isEqualTo(Answer.Success(Unit))
        coVerify { remote.subscribe(topic) }
    }

    @Test
    fun `Given a topic When unsubscribe Then delegates to the remote data source`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        coEvery { remote.unsubscribe(topic) } returns failure

        assertThat(repository.unsubscribe(topic)).isEqualTo(failure)
    }

    @Test
    fun `Given a stored topic When getTopic Then delegates to the local data source`() = runTest {
        coEvery { local.getTopic() } returns Answer.Success(topic)

        assertThat(repository.getTopic()).isEqualTo(Answer.Success(topic))
    }

    @Test
    fun `Given a topic When saveTopic Then delegates to the local data source`() = runTest {
        coEvery { local.saveTopic(topic) } returns Answer.Success(Unit)

        assertThat(repository.saveTopic(topic)).isEqualTo(Answer.Success(Unit))
        coVerify { local.saveTopic(topic) }
    }

    @Test
    fun `Given a topic When isLegacyTopicRemoved Then delegates to the local data source`() = runTest {
        coEvery { local.isLegacyTopicRemoved(topic) } returns Answer.Success(true)

        assertThat(repository.isLegacyTopicRemoved(topic)).isEqualTo(Answer.Success(true))
    }

    @Test
    fun `Given a topic When markLegacyTopicRemoved Then delegates to the local data source`() = runTest {
        coEvery { local.markLegacyTopicRemoved(topic) } returns Answer.Success(Unit)

        assertThat(repository.markLegacyTopicRemoved(topic)).isEqualTo(Answer.Success(Unit))
        coVerify { local.markLegacyTopicRemoved(topic) }
    }
}
