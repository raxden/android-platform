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

internal class SubscribeFCMTopicUseCaseImplTest {

    private val fcmRepository: FCMRepository = mockk()
    private val useCase = SubscribeFCMTopicUseCaseImpl(fcmRepository)

    @Test
    fun `invoke subscribes to topic in repository`() = runTest {
        coEvery { fcmRepository.subscribeToTopic("news") } returns Answer.Success(Unit)

        val result = useCase(SubscribeFCMTopicUseCase.Params("news"))

        assertThat(result).isEqualTo(Answer.Success(Unit))
        coVerify { fcmRepository.subscribeToTopic("news") }
    }

    @Test
    fun `invoke propagates failure`() = runTest {
        val failure = Failure.Unknown("error")
        coEvery { fcmRepository.subscribeToTopic("news") } returns Answer.Failure(failure)

        val result = useCase(SubscribeFCMTopicUseCase.Params("news"))

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }
}
