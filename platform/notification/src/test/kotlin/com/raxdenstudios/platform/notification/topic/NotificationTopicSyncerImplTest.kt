package com.raxdenstudios.platform.notification.topic

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.domain.SyncNotificationTopicUseCase
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicPolicy
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicSync
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

internal class NotificationTopicSyncerImplTest {

    private val useCase: SyncNotificationTopicUseCase = mockk()
    private val listener: NotificationTopicSyncListener = mockk(relaxed = true)
    private val policy = NotificationTopicPolicy(prefix = "daily_")
    private val topic = NotificationTopic("daily_p1")

    @Before
    fun setUp() {
        coEvery { useCase(any()) } returns Answer.Success(NotificationTopicSync(topic, null, emptySet()))
    }

    private fun syncer(
        policy: NotificationTopicPolicy? = this.policy,
        listener: NotificationTopicSyncListener? = this.listener,
    ) = NotificationTopicSyncerImpl(useCase, policy, listener)

    @Test
    fun `Given no policy When sync Then the use case is not called`() = runTest {
        syncer(policy = null).sync()

        coVerify(exactly = 0) { useCase(any()) }
        verify(exactly = 0) { listener.onTopicChanged(any()) }
    }

    @Test
    fun `Given a sync with changes When sync Then the listener is notified`() = runTest {
        val sync = NotificationTopicSync(topic, null, emptySet())
        coEvery { useCase(any()) } returns Answer.Success(sync)

        syncer().sync()

        verify(exactly = 1) { listener.onTopicChanged(sync) }
    }

    @Test
    fun `Given a sync without changes When sync Then the listener is not notified`() = runTest {
        coEvery { useCase(any()) } returns Answer.Success(NotificationTopicSync(topic, topic, emptySet()))

        syncer().sync()

        verify(exactly = 0) { listener.onTopicChanged(any()) }
    }

    @Test
    fun `Given no listener When sync with changes Then it completes`() = runTest {
        syncer(listener = null).sync()

        coVerify(exactly = 1) { useCase(any()) }
    }

    @Test
    fun `Given the sync fails When sync Then the listener is not notified and nothing throws`() = runTest {
        coEvery { useCase(any()) } returns Answer.Failure(Failure.Unknown("boom"))

        syncer().sync()

        verify(exactly = 0) { listener.onTopicChanged(any()) }
    }
}
