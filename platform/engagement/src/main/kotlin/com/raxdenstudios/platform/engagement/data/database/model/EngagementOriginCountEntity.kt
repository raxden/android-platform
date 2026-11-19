package com.raxdenstudios.platform.engagement.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Recorded occurrences of one `EngagementUseCase.Params.origin`. */
@Entity(tableName = EngagementOriginCountEntity.TABLE_NAME)
internal data class EngagementOriginCountEntity(
    @PrimaryKey @ColumnInfo(name = "origin") val origin: String,
    @ColumnInfo(name = "occurrences") val occurrences: Int,
) {

    companion object {
        const val TABLE_NAME = "engagement_origin_count"
    }
}
