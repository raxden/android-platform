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

internal class SaveFCMTokenUseCaseImplTest {

    private val fcmRepository: FCMRepository = mockk()
    private val useCase = SaveFCMTokenUseCaseImpl(fcmRepository)

    @Test
    fun `invoke saves token in repository`() = runTest {
        coEvery { fcmRepository.saveToken("token") } returns Answer.Success(Unit)

        val result = useCase(SaveFCMTokenUseCase.Params("token"))

        assertThat(result).isEqualTo(Answer.Success(Unit))
        coVerify { fcmRepository.saveToken("token") }
    }

    @Test
    fun `invoke propagates failure`() = runTest {
        val failure = Failure.Unknown("error")
        coEvery { fcmRepository.saveToken("token") } returns Answer.Failure(failure)

        val result = useCase(SaveFCMTokenUseCase.Params("token"))

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }
}
