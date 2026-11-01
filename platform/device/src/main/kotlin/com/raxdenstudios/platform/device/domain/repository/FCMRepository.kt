package com.raxdenstudios.platform.device.domain.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure

interface FCMRepository {
    suspend fun subscribeToTopic(topic: String): Answer<Unit, Failure>
    suspend fun saveToken(token: String): Answer<Unit, Failure>
    suspend fun getToken(): Answer<String, Failure>
    suspend fun clearToken(): Answer<Unit, Failure>
}
