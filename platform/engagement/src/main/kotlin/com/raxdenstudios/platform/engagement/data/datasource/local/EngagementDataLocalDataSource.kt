package com.raxdenstudios.platform.engagement.data.datasource.local

import androidx.room.withTransaction
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.commons.coroutines.ext.toAnswer
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.database.EngagementDatabase
import com.raxdenstudios.platform.engagement.data.database.dao.EngagementDao
import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity
import com.raxdenstudios.platform.engagement.data.datasource.local.legacy.LegacyEngagementDataMigrator
import com.raxdenstudios.platform.engagement.data.datasource.local.mapper.EngagementEntityDataMapper
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
 * Every access first runs [LegacyEngagementDataMigrator], which moves the progress stored by older
 * versions in `SharedPreferences` into the database.
 */
internal class EngagementDataLocalDataSourceImpl(
    private val database: EngagementDatabase,
    private val legacyEngagementDataMigrator: LegacyEngagementDataMigrator,
    private val engagementEntityDataMapper: EngagementEntityDataMapper,
    private val dispatcherProvider: DispatcherProvider,
) : EngagementDataLocalDataSource {

    private val engagementDao: EngagementDao
        get() = database.engagementDao()

    /** A failed legacy migration is emitted first; the stored data is observed regardless. */
    override fun observeEngagementData(): Flow<Answer<EngagementData, Failure>> = flow {
        val migration = legacyEngagementDataMigrator.migrate()
        if (migration is Answer.Failure) emit(migration)
        emitAll(observeStoredEngagementData())
    }.flowOn(dispatcherProvider.io)

    override suspend fun getEngagementData(): Answer<EngagementData, Failure> =
        legacyEngagementDataMigrator.migrate().coFlatMap {
            coRunCatching { database.withTransaction { readEngagementData() } }
                .thenFailure { error -> error.toUnknownError("Error to get engagement data") }
        }

    override suspend fun updateEngagementData(
        update: (EngagementData) -> EngagementData,
    ): Answer<EngagementData, Failure> = legacyEngagementDataMigrator.migrate().coFlatMap {
        coRunCatching { updateStoredEngagementData(update) }
            .thenFailure { error -> error.toUnknownError("Error to update engagement data") }
    }

    private fun observeStoredEngagementData(): Flow<Answer<EngagementData, Failure>> =
        database.invalidationTracker.createFlow(*OBSERVED_TABLES)
            .map { database.withTransaction { readEngagementData() } }
            .distinctUntilChanged()
            .toAnswer { error -> error.toUnknownError("Error to observe engagement data") }

    private suspend fun updateStoredEngagementData(
        update: (EngagementData) -> EngagementData,
    ): EngagementData = database.withTransaction {
        val updated = update(readEngagementData())
        writeEngagementData(updated)
        updated
    }

    private suspend fun readEngagementData(): EngagementData = engagementEntityDataMapper.toEngagementData(
        state = engagementDao.getState(),
        originCounts = engagementDao.getOriginCounts(),
    )

    private suspend fun writeEngagementData(
        engagementData: EngagementData,
    ) = engagementDao.replace(
        state = engagementEntityDataMapper.toStateEntity(engagementData),
        originCounts = engagementEntityDataMapper.toOriginCountEntities(engagementData),
    )

    private companion object {
        val OBSERVED_TABLES = arrayOf(
            EngagementStateEntity.TABLE_NAME,
            EngagementOriginCountEntity.TABLE_NAME,
        )
    }
}
