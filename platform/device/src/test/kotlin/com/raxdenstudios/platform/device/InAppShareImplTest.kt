package com.raxdenstudios.platform.device

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.core.util.IntentFactoryImpl
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppShareImplTest {

    private val receiverSlot = slot<BroadcastReceiver>()
    private val context: Context = mockk {
        every { applicationContext } returns this
        every { packageName } returns "com.test"
        every { unregisterReceiver(any()) } just Runs
    }
    private var inAppShare = InAppShareImpl(context, UnconfinedTestDispatcher())
    private val shareLauncher: ActivityResultLauncher<Intent> = mockk {
        every { launch(any()) } just Runs
        every { unregister() } just Runs
    }
    private val resultCallback = slot<ActivityResultCallback<ActivityResult>>()
    private val activityResultRegistry: ActivityResultRegistry = mockk {
        every {
            register(
                any<String>(),
                any<LifecycleOwner>(),
                any<ActivityResultContract<Intent, ActivityResult>>(),
                capture(resultCallback),
            )
        } returns shareLauncher
    }
    private lateinit var lifecycle: Lifecycle
    private lateinit var activity: ComponentActivity

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given no activity attached When launch Then returns resource not found failure`() =
        runTest {
            val result = inAppShare.launch(InAppShareContent(subject = "subject", text = "text"))

            assertThat(result).isEqualTo(Answer.Failure(Failure.ResourceNotFound("No activity attached")))
        }

    @Test
    fun `Given an attached activity When attach is called again with it Then it is a no-op`() {
        attachActivity()

        inAppShare.attach(activity)

        verify(exactly = 1) { lifecycle.addObserver(any()) }
    }

    @Test
    fun `Given an attached activity When a different activity is attached Then the observer moves`() {
        attachActivity()
        val firstLifecycle = lifecycle

        attachActivity()

        verify { firstLifecycle.removeObserver(inAppShare) }
        verify { lifecycle.addObserver(inAppShare) }
    }

    @Test
    fun `Given attached activity destroyed When launch Then returns resource not found failure`() =
        runTest {
            attachActivity()
            inAppShare.onDestroy(activity)

            val result = inAppShare.launch(InAppShareContent(subject = "subject", text = "text"))

            assertThat(result).isEqualTo(
                Answer.Failure(Failure.ResourceNotFound("No activity attached")),
            )
            verify { lifecycle.removeObserver(inAppShare) }
        }

    @Test
    fun `Given a share already in progress When launch Then returns already in progress failure`() =
        runTest {
            stubShareFramework()
            inAppShare = createInAppShare()
            attachActivity()
            val content = InAppShareContent(subject = "subject", text = "text")
            val pending = launch { inAppShare.launch(content) }
            runCurrent()

            val result = inAppShare.launch(content)

            assertThat(result).isEqualTo(
                Answer.Failure(Failure.Unknown("A share is already in progress")),
            )
            pending.cancel()
        }

    @Test
    fun `Given share launched When the sheet is closed without a target Then returns dismissed`() =
        runTest {
            stubShareFramework()
            inAppShare = createInAppShare()
            attachActivity()
            var result: Answer<InAppShareResult, Failure>? = null
            launch {
                result = inAppShare.launch(InAppShareContent(subject = "subject", text = "text"))
            }
            runCurrent()

            resultCallback.captured.onActivityResult(
                ActivityResult(Activity.RESULT_CANCELED, null),
            )
            advanceUntilIdle()

            assertThat(result).isEqualTo(Answer.Success(InAppShareResult.Dismissed))
            verify { context.unregisterReceiver(receiverSlot.captured) }
        }

    @Test
    fun `Given share launched When a target is chosen Then returns target chosen`() = runTest {
        stubShareFramework()
        inAppShare = createInAppShare()
        attachActivity()
        var result: Answer<InAppShareResult, Failure>? = null
        launch {
            result = inAppShare.launch(InAppShareContent(subject = "subject", text = "text"))
        }
        runCurrent()

        chosenTarget("com.whatsapp")
        resultCallback.captured.onActivityResult(
            ActivityResult(Activity.RESULT_OK, null),
        )
        advanceUntilIdle()

        assertThat(result).isEqualTo(
            Answer.Success(InAppShareResult.TargetChosen("com.whatsapp")),
        )
    }

    @Test
    fun `Given share launched When activity is destroyed Then pending share resolves as dismissed`() =
        runTest {
            stubShareFramework()
            inAppShare = createInAppShare()
            attachActivity()
            var result: Answer<InAppShareResult, Failure>? = null
            launch {
                result = inAppShare.launch(InAppShareContent(subject = "subject", text = "text"))
            }
            runCurrent()

            inAppShare.onDestroy(activity)
            advanceUntilIdle()

            assertThat(result).isEqualTo(Answer.Success(InAppShareResult.Dismissed))
        }

    private fun TestScope.createInAppShare(): InAppShareImpl =
        InAppShareImpl(context, UnconfinedTestDispatcher(testScheduler))

    private fun attachActivity() {
        lifecycle = mockk()
        activity = mockk()
        every { lifecycle.currentState } returns Lifecycle.State.INITIALIZED
        every { lifecycle.addObserver(any()) } just Runs
        every { lifecycle.removeObserver(any()) } just Runs
        every { activity.lifecycle } returns lifecycle
        every { activity.activityResultRegistry } returns activityResultRegistry
        inAppShare.attach(activity)
    }

    @Suppress("DEPRECATION")
    private fun chosenTarget(targetPackage: String) {
        val component: ComponentName = mockk { every { packageName } returns targetPackage }
        val intent: Intent = mockk {
            every { getParcelableExtra<ComponentName>(Intent.EXTRA_CHOSEN_COMPONENT) } returns
                component
        }
        receiverSlot.captured.onReceive(context, intent)
    }

    /**
     * Stubs the framework wiring done inside `launch` (`Intent`/`IntentFilter`/`PendingIntent` are
     * android.jar stubs in local unit tests) and captures the broadcast receiver registered for the
     * chosen-component callback.
     */
    private fun stubShareFramework() {
        mockkConstructor(IntentFactoryImpl::class)
        every { anyConstructed<IntentFactoryImpl>().build(any()) } returns mockk()
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setPackage(any()) } returns mockk()
        mockkConstructor(IntentFilter::class)
        mockkStatic(Intent::class)
        every { Intent.createChooser(any(), any(), any()) } returns mockk()
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk {
            every { intentSender } returns mockk()
        }
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.registerReceiver(any(), capture(receiverSlot), any(), any())
        } returns mockk()
    }
}
