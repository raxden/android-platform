package com.raxdenstudios.platform.notification.domain.model

/**
 * How the app names its UTC offset topics. Bound by the app, never by the library: while it is not bound, topic
 * sync is disabled.
 *
 * @property prefix Prefix of every UTC offset topic, e.g. `radio_daily_utc_` ([NotificationTopic.utcOffset]).
 * @property legacyTopics Topics older app versions subscribed to; each one is left once and then remembered.
 */
data class NotificationTopicPolicy(
    val prefix: String,
    val legacyTopics: Set<NotificationTopic> = emptySet(),
)
