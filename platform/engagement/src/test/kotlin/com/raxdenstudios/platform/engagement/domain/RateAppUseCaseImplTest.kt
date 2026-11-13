package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppReview
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class RateAppUseCaseImplTest {

    private val inAppReview: InAppReview = mockk()
    private val engagementDataRepository: EngagementDataRepository = mockk()

    private val failure = Failure.Network.Unknown("error")

    private lateinit var useCase: RateAppUseCaseImpl

    @Before
    fun setUp() {
        coEvery { inAppReview.launch() } returns Answer.Success(Unit)
        coEvery { engagementDataRepository.getEngagementData() } returns
            Answer.Success(EngagementData())
        coEvery { engagementDataRepository.updateEngagementData(any()) } returns
            Answer.Success(Unit)

        useCase = RateAppUseCaseImpl(
            inAppReview = inAppReview,
            engagementDataRepository = engagementDataRepository,
        )
    }

    @Test
    fun `Given review launches successfully When invoked Then app is marked as rated`() = runTest {
        coEvery { engagementDataRepository.getEngagementData() } returns
            Answer.Success(EngagementData(counts = mapOf("app_opened" to 3)))

        val result = useCase(RateAppUseCase.Params)

        assertEquals(Answer.Success(false), result)
        coVerify {
            engagementDataRepository.updateEngagementData(
                EngagementData(counts = mapOf("app_opened" to 3), hasRated = true),
            )
        }
    }

    @Test
    fun `Given review fails to launch When invoked Then returns true and stats are not touched`() = runTest {
        coEvery { inAppReview.launch() } returns Answer.Failure(failure)

        val result = useCase(RateAppUseCase.Params)

        assertEquals(Answer.Success(true), result)
        coVerify(exactly = 0) { engagementDataRepository.getEngagementData() }
        coVerify(exactly = 0) { engagementDataRepository.updateEngagementData(any()) }
    }

    @Test
    fun `Given stats fetch fails When invoked Then failure is propagated`() = runTest {
        coEvery { engagementDataRepository.getEngagementData() } returns Answer.Failure(failure)

        val result = useCase(RateAppUseCase.Params)

        assertEquals(Answer.Failure(failure), result)
        coVerify(exactly = 0) { engagementDataRepository.updateEngagementData(any()) }
    }

    @Test
    fun `Given stats update fails When invoked Then failure is propagated`() = runTest {
        coEvery { engagementDataRepository.updateEngagementData(any()) } returns Answer.Failure(failure)

        val result = useCase(RateAppUseCase.Params)

        assertEquals(Answer.Failure(failure), result)
    }
}
