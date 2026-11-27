package com.raxdenstudios.platform.device.inapp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppUpdateFlexibleAndStoreTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val installListener = slot<InstallStateUpdatedListener>()
    private val manager: AppUpdateManager = mockk {
        every { registerListener(capture(installListener)) } just Runs
        every { unregisterListener(any()) } just Runs
    }
    private val tracker: InAppUpdateTracker = mockk(relaxed = true)
    private var lifecycleState = Lifecycle.State.INITIALIZED
    private var inAppUpdate = createInAppUpdate()

    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> = mockk {
        every { launch(any()) } just Runs
        every { unregister() } just Runs
    }
    private val resultCallback = slot<ActivityResultCallback<ActivityResult>>()
    private val activityResultRegistry: ActivityResultRegistry = mockk {
        every {
            register(
                any<String>(),
                any<LifecycleOwner>(),
                any<ActivityResultContract<IntentSenderRequest, ActivityResult>>(),
                capture(resultCallback),
            )
        } returns updateLauncher
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given flexible type When launch Then starts flexible update flow and tracks type`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        val info = appUpdateInfo()
        every { manager.appUpdateInfo } returns Tasks.forResult(info)
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true

        val result = inAppUpdate.launch(InAppUpdateType.FLEXIBLE)

        assertThat(result).isEqualTo(Answer.Success(InAppUpdateLaunchResult.Started))
        verify {
            manager.startUpdateFlowForResult(
                info,
                any(),
                match { it.appUpdateType() == AppUpdateType.FLEXIBLE },
            )
        }
        verify { tracker.launched(InAppUpdateType.FLEXIBLE, InAppUpdateLaunchResult.Started) }
    }

    @Test
    fun `Given flexible not allowed by Play When launch flexible Then returns not allowed`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        val info = appUpdateInfo()
        every { info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) } returns false
        every { manager.appUpdateInfo } returns Tasks.forResult(info)

        val result = inAppUpdate.launch(InAppUpdateType.FLEXIBLE)

        assertThat(result).isEqualTo(Answer.Success(InAppUpdateLaunchResult.NotAllowed))
        verify(exactly = 0) { manager.startUpdateFlowForResult(any(), any(), any()) }
    }

    @Test
    fun `Given update already downloaded When launch Then returns already in progress and state is downloaded`() =
        runTest {
            attachActivity()
            lifecycleState = Lifecycle.State.RESUMED
            every { manager.appUpdateInfo } returns Tasks.forResult(
                appUpdateInfo(installStatus = InstallStatus.DOWNLOADED),
            )

            val result = inAppUpdate.launch()

            assertThat(result).isEqualTo(Answer.Success(InAppUpdateLaunchResult.AlreadyInProgress))
            assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Downloaded)
        }

    @Test
    fun `Given flexible launched and update in progress When check Then state is downloading with progress`() =
        runTest {
            attachActivity()
            lifecycleState = Lifecycle.State.RESUMED
            every { manager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo())
            every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true
            inAppUpdate.launch(InAppUpdateType.FLEXIBLE)
            every { manager.appUpdateInfo } returns Tasks.forResult(
                appUpdateInfo(
                    availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                    installStatus = InstallStatus.DOWNLOADING,
                    bytesDownloaded = 10L,
                    totalBytes = 100L,
                ),
            )

            inAppUpdate.check()

            assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Downloading(10L, 100L))
        }

    @Test
    fun `Given downloaded update When check Then state is downloaded`() = runTest {
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(
                availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                installStatus = InstallStatus.DOWNLOADED,
            ),
        )

        inAppUpdate.check()

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Downloaded)
    }

    @Test
    fun `Given flexible update launched and in progress When activity resumes Then flow is not relaunched`() = runTest {
        val activity = attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo())
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true
        inAppUpdate.launch(InAppUpdateType.FLEXIBLE)
        every { manager.appUpdateInfo } returns completedUpdateInfoTask(
            appUpdateInfo(
                availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                installStatus = InstallStatus.DOWNLOADING,
                bytesDownloaded = 5L,
                totalBytes = 50L,
            ),
        )

        inAppUpdate.onResume(activity)

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Downloading(5L, 50L))
        verify(exactly = 1) { manager.startUpdateFlowForResult(any(), any(), any()) }
        verify(exactly = 0) { tracker.resumed() }
    }

    @Test
    fun `Given downloaded update When activity resumes Then state is downloaded and flow is not relaunched`() =
        runTest {
            val activity = attachActivity()
            lifecycleState = Lifecycle.State.RESUMED
            every { manager.appUpdateInfo } returns completedUpdateInfoTask(
                appUpdateInfo(
                    availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                    installStatus = InstallStatus.DOWNLOADED,
                ),
            )

            inAppUpdate.onResume(activity)

            assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Downloaded)
            verify(exactly = 0) { manager.startUpdateFlowForResult(any(), any(), any()) }
        }

    @Test
    fun `Given attached activity When install state is downloading Then state is downloading`() = runTest {
        attachActivity()

        installListener.captured.onStateUpdate(
            installState(InstallStatus.DOWNLOADING, bytes = 20L, total = 200L),
        )

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Downloading(20L, 200L))
    }

    @Test
    fun `Given attached activity When install state is downloaded Then state is downloaded and tracked`() = runTest {
        attachActivity()

        installListener.captured.onStateUpdate(installState(InstallStatus.DOWNLOADED))

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Downloaded)
        verify { tracker.downloaded() }
    }

    @Test
    fun `Given attached activity When install state is failed Then state is failed and flow failure tracked`() =
        runTest {
            attachActivity()

            installListener.captured.onStateUpdate(installState(InstallStatus.FAILED))

            assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Failed)
            verify { tracker.flowFailed() }
        }

    @Test
    fun `Given downloading state When install state is canceled Then state is idle`() = runTest {
        attachActivity()
        installListener.captured.onStateUpdate(installState(InstallStatus.DOWNLOADING))

        installListener.captured.onStateUpdate(installState(InstallStatus.CANCELED))

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
    }

    @Test
    fun `Given downloaded state When install state is installed Then state is idle`() = runTest {
        attachActivity()
        installListener.captured.onStateUpdate(installState(InstallStatus.DOWNLOADED))

        installListener.captured.onStateUpdate(installState(InstallStatus.INSTALLED))

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
    }

    @Test
    fun `Given other install status When install state updates Then state is unchanged`() = runTest {
        attachActivity()

        installListener.captured.onStateUpdate(installState(InstallStatus.PENDING))

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
    }

    @Test
    fun `Given attached activity When destroyed Then install listener is unregistered`() = runTest {
        val activity = attachActivity()

        inAppUpdate.onDestroy(activity)

        verify { manager.unregisterListener(installListener.captured) }
    }

    @Test
    fun `Given update downloaded When completeUpdate Then returns success and tracks completed`() = runTest {
        every { manager.completeUpdate() } returns completedTask<Void>()

        val result = inAppUpdate.completeUpdate()

        assertThat(result).isEqualTo(Answer.Success(Unit))
        verify { tracker.completed() }
    }

    @Test
    fun `Given complete update fails When completeUpdate Then returns failure and tracks error`() = runTest {
        every { manager.completeUpdate() } returns completedTask<Void>(error = Exception("boom"))

        val result = inAppUpdate.completeUpdate()

        assertThat(result).isInstanceOf(Answer.Failure::class.java)
        verify { tracker.failed(InAppUpdateTracker.Reason.ERROR) }
        verify(exactly = 0) { tracker.completed() }
    }

    @Test
    fun `Given no attached activity When openStoreListing Then returns resource not found failure`() {
        val result = inAppUpdate.openStoreListing()

        assertThat(result).isEqualTo(Answer.Failure(Failure.ResourceNotFound("No activity attached")))
        verify(exactly = 0) { tracker.storeOpened() }
    }

    @Test
    fun `Given attached activity When openStoreListing Then opens market uri and tracks store opened`() {
        val activity = attachActivity()
        mockIntentAndUri()
        every { activity.packageName } returns "com.example"
        every { activity.startActivity(any()) } just Runs

        val result = inAppUpdate.openStoreListing()

        assertThat(result).isEqualTo(Answer.Success(Unit))
        verify { Uri.parse("market://details?id=com.example") }
        verify(exactly = 1) { activity.startActivity(any()) }
        verify { tracker.storeOpened() }
    }

    @Test
    fun `Given Play Store missing When openStoreListing Then falls back to web uri`() {
        val activity = attachActivity()
        mockIntentAndUri()
        every { activity.packageName } returns "com.example"
        var calls = 0
        every { activity.startActivity(any()) } answers {
            if (calls++ == 0) throw ActivityNotFoundException()
        }

        val result = inAppUpdate.openStoreListing()

        assertThat(result).isEqualTo(Answer.Success(Unit))
        verify { Uri.parse("https://play.google.com/store/apps/details?id=com.example") }
        verify(exactly = 2) { activity.startActivity(any()) }
        verify { tracker.storeOpened() }
    }

    @Test
    fun `Given no browser either When openStoreListing Then returns failure`() {
        val activity = attachActivity()
        mockIntentAndUri()
        every { activity.packageName } returns "com.example"
        every { activity.startActivity(any()) } throws ActivityNotFoundException()

        val result = inAppUpdate.openStoreListing()

        assertThat(result).isInstanceOf(Answer.Failure::class.java)
        verify(exactly = 0) { tracker.storeOpened() }
    }

    @Test
    fun `Given unknown launched type and downloading update When check Then state is downloading`() = runTest {
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(
                availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                installStatus = InstallStatus.DOWNLOADING,
                bytesDownloaded = 1L,
                totalBytes = 2L,
            ),
        )

        inAppUpdate.check()

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Downloading(1L, 2L))
    }

    @Test
    fun `Given immediate launched and downloading update When check Then state is in progress`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo())
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true
        inAppUpdate.launch(InAppUpdateType.IMMEDIATE)
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(
                availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                installStatus = InstallStatus.DOWNLOADING,
            ),
        )

        inAppUpdate.check()

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.InProgress)
    }

    @Test
    fun `Given immediate launched When install state is canceled and activity resumes Then flow is not relaunched`() =
        runTest {
            assertNoResumeAfterInstallStatus(InstallStatus.CANCELED)
        }

    @Test
    fun `Given immediate launched When install state is failed and activity resumes Then flow is not relaunched`() =
        runTest {
            assertNoResumeAfterInstallStatus(InstallStatus.FAILED)
        }

    @Test
    fun `Given immediate launched When install state is installed and activity resumes Then flow is not relaunched`() =
        runTest {
            assertNoResumeAfterInstallStatus(InstallStatus.INSTALLED)
        }

    private fun assertNoResumeAfterInstallStatus(
        status: Int,
    ) {
        val activity = attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo())
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true
        kotlinx.coroutines.runBlocking { inAppUpdate.launch(InAppUpdateType.IMMEDIATE) }
        resultCallback.captured.onActivityResult(ActivityResult(Activity.RESULT_OK, null))
        installListener.captured.onStateUpdate(installState(status))
        every { manager.appUpdateInfo } returns completedUpdateInfoTask(
            appUpdateInfo(availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS),
        )

        inAppUpdate.onResume(activity)

        verify(exactly = 1) { manager.startUpdateFlowForResult(any(), any(), any()) }
    }

    @Test
    fun `Given no activity When debug openStoreListing Then returns resource not found failure`() {
        val result = DebugInAppUpdate().openStoreListing()

        assertThat(result).isEqualTo(Answer.Failure(Failure.ResourceNotFound("No activity attached")))
    }

    @Test
    fun `Given attached activity When debug openStoreListing Then opens the store listing`() {
        val activity: ComponentActivity = mockk()
        mockIntentAndUri()
        every { activity.packageName } returns "com.example"
        every { activity.startActivity(any()) } just Runs
        val debug = DebugInAppUpdate()
        debug.attach(activity)

        val result = debug.openStoreListing()

        assertThat(result).isEqualTo(Answer.Success(Unit))
        verify { Uri.parse("market://details?id=com.example") }
    }

    private fun mockIntentAndUri() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
        mockkConstructor(Intent::class)
    }

    private fun installState(
        status: Int,
        bytes: Long = 0L,
        total: Long = 0L,
    ): InstallState = mockk {
        every { installStatus() } returns status
        every { bytesDownloaded() } returns bytes
        every { totalBytesToDownload() } returns total
    }

    private fun createInAppUpdate(
        config: InAppUpdateConfig = InAppUpdateConfig(),
    ) = InAppUpdateImpl(
        manager = manager,
        tracker = tracker,
        config = config,
        mainDispatcher = testDispatcher,
    )

    private fun attachActivity(): ComponentActivity {
        val lifecycle: Lifecycle = mockk()
        val activity: ComponentActivity = mockk()
        lifecycleState = Lifecycle.State.INITIALIZED
        every { lifecycle.currentState } answers { lifecycleState }
        every { lifecycle.addObserver(any()) } just Runs
        every { lifecycle.removeObserver(any()) } just Runs
        every { activity.lifecycle } returns lifecycle
        every { activity.activityResultRegistry } returns activityResultRegistry
        inAppUpdate.attach(activity)
        return activity
    }

    private fun appUpdateInfo(
        availability: Int = UpdateAvailability.UPDATE_AVAILABLE,
        versionCode: Int = 2,
        priority: Int = 0,
        staleness: Int? = null,
        immediateAllowed: Boolean = true,
        installStatus: Int = InstallStatus.UNKNOWN,
        bytesDownloaded: Long = 0L,
        totalBytes: Long = 0L,
    ): AppUpdateInfo = mockk {
        every { installStatus() } returns installStatus
        every { bytesDownloaded() } returns bytesDownloaded
        every { totalBytesToDownload() } returns totalBytes
        every { isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) } returns true
        every { updateAvailability() } returns availability
        every { availableVersionCode() } returns versionCode
        every { updatePriority() } returns priority
        every { clientVersionStalenessDays() } returns staleness
        every { isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) } returns immediateAllowed
    }

    /**
     * A [Task] completed with [info] whose listeners run inline. [Tasks.forResult] cannot be used
     * for the listener-based `onResume` path because `addOnSuccessListener` posts through
     * `Handler(Looper.getMainLooper())`, which does not exist in local unit tests.
     */
    private fun completedUpdateInfoTask(
        info: AppUpdateInfo,
    ): Task<AppUpdateInfo> {
        val task = mockk<Task<AppUpdateInfo>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.isCanceled } returns false
        every { task.exception } answers { null }
        every { task.result } returns info
        every { task.addOnSuccessListener(any<OnSuccessListener<AppUpdateInfo>>()) } answers {
            firstArg<OnSuccessListener<AppUpdateInfo>>().onSuccess(info)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        return task
    }

    private fun failedUpdateInfoTask(
        error: Exception,
    ): Task<AppUpdateInfo> {
        val task = mockk<Task<AppUpdateInfo>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns false
        every { task.isCanceled } returns false
        every { task.exception } returns error
        every { task.addOnSuccessListener(any<OnSuccessListener<AppUpdateInfo>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(error)
            task
        }
        return task
    }

    /**
     * A [Task] already completed whose listeners run inline: `awaitAnswer` registers through
     * `addOnCompleteListener`, whose executor needs the main Looper that local unit tests lack.
     */
    private fun <T> completedTask(
        value: T? = null,
        error: Exception? = null,
    ): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isSuccessful } returns (error == null)
        every { task.isCanceled } returns false
        every { task.exception } returns error
        every { task.result } returns value
        every { task.addOnCompleteListener(any<OnCompleteListener<T>>()) } answers {
            firstArg<OnCompleteListener<T>>().onComplete(task)
            task
        }
        return task
    }
}
