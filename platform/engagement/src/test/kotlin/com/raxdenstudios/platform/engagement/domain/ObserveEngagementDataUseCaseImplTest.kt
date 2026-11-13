package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ObserveEngagementDataUseCaseImplTest {

    private val engagementDataRepository: EngagementDataRepository = mockk()
    private val useCase = ObserveEngagementDataUseCaseImpl(engagementDataRepository)

    @Test
    fun `Given repository emissions When invoked Then they are delegated`() = runTest {
        val data = EngagementData(hasRated = true)
        val failure = Failure.Network.Unknown("error")
        every { engagementDataRepository.observeEngagementData() } returns
            flowOf(Answer.Success(data), Answer.Failure(failure))

        val emissions = useCase(ObserveEngagementDataUseCase.Params).toList()

        assertEquals(
            listOf(Answer.Success(data), Answer.Failure(failure)),
            emissions,
        )
        verify { engagementDataRepository.observeEngagementData() }
    }
}
