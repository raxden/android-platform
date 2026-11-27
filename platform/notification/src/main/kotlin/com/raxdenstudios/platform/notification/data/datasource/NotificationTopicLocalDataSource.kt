package com.raxdenstudios.platform.notification.data.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.NotificationStorage
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic

/**
 * Remembers the topic the device subscribed to, so a later time zone can leave it, and which legacy topics it
 * already left.
 */
internal interface NotificationTopicLocalDataSource {

    suspend fun getTopic(): Answer<NotificationTopic?, Failure>

    suspend fun saveTopic(
        topic: NotificationTopic,
    ): Answer<Unit, Failure>

    suspend fun isLegacyTopicRemoved(
        topic: NotificationTopic,
    ): Answer<Boolean, Failure>

    suspend fun markLegacyTopicRemoved(
        topic: NotificationTopic,
    ): Answer<Unit, Failure>
}

internal class NotificationTopicLocalDataSourceImpl(
    private val sharedPreferences: SharedPreferences,
) : NotificationTopicLocalDataSource {

    override suspend fun getTopic(): Answer<NotificationTopic?, Failure> = coRunCatching {
        sharedPreferences.getString(NotificationStorage.DAILY_TOPIC_KEY, null)?.let(::NotificationTopic)
    }.thenFailure { error -> error.toUnknownError("Error reading the notification topic") }

    override suspend fun saveTopic(
        topic: NotificationTopic,
    ): Answer<Unit, Failure> = coRunCatching {
        sharedPreferences.edit { putString(NotificationStorage.DAILY_TOPIC_KEY, topic.value) }
    }.thenFailure { error -> error.toUnknownError("Error saving the notification topic") }

    override suspend fun isLegacyTopicRemoved(
        topic: NotificationTopic,
    ): Answer<Boolean, Failure> = coRunCatching {
        sharedPreferences.getBoolean(NotificationStorage.legacyTopicRemovedKey(topic), false)
    }.thenFailure { error -> error.toUnknownError("Error reading the legacy notification topic ${topic.value}") }

    override suspend fun markLegacyTopicRemoved(
        topic: NotificationTopic,
    ): Answer<Unit, Failure> = coRunCatching {
        sharedPreferences.edit { putBoolean(NotificationStorage.legacyTopicRemovedKey(topic), true) }
    }.thenFailure { error -> error.toUnknownError("Error saving the legacy notification topic ${topic.value}") }
}
