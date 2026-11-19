package com.raxdenstudios.platform.engagement.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.raxdenstudios.platform.engagement.data.database.dao.EngagementDao
import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity

/**
 * Room database owned by the engagement module (`platform_engagement.db`). It never falls back to
 * a destructive migration: engagement progress is user data, so every schema change must ship a
 * [androidx.room.migration.Migration] (schemas are exported to `schemas/`).
 */
@Database(
    entities = [
        EngagementStateEntity::class,
        EngagementOriginCountEntity::class,
    ],
    version = 1,
)
internal abstract class EngagementDatabase : RoomDatabase() {

    abstract fun engagementDao(): EngagementDao

    companion object {

        private const val NAME = "platform_engagement.db"

        /** Builds a new instance; DI keeps it as a single, since Room expects one per process. */
        fun create(
            context: Context,
        ): EngagementDatabase = Room.databaseBuilder(context, EngagementDatabase::class.java, NAME)
            .build()
    }
}
