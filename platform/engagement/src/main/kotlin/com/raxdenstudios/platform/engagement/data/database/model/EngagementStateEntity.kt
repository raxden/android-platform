package com.raxdenstudios.platform.engagement.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the scalar fields of
 * [com.raxdenstudios.platform.engagement.domain.model.EngagementData]; the row always uses
 * [SINGLE_ROW_ID]. Per-origin counts live in [EngagementOriginCountEntity].
 */
@Entity(tableName = EngagementStateEntity.TABLE_NAME)
internal data class EngagementStateEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "has_rated") val hasRated: Boolean,
    @ColumnInfo(name = "has_shared") val hasShared: Boolean,
    @ColumnInfo(name = "share_declines") val shareDeclines: Int,
    @ColumnInfo(name = "last_share_prompt_at") val lastSharePromptAt: Long?,
    @ColumnInfo(name = "review_attempts") val reviewAttempts: Int,
    @ColumnInfo(name = "last_review_at") val lastReviewAt: Long?,
    @ColumnInfo(name = "pending_prompt") val pendingPrompt: Boolean,
) {

    companion object {
        const val TABLE_NAME = "engagement_state"
        const val SINGLE_ROW_ID = 0
    }
}
