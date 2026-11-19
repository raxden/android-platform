package com.raxdenstudios.platform.engagement.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity

/**
 * Access to both engagement tables. It does not extend `BaseDao`: callers write only the rows that
 * changed, inside a transaction they own (see `EngagementDataLocalDataSource`).
 */
@Dao
internal interface EngagementDao {

    @Query("SELECT * FROM engagement_state WHERE id = ${EngagementStateEntity.SINGLE_ROW_ID}")
    suspend fun getState(): EngagementStateEntity?

    @Query("SELECT * FROM engagement_origin_count")
    suspend fun getOriginCounts(): List<EngagementOriginCountEntity>

    @Upsert
    suspend fun upsertState(
        state: EngagementStateEntity,
    )

    @Upsert
    suspend fun upsertOriginCounts(
        originCounts: List<EngagementOriginCountEntity>,
    )

    @Query("DELETE FROM engagement_origin_count WHERE origin IN (:origins)")
    suspend fun deleteOriginCounts(
        origins: List<String>,
    )
}
