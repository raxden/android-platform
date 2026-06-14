package com.raxdenstudios.platform.device.data

import com.google.firebase.messaging.FirebaseMessaging
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.commons.core.Answer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface FirebaseMessagingProvider {
    suspend fun subscribeToTopic(topic: String): Answer<Unit, Failure>
    suspend fun getToken(): Answer<String, Failure>
}

internal class FirebaseMessagingProviderImpl :
    com.raxdenstudios.platform.device.data.FirebaseMessagingProvider {

    override suspend fun subscribeToTopic(topic: String): Answer<Unit, Failure> =
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnSuccessListener {
                    continuation.resume(Answer.Success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(
                        Answer.Failure(
                            Failure.Unknown("Failed to subscribe to topic: ${exception.message}")
                        )
                    )
                }
        }

    override suspend fun getToken(): Answer<String, Failure> =
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    continuation.resume(Answer.Success(token))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(
                        Answer.Failure(
                            Failure.Unknown("Failed to get FCM token: ${exception.message}")
                        )
                    )
                }
        }
}
