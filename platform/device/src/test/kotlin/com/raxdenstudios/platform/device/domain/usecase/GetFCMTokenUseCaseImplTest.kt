package com.raxdenstudios.platform.device.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.repository.FCMRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GetFCMTokenUseCaseImplTest {

    private val fcmRepository: FCMRepository = mockk()
    private val useCase = GetFCMTokenUseCaseImpl(fcmRepository)

    @Test
    fun `invoke returns repository answer`() = runTest {
        coEvery { fcmRepository.getToken() } returns Answer.Success("token")

        val result = useCase(GetFCMTokenUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success("token"))
        coVerify { fcmRepository.getToken() }
    }

    @Test
    fun `invoke propagates failure`() = runTest {
        val failure = Failure.Unknown("error")
        coEvery { fcmRepository.getToken() } returns Answer.Failure(failure)

        val result = useCase(GetFCMTokenUseCase.Params)

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }
}
