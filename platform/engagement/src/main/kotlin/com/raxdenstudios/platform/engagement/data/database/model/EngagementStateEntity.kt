package com.raxdenstudios.platform.engagement.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the scalar fields of
 * [com.raxdenstudios.platform.engagement.domain.model.EngagementData], flattened: `review_*`
 * columns hold its `ReviewState` and `share_*` columns its `ShareState`. The row always uses
 * [SINGLE_ROW_ID]. Per-origin counts live in [EngagementOriginCountEntity].
 */
@Entity(tableName = EngagementStateEntity.TABLE_NAME)
internal data class EngagementStateEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "review_attempts") val reviewAttempts: Int,
    @ColumnInfo(name = "review_last_at") val reviewLastAt: Long?,
    @ColumnInfo(name = "review_rated_at") val reviewRatedAt: Long?,
    @ColumnInfo(name = "share_declines") val shareDeclines: Int,
    @ColumnInfo(name = "share_last_prompt_at") val shareLastPromptAt: Long?,
    @ColumnInfo(name = "share_shared_at") val shareSharedAt: Long?,
    @ColumnInfo(name = "pending_prompt_at") val pendingPromptAt: Long?,
) {

    companion object {
        const val TABLE_NAME = "engagement_state"
        const val SINGLE_ROW_ID = 0
    }
}
