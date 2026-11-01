package com.raxdenstudios.platform.device.data.repository

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.datasource.NotificationDataSource
import com.raxdenstudios.platform.device.domain.model.NotificationData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class NotificationRepositoryImplTest {

    private val notificationDataSource: NotificationDataSource = mockk()
    private val repository = NotificationRepositoryImpl(notificationDataSource)

    @Test
    fun `send delegates to datasource`() = runTest {
        val notification = mockk<NotificationData>()
        coEvery { notificationDataSource.send(notification) } returns Answer.Success(Unit)

        val result = repository.send(notification)

        assertThat(result).isEqualTo(Answer.Success(Unit))
        coVerify { notificationDataSource.send(notification) }
    }

    @Test
    fun `send propagates datasource failure`() = runTest {
        val notification = mockk<NotificationData>()
        val failure = Failure.Unknown("send failed")
        coEvery { notificationDataSource.send(notification) } returns Answer.Failure(failure)

        val result = repository.send(notification)

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }
}
