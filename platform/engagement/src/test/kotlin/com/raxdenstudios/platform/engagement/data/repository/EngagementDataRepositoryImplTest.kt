package com.raxdenstudios.platform.engagement.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSource
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
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
        val data = EngagementData(hasRated = true)
        every { engagementDataLocalDataSource.observeEngagementData() } returns
            flowOf(Answer.Success(data))

        val emissions = repository.observeEngagementData().toList()

        assertEquals(listOf(Answer.Success(data)), emissions)
        verify { engagementDataLocalDataSource.observeEngagementData() }
    }

    @Test
    fun `Given stored data When data is read Then the data source answer is returned`() = runTest {
        val data = EngagementData(hasRated = true, counts = mapOf("app_opened" to 3))
        coEvery { engagementDataLocalDataSource.getEngagementData() } returns Answer.Success(data)

        val result = repository.getEngagementData()

        assertEquals(Answer.Success(data), result)
        coVerify { engagementDataLocalDataSource.getEngagementData() }
    }

    @Test
    fun `Given new data When data is updated Then it is delegated to the data source`() = runTest {
        val data = EngagementData(hasRated = true)
        coEvery { engagementDataLocalDataSource.updateEngagementData(any()) } returns
            Answer.Success(Unit)

        val result = repository.updateEngagementData(data)

        assertEquals(Answer.Success(Unit), result)
        coVerify { engagementDataLocalDataSource.updateEngagementData(data) }
    }

    @Test
    fun `Given a data source failure When data is updated Then failure is propagated`() = runTest {
        coEvery { engagementDataLocalDataSource.updateEngagementData(any()) } returns
            Answer.Failure(failure)

        val result = repository.updateEngagementData(EngagementData())

        assertEquals(Answer.Failure(failure), result)
    }
}
