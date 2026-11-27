package com.raxdenstudios.platform.notification.domain

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class WasNotificationPermissionRequestedUseCaseImplTest {

    private val repository: NotificationPermissionRepository = mockk()
    private val useCase = WasNotificationPermissionRequestedUseCaseImpl(repository)

    @Test
    fun `Given the dialog was shown before When invoked Then returns true`() = runTest {
        coEvery { repository.wasRequested() } returns Answer.Success(true)

        assertThat(useCase(WasNotificationPermissionRequestedUseCase.Params)).isEqualTo(Answer.Success(true))
    }

    @Test
    fun `Given the dialog was never shown When invoked Then returns false`() = runTest {
        coEvery { repository.wasRequested() } returns Answer.Success(false)

        assertThat(useCase(WasNotificationPermissionRequestedUseCase.Params)).isEqualTo(Answer.Success(false))
    }

    @Test
    fun `Given the repository fails When invoked Then propagates the failure`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        coEvery { repository.wasRequested() } returns failure

        assertThat(useCase(WasNotificationPermissionRequestedUseCase.Params)).isEqualTo(failure)
    }
}
