package com.raxdenstudios.platform.notification.data.repository

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.datasource.NotificationPermissionLocalDataSource
import com.raxdenstudios.platform.notification.permission.NotificationPermissionLauncher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class NotificationPermissionRepositoryImplTest {

    private val launcher: NotificationPermissionLauncher = mockk()
    private val localDataSource: NotificationPermissionLocalDataSource = mockk()
    private val repository = NotificationPermissionRepositoryImpl(launcher, localDataSource)

    @Test
    fun `Given the launcher When reading the platform state Then wraps each value`() {
        every { launcher.areNotificationsEnabled() } returns true
        every { launcher.isRuntimePermission() } returns false
        every { launcher.isPermissionGranted() } returns true
        every { launcher.shouldShowRationale() } returns false

        assertThat(repository.areNotificationsEnabled()).isEqualTo(Answer.Success(true))
        assertThat(repository.isRuntimePermission()).isEqualTo(Answer.Success(false))
        assertThat(repository.isPermissionGranted()).isEqualTo(Answer.Success(true))
        assertThat(repository.shouldShowRationale()).isEqualTo(Answer.Success(false))
    }

    @Test
    fun `Given the data source When wasRequested Then delegates`() = runTest {
        coEvery { localDataSource.wasRequested() } returns Answer.Success(true)

        assertThat(repository.wasRequested()).isEqualTo(Answer.Success(true))
    }

    @Test
    fun `Given the dialog was shown When request Then marks it as requested`() = runTest {
        coEvery { launcher.launch() } returns true
        coEvery { localDataSource.markAsRequested() } returns Answer.Success(Unit)

        assertThat(repository.request()).isEqualTo(Answer.Success(true))
        coVerify(exactly = 1) { localDataSource.markAsRequested() }
    }

    @Test
    fun `Given the dialog was not shown When request Then does not mark it`() = runTest {
        coEvery { launcher.launch() } returns false

        assertThat(repository.request()).isEqualTo(Answer.Success(false))
        coVerify(exactly = 0) { localDataSource.markAsRequested() }
    }

    @Test
    fun `Given marking fails When request Then the result is still success`() = runTest {
        coEvery { launcher.launch() } returns true
        coEvery { localDataSource.markAsRequested() } returns Answer.Failure(mockk<Failure>())

        assertThat(repository.request()).isEqualTo(Answer.Success(true))
    }
}
