package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppShareResult
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import com.raxdenstudios.platform.engagement.domain.model.EngagementPolicy
import com.raxdenstudios.platform.engagement.domain.model.ReviewState
import com.raxdenstudios.platform.engagement.domain.model.ShareState
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Suppress("LargeClass")
@OptIn(ExperimentalCoroutinesApi::class)
internal class EngagementUseCaseImplTest {

    private val rateAppUseCase: RateAppUseCase = mockk()
    private val shareAppUseCase: ShareAppUseCase = mockk()
    private val engagementDataRepository: EngagementDataRepository = mockk()
    private val engagementEligibility: EngagementEligibility = mockk()

    private val failure = Failure.Network.Unknown("error")

    /**
     * In-memory stand-in for the persisted state: the repository mock applies every update
     * lambda to it and stores the result, like the real repository does.
     */
    private var storedData = EngagementData()
    private var clock = NOW

    private lateinit var useCase: EngagementUseCaseImpl

    @Before
    fun setUp() {
        storedData = EngagementData()
        clock = NOW
        coEvery { engagementDataRepository.updateEngagementData(any()) } coAnswers {
            storedData = firstArg<(EngagementData) -> EngagementData>().invoke(storedData)
            Answer.Success(storedData)
        }
        coEvery { rateAppUseCase(any()) } returns Answer.Success(false)
        coEvery { shareAppUseCase(any()) } returns Answer.Success(InAppShareResult.Dismissed)
        every { engagementEligibility.isEligible(any(), any()) } returns false

        useCase = createUseCase()
    }

    private fun createUseCase(
        policy: EngagementPolicy = EngagementPolicy(),
        installTime: Long = NOW - INSTALL_AGE,
    ) = EngagementUseCaseImpl(
        rateAppUseCase = rateAppUseCase,
        shareAppUseCase = shareAppUseCase,
        engagementDataRepository = engagementDataRepository,
        engagementEligibility = engagementEligibility,
        engagementPolicy = policy,
        firstInstallTimeMillis = { installTime },
        currentTimeMillis = { clock },
    )

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
        assertTrue(storedData.share.hasShared)
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
    }

    @Test
    fun `Given review unavailable and share not chosen When invoked Then returns ReviewUnavailable`() = runTest {
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { rateAppUseCase(any()) } returns Answer.Success(true)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewUnavailable), result)
        assertFalse(storedData.share.hasShared)
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
    }

    @Test
    fun `Given review unavailable and app already shared When invoked Then share is skipped`() = runTest {
        storedData = EngagementData(share = ShareState(sharedAt = 1L))
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
            review = ReviewState(ratedAt = 1L),
            share = ShareState(sharedAt = 1L),
            counts = mapOf("app_opened" to 2),
        )
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertEquals(
            EngagementData(
                review = ReviewState(ratedAt = 1L),
                share = ShareState(sharedAt = 1L),
                counts = mapOf("app_opened" to 3),
            ),
            storedData,
        )
        coVerify(exactly = 0) { rateAppUseCase(any()) }
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given already rated and eligible When invoked Then share is launched instead of review`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        coVerify(exactly = 0) { rateAppUseCase(any()) }
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
    }

    @Test
    fun `Given target chosen on share When invoked Then app is marked as shared`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns
            Answer.Success(InAppShareResult.TargetChosen("com.whatsapp"))

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareChosen), result)
        assertTrue(storedData.share.hasShared)
    }

    @Test
    fun `Given share is dismissed When invoked Then app is not marked as shared`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
        assertFalse(storedData.share.hasShared)
    }

    @Test
    fun `Given share is declined When invoked Then app is not marked as shared`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns Answer.Success(InAppShareResult.Declined)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        coVerify { shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)) }
        assertFalse(storedData.share.hasShared)
    }

    @Test
    fun `Given share fails When invoked Then it is swallowed and prompt is kept pending`() = runTest {
        val initial = EngagementData(review = ReviewState(ratedAt = 1L), counts = mapOf("app_opened" to 2))
        storedData = initial
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns Answer.Failure(failure)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        assertFalse(storedData.share.hasShared)
        assertEquals(initial.copy(counts = mapOf("app_opened" to 3), pendingPromptAt = NOW), storedData)
        coVerify(exactly = 2) { engagementDataRepository.updateEngagementData(any()) }
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
            coVerify(exactly = 3) { engagementDataRepository.updateEngagementData(any()) }
            assertEquals(2, storedData.counts["app_opened"])
            assertEquals(NOW, storedData.pendingPromptAt)

            gate.complete(Unit)
            assertEquals(Answer.Success(EngagementResult.ReviewLaunched), first.await())
            coVerify(exactly = 1) { rateAppUseCase(any()) }
        }

    @Test
    fun `Given engagement in progress and non milestone When invoked concurrently Then pending is not set`() = runTest {
        every { engagementEligibility.isEligible(any(), any()) } answers { secondArg<Int>() == 1 }
        val gate = CompletableDeferred<Unit>()
        coEvery { rateAppUseCase(any()) } coAnswers {
            gate.await()
            Answer.Success(false)
        }

        val first = async { useCase(EngagementUseCase.Params("app_opened")) }
        runCurrent()
        val second = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), second)
        assertNull(storedData.pendingPromptAt)
        gate.complete(Unit)
        first.await()
    }

    @Test
    fun `Given engagement in progress and nothing allowed When invoked concurrently Then pending is not set`() =
        runTest {
            every { engagementEligibility.isEligible(any(), any()) } returns true
            val gate = CompletableDeferred<Unit>()
            coEvery { rateAppUseCase(any()) } coAnswers {
                gate.await()
                Answer.Success(false)
            }

            val first = async { useCase(EngagementUseCase.Params("app_opened")) }
            runCurrent()
            storedData = storedData.copy(review = ReviewState(ratedAt = 1L), share = ShareState(sharedAt = 1L))
            val second = useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(Answer.Success(EngagementResult.None), second)
            assertNull(storedData.pendingPromptAt)
            gate.complete(Unit)
            first.await()
        }

    @Test
    fun `Given share is declined When invoked Then declines are incremented and timestamp is stored`() = runTest {
        storedData = EngagementData(
            review = ReviewState(ratedAt = 1L),
            share = ShareState(declines = 1),
            counts = mapOf("app_opened" to 2),
        )
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns Answer.Success(InAppShareResult.Declined)

        useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(2, storedData.share.declines)
        assertEquals(NOW, storedData.share.lastPromptAt)
        assertFalse(storedData.share.hasShared)
    }

    @Test
    fun `Given share is dismissed When invoked Then it counts as a decline and timestamp is stored`() = runTest {
        storedData = EngagementData(
            review = ReviewState(ratedAt = 1L),
            share = ShareState(declines = 1),
            counts = mapOf("app_opened" to 2),
        )
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns Answer.Success(InAppShareResult.Dismissed)

        useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(2, storedData.share.declines)
        assertEquals(NOW, storedData.share.lastPromptAt)
        assertFalse(storedData.share.hasShared)
    }

    @Test
    fun `Given target chosen When invoked Then timestamp is stored`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), counts = mapOf("app_opened" to 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns
            Answer.Success(InAppShareResult.TargetChosen("com.whatsapp"))

        useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(NOW, storedData.share.lastPromptAt)
        assertEquals(NOW, storedData.share.sharedAt)
        assertEquals(0, storedData.share.declines)
    }

    @Test
    fun `Given share prompted within cooldown When invoked Then share is not prompted`() = runTest {
        storedData = EngagementData(
            review = ReviewState(ratedAt = 1L),
            share = ShareState(lastPromptAt = NOW - COOLDOWN + 1),
            counts = mapOf("app_opened" to 2),
        )
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertEquals(NOW - COOLDOWN + 1, storedData.share.lastPromptAt)
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given cooldown elapsed When invoked Then share is prompted again`() = runTest {
        storedData = EngagementData(
            review = ReviewState(ratedAt = 1L),
            share = ShareState(lastPromptAt = NOW - COOLDOWN),
            counts = mapOf("app_opened" to 2),
        )
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        assertEquals(NOW, storedData.share.lastPromptAt)
        coVerify(exactly = 1) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given max declines reached When invoked Then share is never prompted`() = runTest {
        storedData = EngagementData(
            review = ReviewState(ratedAt = 1L),
            share = ShareState(declines = 2),
            counts = mapOf("app_opened" to 2),
        )
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given review unavailable and max declines reached When invoked Then share is skipped`() = runTest {
        storedData = EngagementData(share = ShareState(declines = 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { rateAppUseCase(any()) } returns Answer.Success(true)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewUnavailable), result)
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given review unavailable and share not allowed When invoked Then share is skipped and prompt is pending`() =
        runTest {
            storedData = EngagementData(share = ShareState(declines = 2, lastPromptAt = NOW - GAP))
            every { engagementEligibility.isEligible(any(), any()) } returns true
            coEvery { rateAppUseCase(any()) } returns Answer.Success(true)

            val result = useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(Answer.Success(EngagementResult.ReviewUnavailable), result)
            assertEquals(NOW, storedData.pendingPromptAt)
            coVerify(exactly = 0) { shareAppUseCase(any()) }
        }

    @Test
    fun `Given review attempts below cap When eligible Then review is launched`() = runTest {
        storedData = EngagementData(review = ReviewState(attempts = 2))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewLaunched), result)
    }

    @Test
    fun `Given review attempts cap reached When eligible Then review is not launched and share is`() = runTest {
        storedData = EngagementData(review = ReviewState(attempts = 3))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        coVerify(exactly = 0) { rateAppUseCase(any()) }
        coVerify(exactly = 1) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given review within the review cooldown When eligible Then review is not launched`() = runTest {
        storedData = EngagementData(review = ReviewState(attempts = 1, lastAt = NOW - REVIEW_COOLDOWN + 1))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        useCase(EngagementUseCase.Params("app_opened"))

        coVerify(exactly = 0) { rateAppUseCase(any()) }
    }

    @Test
    fun `Given review cooldown elapsed When eligible Then review is launched again`() = runTest {
        storedData = EngagementData(review = ReviewState(attempts = 1, lastAt = NOW - REVIEW_COOLDOWN))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewLaunched), result)
    }

    @Test
    fun `Given user rated the app When eligible Then review is never launched`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), share = ShareState(sharedAt = 1L))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        useCase(EngagementUseCase.Params("app_opened"))

        coVerify(exactly = 0) { rateAppUseCase(any()) }
    }

    @Test
    fun `Given review launched within gap When eligible Then share is not prompted`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L, lastAt = NOW - GAP + 1))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given review launched a gap ago When eligible Then share is prompted`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L, lastAt = NOW - GAP))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        coVerify(exactly = 1) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given share prompted within gap When eligible Then review is not launched`() = runTest {
        storedData = EngagementData(share = ShareState(lastPromptAt = NOW - GAP + 1))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        useCase(EngagementUseCase.Params("app_opened"))

        coVerify(exactly = 0) { rateAppUseCase(any()) }
    }

    @Test
    fun `Given share prompted a gap ago When eligible Then review is launched`() = runTest {
        storedData = EngagementData(share = ShareState(lastPromptAt = NOW - GAP))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewLaunched), result)
    }

    @Test
    fun `Given review fails and share not allowed When eligible Then prompt is kept pending`() = runTest {
        storedData = EngagementData(share = ShareState(sharedAt = 1L))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { rateAppUseCase(any()) } returns Answer.Success(true)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewUnavailable), result)
        assertEquals(NOW, storedData.pendingPromptAt)
    }

    @Test
    fun `Given share returns no result When eligible Then prompt is kept pending`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns Answer.Failure(failure)

        useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(NOW, storedData.pendingPromptAt)
    }

    @Test
    fun `Given nothing allowed When milestone is reached Then pending is not set`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), share = ShareState(sharedAt = 1L))
        every { engagementEligibility.isEligible(any(), any()) } returns true

        useCase(EngagementUseCase.Params("app_opened"))

        assertNull(storedData.pendingPromptAt)
    }

    @Test
    fun `Given pending prompt When non milestone call is made Then prompt is shown`() = runTest {
        storedData = EngagementData(pendingPromptAt = NOW - 1)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewLaunched), result)
        coVerify(exactly = 1) { rateAppUseCase(any()) }
    }

    @Test
    fun `Given pending prompt and review not allowed When share is shown Then pending is cleared`() = runTest {
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), pendingPromptAt = NOW - 1)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        assertNull(storedData.pendingPromptAt)
        assertEquals(NOW, storedData.share.lastPromptAt)
    }

    @Test
    fun `Given stale pending prompt and nothing allowed When invoked Then pending is cleared`() = runTest {
        storedData = EngagementData(
            review = ReviewState(ratedAt = 1L),
            share = ShareState(sharedAt = 1L),
            pendingPromptAt = NOW - 1,
        )

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertNull(storedData.pendingPromptAt)
        coVerify(exactly = 0) { rateAppUseCase(any()) }
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given share is dismissed with one decline left When invoked again Then share is no longer prompted`() =
        runTest {
            storedData = EngagementData(
                review = ReviewState(ratedAt = 1L),
                share = ShareState(declines = 1),
                counts = mapOf("app_opened" to 2),
            )
            every { engagementEligibility.isEligible(any(), any()) } returns true

            useCase(EngagementUseCase.Params("app_opened"))
            storedData = storedData.copy(share = storedData.share.copy(lastPromptAt = null))
            val result = useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(Answer.Success(EngagementResult.None), result)
            coVerify(exactly = 1) { shareAppUseCase(any()) }
        }

    @Test
    fun `Given young install When milestone is reached Then nothing is launched or pending`() = runTest {
        useCase = createUseCase(installTime = NOW - MIN_INSTALL_AGE + 1)
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertEquals(EngagementData(counts = mapOf("app_opened" to 1)), storedData)
        coVerify(exactly = 0) { rateAppUseCase(any()) }
        coVerify(exactly = 0) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given app installed exactly the minimum age ago When milestone is reached Then review is launched`() =
        runTest {
            useCase = createUseCase(installTime = NOW - MIN_INSTALL_AGE)
            every { engagementEligibility.isEligible(any(), any()) } returns true

            val result = useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(Answer.Success(EngagementResult.ReviewLaunched), result)
        }

    @Test
    fun `Given young install and engagement in progress When invoked concurrently Then pending is not set`() = runTest {
        useCase = createUseCase(installTime = NOW - MIN_INSTALL_AGE + 1)
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
        assertNull(storedData.pendingPromptAt)
        gate.complete(Unit)
        first.await()
    }

    @Test
    fun `Given pending prompt younger than the ttl When non milestone call is made Then prompt is shown`() = runTest {
        storedData = EngagementData(pendingPromptAt = NOW - TTL + 1)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewLaunched), result)
    }

    @Test
    fun `Given pending as old as the ttl When non milestone call is made Then it is cleared and nothing shown`() =
        runTest {
            storedData = EngagementData(pendingPromptAt = NOW - TTL)

            val result = useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(Answer.Success(EngagementResult.None), result)
            assertEquals(EngagementData(counts = mapOf("app_opened" to 1)), storedData)
            coVerify(exactly = 0) { rateAppUseCase(any()) }
            coVerify(exactly = 0) { shareAppUseCase(any()) }
        }

    @Test
    fun `Given expired pending prompt When milestone is reached Then prompt is shown and pending is cleared`() =
        runTest {
            storedData = EngagementData(pendingPromptAt = NOW - TTL - 1)
            every { engagementEligibility.isEligible(any(), any()) } returns true

            val result = useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(Answer.Success(EngagementResult.ReviewLaunched), result)
            assertNull(storedData.pendingPromptAt)
        }

    @Test
    fun `Given review unavailable and share not allowed When invoked Then pending stores the current time`() = runTest {
        storedData = EngagementData(share = ShareState(sharedAt = 1L))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { rateAppUseCase(any()) } returns Answer.Success(true)

        useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(NOW, storedData.pendingPromptAt)
    }

    @Test
    fun `Given valid pending prompt and engagement in progress When milestone is deferred Then pending is refreshed`() =
        runTest {
            every { engagementEligibility.isEligible(any(), any()) } returns true
            val gate = CompletableDeferred<Unit>()
            coEvery { rateAppUseCase(any()) } coAnswers {
                gate.await()
                Answer.Success(false)
            }

            val first = async { useCase(EngagementUseCase.Params("app_opened")) }
            runCurrent()
            storedData = storedData.copy(pendingPromptAt = NOW - 100)
            useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(NOW, storedData.pendingPromptAt)
            gate.complete(Unit)
            first.await()
        }

    @Test
    fun `Given custom policy with zero review attempts When eligible Then review is never launched and share is`() =
        runTest {
            useCase = createUseCase(policy = EngagementPolicy(maxReviewAttempts = 0))
            every { engagementEligibility.isEligible(any(), any()) } returns true

            val result = useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
            coVerify(exactly = 0) { rateAppUseCase(any()) }
            coVerify(exactly = 1) { shareAppUseCase(any()) }
        }

    @Test
    fun `Given custom policy with a shorter ttl When pending is older than it Then pending is cleared`() = runTest {
        useCase = createUseCase(policy = EngagementPolicy(pendingPromptTtl = 1.hours))
        storedData = EngagementData(pendingPromptAt = NOW - 1.hours.inWholeMilliseconds)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertNull(storedData.pendingPromptAt)
    }

    @Test
    fun `Given custom policy with a share cooldown When elapsed Then share is prompted`() = runTest {
        useCase = createUseCase(policy = EngagementPolicy(shareCooldown = 1.days, minPromptsGap = 1.days))
        storedData = EngagementData(
            review = ReviewState(ratedAt = 1L),
            share = ShareState(lastPromptAt = NOW - 1.days.inWholeMilliseconds),
        )
        every { engagementEligibility.isEligible(any(), any()) } returns true

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
    }

    @Test
    fun `Given pending prompt and no prompt allowed When invoked Then pending is cleared`() = runTest {
        storedData = EngagementData(
            review = ReviewState(ratedAt = 1L),
            share = ShareState(declines = 2),
            pendingPromptAt = NOW - 1,
        )

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertNull(storedData.pendingPromptAt)
    }

    @Test
    fun `Given valid pending and share cannot show When non milestone call Then timestamp is unchanged`() = runTest {
        val pendingAt = NOW - 1.hours.inWholeMilliseconds
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), pendingPromptAt = pendingAt)
        coEvery { shareAppUseCase(any()) } returns Answer.Failure(failure)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ShareNotChosen), result)
        assertEquals(pendingAt, storedData.pendingPromptAt)
    }

    @Test
    fun `Given valid pending and review unavailable When non milestone call Then timestamp is unchanged`() = runTest {
        val pendingAt = NOW - 1.hours.inWholeMilliseconds
        storedData = EngagementData(share = ShareState(sharedAt = 1L), pendingPromptAt = pendingAt)
        coEvery { rateAppUseCase(any()) } returns Answer.Success(true)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewUnavailable), result)
        assertEquals(pendingAt, storedData.pendingPromptAt)
    }

    @Test
    fun `Given valid pending and review unavailable When milestone call Then timestamp is refreshed`() = runTest {
        storedData = EngagementData(
            share = ShareState(sharedAt = 1L),
            pendingPromptAt = NOW - 1.hours.inWholeMilliseconds,
        )
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { rateAppUseCase(any()) } returns Answer.Success(true)

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.ReviewUnavailable), result)
        assertEquals(NOW, storedData.pendingPromptAt)
    }

    @Test
    fun `Given older valid pending and engagement in progress When milestone is deferred Then it is refreshed`() =
        runTest {
            every { engagementEligibility.isEligible(any(), any()) } returns true
            val gate = CompletableDeferred<Unit>()
            coEvery { rateAppUseCase(any()) } coAnswers {
                gate.await()
                Answer.Success(false)
            }

            val first = async { useCase(EngagementUseCase.Params("app_opened")) }
            runCurrent()
            storedData = storedData.copy(pendingPromptAt = NOW - 1.hours.inWholeMilliseconds)
            val second = useCase(EngagementUseCase.Params("app_opened"))

            assertEquals(Answer.Success(EngagementResult.None), second)
            assertEquals(NOW, storedData.pendingPromptAt)
            gate.complete(Unit)
            first.await()
        }

    @Test
    fun `Given pending kept by failing calls When ttl passes Then next non milestone call clears it`() = runTest {
        val pendingAt = NOW - TTL + 1
        storedData = EngagementData(review = ReviewState(ratedAt = 1L), pendingPromptAt = pendingAt)
        coEvery { shareAppUseCase(any()) } returns Answer.Failure(failure)

        useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(pendingAt, storedData.pendingPromptAt)

        clock = NOW + 1
        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertNull(storedData.pendingPromptAt)
        coVerify(exactly = 1) { shareAppUseCase(any()) }
    }

    @Test
    fun `Given zero ttl When pending was just marked Then next non milestone call does not count it`() = runTest {
        useCase = createUseCase(policy = EngagementPolicy(pendingPromptTtl = Duration.ZERO))
        storedData = EngagementData(review = ReviewState(ratedAt = 1L))
        every { engagementEligibility.isEligible(any(), any()) } returns true
        coEvery { shareAppUseCase(any()) } returns Answer.Failure(failure)
        useCase(EngagementUseCase.Params("app_opened"))
        assertEquals(NOW, storedData.pendingPromptAt)
        every { engagementEligibility.isEligible(any(), any()) } returns false

        val result = useCase(EngagementUseCase.Params("app_opened"))

        assertEquals(Answer.Success(EngagementResult.None), result)
        assertNull(storedData.pendingPromptAt)
        coVerify(exactly = 1) { shareAppUseCase(any()) }
    }

    private companion object {
        const val NOW = 1_000_000_000_000L
        val COOLDOWN = 30.days.inWholeMilliseconds
        val REVIEW_COOLDOWN = 90.days.inWholeMilliseconds
        val GAP = 14.days.inWholeMilliseconds
        val TTL = 48.hours.inWholeMilliseconds
        val MIN_INSTALL_AGE = 3.days.inWholeMilliseconds
        val INSTALL_AGE = 10.days.inWholeMilliseconds
    }
}
