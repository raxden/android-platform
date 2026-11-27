package com.raxdenstudios.platform.notification.domain

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepository
import com.raxdenstudios.platform.notification.domain.model.NotificationPermissionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

internal class GetNotificationPermissionStateUseCaseImplTest {

    private val repository: NotificationPermissionRepository = mockk()
    private val useCase = GetNotificationPermissionStateUseCaseImpl(repository)

    @Before
    fun setUp() {
        every { repository.areNotificationsEnabled() } returns Answer.Success(false)
        every { repository.isRuntimePermission() } returns Answer.Success(true)
        every { repository.isPermissionGranted() } returns Answer.Success(false)
        every { repository.shouldShowRationale() } returns Answer.Success(false)
        coEveryWasRequested(false)
    }

    private fun coEveryWasRequested(
        requested: Boolean,
    ) {
        io.mockk.coEvery { repository.wasRequested() } returns Answer.Success(requested)
    }

    private suspend fun state() = useCase(GetNotificationPermissionStateUseCase.Params)

    @Test
    fun `Given notifications enabled When invoked Then Granted`() = runTest {
        every { repository.areNotificationsEnabled() } returns Answer.Success(true)

        assertThat(state()).isEqualTo(Answer.Success(NotificationPermissionState.Granted))
        verify(exactly = 0) { repository.isRuntimePermission() }
    }

    @Test
    fun `Given disabled below Android 13 When invoked Then Disabled`() = runTest {
        every { repository.isRuntimePermission() } returns Answer.Success(false)

        assertThat(state()).isEqualTo(Answer.Success(NotificationPermissionState.Disabled))
    }

    @Test
    fun `Given permission granted but notifications off When invoked Then Disabled`() = runTest {
        every { repository.isPermissionGranted() } returns Answer.Success(true)

        assertThat(state()).isEqualTo(Answer.Success(NotificationPermissionState.Disabled))
    }

    @Test
    fun `Given rationale should be shown When invoked Then Requestable`() = runTest {
        every { repository.shouldShowRationale() } returns Answer.Success(true)
        coEveryWasRequested(true)

        assertThat(state()).isEqualTo(Answer.Success(NotificationPermissionState.Requestable))
    }

    @Test
    fun `Given no rationale and asked before When invoked Then Blocked`() = runTest {
        coEveryWasRequested(true)

        assertThat(state()).isEqualTo(Answer.Success(NotificationPermissionState.Blocked))
    }

    @Test
    fun `Given never asked When invoked Then Requestable`() = runTest {
        assertThat(state()).isEqualTo(Answer.Success(NotificationPermissionState.Requestable))
    }

    @Test
    fun `Given the repository fails When invoked Then propagates the failure`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        every { repository.areNotificationsEnabled() } returns failure

        assertThat(state()).isEqualTo(failure)
    }

    @Test
    fun `Given wasRequested fails When invoked Then propagates the failure`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        io.mockk.coEvery { repository.wasRequested() } returns failure

        assertThat(state()).isEqualTo(failure)
    }
}
