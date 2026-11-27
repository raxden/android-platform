package com.raxdenstudios.platform.notification.data.datasource

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.google.firebase.messaging.FirebaseMessaging
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.notification.domain.model.NotificationTopic
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class NotificationTopicRemoteDataSourceImplTest {

    private val firebaseMessaging: FirebaseMessaging = mockk()
    private val task: Task<Void> = mockk()
    private val topic = NotificationTopic("daily_p1")
    private val dataSource = NotificationTopicRemoteDataSourceImpl { firebaseMessaging }

    private fun completeTaskWith(
        successful: Boolean,
        error: Exception? = null,
        cancelled: Boolean = false,
    ) {
        every { task.isSuccessful } returns successful
        every { task.isCanceled } returns cancelled
        every { task.result } returns null
        every { task.exception } returns error
        every { task.addOnCompleteListener(any<OnCompleteListener<Void>>()) } answers {
            firstArg<OnCompleteListener<Void>>().onComplete(task)
            task
        }
    }

    @Test
    fun `Given the provider When constructed Then Firebase is not obtained until first use`() = runTest {
        var calls = 0
        val lazyDataSource = NotificationTopicRemoteDataSourceImpl {
            calls++
            firebaseMessaging
        }
        assertThat(calls).isEqualTo(0)
        every { firebaseMessaging.subscribeToTopic(any()) } returns task
        every { firebaseMessaging.unsubscribeFromTopic(any()) } returns task
        completeTaskWith(successful = true)

        lazyDataSource.subscribe(topic)
        lazyDataSource.unsubscribe(topic)

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `Given the task succeeds When subscribe Then returns success`() = runTest {
        every { firebaseMessaging.subscribeToTopic("daily_p1") } returns task
        completeTaskWith(successful = true)

        assertThat(dataSource.subscribe(topic)).isEqualTo(Answer.Success(Unit))
        verify { firebaseMessaging.subscribeToTopic("daily_p1") }
    }

    @Test
    fun `Given the task fails When subscribe Then returns failure`() = runTest {
        every { firebaseMessaging.subscribeToTopic(any()) } returns task
        completeTaskWith(successful = false, error = IllegalStateException("boom"))

        assertThat(dataSource.subscribe(topic)).isInstanceOf(Answer.Failure::class.java)
    }

    @Test
    fun `Given the task succeeds When unsubscribe Then returns success`() = runTest {
        every { firebaseMessaging.unsubscribeFromTopic("daily_p1") } returns task
        completeTaskWith(successful = true)

        assertThat(dataSource.unsubscribe(topic)).isEqualTo(Answer.Success(Unit))
        verify { firebaseMessaging.unsubscribeFromTopic("daily_p1") }
    }

    @Test
    fun `Given the task fails When unsubscribe Then returns failure`() = runTest {
        every { firebaseMessaging.unsubscribeFromTopic(any()) } returns task
        completeTaskWith(successful = false, cancelled = true)

        assertThat(dataSource.unsubscribe(topic)).isInstanceOf(Answer.Failure::class.java)
    }

    @Test
    fun `Given the task never completes When subscribe Then fails after 10 seconds`() = runTest {
        every { firebaseMessaging.subscribeToTopic(any()) } returns task
        every { task.addOnCompleteListener(any<OnCompleteListener<Void>>()) } returns task

        val result = async { dataSource.subscribe(topic) }
        advanceTimeBy(9_999)
        runCurrent()
        assertThat(result.isActive).isTrue()
        advanceTimeBy(1)
        runCurrent()

        assertThat(result.await()).isInstanceOf(Answer.Failure::class.java)
        assertThat(currentTime).isEqualTo(10_000L)
    }

    @Test
    fun `Given the task never completes When unsubscribe Then fails after 10 seconds`() = runTest {
        every { firebaseMessaging.unsubscribeFromTopic(any()) } returns task
        every { task.addOnCompleteListener(any<OnCompleteListener<Void>>()) } returns task

        val result = async { dataSource.unsubscribe(topic) }
        advanceTimeBy(10_000)
        runCurrent()

        assertThat(result.await()).isInstanceOf(Answer.Failure::class.java)
        assertThat(currentTime).isEqualTo(10_000L)
    }
}
