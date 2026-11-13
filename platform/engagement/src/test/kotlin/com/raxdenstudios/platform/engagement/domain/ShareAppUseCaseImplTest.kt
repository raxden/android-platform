package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppShare
import com.raxdenstudios.platform.device.inapp.InAppShareContent
import com.raxdenstudios.platform.device.inapp.InAppShareResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class ShareAppUseCaseImplTest {

    private val shareContentProvider: ShareContentProvider = mockk()
    private val inAppShare: InAppShare = mockk()

    private val failure = Failure.Unknown("error")
    private val content = InAppShareContent(subject = "subject", text = "text")

    private lateinit var useCase: ShareAppUseCaseImpl

    @Before
    fun setUp() {
        every { shareContentProvider.provide() } returns content
        coEvery { inAppShare.share(any()) } returns Answer.Success(InAppShareResult.Dismissed)
        coEvery { inAppShare.launch(any()) } returns Answer.Success(InAppShareResult.Dismissed)

        useCase = ShareAppUseCaseImpl(
            shareContentProvider = shareContentProvider,
            inAppShare = inAppShare,
        )
    }

    @Test
    fun `Given share content When invoked without prompt Then share sheet is opened with provider content`() = runTest {
        useCase(ShareAppUseCase.Params())

        verify { shareContentProvider.provide() }
        coVerify { inAppShare.share(content) }
        coVerify(exactly = 0) { inAppShare.launch(any()) }
    }

    @Test
    fun `Given share content When invoked with prompt Then share is launched with the modal prompt`() = runTest {
        val answer = useCase(ShareAppUseCase.Params(withPrompt = true))

        assertEquals(Answer.Success(InAppShareResult.Dismissed), answer)
        coVerify { inAppShare.launch(content) }
        coVerify(exactly = 0) { inAppShare.share(any()) }
    }

    @Test
    fun `Given target chosen When invoked Then result is returned`() = runTest {
        val result = InAppShareResult.TargetChosen("com.whatsapp")
        coEvery { inAppShare.share(any()) } returns Answer.Success(result)

        val answer = useCase(ShareAppUseCase.Params())

        assertEquals(Answer.Success(result), answer)
    }

    @Test
    fun `Given share failure When invoked Then failure is propagated`() = runTest {
        coEvery { inAppShare.share(any()) } returns Answer.Failure(failure)

        val answer = useCase(ShareAppUseCase.Params())

        assertEquals(Answer.Failure(failure), answer)
    }
}
