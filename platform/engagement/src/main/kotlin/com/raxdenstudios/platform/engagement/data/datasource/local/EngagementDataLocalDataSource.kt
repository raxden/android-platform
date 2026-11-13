package com.raxdenstudios.platform.engagement.data.datasource.local

import android.content.SharedPreferences
import androidx.core.content.edit
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal interface EngagementDataLocalDataSource {

    fun observeEngagementData(): Flow<Answer<EngagementData, Failure>>

    suspend fun getEngagementData(): Answer<EngagementData, Failure>

    suspend fun updateEngagementData(engagementData: EngagementData): Answer<Unit, Failure>
}

internal class EngagementDataLocalDataSourceImpl(
    private val sharedPreferences: SharedPreferences,
    private val json: Json = DEFAULT_JSON,
    private val dispatcherProvider: DispatcherProvider,
) : EngagementDataLocalDataSource {

    private val initMutex = Mutex()
    private var state: MutableStateFlow<EngagementData>? = null

    override fun observeEngagementData(): Flow<Answer<EngagementData, Failure>> = flow {
        emitAll(state().map { Answer.Success(it) })
    }

    override suspend fun getEngagementData(): Answer<EngagementData, Failure> = coRunCatching { state().value }
        .thenFailure { error -> error.toUnknownError("Error to get engagement data") }

    override suspend fun updateEngagementData(engagementData: EngagementData): Answer<Unit, Failure> = coRunCatching {
        val current = state()
        withContext(dispatcherProvider.io) { sharedPreferences.persist(engagementData) }
        current.value = engagementData
    }.thenFailure { error -> error.toUnknownError("Error to update engagement data") }

    private suspend fun state(): MutableStateFlow<EngagementData> = initMutex.withLock {
        state ?: MutableStateFlow(withContext(dispatcherProvider.io) { load() }).also { state = it }
    }

    private fun load(): EngagementData = sharedPreferences.getString(ENGAGEMENT_DATA_KEY, null)
        ?.let { runCatching { json.decodeFromString<EngagementData>(it) }.getOrNull() }
        ?: EngagementData()

    private fun SharedPreferences.persist(engagementData: EngagementData) {
        edit { putString(ENGAGEMENT_DATA_KEY, json.encodeToString<EngagementData>(engagementData)) }
    }

    companion object {
        const val PREFERENCES_NAME = "platform_engagement"
        private const val ENGAGEMENT_DATA_KEY = "engagement_data"
        private val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
