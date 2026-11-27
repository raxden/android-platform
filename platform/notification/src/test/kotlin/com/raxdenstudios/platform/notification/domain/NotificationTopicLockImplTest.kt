package com.raxdenstudios.platform.notification.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class NotificationTopicLockImplTest {

    private val lock = NotificationTopicLockImpl()

    @Test
    fun `Given a block When withLock Then returns its result`() = runTest {
        assertThat(lock.withLock { 42 }).isEqualTo(42)
    }

    @Test
    fun `Given two concurrent blocks When withLock Then they run one after the other`() = runTest {
        val events = mutableListOf<String>()

        val first = async {
            lock.withLock {
                events += "first start"
                delay(100)
                events += "first end"
            }
        }
        val second = async {
            lock.withLock {
                events += "second start"
                events += "second end"
            }
        }
        advanceUntilIdle()
        first.await()
        second.await()

        assertThat(events).containsExactly("first start", "first end", "second start", "second end").inOrder()
    }

    @Test
    fun `Given a block that throws When withLock Then the lock is released`() = runTest {
        runCatching { lock.withLock { error("boom") } }

        assertThat(lock.withLock { "ok" }).isEqualTo("ok")
    }
}
