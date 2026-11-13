package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppShareResult
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class EngagementUseCaseImplTest {

    private val rateAppUseCase: RateAppUseCase = mockk()
    private val shareAppUseCase: ShareAppUseCase = mockk()
    private val engagementDataRepository: EngagementDataRepository = mockk()
    private val engagementEligibility: EngagementEligibility = mockk()

    private val failure = Failure.Network.Unknown("error")

    /**
     * In-memory stand-in for the persisted state: the repository mock reads it back on
     * `getEngagementData` and stores every update, like the real repository does.
     */
    private var storedData = EngagementData()

    private lateinit var useCase: EngagementUseCaseImpl

    @Before
    fun setUp() {
        storedData = EngagementData()
        coEvery { engagementDataRepository.getEngagementData() } answers {
            Answer.Success(storedData)
        }
        coEvery { engagementDataRepository.updateEngagementData(any()) } coAnswers {
            storedData = firstArg()
            Answer.Success(Unit)
        }
        coEvery { rateAppUseCase(any()) } returns Answer.Success(false)
        coEvery { shareAppUseCase(any()) } returns Answer.Success(InAppShareResult.Dismissed)
        every { engagementEligibility.isEligible(any(), any()) } returns false

        useCase = EngagementUseCaseImpl(
            rateAppUseCase = rateAppUseCase,
            shareAppUseCase = shareAppUseCase,
            engagementDataRepository = engagementDataRepository,
            engagementEligibility = engagementEligibility,
        )
    }

    @Test
    fun `Given repository failure When invoked Then failure is propagated`() = runTest {
        coEvery { engagementDataRepository.updateEngagementData(any()) } returns
            Answer.Failure(failure)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Failure(failure), result)
        coVerify(exactly = 0) { rateAppUseCase(any()) }
    }

    @Test
    fun `Given eligible occurrence and app not rated When invoked Then review is launched`() = runTest {
        storedData = EngagementData(counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible("app_opened", 3) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewLaunched), result)
        assertEquals(EngagementData(counts = mapOf("app_opened" to 3)), storedData)
        verify { engagementEligibility.isEligible("app_opened", 3) }
        coVerify { rateAppUseCase(RateAppUseCase.Params) }
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given review unavailable and app not shared When invoked Then share is tried in the same call`() = runTest {
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { rateAppUseCase(any()) } returns Answer.Success(true)
        coEvery { shareAppUseCase(any()) } returns
            Answer.Success(InAppShareResult.TargetChosen("com.whatsapp"))

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareChosen), result)
        assertTrue(storedData.hasShared)
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
    }

    @Test
    fun `Given review unavailable and share not chosen When invoked Then returns ReviewUnavailable`() = runTest {
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { rateAppUseCase(any()) } returns Answer.Success(true)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewUnavailable), result)
        assertFalse(storedData.hasShared)
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
    }

    @Test
    fun `Given review unavailable and app already shared When invoked Then share is skipped`() = runTest {
        storedData = EngagementData(hasShared = true)
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { rateAppUseCase(any()) } returns Answer.Success(true)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewUnavailable), result)
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given rate app fails When invoked Then failure is propagated`() = runTest {
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { rateAppUseCase(any()) } returns Answer.Failure(failure)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Failure(failure), result)
    }

    @Test
    fun `Given non eligible occurrence When invoked Then count is tracked but nothing is launched`() = runTest {
        storedData = EngagementData(counts = mapOf("app_opened" to 3))

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertEquals(EngagementData(counts = mapOf("app_opened" to 4)), storedData)
        coVerify(exactly = 0) { rateAppUseCase(any()) }
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given unseen origin When invoked Then origin count starts at one`() = runTest {
        val result = useCase(EngagementUseCase.Params("new_origin"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertEquals(EngagementData(counts = mapOf("new_origin" to 1)), storedData)
        verify { engagementEligibility.isEligible("new_origin", 1) }
    }

    @Test
    fun `Given eligible occurrence on another origin When invoked Then review is launched`() = runTest {
        storedData = EngagementData(counts = mapOf("favourite_added" to 4))
        every { engagementEligibility.isEligible("favourite_added", 5) } returns true

        val result = useCase(EngagementUseCase.Params("favourite_added"))

        assertEquals(Answer.Success(EngagementResult.ReviewLaunched), result)
        coVerify { rateAppUseCase(RateAppUseCase.Params) }
    }

    @Test
    fun `Given already rated and shared When invoked Then stats are tracked but nothing is launched`() = runTest {
        storedData = EngagementData(
            hasRated = true,
            hasShared = true,
            counts = mapOf("app_opened" to 2),
        )
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertEquals(
            EngagementData(hasRated = true, hasShared = true, counts = mapOf("app_opened" to 3)),
            storedData,
        )
        coVerify(exactly = 0) { rateAppUseCase(any()) }
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given already rated and eligible When invoked Then share is launched instead of review`() = runTest {
        storedData = EngagementData(hasRated = true, counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        coVerify(exactly = 0) { rateAppUseCase(any()) }
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
    }

    @Test
    fun `Given target chosen on share When invoked Then app is marked as shared`() = runTest {
        storedData = EngagementData(hasRated = true, counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns
            Answer.Success(InAppShareResult.TargetChosen("com.whatsapp"))

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareChosen), result)
        assertTrue(storedData.hasShared)
    }

    @Test
    fun `Given share is dismissed When invoked Then app is not marked as shared`() = runTest {
        storedData = EngagementData(hasRated = true, counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
        assertFalse(storedData.hasShared)
    }

    @Test
    fun `Given share is declined When invoked Then app is not marked as shared`() = runTest {
        storedData = EngagementData(hasRated = true, counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns Answer.Success(InAppShareResult.Declined)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
        assertFalse(storedData.hasShared)
    }

    @Test
    fun `Given share fails When invoked Then it is swallowed and app is not marked as shared`() = runTest {
        storedData = EngagementData(hasRated = true, counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns Answer.Failure(failure)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        assertFalse(storedData.hasShared)
    }

    @Test
    fun `Given engagement in progress When invoked concurrently Then second call is counted but shows nothing`() =
        runTest {
            every { engagementEligibility.isEligible(any(), any()) } returns true
            val gate = CompletableDeferred<Unit>()
            coEvery { rateAppUseCase(any()) } coAnswers {
                gate.await()
                Answer.Success(false)
            }

            val first = async { useCase(EngagementUseCase.Params("app_opened")) }
            runCurrent()

            val second = useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(Answer.Success(EngagementResult.None), second)
            coVerify(exactly = 2) { engagementDataRepository.updateEngagementData(any()) }
            assertEquals(2, storedData.counts["app_opened"])

            gate.complete(Unit)
            assertEquals(Answer.Success(EngagementResult.ReviewLaunched), first.await())
            coVerify(exactly = 1) { rateAppUseCase(any()) }
        }
}
