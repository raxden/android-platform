package com.raxdenstudios.platform.device.inapp

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppUpdateImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val manager: AppUpdateManager = mockk()
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

    @Test
    fun `Given no update available When check Then returns success with null and state stays idle`() = runTest {
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(availability = UpdateAvailability.UPDATE_NOT_AVAILABLE),
        )

        val result = inAppUpdate.check()

        assertThat(result).isEqualTo(Answer.Success(null))
        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
        verify {
            tracker.checked(
                available = false,
                versionCode = 2L,
                priority = 0L,
                stalenessDays = null,
            )
        }
    }

    @Test
    fun `Given update available When check Then returns update info`() = runTest {
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(versionCode = 2, priority = 4, staleness = 3),
        )

        val result = inAppUpdate.check()

        val expected = InAppUpdateInfo(
            availableVersionCode = 2,
            priority = 4,
            stalenessDays = 3,
            isRecommended = true,
        )
        assertThat(result).isEqualTo(Answer.Success(expected))
        verify {
            tracker.checked(
                available = true,
                versionCode = 2L,
                priority = 4L,
                stalenessDays = 3L,
            )
        }
    }

    @Test
    fun `Given update recommended by default config When check Then state is available`() = runTest {
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(versionCode = 2, priority = 4, staleness = 3),
        )

        inAppUpdate.check()

        val expected = InAppUpdateInfo(
            availableVersionCode = 2,
            priority = 4,
            stalenessDays = 3,
            isRecommended = true,
        )
        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Available(expected))
    }

    @Test
    fun `Given update check fails When check Then returns unknown failure`() = runTest {
        every { manager.appUpdateInfo } returns Tasks.forException(Exception("boom"))

        val result = inAppUpdate.check()

        assertThat(result).isEqualTo(
            Answer.Failure(Failure.Unknown("In-app update status could not be checked: boom")),
        )
        verify { tracker.failed(InAppUpdateTracker.Reason.ERROR) }
    }

    @Test
    fun `Given minimum priority not met When check Then info is not recommended and state is idle`() = runTest {
        inAppUpdate = createInAppUpdate(InAppUpdateConfig(minPriority = 4))
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(priority = 1),
        )

        val result = inAppUpdate.check()

        val expected = InAppUpdateInfo(
            availableVersionCode = 2,
            priority = 1,
            stalenessDays = null,
            isRecommended = false,
        )
        assertThat(result).isEqualTo(Answer.Success(expected))
        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
    }

    @Test
    fun `Given minimum staleness not met When check Then info is not recommended`() = runTest {
        inAppUpdate = createInAppUpdate(InAppUpdateConfig(minStalenessDays = 5))
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(staleness = 2),
        )

        val result = inAppUpdate.check()

        assertThat((result as Answer.Success).value?.isRecommended).isFalse()
        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
    }

    @Test
    fun `Given unknown staleness When check with minimum staleness Then staleness is treated as zero`() = runTest {
        inAppUpdate = createInAppUpdate(InAppUpdateConfig(minStalenessDays = 1))
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(staleness = null),
        )

        val result = inAppUpdate.check()

        assertThat((result as Answer.Success).value?.isRecommended).isFalse()
        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
    }

    @Test
    fun `Given immediate not allowed by Play When check Then info is not recommended and state is idle`() = runTest {
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(immediateAllowed = false),
        )

        val result = inAppUpdate.check()

        assertThat((result as Answer.Success).value?.isRecommended).isFalse()
        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
    }

    @Test
    fun `Given developer triggered update in progress When check Then state is in progress`() = runTest {
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(
                availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            ),
        )

        val result = inAppUpdate.check()

        assertThat(result).isEqualTo(Answer.Success(null))
        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.InProgress)
        verify {
            tracker.checked(
                available = false,
                versionCode = 2L,
                priority = 0L,
                stalenessDays = null,
            )
        }
    }

    @Test
    fun `Given no attached activity When launch Then returns resource not found failure`() = runTest {
        val result = inAppUpdate.launch()

        assertThat(result).isEqualTo(
            Answer.Failure(Failure.ResourceNotFound("No activity attached")),
        )
        verify { tracker.failed(InAppUpdateTracker.Reason.NO_ACTIVITY) }
    }

    @Test
    fun `Given attached activity not resumed When launch Then returns unknown failure`() = runTest {
        attachActivity()

        val result = inAppUpdate.launch()

        assertThat(result).isEqualTo(
            Answer.Failure(
                Failure.Unknown("In-app update could not be launched: Activity is not resumed"),
            ),
        )
        verify { tracker.failed(InAppUpdateTracker.Reason.ERROR) }
    }

    @Test
    fun `Given developer triggered update in progress When launch Then returns already in progress`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(
                availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            ),
        )

        val result = inAppUpdate.launch()

        assertThat(result)
            .isEqualTo(Answer.Success(InAppUpdateLaunchResult.AlreadyInProgress))
        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.InProgress)
        verify { tracker.launched(InAppUpdateLaunchResult.AlreadyInProgress) }
    }

    @Test
    fun `Given update not available When launch Then returns not available`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(availability = UpdateAvailability.UPDATE_NOT_AVAILABLE),
        )

        val result = inAppUpdate.launch()

        assertThat(result)
            .isEqualTo(Answer.Success(InAppUpdateLaunchResult.NotAvailable))
        verify { tracker.launched(InAppUpdateLaunchResult.NotAvailable) }
    }

    @Test
    fun `Given immediate not allowed by Play When launch Then returns not allowed`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(
            appUpdateInfo(immediateAllowed = false),
        )

        val result = inAppUpdate.launch()

        assertThat(result)
            .isEqualTo(Answer.Success(InAppUpdateLaunchResult.NotAllowed))
        verify { tracker.launched(InAppUpdateLaunchResult.NotAllowed) }
    }

    @Test
    fun `Given update flow cannot be started When launch Then returns not allowed`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo())
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns false

        val result = inAppUpdate.launch()

        assertThat(result)
            .isEqualTo(Answer.Success(InAppUpdateLaunchResult.NotAllowed))
        verify { tracker.launched(InAppUpdateLaunchResult.NotAllowed) }
    }

    @Test
    fun `Given update available When launch Then starts immediate update flow`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        val info = appUpdateInfo()
        every { manager.appUpdateInfo } returns Tasks.forResult(info)
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true

        val result = inAppUpdate.launch()

        assertThat(result).isEqualTo(Answer.Success(InAppUpdateLaunchResult.Started))
        verify {
            manager.startUpdateFlowForResult(
                info,
                any(),
                match { it.appUpdateType() == AppUpdateType.IMMEDIATE },
            )
        }
        verify { tracker.launched(InAppUpdateLaunchResult.Started) }
    }

    @Test
    fun `Given a launch already started When launch Then returns already in progress`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo())
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true
        inAppUpdate.launch()

        val result = inAppUpdate.launch()

        assertThat(result)
            .isEqualTo(Answer.Success(InAppUpdateLaunchResult.AlreadyInProgress))
        verify { tracker.launched(InAppUpdateLaunchResult.Started) }
        verify { tracker.launched(InAppUpdateLaunchResult.AlreadyInProgress) }
    }

    @Test
    fun `Given policy not met When launch Then ignores policy and starts update flow`() = runTest {
        inAppUpdate = createInAppUpdate(InAppUpdateConfig(minPriority = 5))
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        val info = appUpdateInfo(priority = 0)
        every { manager.appUpdateInfo } returns Tasks.forResult(info)
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true

        val result = inAppUpdate.launch()

        assertThat(result).isEqualTo(Answer.Success(InAppUpdateLaunchResult.Started))
        verify {
            manager.startUpdateFlowForResult(
                info,
                any(),
                match { it.appUpdateType() == AppUpdateType.IMMEDIATE },
            )
        }
    }

    @Test
    fun `Given update available When activity resumes Then state is refreshed to available`() = runTest {
        val activity = attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns completedUpdateInfoTask(
            appUpdateInfo(versionCode = 2, priority = 4, staleness = 3),
        )

        inAppUpdate.onResume(activity)

        val expected = InAppUpdateInfo(
            availableVersionCode = 2,
            priority = 4,
            stalenessDays = 3,
            isRecommended = true,
        )
        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Available(expected))
    }

    @Test
    fun `Given in-progress update When activity resumes Then update flow is restarted`() = runTest {
        val activity = attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        val info = appUpdateInfo(
            availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
        )
        every { manager.appUpdateInfo } returns completedUpdateInfoTask(info)
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true

        inAppUpdate.onResume(activity)

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.InProgress)
        verify {
            manager.startUpdateFlowForResult(
                info,
                any(),
                match { it.appUpdateType() == AppUpdateType.IMMEDIATE },
            )
        }
        verify { tracker.resumed() }
    }

    @Test
    fun `Given in-progress update When resume cannot restart the flow Then resumed is not tracked`() = runTest {
        val activity = attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        val info = appUpdateInfo(
            availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
        )
        every { manager.appUpdateInfo } returns completedUpdateInfoTask(info)
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns false

        inAppUpdate.onResume(activity)

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.InProgress)
        verify(exactly = 0) { tracker.resumed() }
    }

    @Test
    fun `Given in-progress update restarted on resume When launch Then returns already in progress`() = runTest {
        val activity = attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        val info = appUpdateInfo(
            availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
        )
        every { manager.appUpdateInfo } returns completedUpdateInfoTask(info)
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true
        inAppUpdate.onResume(activity)

        val result = inAppUpdate.launch()

        assertThat(result)
            .isEqualTo(Answer.Success(InAppUpdateLaunchResult.AlreadyInProgress))
    }

    @Test
    fun `Given in-progress update When owner is not resumed Then update flow is not restarted`() = runTest {
        val activity = attachActivity()
        every { manager.appUpdateInfo } returns completedUpdateInfoTask(
            appUpdateInfo(
                availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            ),
        )

        inAppUpdate.onResume(activity)

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.InProgress)
        verify(exactly = 0) {
            manager.startUpdateFlowForResult(any(), any(), any())
        }
    }

    @Test
    fun `Given resume event from a different owner When onResume Then refresh is skipped`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns completedUpdateInfoTask(appUpdateInfo())
        val other: ComponentActivity = mockk()

        inAppUpdate.onResume(other)

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
        verify(exactly = 0) {
            manager.startUpdateFlowForResult(any(), any(), any())
        }
    }

    @Test
    fun `Given refresh request fails When activity resumes Then state stays idle`() = runTest {
        val activity = attachActivity()
        every { manager.appUpdateInfo } returns failedUpdateInfoTask(Exception("boom"))

        inAppUpdate.onResume(activity)

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
    }

    @Test
    fun `Given app is not installed from Play When activity resumes Then state stays idle`() = runTest {
        val activity = attachActivity()
        val error = InstallException(InstallErrorCode.ERROR_APP_NOT_OWNED)
        every { manager.appUpdateInfo } returns failedUpdateInfoTask(error)

        inAppUpdate.onResume(activity)

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
    }

    @Test
    fun `Given update flow launched When result is ok Then accepted is tracked`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo())
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true
        inAppUpdate.launch()

        resultCallback.captured.onActivityResult(
            ActivityResult(Activity.RESULT_OK, null),
        )

        verify { tracker.accepted() }
    }

    @Test
    fun `Given update flow launched When result is canceled Then state is declined`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo())
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true
        inAppUpdate.launch()

        resultCallback.captured.onActivityResult(
            ActivityResult(Activity.RESULT_CANCELED, null),
        )

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Declined)
        verify { tracker.declined() }
    }

    @Test
    fun `Given update flow launched When result is unexpected Then state is failed`() = runTest {
        attachActivity()
        lifecycleState = Lifecycle.State.RESUMED
        every { manager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo())
        every { manager.startUpdateFlowForResult(any(), any(), any()) } returns true
        inAppUpdate.launch()

        resultCallback.captured.onActivityResult(
            ActivityResult(Activity.RESULT_FIRST_USER, null),
        )

        assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Failed)
        verify { tracker.flowFailed() }
    }

    @Test
    fun `Given attached activity destroyed When launch Then returns resource not found failure`() = runTest {
        val activity = attachActivity()
        inAppUpdate.onDestroy(activity)

        val result = inAppUpdate.launch()

        assertThat(result).isEqualTo(
            Answer.Failure(Failure.ResourceNotFound("No activity attached")),
        )
        verify { tracker.failed(InAppUpdateTracker.Reason.NO_ACTIVITY) }
    }

    private fun createInAppUpdate(config: InAppUpdateConfig = InAppUpdateConfig()) = InAppUpdateImpl(
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
    ): AppUpdateInfo = mockk {
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
    private fun completedUpdateInfoTask(info: AppUpdateInfo): Task<AppUpdateInfo> {
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

    private fun failedUpdateInfoTask(error: Exception): Task<AppUpdateInfo> {
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
}
