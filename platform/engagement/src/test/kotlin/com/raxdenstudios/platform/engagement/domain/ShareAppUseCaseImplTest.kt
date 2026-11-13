package com.raxdenstudios.platform.engagement.domain

import android.content.Context
import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppShare
import com.raxdenstudios.platform.device.inapp.InAppShareContent
import com.raxdenstudios.platform.device.inapp.InAppShareResult
import com.raxdenstudios.platform.engagement.R
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

    private val context: Context = mockk()
    private val stringProvider: StringProvider = mockk()
    private val inAppShare: InAppShare = mockk()

    private val failure = Failure.Unknown("error")

    private lateinit var useCase: ShareAppUseCaseImpl

    @Before
    fun setUp() {
        every { context.packageName } returns "com.raxdenstudios.app.debug"
        every { stringProvider.getString(R.string.account_share_text_subject) } returns "subject"
        every { stringProvider.getString(R.string.account_share_text_value, any()) } returns "value"
        coEvery { inAppShare.share(any()) } returns Answer.Success(InAppShareResult.Dismissed)
        coEvery { inAppShare.launch(any()) } returns Answer.Success(InAppShareResult.Dismissed)

        useCase = ShareAppUseCaseImpl(
            context = context,
            stringProvider = stringProvider,
            inAppShare = inAppShare,
        )
    }

    @Test
    fun `Given debug package name When invoked without prompt Then share sheet is opened with app content`() = runTest {
        useCase(ShareAppUseCase.Params())

        coVerify {
            inAppShare.share(
                InAppShareContent(
                    subject = "subject",
                    text = "value",
                ),
            )
        }
        coVerify(exactly = 0) { inAppShare.launch(any()) }
        verify {
            stringProvider.getString(
                R.string.account_share_text_value,
                "com.raxdenstudios.app",
            )
        }
    }

    @Test
    fun `Given with prompt When invoked Then share is launched with the modal prompt`() = runTest {
        val answer = useCase(ShareAppUseCase.Params(withPrompt = true))

        assertEquals(Answer.Success(InAppShareResult.Dismissed), answer)
        coVerify {
            inAppShare.launch(
                InAppShareContent(
                    subject = "subject",
                    text = "value",
                ),
            )
        }
        coVerify(exactly = 0) { inAppShare.share(any()) }
    }

    @Test
    fun `Given release package name When invoked Then package name is kept`() = runTest {
        every { context.packageName } returns "com.raxdenstudios.app"

        useCase(ShareAppUseCase.Params())

        verify {
            stringProvider.getString(
                R.string.account_share_text_value,
                "com.raxdenstudios.app",
            )
        }
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
