package com.raxdenstudios.platform.device

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppUpdateImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val manager: AppUpdateManager = mockk()
    private var lifecycleState = Lifecycle.State.INITIALIZED
    private var inAppUpdate = createInAppUpdate()

    private val activityResultRegistry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) = Unit
    }

    @Test
    fun `Given no update available When check Then returns success with null and state stays idle`() =
        runTest {
            every { manager.appUpdateInfo } returns Tasks.forResult(
                appUpdateInfo(availability = UpdateAvailability.UPDATE_NOT_AVAILABLE),
            )

            val result = inAppUpdate.check()

            assertThat(result).isEqualTo(Answer.Success(null))
            assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
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
    }

    @Test
    fun `Given update recommended by default config When check Then state is available`() =
        runTest {
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
    }

    @Test
    fun `Given minimum priority not met When check Then info is not recommended and state is idle`() =
        runTest {
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
    fun `Given unknown staleness When check with minimum staleness Then staleness is treated as zero`() =
        runTest {
            inAppUpdate = createInAppUpdate(InAppUpdateConfig(minStalenessDays = 1))
            every { manager.appUpdateInfo } returns Tasks.forResult(
                appUpdateInfo(staleness = null),
            )

            val result = inAppUpdate.check()

            assertThat((result as Answer.Success).value?.isRecommended).isFalse()
            assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
        }

    @Test
    fun `Given immediate not allowed by Play When check Then info is not recommended and state is idle`() =
        runTest {
            every { manager.appUpdateInfo } returns Tasks.forResult(
                appUpdateInfo(immediateAllowed = false),
            )

            val result = inAppUpdate.check()

            assertThat((result as Answer.Success).value?.isRecommended).isFalse()
            assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.Idle)
        }

    @Test
    fun `Given developer triggered update in progress When check Then state is in progress`() =
        runTest {
            every { manager.appUpdateInfo } returns Tasks.forResult(
                appUpdateInfo(
                    availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                ),
            )

            val result = inAppUpdate.check()

            assertThat(result).isEqualTo(Answer.Success(null))
            assertThat(inAppUpdate.state.value).isEqualTo(InAppUpdateState.InProgress)
        }

    @Test
    fun `Given no attached activity When launch Then returns resource not found failure`() = runTest {
        val result = inAppUpdate.launch()

        assertThat(result).isEqualTo(
            Answer.Failure(Failure.ResourceNotFound("No activity attached")),
        )
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
    }

    @Test
    fun `Given developer triggered update in progress When launch Then returns already in progress`() =
        runTest {
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

    private fun createInAppUpdate(
        config: InAppUpdateConfig = InAppUpdateConfig(),
    ) = InAppUpdateImpl(
        manager = manager,
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
}
