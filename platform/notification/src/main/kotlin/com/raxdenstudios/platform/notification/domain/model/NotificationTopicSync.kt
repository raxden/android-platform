package com.raxdenstudios.platform.notification.domain.model

/**
 * Outcome of syncing the topic with the device time zone.
 *
 * @property topic The topic the device is subscribed to now.
 * @property previousTopic The topic it was subscribed to before; `null` on the first sync.
 * @property removedLegacyTopics The legacy topics the device left in this sync.
 */
data class NotificationTopicSync(
    val topic: NotificationTopic,
    val previousTopic: NotificationTopic?,
    val removedLegacyTopics: Set<NotificationTopic>,
) {

    /** Whether the subscriptions changed in this sync, the only case worth reporting. */
    val hasChanges: Boolean
        get() = topic != previousTopic || removedLegacyTopics.isNotEmpty()
}
