package com.raxdenstudios.platform.engagement.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSource
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import com.raxdenstudios.platform.engagement.domain.model.ReviewState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class EngagementDataRepositoryImplTest {

    private val engagementDataLocalDataSource: EngagementDataLocalDataSource = mockk()
    private val repository = EngagementDataRepositoryImpl(engagementDataLocalDataSource)

    private val failure = Failure.Network.Unknown("error")

    @Test
    fun `Given data source emissions When observing Then they are delegated`() = runTest {
        val data = EngagementData(review = ReviewState(ratedAt = 1L))
        every { engagementDataLocalDataSource.observeEngagementData() } returns
            flowOf(Answer.Success(data))

        val emissions = repository.observeEngagementData().toList()

        assertEquals(listOf(Answer.Success(data)), emissions)
        verify { engagementDataLocalDataSource.observeEngagementData() }
    }

    @Test
    fun `Given stored data When data is read Then the data source answer is returned`() = runTest {
        val data = EngagementData(review = ReviewState(ratedAt = 1L), counts = mapOf("app_opened" to 3))
        coEvery { engagementDataLocalDataSource.getEngagementData() } returns Answer.Success(data)

        val result = repository.getEngagementData()

        assertEquals(Answer.Success(data), result)
        coVerify { engagementDataLocalDataSource.getEngagementData() }
    }

    @Test
    fun `Given an update lambda When data is updated Then it is delegated and the stored result returned`() = runTest {
        val updated = EngagementData(review = ReviewState(ratedAt = 1L))
        val update: (EngagementData) -> EngagementData = { it.copy(review = ReviewState(ratedAt = 1L)) }
        coEvery { engagementDataLocalDataSource.updateEngagementData(any()) } returns
            Answer.Success(updated)

        val result = repository.updateEngagementData(update)

        assertEquals(Answer.Success(updated), result)
        coVerify { engagementDataLocalDataSource.updateEngagementData(update) }
    }

    @Test
    fun `Given a data source failure When data is updated Then failure is propagated`() = runTest {
        coEvery { engagementDataLocalDataSource.updateEngagementData(any()) } returns
            Answer.Failure(failure)

        val result = repository.updateEngagementData { it }

        assertEquals(Answer.Failure(failure), result)
    }
}
