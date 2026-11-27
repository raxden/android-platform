package com.raxdenstudios.platform.notification.domain

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class AreNotificationsEnabledUseCaseImplTest {

    private val repository: NotificationPermissionRepository = mockk()
    private val useCase = AreNotificationsEnabledUseCaseImpl(repository)

    @Test
    fun `Given notifications enabled When invoked Then returns true`() = runTest {
        every { repository.areNotificationsEnabled() } returns Answer.Success(true)

        assertThat(useCase(AreNotificationsEnabledUseCase.Params)).isEqualTo(Answer.Success(true))
    }

    @Test
    fun `Given notifications disabled When invoked Then returns false`() = runTest {
        every { repository.areNotificationsEnabled() } returns Answer.Success(false)

        assertThat(useCase(AreNotificationsEnabledUseCase.Params)).isEqualTo(Answer.Success(false))
    }

    @Test
    fun `Given the repository fails When invoked Then propagates the failure`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        every { repository.areNotificationsEnabled() } returns failure

        assertThat(useCase(AreNotificationsEnabledUseCase.Params)).isEqualTo(failure)
    }
}
