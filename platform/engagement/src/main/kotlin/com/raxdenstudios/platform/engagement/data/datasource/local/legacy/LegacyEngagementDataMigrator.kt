package com.raxdenstudios.platform.engagement.data.datasource.local.legacy

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.withTransaction
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.database.EngagementDatabase
import com.raxdenstudios.platform.engagement.data.database.dao.EngagementDao
import com.raxdenstudios.platform.engagement.data.datasource.local.mapper.EngagementEntityDataMapper
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Moves the engagement progress that older versions stored as JSON in the `platform_engagement`
 * `SharedPreferences` file (key `engagement_data`) into [EngagementDatabase].
 *
 * Temporary: remove it (with its DI binding and the `SharedPreferences` file name) once every
 * user has run a version that includes it.
 */
internal interface LegacyEngagementDataMigrator {

    /**
     * Copies the legacy payload into the database when there is one and the database holds no
     * progress yet, then deletes the payload. Idempotent and cheap once done, so it can run before
     * every access.
     */
    suspend fun migrate(): Answer<Unit, Failure>
}

/**
 * The copy runs in a Room transaction and only when the state row is absent, so a progress
 * already stored in Room is never overwritten; the payload is deleted only after the transaction
 * committed. If the process dies in between, the next call finds the row and just deletes the
 * payload, so nothing is lost or migrated twice. An unreadable payload (which older versions also
 * ignored, loading the defaults) is deleted without migrating anything.
 *
 * Payloads written before `reviewAttempts` existed used `hasRated = true` to mean "the in-app
 * review was launched once". They are migrated as `reviewAttempts = 1`, `hasRated = false`,
 * `lastReviewAt = null`, so the review can be requested again under the cooldown policy. Only
 * payloads without a `reviewAttempts` key are affected (later versions always encoded it), so a
 * `hasRated` set by `MarkAppAsRatedUseCase` is never undone.
 */
internal class LegacyEngagementDataMigratorImpl(
    private val sharedPreferences: SharedPreferences,
    private val database: EngagementDatabase,
    private val engagementEntityDataMapper: EngagementEntityDataMapper,
    private val dispatcherProvider: DispatcherProvider,
) : LegacyEngagementDataMigrator {

    private val engagementDao: EngagementDao
        get() = database.engagementDao()

    override suspend fun migrate(): Answer<Unit, Failure> = coRunCatching {
        withContext(dispatcherProvider.io) {
            val payload = sharedPreferences.getString(ENGAGEMENT_DATA_KEY, null) ?: return@withContext
            decode(payload)?.let { engagementData -> insertIfAbsent(engagementData) }
            sharedPreferences.edit(commit = true) { remove(ENGAGEMENT_DATA_KEY) }
        }
    }.thenFailure { error -> error.toUnknownError("Error migrating legacy engagement data") }

    private suspend fun insertIfAbsent(
        engagementData: EngagementData,
    ) = database.withTransaction {
        if (engagementDao.getState() != null) return@withTransaction
        engagementDao.replace(
            state = engagementEntityDataMapper.toStateEntity(engagementData),
            originCounts = engagementEntityDataMapper.toOriginCountEntities(engagementData),
        )
    }

    private fun decode(
        payload: String,
    ): EngagementData? = runCatching { json.decodeFromString<LegacyEngagementDataDto>(payload) }
        .getOrNull()
        ?.toEngagementData()

    private fun LegacyEngagementDataDto.toEngagementData(): EngagementData {
        val engagementData = EngagementData(
            hasRated = hasRated,
            hasShared = hasShared,
            counts = counts,
            shareDeclines = shareDeclines,
            lastSharePromptAt = lastSharePromptAt,
            reviewAttempts = reviewAttempts ?: 0,
            lastReviewAt = lastReviewAt,
            pendingPrompt = pendingPrompt,
        )
        return if (reviewAttempts == null) engagementData.migrateLegacyRating() else engagementData
    }

    /** Old engines set `hasRated` when the in-app review was launched: count it as one attempt. */
    private fun EngagementData.migrateLegacyRating(): EngagementData = when {
        hasRated && reviewAttempts == 0 -> copy(hasRated = false, reviewAttempts = 1, lastReviewAt = null)
        else -> this
    }

    companion object {
        const val PREFERENCES_NAME = "platform_engagement"
        private const val ENGAGEMENT_DATA_KEY = "engagement_data"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * JSON payload written by older versions. [reviewAttempts] is `null` when the key is absent,
 * which identifies payloads written before it existed.
 */
@Serializable
private data class LegacyEngagementDataDto(
    val hasRated: Boolean = false,
    val hasShared: Boolean = false,
    val counts: Map<String, Int> = emptyMap(),
    val shareDeclines: Int = 0,
    val lastSharePromptAt: Long? = null,
    val reviewAttempts: Int? = null,
    val lastReviewAt: Long? = null,
    val pendingPrompt: Boolean = false,
)
