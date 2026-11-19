package com.raxdenstudios.platform.engagement.data.datasource.local.mapper

import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import com.raxdenstudios.platform.engagement.domain.model.ReviewState
import com.raxdenstudios.platform.engagement.domain.model.ShareState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class EngagementEntityDataMapperImplTest {

    private val mapper = EngagementEntityDataMapperImpl()

    private val fullData = EngagementData(
        counts = mapOf("app_opened" to 3, "favourite_added" to 1),
        review = ReviewState(attempts = 2, lastAt = 200L, ratedAt = 300L),
        share = ShareState(declines = 2, lastPromptAt = 100L, sharedAt = 150L),
        pendingPromptAt = 400L,
    )

    private val fullStateEntity = EngagementStateEntity(
        id = EngagementStateEntity.SINGLE_ROW_ID,
        reviewAttempts = 2,
        reviewLastAt = 200L,
        reviewRatedAt = 300L,
        shareDeclines = 2,
        shareLastPromptAt = 100L,
        shareSharedAt = 150L,
        pendingPromptAt = 400L,
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
            originCounts = listOf(EngagementOriginCountEntity(origin = "app_opened", occurrences = 4)),
        )

        assertEquals(EngagementData(counts = mapOf("app_opened" to 4)), result)
    }

    @Test
    fun `Given state and counts When mapped to data Then every field is mapped`() {
        val result = mapper.toEngagementData(
            state = fullStateEntity,
            originCounts = listOf(
                EngagementOriginCountEntity(origin = "app_opened", occurrences = 3),
                EngagementOriginCountEntity(origin = "favourite_added", occurrences = 1),
            ),
        )

        assertEquals(fullData, result)
    }

    @Test
    fun `Given data When mapped to state entity Then it uses the single row id and every field`() {
        val result = mapper.toStateEntity(fullData)

        assertEquals(fullStateEntity, result)
    }

    @Test
    fun `Given default data When mapped to state entity Then nullable timestamps stay null`() {
        val result = mapper.toStateEntity(EngagementData())

        assertNull(result.reviewLastAt)
        assertNull(result.reviewRatedAt)
        assertNull(result.shareLastPromptAt)
        assertNull(result.shareSharedAt)
        assertNull(result.pendingPromptAt)
    }

    @Test
    fun `Given counts When mapped to origin entities Then one entity per origin is returned`() {
        val result = mapper.toOriginCountEntities(fullData.counts)

        assertEquals(
            listOf(
                EngagementOriginCountEntity(origin = "app_opened", occurrences = 3),
                EngagementOriginCountEntity(origin = "favourite_added", occurrences = 1),
            ),
            result,
        )
    }

    @Test
    fun `Given no counts When mapped to origin entities Then the list is empty`() {
        assertTrue(mapper.toOriginCountEntities(emptyMap()).isEmpty())
    }

    @Test
    fun `Given data When mapped to entities and back Then it round-trips`() {
        val result = mapper.toEngagementData(
            state = mapper.toStateEntity(fullData),
            originCounts = mapper.toOriginCountEntities(fullData.counts),
        )

        assertEquals(fullData, result)
    }

    @Test
    fun `Given default data When mapped to entities and back Then nullable timestamps round-trip as null`() {
        val data = EngagementData()

        val result = mapper.toEngagementData(
            state = mapper.toStateEntity(data),
            originCounts = mapper.toOriginCountEntities(data.counts),
        )

        assertEquals(data, result)
    }
}
