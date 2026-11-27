package com.raxdenstudios.platform.notification.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.getValueOrDefault
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.commons.coroutines.ext.coThen
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationTopicRepository
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicPolicy
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicSync
import java.time.Clock
import kotlin.math.roundToInt

/**
 * Subscribes the device to the topic of its current UTC offset ([NotificationTopic.utcOffset] with the
 * [NotificationTopicPolicy.prefix]), so the server can send a push at the same local hour everywhere. The offset is
 * read from the clock zone at the current instant, so daylight saving time counts, and rounded to the nearest whole
 * hour (+5:30 → +6, -3:30 → -3).
 *
 * - Without a [NotificationTopicPolicy] bound by the app, the sync fails without touching anything.
 * - When the saved topic cannot be read, the sync fails without subscribing or saving anything: going on as if there
 *   were none could leave the device in two topics.
 * - The topic is subscribed on every sync, even when unchanged, so a subscription FCM lost is restored.
 * - When the topic differs from the one saved, the old one is left after the new one is subscribed; the new one is
 *   saved only once the old one is left, so a failure is retried in the next sync.
 * - Each [NotificationTopicPolicy.legacyTopics] entry is left once, after the topic is subscribed, and remembered. A
 *   failure is retried in the next sync and does not fail this one. A legacy topic equal to the current one is never
 *   left.
 * - Daylight saving transitions move the offset without any broadcast: the new offset is only picked up by the next
 *   process start, so until then the device keeps the topic of the previous offset.
 */
interface SyncNotificationTopicUseCase : SuspendUseCase<SyncNotificationTopicUseCase.Params, NotificationTopicSync> {

    data object Params
}

internal class SyncNotificationTopicUseCaseImpl(
    private val repository: NotificationTopicRepository,
    private val lock: NotificationTopicLock,
    private val policy: NotificationTopicPolicy?,
    private val clockProvider: () -> Clock,
) : SyncNotificationTopicUseCase {

    override suspend fun invoke(
        params: SyncNotificationTopicUseCase.Params,
    ): Answer<NotificationTopicSync, Failure> {
        val policy = policy ?: return Answer.Failure(Failure.Unknown(MISSING_POLICY_MESSAGE))
        return lock.withLock {
            val topic = NotificationTopic.utcOffset(policy.prefix, currentUtcOffsetHours())
            repository.getTopic().coFlatMap { previousTopic -> sync(policy, topic, previousTopic) }
        }
    }

    private suspend fun sync(
        policy: NotificationTopicPolicy,
        topic: NotificationTopic,
        previousTopic: NotificationTopic?,
    ): Answer<NotificationTopicSync, Failure> = subscribeTopic(topic, previousTopic).coThen {
        NotificationTopicSync(
            topic = topic,
            previousTopic = previousTopic,
            removedLegacyTopics = removeLegacyTopics(policy.legacyTopics - topic),
        )
    }

    // Subscribes before leaving the previous topic, so the device is never left without a topic.
    private suspend fun subscribeTopic(
        topic: NotificationTopic,
        previousTopic: NotificationTopic?,
    ): Answer<Unit, Failure> = repository.subscribe(topic)
        .coFlatMap { leavePreviousTopic(topic, previousTopic) }
        .coFlatMap { repository.saveTopic(topic) }

    private suspend fun leavePreviousTopic(
        topic: NotificationTopic,
        previousTopic: NotificationTopic?,
    ): Answer<Unit, Failure> = if (previousTopic == null || previousTopic == topic) {
        Answer.Success(Unit)
    } else {
        repository.unsubscribe(previousTopic)
    }

    /** Returns the legacy topics left in this call. */
    private suspend fun removeLegacyTopics(
        legacyTopics: Set<NotificationTopic>,
    ): Set<NotificationTopic> = legacyTopics.filterTo(mutableSetOf()) { legacyTopic ->
        removeLegacyTopic(legacyTopic)
    }

    /** Returns whether [legacyTopic] was left in this call. */
    private suspend fun removeLegacyTopic(
        legacyTopic: NotificationTopic,
    ): Boolean {
        if (repository.isLegacyTopicRemoved(legacyTopic).getValueOrDefault(false)) return false
        return repository.unsubscribe(legacyTopic)
            .coFlatMap { repository.markLegacyTopicRemoved(legacyTopic) } is Answer.Success
    }

    private fun currentUtcOffsetHours(): Int {
        val clock = clockProvider()
        val offsetSeconds = clock.zone.rules.getOffset(clock.instant()).totalSeconds
        return (offsetSeconds.toDouble() / SECONDS_PER_HOUR).roundToInt()
    }

    private companion object {
        const val SECONDS_PER_HOUR = 3_600
        const val MISSING_POLICY_MESSAGE = "Notification topic sync is disabled: no NotificationTopicPolicy is bound"
    }
}
