package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class MarkAppAsRatedUseCaseImplTest {

    private val engagementDataRepository: EngagementDataRepository = mockk()

    private val failure = Failure.Network.Unknown("error")

    private var storedData = EngagementData()

    private lateinit var useCase: MarkAppAsRatedUseCaseImpl

    @Before
    fun setUp() {
        storedData = EngagementData()
        coEvery { engagementDataRepository.updateEngagementData(any()) } coAnswers {
            storedData = firstArg<(EngagementData) -> EngagementData>().invoke(storedData)
            Answer.Success(storedData)
        }

        useCase = MarkAppAsRatedUseCaseImpl(engagementDataRepository = engagementDataRepository)
    }

    @Test
    fun `Given engagement data When invoked Then app is marked as rated and the rest is kept`() = runTest {
        storedData = EngagementData(counts = mapOf("app_opened" to 3), reviewAttempts = 2, lastReviewAt = 5L)

        val result = useCase(MarkAppAsRatedUseCase.Params)

        assertEquals(Answer.Success(Unit), result)
        assertEquals(
            EngagementData(
                hasRated = true,
                counts = mapOf("app_opened" to 3),
                reviewAttempts = 2,
                lastReviewAt = 5L,
            ),
            storedData,
        )
    }

    @Test
    fun `Given update fails When invoked Then failure is propagated`() = runTest {
        coEvery { engagementDataRepository.updateEngagementData(any()) } returns Answer.Failure(failure)

        val result = useCase(MarkAppAsRatedUseCase.Params)

        assertEquals(Answer.Failure(failure), result)
        assertEquals(EngagementData(), storedData)
    }
}
