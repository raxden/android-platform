package com.raxdenstudios.platform.core.data.ext

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class TaskExtensionTest {

    private val listeners = mutableListOf<OnCompleteListener<String>>()
    private val task: Task<String> = mockk {
        every { addOnCompleteListener(any<OnCompleteListener<String>>()) } answers {
            listeners.add(firstArg())
            this@mockk
        }
    }

    @Test
    fun `Given successful task When awaitAnswer Then returns success with value`() = runTest {
        every { task.isSuccessful } returns true
        every { task.result } returns "value"

        val deferred = async { task.awaitAnswer("error") }
        runCurrent()
        listeners.single().onComplete(task)

        assertThat(deferred.await()).isEqualTo(Answer.Success("value"))
    }

    @Test
    fun `Given already complete task When awaitAnswer Then returns without waiting`() = runTest {
        every { task.isSuccessful } returns true
        every { task.result } returns "value"
        every { task.addOnCompleteListener(any<OnCompleteListener<String>>()) } answers {
            firstArg<OnCompleteListener<String>>().onComplete(task)
            task
        }

        val result = task.awaitAnswer("error")

        assertThat(result).isEqualTo(Answer.Success("value"))
    }

    @Test
    fun `Given successful void task When awaitUnitAnswer Then returns success with Unit`() = runTest {
        val voidTask: Task<Void> = mockk {
            every { isSuccessful } returns true
            every { result } returns null
            every { addOnCompleteListener(any<OnCompleteListener<Void>>()) } answers {
                firstArg<OnCompleteListener<Void>>().onComplete(this@mockk)
                this@mockk
            }
        }

        val result = voidTask.awaitUnitAnswer("error")

        assertThat(result).isEqualTo(Answer.Success(Unit))
    }

    @Test
    fun `Given failed void task When awaitUnitAnswer Then returns failure`() = runTest {
        val voidTask: Task<Void> = mockk {
            every { isSuccessful } returns false
            every { isCanceled } returns false
            every { exception } returns RuntimeException("boom")
            every { addOnCompleteListener(any<OnCompleteListener<Void>>()) } answers {
                firstArg<OnCompleteListener<Void>>().onComplete(this@mockk)
                this@mockk
            }
        }

        val result = voidTask.awaitUnitAnswer("error")

        assertThat(result).isEqualTo(Answer.Failure(Failure.Unknown("error: boom")))
    }

    @Test
    fun `Given failed task with exception When awaitAnswer Then returns failure with message`() = runTest {
        every { task.isSuccessful } returns false
        every { task.isCanceled } returns false
        every { task.exception } returns RuntimeException("boom")

        val deferred = async { task.awaitAnswer("error") }
        runCurrent()
        listeners.single().onComplete(task)

        assertThat(deferred.await()).isEqualTo(Answer.Failure(Failure.Unknown("error: boom")))
    }

    @Test
    fun `Given failed task without exception When awaitAnswer Then returns failure with default message`() = runTest {
        every { task.isSuccessful } returns false
        every { task.isCanceled } returns false
        every { task.exception } returns null

        val deferred = async { task.awaitAnswer("error") }
        runCurrent()
        listeners.single().onComplete(task)

        assertThat(deferred.await())
            .isEqualTo(Answer.Failure(Failure.Unknown("error: Task failed without exception")))
    }

    @Test
    fun `Given cancelled task When awaitAnswer Then returns task cancelled failure`() = runTest {
        every { task.isSuccessful } returns false
        every { task.isCanceled } returns true

        val deferred = async { task.awaitAnswer("error") }
        runCurrent()
        listeners.single().onComplete(task)

        assertThat(deferred.await())
            .isEqualTo(Answer.Failure(Failure.Unknown("error: task cancelled")))
    }

    @Test
    fun `Given task never completes When timeout elapses Then returns timed out failure`() = runTest {
        val deferred = async { task.awaitAnswer("error", timeout = 5.seconds) }

        advanceTimeBy(5.seconds + 1.seconds)
        runCurrent()

        val failure = (deferred.await() as Answer.Failure).value
        assertThat(failure.message).contains("timed out after")
        assertThat(failure.message).startsWith("error: ")
    }

    @Test
    fun `Given task completes after timeout When listener fires Then late result is dropped`() = runTest {
        every { task.isSuccessful } returns true
        every { task.result } returns "late"
        val deferred = async { task.awaitAnswer("error", timeout = 5.seconds) }
        advanceTimeBy(6.seconds)
        runCurrent()

        listeners.single().onComplete(task)
        runCurrent()

        val failure = (deferred.await() as Answer.Failure).value
        assertThat(failure.message).contains("timed out after")
    }
}
