package com.raxdenstudios.platform.device.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.EMPTY
import com.raxdenstudios.commons.core.ext.getValueOrDefault
import com.raxdenstudios.commons.coroutines.ext.coThen
import com.raxdenstudios.commons.coroutines.ext.onCoSuccess
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.FirebaseMessagingProvider
import com.raxdenstudios.platform.device.data.datasource.FCMLocalDataSource

interface FCMRepository {
    suspend fun subscribeToTopic(topic: String): Answer<Unit, Failure>
    suspend fun saveToken(token: String): Answer<Unit, Failure>
    suspend fun getToken(): Answer<String, Failure>
    suspend fun clearToken(): Answer<Unit, Failure>
}

internal class FCMRepositoryImpl(
    private val fcmLocalDataSource: FCMLocalDataSource,
    private val firebaseMessagingProvider: FirebaseMessagingProvider
) : FCMRepository {

    override suspend fun subscribeToTopic(topic: String): Answer<Unit, Failure> =
        firebaseMessagingProvider.subscribeToTopic(topic)

    override suspend fun saveToken(token: String): Answer<Unit, Failure> =
        fcmLocalDataSource.saveToken(token)

    override suspend fun getToken(): Answer<String, Failure> =
        fcmLocalDataSource.getToken()
            .coThen { token -> token.ifEmpty { getTokenFromFirebaseAndUpdate() } }

    override suspend fun clearToken(): Answer<Unit, Failure> =
        fcmLocalDataSource.clearToken()

    private suspend fun getTokenFromFirebaseAndUpdate(): String =
        firebaseMessagingProvider.getToken().also { result ->
            result.onCoSuccess { token -> saveToken(token) }
        }.getValueOrDefault(String.EMPTY)
}
