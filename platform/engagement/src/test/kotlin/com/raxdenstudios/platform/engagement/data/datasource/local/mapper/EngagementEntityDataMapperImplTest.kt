package com.raxdenstudios.platform.engagement.data.datasource.local.mapper

import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class EngagementEntityDataMapperImplTest {

    private val mapper = EngagementEntityDataMapperImpl()

    private val fullData = EngagementData(
        hasRated = true,
        hasShared = true,
        counts = mapOf("app_opened" to 3, "favourite_added" to 1),
        shareDeclines = 2,
        lastSharePromptAt = 100L,
        reviewAttempts = 2,
        lastReviewAt = 200L,
        pendingPrompt = true,
    )

    @Test
    fun `Given no state and no counts When mapped to data Then defaults are returned`() {
        val result = mapper.toEngagementData(state = null, originCounts = emptyList())

        assertEquals(EngagementData(), result)
    }

    @Test
    fun `Given no state but counts When mapped to data Then defaults with the counts are returned`() {
        val result = mapper.toEngagementData(
            state = null,
            originCounts = listOf(EngagementOriginCountEntity(origin = "app_opened", count = 4)),
        )

        assertEquals(EngagementData(counts = mapOf("app_opened" to 4)), result)
    }

    @Test
    fun `Given state and counts When mapped to data Then every field is mapped`() {
        val result = mapper.toEngagementData(
            state = EngagementStateEntity(
                hasRated = true,
                hasShared = true,
                shareDeclines = 2,
                lastSharePromptAt = 100L,
                reviewAttempts = 2,
                lastReviewAt = 200L,
                pendingPrompt = true,
            ),
            originCounts = listOf(
                EngagementOriginCountEntity(origin = "app_opened", count = 3),
                EngagementOriginCountEntity(origin = "favourite_added", count = 1),
            ),
        )

        assertEquals(fullData, result)
    }

    @Test
    fun `Given data When mapped to state entity Then it uses the single row id and every field`() {
        val result = mapper.toStateEntity(fullData)

        assertEquals(
            EngagementStateEntity(
                id = EngagementStateEntity.SINGLE_ROW_ID,
                hasRated = true,
                hasShared = true,
                shareDeclines = 2,
                lastSharePromptAt = 100L,
                reviewAttempts = 2,
                lastReviewAt = 200L,
                pendingPrompt = true,
            ),
            result,
        )
    }

    @Test
    fun `Given data with null timestamps When mapped to state entity Then timestamps stay null`() {
        val result = mapper.toStateEntity(EngagementData())

        assertEquals(null, result.lastSharePromptAt)
        assertEquals(null, result.lastReviewAt)
    }

    @Test
    fun `Given data with counts When mapped to origin entities Then one entity per origin is returned`() {
        val result = mapper.toOriginCountEntities(fullData)

        assertEquals(
            listOf(
                EngagementOriginCountEntity(origin = "app_opened", count = 3),
                EngagementOriginCountEntity(origin = "favourite_added", count = 1),
            ),
            result,
        )
    }

    @Test
    fun `Given data without counts When mapped to origin entities Then the list is empty`() {
        assertTrue(mapper.toOriginCountEntities(EngagementData()).isEmpty())
    }

    @Test
    fun `Given data When mapped to entities and back Then it round-trips`() {
        val result = mapper.toEngagementData(
            state = mapper.toStateEntity(fullData),
            originCounts = mapper.toOriginCountEntities(fullData),
        )

        assertEquals(fullData, result)
    }

    @Test
    fun `Given default data When mapped to entities and back Then nullable timestamps round-trip as null`() {
        val data = EngagementData()

        val result = mapper.toEngagementData(
            state = mapper.toStateEntity(data),
            originCounts = mapper.toOriginCountEntities(data),
        )

        assertEquals(data, result)
    }
}
