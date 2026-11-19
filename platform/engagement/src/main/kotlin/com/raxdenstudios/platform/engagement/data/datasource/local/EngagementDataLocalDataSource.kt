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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

internal interface EngagementDataLocalDataSource {

    fun observeEngagementData(): Flow<Answer<EngagementData, Failure>>

    suspend fun getEngagementData(): Answer<EngagementData, Failure>

    suspend fun updateEngagementData(
        engagementData: EngagementData,
    ): Answer<Unit, Failure>
}

/**
 * Persists [EngagementData] as JSON in `SharedPreferences`.
 *
 * Legacy migration: payloads written before `reviewAttempts` existed used `hasRated = true` to
 * mean "the in-app review was launched once". They are loaded as `reviewAttempts = 1`,
 * `hasRated = false`, `lastReviewAt = null`, so the review can be requested again under the new
 * cooldown policy. Only payloads without a `reviewAttempts` key are migrated (current payloads
 * always encode it), so a `hasRated` set by `MarkAppAsRatedUseCase` is never undone. The migrated
 * value is kept in memory and persisted by the next write.
 */
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

    override suspend fun updateEngagementData(
        engagementData: EngagementData,
    ): Answer<Unit, Failure> = coRunCatching {
        val current = state()
        withContext(dispatcherProvider.io) { sharedPreferences.persist(engagementData) }
        current.value = engagementData
    }.thenFailure { error -> error.toUnknownError("Error to update engagement data") }

    private suspend fun state(): MutableStateFlow<EngagementData> = initMutex.withLock {
        state ?: MutableStateFlow(withContext(dispatcherProvider.io) { load() }).also { state = it }
    }

    private fun load(): EngagementData = sharedPreferences.getString(ENGAGEMENT_DATA_KEY, null)
        ?.let { runCatching { decode(it) }.getOrNull() }
        ?: EngagementData()

    private fun decode(
        value: String,
    ): EngagementData {
        val jsonObject = json.parseToJsonElement(value).jsonObject
        val engagementData = json.decodeFromJsonElement<EngagementData>(jsonObject)
        return if (jsonObject.isLegacyPayload()) engagementData.migrateLegacyRating() else engagementData
    }

    private fun JsonObject.isLegacyPayload(): Boolean = REVIEW_ATTEMPTS_KEY !in this

    /** Old engines set `hasRated` when the in-app review was launched: count it as one attempt. */
    private fun EngagementData.migrateLegacyRating(): EngagementData = when {
        hasRated && reviewAttempts == 0 -> copy(hasRated = false, reviewAttempts = 1, lastReviewAt = null)
        else -> this
    }

    private fun SharedPreferences.persist(
        engagementData: EngagementData,
    ) {
        edit { putString(ENGAGEMENT_DATA_KEY, json.encodeToString<EngagementData>(engagementData)) }
    }

    companion object {
        const val PREFERENCES_NAME = "platform_engagement"
        private const val ENGAGEMENT_DATA_KEY = "engagement_data"
        private const val REVIEW_ATTEMPTS_KEY = "reviewAttempts"
        private val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
