package com.raxdenstudios.platform.engagement.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity

/**
 * Access to both engagement tables. It does not extend `BaseDao` because the engagement progress
 * is always read and written as a whole (state row + every origin count), see [replace].
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

    @Insert
    suspend fun insertOriginCounts(
        originCounts: List<EngagementOriginCountEntity>,
    )

    @Query("DELETE FROM engagement_origin_count")
    suspend fun deleteOriginCounts()

    /** Overwrites the stored progress with [state] and exactly [originCounts]. */
    @Transaction
    suspend fun replace(
        state: EngagementStateEntity,
        originCounts: List<EngagementOriginCountEntity>,
    ) {
        upsertState(state)
        deleteOriginCounts()
        insertOriginCounts(originCounts)
    }
}
