package com.raxdenstudios.platform.notification.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.datasource.NotificationTopicLocalDataSource
import com.raxdenstudios.platform.notification.data.datasource.NotificationTopicRemoteDataSource
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic

/** FCM topic subscriptions of the device and the record of the topics it subscribed to and left. */
internal interface NotificationTopicRepository {

    suspend fun subscribe(
        topic: NotificationTopic,
    ): Answer<Unit, Failure>

    suspend fun unsubscribe(
        topic: NotificationTopic,
    ): Answer<Unit, Failure>

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

internal class NotificationTopicRepositoryImpl(
    private val remoteDataSource: NotificationTopicRemoteDataSource,
    private val localDataSource: NotificationTopicLocalDataSource,
) : NotificationTopicRepository {

    override suspend fun subscribe(
        topic: NotificationTopic,
    ): Answer<Unit, Failure> = remoteDataSource.subscribe(topic)

    override suspend fun unsubscribe(
        topic: NotificationTopic,
    ): Answer<Unit, Failure> = remoteDataSource.unsubscribe(topic)

    override suspend fun getTopic(): Answer<NotificationTopic?, Failure> = localDataSource.getTopic()

    override suspend fun saveTopic(
        topic: NotificationTopic,
    ): Answer<Unit, Failure> = localDataSource.saveTopic(topic)

    override suspend fun isLegacyTopicRemoved(
        topic: NotificationTopic,
    ): Answer<Boolean, Failure> = localDataSource.isLegacyTopicRemoved(topic)

    override suspend fun markLegacyTopicRemoved(
        topic: NotificationTopic,
    ): Answer<Unit, Failure> = localDataSource.markLegacyTopicRemoved(topic)
}
