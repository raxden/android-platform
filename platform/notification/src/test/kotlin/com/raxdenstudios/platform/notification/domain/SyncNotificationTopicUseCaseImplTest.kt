package com.raxdenstudios.platform.notification.domain

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationTopicRepository
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicPolicy
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicSync
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

internal class SyncNotificationTopicUseCaseImplTest {

    private val repository: NotificationTopicRepository = mockk()
    private val lock: NotificationTopicLock = mockk()
    private val failure = Answer.Failure(mockk<Failure>())
    private val policy = NotificationTopicPolicy(prefix = "daily_")
    private var lockEntered = 0

    @Before
    fun setUp() {
        coEvery { lock.withLock<Answer<NotificationTopicSync, Failure>>(captureLambda()) } coAnswers {
            lockEntered++
            lambda<suspend () -> Answer<NotificationTopicSync, Failure>>().captured.invoke()
        }
        coEvery { repository.getTopic() } returns Answer.Success(null)
        coEvery { repository.subscribe(any()) } returns Answer.Success(Unit)
        coEvery { repository.unsubscribe(any()) } returns Answer.Success(Unit)
        coEvery { repository.saveTopic(any()) } returns Answer.Success(Unit)
        coEvery { repository.isLegacyTopicRemoved(any()) } returns Answer.Success(false)
        coEvery { repository.markLegacyTopicRemoved(any()) } returns Answer.Success(Unit)
    }

    private fun useCase(
        policy: NotificationTopicPolicy? = this.policy,
        zone: String = "Europe/Madrid",
        instant: String = "2026-01-15T12:00:00Z",
    ) = SyncNotificationTopicUseCaseImpl(
        repository = repository,
        lock = lock,
        policy = policy,
        clockProvider = { Clock.fixed(Instant.parse(instant), ZoneId.of(zone)) },
    )

    private suspend fun SyncNotificationTopicUseCaseImpl.sync() = invoke(SyncNotificationTopicUseCase.Params)

    @Test
    fun `Given no policy When sync Then fails without touching the repository`() = runTest {
        val result = useCase(policy = null).sync()

        assertThat(result).isInstanceOf(Answer.Failure::class.java)
        assertThat(lockEntered).isEqualTo(0)
        coVerify(exactly = 0) { repository.subscribe(any()) }
    }

    @Test
    fun `Given the saved topic cannot be read When sync Then fails without subscribing`() = runTest {
        coEvery { repository.getTopic() } returns failure

        assertThat(useCase().sync()).isEqualTo(failure)
        coVerify(exactly = 0) { repository.subscribe(any()) }
    }

    @Test
    fun `Given the first sync When sync Then subscribes saves and reports no previous topic`() = runTest {
        val result = useCase().sync()

        val topic = NotificationTopic("daily_p1")
        assertThat(result).isEqualTo(Answer.Success(NotificationTopicSync(topic, null, emptySet())))
        coVerify { repository.subscribe(topic) }
        coVerify { repository.saveTopic(topic) }
        coVerify(exactly = 0) { repository.unsubscribe(any()) }
    }

    @Test
    fun `Given the same saved topic When sync Then subscribes again without leaving it`() = runTest {
        val topic = NotificationTopic("daily_p1")
        coEvery { repository.getTopic() } returns Answer.Success(topic)

        val result = useCase().sync()

        assertThat(result).isEqualTo(Answer.Success(NotificationTopicSync(topic, topic, emptySet())))
        coVerify(exactly = 1) { repository.subscribe(topic) }
        coVerify(exactly = 0) { repository.unsubscribe(any()) }
    }

    @Test
    fun `Given a previous topic When sync Then subscribes the new one before leaving the old one`() = runTest {
        val previous = NotificationTopic("daily_m5")
        coEvery { repository.getTopic() } returns Answer.Success(previous)

        useCase().sync()

        coVerifyOrder {
            repository.subscribe(NotificationTopic("daily_p1"))
            repository.unsubscribe(previous)
            repository.saveTopic(NotificationTopic("daily_p1"))
        }
    }

    @Test
    fun `Given subscribing fails When sync Then nothing is left or saved`() = runTest {
        coEvery { repository.getTopic() } returns Answer.Success(NotificationTopic("daily_m5"))
        coEvery { repository.subscribe(any()) } returns failure

        assertThat(useCase().sync()).isEqualTo(failure)
        coVerify(exactly = 0) { repository.unsubscribe(any()) }
        coVerify(exactly = 0) { repository.saveTopic(any()) }
    }

    @Test
    fun `Given leaving the previous topic fails When sync Then fails and saves nothing`() = runTest {
        val previous = NotificationTopic("daily_m5")
        coEvery { repository.getTopic() } returns Answer.Success(previous)
        coEvery { repository.unsubscribe(previous) } returns failure

        assertThat(useCase().sync()).isEqualTo(failure)
        coVerify(exactly = 0) { repository.saveTopic(any()) }
    }

    @Test
    fun `Given saving fails When sync Then fails`() = runTest {
        coEvery { repository.saveTopic(any()) } returns failure

        assertThat(useCase().sync()).isEqualTo(failure)
    }

    @Test
    fun `Given legacy topics When sync Then each is left once and remembered`() = runTest {
        val legacyA = NotificationTopic("old_a")
        val legacyB = NotificationTopic("old_b")

        val result = useCase(policy.copy(legacyTopics = setOf(legacyA, legacyB))).sync()

        assertThat((result as Answer.Success).value.removedLegacyTopics).containsExactly(legacyA, legacyB)
        coVerify(exactly = 1) { repository.unsubscribe(legacyA) }
        coVerify(exactly = 1) { repository.unsubscribe(legacyB) }
        coVerify(exactly = 1) { repository.markLegacyTopicRemoved(legacyA) }
        coVerify(exactly = 1) { repository.markLegacyTopicRemoved(legacyB) }
    }

    @Test
    fun `Given a legacy topic already removed When sync Then it is skipped`() = runTest {
        val legacy = NotificationTopic("old_a")
        coEvery { repository.isLegacyTopicRemoved(legacy) } returns Answer.Success(true)

        val result = useCase(policy.copy(legacyTopics = setOf(legacy))).sync()

        assertThat((result as Answer.Success).value.removedLegacyTopics).isEmpty()
        coVerify(exactly = 0) { repository.unsubscribe(legacy) }
    }

    @Test
    fun `Given leaving a legacy topic fails When sync Then it is retried and the sync still succeeds`() = runTest {
        val legacy = NotificationTopic("old_a")
        coEvery { repository.unsubscribe(legacy) } returns failure

        val result = useCase(policy.copy(legacyTopics = setOf(legacy))).sync()

        assertThat(result).isInstanceOf(Answer.Success::class.java)
        assertThat((result as Answer.Success).value.removedLegacyTopics).isEmpty()
        coVerify(exactly = 0) { repository.markLegacyTopicRemoved(legacy) }
    }

    @Test
    fun `Given remembering a legacy topic fails When sync Then the sync still succeeds without it`() = runTest {
        val legacy = NotificationTopic("old_a")
        coEvery { repository.markLegacyTopicRemoved(legacy) } returns failure

        val result = useCase(policy.copy(legacyTopics = setOf(legacy))).sync()

        assertThat((result as Answer.Success).value.removedLegacyTopics).isEmpty()
    }

    @Test
    fun `Given the legacy state cannot be read When sync Then it is treated as not removed`() = runTest {
        val legacy = NotificationTopic("old_a")
        coEvery { repository.isLegacyTopicRemoved(legacy) } returns failure

        val result = useCase(policy.copy(legacyTopics = setOf(legacy))).sync()

        assertThat((result as Answer.Success).value.removedLegacyTopics).containsExactly(legacy)
    }

    @Test
    fun `Given a legacy topic equal to the current one When sync Then it is not left`() = runTest {
        val current = NotificationTopic("daily_p1")

        val result = useCase(policy.copy(legacyTopics = setOf(current))).sync()

        assertThat((result as Answer.Success).value.removedLegacyTopics).isEmpty()
        coVerify(exactly = 0) { repository.unsubscribe(current) }
    }

    @Test
    fun `Given Kolkata with a half hour offset When sync Then rounds up to p6`() = runTest {
        useCase(zone = "Asia/Kolkata").sync()

        coVerify { repository.subscribe(NotificationTopic("daily_p6")) }
    }

    @Test
    fun `Given Newfoundland with a negative half hour offset When sync Then rounds toward zero to m3`() = runTest {
        useCase(zone = "America/St_Johns", instant = "2026-01-15T12:00:00Z").sync()

        coVerify { repository.subscribe(NotificationTopic("daily_m3")) }
    }

    @Test
    fun `Given New York in winter and summer When sync Then uses m5 and m4`() = runTest {
        useCase(zone = "America/New_York", instant = "2026-01-15T12:00:00Z").sync()
        useCase(zone = "America/New_York", instant = "2026-07-15T12:00:00Z").sync()

        coVerify { repository.subscribe(NotificationTopic("daily_m5")) }
        coVerify { repository.subscribe(NotificationTopic("daily_m4")) }
    }

    @Test
    fun `Given a policy When sync Then the work runs under the lock`() = runTest {
        useCase().sync()
        useCase().sync()

        assertThat(lockEntered).isEqualTo(2)
    }

    @Test
    fun `Given the zone changes between syncs When sync Then the clock is read on every sync`() = runTest {
        var zone = "Europe/Madrid"
        val useCase = SyncNotificationTopicUseCaseImpl(
            repository = repository,
            lock = lock,
            policy = policy,
            clockProvider = { Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneId.of(zone)) },
        )

        useCase.sync()
        zone = "America/New_York"
        useCase.sync()

        coVerify { repository.subscribe(NotificationTopic("daily_p1")) }
        coVerify { repository.subscribe(NotificationTopic("daily_m5")) }
    }
}
