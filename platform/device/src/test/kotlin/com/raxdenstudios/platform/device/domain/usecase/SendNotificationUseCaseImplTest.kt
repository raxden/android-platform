package com.raxdenstudios.platform.device.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.model.NotificationData
import com.raxdenstudios.platform.device.domain.repository.NotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class SendNotificationUseCaseImplTest {

    private val notificationRepository: NotificationRepository = mockk()
    private val useCase = SendNotificationUseCaseImpl(notificationRepository)

    @Test
    fun `invoke sends notification through repository`() = runTest {
        val notification = mockk<NotificationData>()
        coEvery { notificationRepository.send(notification) } returns Answer.Success(Unit)

        val result = useCase(SendNotificationUseCase.Params(notification))

        assertThat(result).isEqualTo(Answer.Success(Unit))
        coVerify { notificationRepository.send(notification) }
    }

    @Test
    fun `invoke propagates failure`() = runTest {
        val notification = mockk<NotificationData>()
        val failure = Failure.Unknown("error")
        coEvery { notificationRepository.send(notification) } returns Answer.Failure(failure)

        val result = useCase(SendNotificationUseCase.Params(notification))

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }
}
