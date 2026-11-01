package com.raxdenstudios.platform.device.data.repository

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.FirebaseMessagingProvider
import com.raxdenstudios.platform.device.data.datasource.FCMLocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class FCMRepositoryImplTest {

    private val fcmLocalDataSource: FCMLocalDataSource = mockk()
    private val firebaseMessagingProvider: FirebaseMessagingProvider = mockk()
    private val repository = FCMRepositoryImpl(fcmLocalDataSource, firebaseMessagingProvider)

    @Test
    fun `getToken returns cached token without hitting firebase`() = runTest {
        coEvery { fcmLocalDataSource.getToken() } returns Answer.Success("cached-token")

        val result = repository.getToken()

        assertThat(result).isEqualTo(Answer.Success("cached-token"))
        coVerify(exactly = 0) { firebaseMessagingProvider.getToken() }
    }

    @Test
    fun `getToken fetches and stores firebase token when cache is empty`() = runTest {
        coEvery { fcmLocalDataSource.getToken() } returns Answer.Success("")
        coEvery { firebaseMessagingProvider.getToken() } returns Answer.Success("firebase-token")
        coEvery { fcmLocalDataSource.saveToken("firebase-token") } returns Answer.Success(Unit)

        val result = repository.getToken()

        assertThat(result).isEqualTo(Answer.Success("firebase-token"))
        coVerify { fcmLocalDataSource.saveToken("firebase-token") }
    }

    @Test
    fun `getToken returns empty token when cache is empty and firebase fails`() = runTest {
        coEvery { fcmLocalDataSource.getToken() } returns Answer.Success("")
        coEvery { firebaseMessagingProvider.getToken() } returns
            Answer.Failure(Failure.Unknown("no token"))

        val result = repository.getToken()

        assertThat(result).isEqualTo(Answer.Success(""))
        coVerify(exactly = 0) { fcmLocalDataSource.saveToken(any()) }
    }

    @Test
    fun `saveToken delegates to local datasource`() = runTest {
        coEvery { fcmLocalDataSource.saveToken("token") } returns Answer.Success(Unit)

        val result = repository.saveToken("token")

        assertThat(result).isEqualTo(Answer.Success(Unit))
        coVerify { fcmLocalDataSource.saveToken("token") }
    }

    @Test
    fun `clearToken delegates to local datasource`() = runTest {
        coEvery { fcmLocalDataSource.clearToken() } returns Answer.Success(Unit)

        val result = repository.clearToken()

        assertThat(result).isEqualTo(Answer.Success(Unit))
        coVerify { fcmLocalDataSource.clearToken() }
    }

    @Test
    fun `subscribeToTopic delegates to firebase provider`() = runTest {
        coEvery { firebaseMessagingProvider.subscribeToTopic("news") } returns Answer.Success(Unit)

        val result = repository.subscribeToTopic("news")

        assertThat(result).isEqualTo(Answer.Success(Unit))
        coVerify { firebaseMessagingProvider.subscribeToTopic("news") }
    }
}
