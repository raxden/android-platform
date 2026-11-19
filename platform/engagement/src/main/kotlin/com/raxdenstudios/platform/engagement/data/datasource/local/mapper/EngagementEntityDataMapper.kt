package com.raxdenstudios.platform.engagement.data.datasource.local.mapper

import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import com.raxdenstudios.platform.engagement.domain.model.ReviewState
import com.raxdenstudios.platform.engagement.domain.model.ShareState

internal interface EngagementEntityDataMapper {

    /** Builds the domain model; a missing [state] row (nothing stored yet) yields the defaults. */
    fun toEngagementData(
        state: EngagementStateEntity?,
        originCounts: List<EngagementOriginCountEntity>,
    ): EngagementData

    fun toStateEntity(
        engagementData: EngagementData,
    ): EngagementStateEntity

    /** One entity per entry of [counts] (origin to occurrences). */
    fun toOriginCountEntities(
        counts: Map<String, Int>,
    ): List<EngagementOriginCountEntity>
}

internal class EngagementEntityDataMapperImpl : EngagementEntityDataMapper {

    override fun toEngagementData(
        state: EngagementStateEntity?,
        originCounts: List<EngagementOriginCountEntity>,
    ): EngagementData {
        val counts = originCounts.associate { entity -> entity.origin to entity.occurrences }
        return state?.toEngagementData(counts) ?: EngagementData(counts = counts)
    }

    override fun toStateEntity(
        engagementData: EngagementData,
    ): EngagementStateEntity = EngagementStateEntity(
        reviewAttempts = engagementData.review.attempts,
        reviewLastAt = engagementData.review.lastAt,
        reviewRatedAt = engagementData.review.ratedAt,
        shareDeclines = engagementData.share.declines,
        shareLastPromptAt = engagementData.share.lastPromptAt,
        shareSharedAt = engagementData.share.sharedAt,
        pendingPromptAt = engagementData.pendingPromptAt,
    )

    override fun toOriginCountEntities(
        counts: Map<String, Int>,
    ): List<EngagementOriginCountEntity> = counts.map { (origin, occurrences) ->
        EngagementOriginCountEntity(
            origin = origin,
            occurrences = occurrences,
        )
    }

    private fun EngagementStateEntity.toEngagementData(
        counts: Map<String, Int>,
    ): EngagementData = EngagementData(
        counts = counts,
        review = ReviewState(
            attempts = reviewAttempts,
            lastAt = reviewLastAt,
            ratedAt = reviewRatedAt,
        ),
        share = ShareState(
            declines = shareDeclines,
            lastPromptAt = shareLastPromptAt,
            sharedAt = shareSharedAt,
        ),
        pendingPromptAt = pendingPromptAt,
    )
}
