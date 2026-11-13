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

    /**
     * In-memory stand-in for the persisted state: the repository mock reads it back on
     * `getEngagementData` and stores every update, like the real repository does.
     */
    private var storedData = EngagementData()

    private lateinit var useCase: RateAppUseCaseImpl

    @Before
    fun setUp() {
        storedData = EngagementData()
        coEvery { inAppReview.launch() } returns Answer.Success(Unit)
        coEvery { engagementDataRepository.getEngagementData() } answers {
            Answer.Success(storedData)
        }
        coEvery { engagementDataRepository.updateEngagementData(any()) } coAnswers {
            storedData = firstArg()
            Answer.Success(Unit)
        }

        useCase = RateAppUseCaseImpl(
            inAppReview = inAppReview,
            engagementDataRepository = engagementDataRepository,
        )
    }

    @Test
    fun `Given review launches successfully When invoked Then app is marked as rated`() = runTest {
        storedData = EngagementData(counts = mapOf("app_opened" to 3))

        val result = useCase(RateAppUseCase.Params)

        assertEquals(Answer.Success(false), result)
        assertEquals(
            EngagementData(counts = mapOf("app_opened" to 3), hasRated = true),
            storedData,
        )
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
    fun `Given stats update fails When invoked Then failure is propagated`() = runTest {
        coEvery { engagementDataRepository.updateEngagementData(any()) } returns Answer.Failure(failure)

        val result = useCase(RateAppUseCase.Params)

        assertEquals(Answer.Failure(failure), result)
    }
}
