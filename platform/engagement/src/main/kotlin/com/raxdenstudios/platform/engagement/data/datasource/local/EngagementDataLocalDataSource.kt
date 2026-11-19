package com.raxdenstudios.platform.engagement.data.datasource.local

import androidx.room.withTransaction
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.commons.coroutines.ext.toAnswer
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.database.EngagementDatabase
import com.raxdenstudios.platform.engagement.data.database.dao.EngagementDao
import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity
import com.raxdenstudios.platform.engagement.data.datasource.local.mapper.EngagementEntityDataMapper
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface EngagementDataLocalDataSource {

    fun observeEngagementData(): Flow<Answer<EngagementData, Failure>>

    suspend fun getEngagementData(): Answer<EngagementData, Failure>

    /**
     * Atomically replaces the stored data with the result of [update] applied to its current
     * value, and returns the stored result. Concurrent updates never overwrite each other.
     */
    suspend fun updateEngagementData(
        update: (EngagementData) -> EngagementData,
    ): Answer<EngagementData, Failure>
}

/**
 * Persists [EngagementData] in [EngagementDatabase]: the scalar fields in the single-row
 * `engagement_state` table and the per-origin counts in `engagement_origin_count`. Reads and
 * read-modify-write updates run in a Room transaction, so they always see both tables
 * consistently and concurrent updates are serialized.
 *
 * An update writes only what changed: nothing when the update returns the same data; otherwise
 * the state row only when its fields changed, or when something changed and the row does not
 * exist yet; and only the origins whose count changed (origins removed from the map are
 * deleted).
 */
internal class EngagementDataLocalDataSourceImpl(
    private val database: EngagementDatabase,
    private val engagementEntityDataMapper: EngagementEntityDataMapper,
    private val dispatcherProvider: DispatcherProvider,
) : EngagementDataLocalDataSource {

    private val engagementDao: EngagementDao
        get() = database.engagementDao()

    override fun observeEngagementData(): Flow<Answer<EngagementData, Failure>> =
        database.invalidationTracker.createFlow(*OBSERVED_TABLES)
            .map { database.withTransaction { readEngagementData() } }
            .distinctUntilChanged()
            .toAnswer { error -> error.toUnknownError("Error to observe engagement data") }
            .flowOn(dispatcherProvider.io)

    override suspend fun getEngagementData(): Answer<EngagementData, Failure> =
        coRunCatching { database.withTransaction { readEngagementData() } }
            .thenFailure { error -> error.toUnknownError("Error to get engagement data") }

    override suspend fun updateEngagementData(
        update: (EngagementData) -> EngagementData,
    ): Answer<EngagementData, Failure> = coRunCatching { updateStoredEngagementData(update) }
        .thenFailure { error -> error.toUnknownError("Error to update engagement data") }

    private suspend fun updateStoredEngagementData(
        update: (EngagementData) -> EngagementData,
    ): EngagementData = database.withTransaction {
        val storedState = engagementDao.getState()
        val current = engagementEntityDataMapper.toEngagementData(storedState, engagementDao.getOriginCounts())
        val updated = update(current)
        if (updated != current) {
            writeState(storedState, updated)
            writeOriginCounts(current.counts, updated.counts)
        }
        updated
    }

    private suspend fun readEngagementData(): EngagementData = engagementEntityDataMapper.toEngagementData(
        state = engagementDao.getState(),
        originCounts = engagementDao.getOriginCounts(),
    )

    private suspend fun writeState(
        storedState: EngagementStateEntity?,
        updated: EngagementData,
    ) {
        val state = engagementEntityDataMapper.toStateEntity(updated)
        if (state != storedState) engagementDao.upsertState(state)
    }

    private suspend fun writeOriginCounts(
        current: Map<String, Int>,
        updated: Map<String, Int>,
    ) {
        val removedOrigins = (current.keys - updated.keys).toList()
        if (removedOrigins.isNotEmpty()) engagementDao.deleteOriginCounts(removedOrigins)
        val changedCounts = updated.filter { (origin, count) -> current[origin] != count }
        if (changedCounts.isNotEmpty()) {
            engagementDao.upsertOriginCounts(engagementEntityDataMapper.toOriginCountEntities(changedCounts))
        }
    }

    private companion object {
        val OBSERVED_TABLES = arrayOf(
            EngagementStateEntity.TABLE_NAME,
            EngagementOriginCountEntity.TABLE_NAME,
        )
    }
}
