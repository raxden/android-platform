package com.raxdenstudios.platform.notification.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class NotificationPermissionLauncherImplTest {

    private val context: Context = mockk(relaxed = true)
    private val launcher = NotificationPermissionLauncherImpl(context = context)

    @Before
    fun setUp() {
        denyPermission()
    }

    private fun denyPermission() {
        every { context.checkPermission(Manifest.permission.POST_NOTIFICATIONS, any(), any()) } returns
            PackageManager.PERMISSION_DENIED
    }

    @Test
    fun `Given the permission is granted When isPermissionGranted Then returns true`() {
        every { context.checkPermission(Manifest.permission.POST_NOTIFICATIONS, any(), any()) } returns
            PackageManager.PERMISSION_GRANTED

        assertThat(launcher.isPermissionGranted()).isTrue()
    }

    @Test
    fun `Given the permission is denied When isPermissionGranted Then returns false`() {
        assertThat(launcher.isPermissionGranted()).isFalse()
    }

    @Test
    @Config(sdk = [32])
    fun `Given below Android 13 When isPermissionGranted Then returns true`() {
        assertThat(launcher.isPermissionGranted()).isTrue()
    }

    @Test
    fun `Given the permission is granted When launch Then returns false and shows nothing`() = runTest {
        val activity = attachedActivity()
        every { context.checkPermission(Manifest.permission.POST_NOTIFICATIONS, any(), any()) } returns
            PackageManager.PERMISSION_GRANTED

        assertThat(launcher.launch()).isFalse()

        verify(exactly = 0) { activity.permissionLauncher.launch(any()) }
    }

    @Test
    fun `Given Android 13 When isRuntimePermission Then returns true`() {
        assertThat(launcher.isRuntimePermission()).isTrue()
    }

    @Test
    @Config(sdk = [32])
    fun `Given below Android 13 When isRuntimePermission Then returns false`() {
        assertThat(launcher.isRuntimePermission()).isFalse()
    }

    @Test
    fun `Given no attached activity When launch Then returns false`() = runTest {
        assertThat(launcher.launch()).isFalse()
    }

    @Test
    @Config(sdk = [32])
    fun `Given below Android 13 When launch Then returns false and shows nothing`() = runTest {
        val activity = attachedActivity()

        assertThat(launcher.launch()).isFalse()

        verify(exactly = 0) { activity.permissionLauncher.launch(any()) }
    }

    @Test
    fun `Given an attached activity When launch and the user answers Then returns true`() = runTest {
        val activity = attachedActivity()

        val result = async { launcher.launch() }
        runCurrent()
        activity.answer(granted = true)

        assertThat(result.await()).isTrue()
        verify { activity.permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    @Test
    fun `Given several attached activities When launch Then uses the newest one`() = runTest {
        val first = attachedActivity()
        val second = attachedActivity()

        val result = async { launcher.launch() }
        runCurrent()
        second.answer(granted = false)

        assertThat(result.await()).isTrue()
        verify(exactly = 1) { second.permissionLauncher.launch(any()) }
        verify(exactly = 0) { first.permissionLauncher.launch(any()) }
    }

    @Test
    fun `Given the top activity is destroyed When launch Then falls back to the one below`() = runTest {
        val first = attachedActivity()
        val second = attachedActivity()
        every { second.activity.isDestroyed } returns true

        val result = async { launcher.launch() }
        runCurrent()
        first.answer(granted = true)

        assertThat(result.await()).isTrue()
        verify(exactly = 1) { first.permissionLauncher.launch(any()) }
        verify(exactly = 0) { second.permissionLauncher.launch(any()) }
    }

    @Test
    fun `Given the top activity lifecycle ends When launch Then falls back to the one below`() = runTest {
        val first = attachedActivity()
        val second = attachedActivity()
        second.destroy()

        val result = async { launcher.launch() }
        runCurrent()
        first.answer(granted = true)

        assertThat(result.await()).isTrue()
        verify(exactly = 1) { first.permissionLauncher.launch(any()) }
    }

    @Test
    fun `Given the only activity is destroyed When launch Then returns false`() = runTest {
        val activity = attachedActivity()
        activity.destroy()

        assertThat(launcher.launch()).isFalse()
    }

    @Test
    fun `Given the top activity reports a rationale When shouldShowRationale Then returns true`() {
        val first = attachedActivity()
        val second = attachedActivity()
        every {
            first.activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } returns false
        every {
            second.activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } returns true

        assertThat(launcher.shouldShowRationale()).isTrue()
    }

    @Test
    fun `Given no attached activity When shouldShowRationale Then returns false`() {
        assertThat(launcher.shouldShowRationale()).isFalse()
    }

    @Test
    @Config(sdk = [32])
    fun `Given below Android 13 When shouldShowRationale Then returns false`() {
        val activity = attachedActivity()
        every {
            activity.activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } returns true

        assertThat(launcher.shouldShowRationale()).isFalse()
    }

    @Test
    fun `Given the dialog cannot be launched When launch Then returns false and a later answer has no effect`() =
        runTest {
            val activity = attachedActivity()
            every { activity.permissionLauncher.launch(any()) } throws IllegalStateException("boom")

            assertThat(launcher.launch()).isFalse()
            activity.answer(granted = true)
        }

    @Test
    fun `Given a pending request When launch again Then the first returns false and the second true after answer`() =
        runTest {
            val activity = attachedActivity()

            val first = async { launcher.launch() }
            runCurrent()
            val second = async { launcher.launch() }
            runCurrent()
            assertThat(first.await()).isFalse()
            assertThat(second.isCompleted).isFalse()
            activity.answer(granted = true)

            assertThat(second.await()).isTrue()
        }

    @Test
    fun `Given the owner is destroyed while pending When not changing configuration Then launch returns false`() =
        runTest {
            val activity = attachedActivity()

            val result = async { launcher.launch() }
            runCurrent()
            activity.destroy()

            assertThat(result.await()).isFalse()
        }

    @Test
    fun `Given the owner is destroyed in a configuration change When the new activity answers Then returns true`() =
        runTest {
            val old = attachedActivity()

            val result = async { launcher.launch() }
            runCurrent()
            old.changingConfigurations = true
            old.destroy()
            runCurrent()
            assertThat(result.isCompleted).isFalse()
            val recreated = attachedActivity()
            recreated.answer(granted = true)

            assertThat(result.await()).isTrue()
        }

    @Test
    fun `Given a non owner activity is destroyed When pending Then the request stays pending`() = runTest {
        val first = attachedActivity()
        val second = attachedActivity()

        val result = async { launcher.launch() }
        runCurrent()
        first.destroy()
        runCurrent()
        assertThat(result.isCompleted).isFalse()
        second.answer(granted = true)

        assertThat(result.await()).isTrue()
    }

    private fun attachedActivity(): FakeActivity {
        val fake = FakeActivity()
        launcher.attach(fake.activity)
        fake.create()
        return fake
    }

    private class FakeActivity {

        val permissionLauncher: ActivityResultLauncher<String> = mockk(relaxed = true)
        val activity: ComponentActivity = mockk(relaxed = true)
        private val lifecycle: Lifecycle = mockk(relaxed = true)
        private val registry: ActivityResultRegistry = mockk()
        private lateinit var observer: DefaultLifecycleObserver
        var changingConfigurations = false
        private lateinit var callback: ActivityResultCallback<Boolean>

        init {
            every { activity.isDestroyed } returns false
            every { activity.isChangingConfigurations } answers { changingConfigurations }
            every { activity.lifecycle } returns lifecycle
            every { activity.activityResultRegistry } returns registry
            every { lifecycle.addObserver(any()) } answers { observer = firstArg() }
            every { lifecycle.removeObserver(any()) } just runs
            every {
                registry.register(
                    any<String>(),
                    any<LifecycleOwner>(),
                    any<ActivityResultContract<String, Boolean>>(),
                    any<ActivityResultCallback<Boolean>>(),
                )
            } answers {
                callback = arg(3)
                permissionLauncher
            }
        }

        fun create() {
            observer.onCreate(activity)
        }

        fun destroy() {
            observer.onDestroy(activity)
        }

        fun answer(
            granted: Boolean,
        ) {
            callback.onActivityResult(granted)
        }
    }
}
