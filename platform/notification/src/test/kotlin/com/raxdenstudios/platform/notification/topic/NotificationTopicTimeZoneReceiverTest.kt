package com.raxdenstudios.platform.notification.topic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raxdenstudios.commons.coroutines.CoroutineScopeProvider
import com.raxdenstudios.commons.coroutines.test.TestCoroutineScopeProvider
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class NotificationTopicTimeZoneReceiverTest {

    private val syncer: NotificationTopicSyncer = mockk(relaxed = true)
    private val pendingResult: BroadcastReceiver.PendingResult = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun TestScope.receiver(): NotificationTopicTimeZoneReceiver {
        val scopeProvider: CoroutineScopeProvider = TestCoroutineScopeProvider(this)
        startKoin {
            modules(
                module {
                    single { syncer }
                    single { scopeProvider }
                },
            )
        }
        return NotificationTopicTimeZoneReceiver().apply {
            // setPendingResult is hidden API: the system calls it before goAsync() can be used.
            BroadcastReceiver::class.java
                .getDeclaredMethod("setPendingResult", BroadcastReceiver.PendingResult::class.java)
                .invoke(this, pendingResult)
        }
    }

    @Test
    fun `Given another action When received Then does nothing`() = runTest {
        receiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        runCurrent()

        coVerify(exactly = 0) { syncer.sync() }
        verify(exactly = 0) { pendingResult.finish() }
    }

    @Test
    fun `Given a time zone change When received Then syncs and finishes the broadcast`() = runTest {
        receiver().onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))
        runCurrent()

        coVerify(exactly = 1) { syncer.sync() }
        verify(exactly = 1) { pendingResult.finish() }
    }
}
