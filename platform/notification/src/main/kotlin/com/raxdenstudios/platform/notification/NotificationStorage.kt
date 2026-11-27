package com.raxdenstudios.platform.notification

import com.raxdenstudios.platform.notification.domain.model.NotificationTopic

/**
 * Where the module persists its flags: its own `SharedPreferences` file, never the app's default one. Public so an
 * app that kept these values in its own copy of the code can move them into [PREFERENCES_NAME] once.
 */
object NotificationStorage {

    /** Name of the `SharedPreferences` file (`Context.MODE_PRIVATE`). */
    const val PREFERENCES_NAME = "platform_notification"

    /** Boolean: the system notification dialog was shown at least once. */
    const val PERMISSION_REQUESTED_KEY = "permission_requested"

    /** String: the UTC offset topic the device is subscribed to. */
    const val DAILY_TOPIC_KEY = "daily_topic"

    private const val LEGACY_TOPIC_REMOVED_KEY_PREFIX = "legacy_topic_removed_"

    /** Boolean: the device already left the legacy [topic]. */
    fun legacyTopicRemovedKey(
        topic: NotificationTopic,
    ): String = "$LEGACY_TOPIC_REMOVED_KEY_PREFIX${topic.value}"
}
