package com.raxdenstudios.platform.notification.domain

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationTopicRepository
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicPolicy
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Runs [SyncNotificationTopicUseCaseImpl] over the real [NotificationTopicLockImpl] and an in-memory repository. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SyncNotificationTopicConcurrencyTest {

    private val repository = InMemoryNotificationTopicRepository()
    private val zones = ArrayDeque(listOf("Europe/Madrid", "America/New_York"))
    private val useCase = SyncNotificationTopicUseCaseImpl(
        repository = repository,
        lock = NotificationTopicLockImpl(),
        policy = NotificationTopicPolicy(prefix = "daily_"),
        clockProvider = { Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneId.of(zones.removeFirst())) },
    )

    @Test
    fun `Given two concurrent syncs When both run Then the second sees the topic saved by the first`() = runTest {
        val first = async { useCase(SyncNotificationTopicUseCase.Params) }
        val second = async { useCase(SyncNotificationTopicUseCase.Params) }
        advanceUntilIdle()

        val firstSync = (first.await() as Answer.Success).value
        val secondSync = (second.await() as Answer.Success).value
        assertThat(firstSync).isEqualTo(
            NotificationTopicSync(NotificationTopic("daily_p1"), null, emptySet()),
        )
        assertThat(secondSync).isEqualTo(
            NotificationTopicSync(NotificationTopic("daily_m5"), NotificationTopic("daily_p1"), emptySet()),
        )
        assertThat(repository.subscribed).containsExactly(NotificationTopic("daily_m5"))
    }

    private class InMemoryNotificationTopicRepository : NotificationTopicRepository {

        private var saved: NotificationTopic? = null
        val subscribed = mutableSetOf<NotificationTopic>()

        override suspend fun subscribe(
            topic: NotificationTopic,
        ): Answer<Unit, Failure> {
            delay(DELAY_MS)
            subscribed += topic
            return Answer.Success(Unit)
        }

        override suspend fun unsubscribe(
            topic: NotificationTopic,
        ): Answer<Unit, Failure> {
            delay(DELAY_MS)
            subscribed -= topic
            return Answer.Success(Unit)
        }

        override suspend fun getTopic(): Answer<NotificationTopic?, Failure> {
            delay(DELAY_MS)
            return Answer.Success(saved)
        }

        override suspend fun saveTopic(
            topic: NotificationTopic,
        ): Answer<Unit, Failure> {
            delay(DELAY_MS)
            saved = topic
            return Answer.Success(Unit)
        }

        override suspend fun isLegacyTopicRemoved(
            topic: NotificationTopic,
        ): Answer<Boolean, Failure> = Answer.Success(false)

        override suspend fun markLegacyTopicRemoved(
            topic: NotificationTopic,
        ): Answer<Unit, Failure> = Answer.Success(Unit)
    }

    private companion object {
        const val DELAY_MS = 10L
    }
}
