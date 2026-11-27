package com.raxdenstudios.platform.notification.data.datasource

import com.google.firebase.messaging.FirebaseMessaging
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.data.ext.awaitUnitAnswer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic

/**
 * The FCM topic subscriptions of this device, through [FirebaseMessaging]. FCM queues a subscription made offline
 * and may never complete its task, so every call gives up after the platform task timeout with a failure: callers
 * holding a lock or a broadcast never wait forever, and the next sync tries again.
 *
 * [FirebaseMessaging] is only obtained on the first call, so building this data source never touches Firebase (an
 * app without topics or without Firebase never reaches it).
 */
internal interface NotificationTopicRemoteDataSource {

    suspend fun subscribe(
        topic: NotificationTopic,
    ): Answer<Unit, Failure>

    suspend fun unsubscribe(
        topic: NotificationTopic,
    ): Answer<Unit, Failure>
}

internal class NotificationTopicRemoteDataSourceImpl(
    firebaseMessagingProvider: () -> FirebaseMessaging,
) : NotificationTopicRemoteDataSource {

    private val firebaseMessaging: FirebaseMessaging by lazy(firebaseMessagingProvider)

    override suspend fun subscribe(
        topic: NotificationTopic,
    ): Answer<Unit, Failure> = firebaseMessaging.subscribeToTopic(topic.value)
        .awaitUnitAnswer("Error subscribing to topic ${topic.value}")

    override suspend fun unsubscribe(
        topic: NotificationTopic,
    ): Answer<Unit, Failure> = firebaseMessaging.unsubscribeFromTopic(topic.value)
        .awaitUnitAnswer("Error unsubscribing from topic ${topic.value}")
}
