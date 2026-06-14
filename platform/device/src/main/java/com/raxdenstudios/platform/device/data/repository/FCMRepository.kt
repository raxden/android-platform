package com.raxdenstudios.platform.device.data.repository

import com.raxdenstudios.platform.device.data.FirebaseMessagingProvider
import com.raxdenstudios.platform.device.data.datasource.FCMLocalDataSource
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.EMPTY
import com.raxdenstudios.commons.core.ext.getValueOrDefault
import com.raxdenstudios.commons.coroutines.ext.coThen
import com.raxdenstudios.commons.coroutines.ext.onCoSuccess
import com.raxdenstudios.platform.core.domain.model.Failure

interface FCMRepository {
    suspend fun subscribeToTopic(topic: String): Answer<Unit, Failure>
    suspend fun saveToken(token: String): Answer<Unit, Failure>
    suspend fun getToken(): Answer<String, Failure>
    suspend fun clearToken(): Answer<Unit, Failure>
}

internal class FCMRepositoryImpl(
    private val fcmLocalDataSource: com.raxdenstudios.platform.device.data.datasource.FCMLocalDataSource,
    private val firebaseMessagingProvider: com.raxdenstudios.platform.device.data.FirebaseMessagingProvider
) : com.raxdenstudios.platform.device.data.repository.FCMRepository {

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
