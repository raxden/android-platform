package com.raxdenstudios.platform.engagement.data.datasource.local.mapper

import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity
import com.raxdenstudios.platform.engagement.domain.model.EngagementData

internal interface EngagementEntityDataMapper {

    /** Builds the domain model; a missing [state] row (nothing stored yet) yields the defaults. */
    fun toEngagementData(
        state: EngagementStateEntity?,
        originCounts: List<EngagementOriginCountEntity>,
    ): EngagementData

    fun toStateEntity(
        engagementData: EngagementData,
    ): EngagementStateEntity

    fun toOriginCountEntities(
        engagementData: EngagementData,
    ): List<EngagementOriginCountEntity>
}

internal class EngagementEntityDataMapperImpl : EngagementEntityDataMapper {

    override fun toEngagementData(
        state: EngagementStateEntity?,
        originCounts: List<EngagementOriginCountEntity>,
    ): EngagementData {
        val counts = originCounts.associate { entity -> entity.origin to entity.count }
        return state?.toEngagementData(counts) ?: EngagementData(counts = counts)
    }

    override fun toStateEntity(
        engagementData: EngagementData,
    ): EngagementStateEntity = EngagementStateEntity(
        hasRated = engagementData.hasRated,
        hasShared = engagementData.hasShared,
        shareDeclines = engagementData.shareDeclines,
        lastSharePromptAt = engagementData.lastSharePromptAt,
        reviewAttempts = engagementData.reviewAttempts,
        lastReviewAt = engagementData.lastReviewAt,
        pendingPrompt = engagementData.pendingPrompt,
    )

    override fun toOriginCountEntities(
        engagementData: EngagementData,
    ): List<EngagementOriginCountEntity> = engagementData.counts.map { (origin, count) ->
        EngagementOriginCountEntity(
            origin = origin,
            count = count,
        )
    }

    private fun EngagementStateEntity.toEngagementData(
        counts: Map<String, Int>,
    ): EngagementData = EngagementData(
        hasRated = hasRated,
        hasShared = hasShared,
        counts = counts,
        shareDeclines = shareDeclines,
        lastSharePromptAt = lastSharePromptAt,
        reviewAttempts = reviewAttempts,
        lastReviewAt = lastReviewAt,
        pendingPrompt = pendingPrompt,
    )
}
