package com.raxdenstudios.platform.notification.domain

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepository
import com.raxdenstudios.platform.notification.domain.model.NotificationPermissionDialogResult
import com.raxdenstudios.platform.notification.domain.model.NotificationPermissionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class LaunchNotificationPermissionDialogUseCaseImplTest {

    private val repository: NotificationPermissionRepository = mockk()
    private val getState: GetNotificationPermissionStateUseCase = mockk()
    private val useCase = LaunchNotificationPermissionDialogUseCaseImpl(repository, getState)

    private suspend fun launch() = useCase(LaunchNotificationPermissionDialogUseCase.Params)

    @Test
    fun `Given a requestable state When invoked Then requests and returns the new state`() = runTest {
        coEvery { getState(any()) } returnsMany listOf(
            Answer.Success(NotificationPermissionState.Requestable),
            Answer.Success(NotificationPermissionState.Granted),
        )
        coEvery { repository.request() } returns Answer.Success(true)

        assertThat(launch()).isEqualTo(
            Answer.Success(NotificationPermissionDialogResult(NotificationPermissionState.Granted, true)),
        )
    }

    @Test
    fun `Given a requestable state and no dialog shown When invoked Then reports isDialogShown false`() = runTest {
        coEvery { getState(any()) } returns Answer.Success(NotificationPermissionState.Requestable)
        coEvery { repository.request() } returns Answer.Success(false)

        assertThat(launch()).isEqualTo(
            Answer.Success(NotificationPermissionDialogResult(NotificationPermissionState.Requestable, false)),
        )
    }

    @Test
    fun `Given a state that is not requestable When invoked Then does not request`() = runTest {
        listOf(
            NotificationPermissionState.Granted,
            NotificationPermissionState.Blocked,
            NotificationPermissionState.Disabled,
        ).forEach { state ->
            coEvery { getState(any()) } returns Answer.Success(state)

            assertThat(launch()).isEqualTo(Answer.Success(NotificationPermissionDialogResult(state, false)))
        }
        coVerify(exactly = 0) { repository.request() }
    }

    @Test
    fun `Given the state fails When invoked Then propagates the failure`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        coEvery { getState(any()) } returns failure

        assertThat(launch()).isEqualTo(failure)
    }

    @Test
    fun `Given the request fails When invoked Then propagates the failure`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        coEvery { getState(any()) } returns Answer.Success(NotificationPermissionState.Requestable)
        coEvery { repository.request() } returns failure

        assertThat(launch()).isEqualTo(failure)
    }
}
