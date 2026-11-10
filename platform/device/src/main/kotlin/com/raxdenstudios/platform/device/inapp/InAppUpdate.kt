package com.raxdenstudios.platform.device.inapp

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.UpdateAvailability
import com.raxdenstudios.commons.android.ActivityHolder
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.onFailure
import com.raxdenstudios.commons.core.ext.onSuccess
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.model.Failure
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Google Play immediate in-app updates.
 *
 * Play shows its own full-screen UI, downloads and installs the update and restarts the app, so
 * there is no download state to track nor an install step to trigger. If the user leaves the
 * flow midway, it is resumed automatically the next time the attached activity resumes.
 *
 * ## Setup
 *
 * Bind the running activities once, e.g. in `Application.ActivityLifecycleCallbacks`:
 *
 * ```kotlin
 * override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
 *     if (activity !is ComponentActivity) return
 *     inAppUpdate.attach(activity)   // must run before the activity is STARTED
 * }
 * ```
 *
 * Optionally restrict when an update is considered worth offering (default: always):
 *
 * ```kotlin
 * single { InAppUpdateConfig(minPriority = 4, minStalenessDays = 0) }
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * // Typically on app start / screen start: ask Play and launch when the policy accepts it.
 * safeLaunch {
 *     inAppUpdate.check().onSuccess { info ->
 *         if (info?.isRecommended == true) inAppUpdate.launch()
 *     }
 * }
 *
 * // Or only react to the state, e.g. to show your own banner before launching:
 * when (uiState.inAppUpdate) {
 *     is InAppUpdateState.Available -> ShowUpdateBanner { safeLaunch { inAppUpdate.launch() } }
 *     else -> Unit
 * }
 * ```
 */
interface InAppUpdate : DefaultLifecycleObserver {

    /** Current update state. Refreshed on every resume of the attached activity. */
    val state: StateFlow<InAppUpdateState>

    /**
     * Binds this helper to [activity]. Call in `Activity.onCreate` before STARTED, including
     * after recreation. The activity is held weakly and released when it is destroyed.
     */
    @MainThread
    fun attach(activity: ComponentActivity)

    /**
     * Queries Play for a pending update without showing any UI.
     *
     * Success(null) means there is no update available. [InAppUpdateInfo.isRecommended] tells
     * whether the update satisfies the configured [InAppUpdateConfig] and is allowed by Play.
     */
    suspend fun check(): Answer<InAppUpdateInfo?, Failure>

    /**
     * Starts the immediate update flow on the attached activity, regardless of the configured
     * policy (calling it is an explicit decision). Success only describes the outcome in
     * [InAppUpdateLaunchResult]; the user's decision arrives through [state].
     */
    suspend fun launch(): Answer<InAppUpdateLaunchResult, Failure>
}

/**
 * Rule deciding when an update is worth offering. Both conditions must hold.
 *
 * @property minPriority minimum `updatePriority` (0..5) set when releasing in Play Console.
 * @property minStalenessDays minimum days since the update became available to this device.
 */
data class InAppUpdateConfig(
    val minPriority: Int = 0,
    val minStalenessDays: Int = 0,
)

/**
 * Snapshot of the update Play currently offers, without exposing Play types.
 *
 * @property availableVersionCode version code of the update published on Play.
 * @property priority `updatePriority` (0..5) assigned to the release in Play Console.
 * @property stalenessDays days since the update became available to this device; null when Play
 * does not report it.
 */
data class InAppUpdateInfo(
    val availableVersionCode: Int,
    val priority: Int,
    val stalenessDays: Int?,
    /** True when the configured [InAppUpdateConfig] accepts the update and Play allows it. */
    val isRecommended: Boolean,
)

/**
 * Outcome of asking Play to start the update flow. The user's decision inside the Play UI is
 * reported through [InAppUpdate.state], not here.
 */
sealed interface InAppUpdateLaunchResult {

    /** Play accepted the request and its update UI is on screen. */
    data object Started : InAppUpdateLaunchResult

    /** No update is pending for this device. */
    data object NotAvailable : InAppUpdateLaunchResult

    /** A Play update flow is already running (launched earlier or interrupted mid-way). */
    data object AlreadyInProgress : InAppUpdateLaunchResult

    /** Play does not allow an immediate update for this release. */
    data object NotAllowed : InAppUpdateLaunchResult
}

/** Update status exposed to the UI; refreshed on every resume of the attached activity. */
sealed interface InAppUpdateState {

    /** Nothing to report: no pending update, or one the configured policy does not recommend. */
    data object Idle : InAppUpdateState

    /** An update is pending and satisfies the configured [InAppUpdateConfig]. */
    data class Available(val info: InAppUpdateInfo) : InAppUpdateState

    /** A Play immediate update is currently running. */
    data object InProgress : InAppUpdateState

    /** The user left the Play flow; the next resume refresh reports it as available again. */
    data object Declined : InAppUpdateState

    /** The Play flow finished with an error result instead of completing the update. */
    data object Failed : InAppUpdateState
}

/** Debug builds are not installed from Play, so no update can ever be offered or launched. */
internal class DebugInAppUpdate : InAppUpdate {

    override val state: StateFlow<InAppUpdateState> =
        MutableStateFlow<InAppUpdateState>(InAppUpdateState.Idle).asStateFlow()

    override fun attach(activity: ComponentActivity) = Unit

    override suspend fun check(): Answer<InAppUpdateInfo?, Failure> = Answer.Success(null)

    override suspend fun launch(): Answer<InAppUpdateLaunchResult, Failure> =
        Answer.Success(InAppUpdateLaunchResult.NotAvailable)
}

internal class InAppUpdateImpl(
    private val manager: AppUpdateManager,
    private val tracker: InAppUpdateTracker,
    private val config: InAppUpdateConfig = InAppUpdateConfig(),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : InAppUpdate {

    private val mutableState = MutableStateFlow<InAppUpdateState>(InAppUpdateState.Idle)
    override val state = mutableState.asStateFlow()
    private val activityHolder = ActivityHolder()
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var launching = false

    override fun attach(activity: ComponentActivity) {
        if (activityHolder.activity === activity) return

        check(!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            "Attach InAppUpdate before STARTED"
        }
        launcher = registerInAppUpdateLauncher(activity)
        activityHolder.attach(activity)
        activity.lifecycle.addObserver(this)
    }

    private fun registerInAppUpdateLauncher(activity: ComponentActivity): ActivityResultLauncher<IntentSenderRequest> =
        activity.activityResultRegistry.register(
            LAUNCHER_KEY,
            activity,
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            launching = false
            when (result.resultCode) {
                Activity.RESULT_OK -> tracker.accepted()
                Activity.RESULT_CANCELED -> {
                    tracker.declined()
                    mutableState.value = InAppUpdateState.Declined
                }
                else -> {
                    tracker.flowFailed()
                    mutableState.value = InAppUpdateState.Failed
                }
            }
        }

    override fun onResume(owner: LifecycleOwner) {
        manager.appUpdateInfo
            .addOnSuccessListener { info -> refreshState(owner, info) }
            .addOnFailureListener { error -> logRefreshFailure(error) }
    }

    /**
     * Publishes the new state and, when Play reports an update interrupted mid-flow (e.g. the
     * user left the app during the download), relaunches it so the flow resumes where it
     * stopped instead of stalling in [InAppUpdateState.InProgress].
     */
    private fun refreshState(owner: LifecycleOwner, info: AppUpdateInfo) {
        if (activityHolder.activity !== owner) return
        mutableState.value = info.toState()
        if (info.isInProgress() && !launching && owner.isResumed()) resumeInProgressUpdate(info)
    }

    private fun resumeInProgressUpdate(info: AppUpdateInfo) {
        runCatching {
            launching = startFlow(info)
            if (launching) tracker.resumed()
        }.onFailure { Timber.w(it, "Could not resume the in-progress update") }
    }

    /**
     * ERROR_APP_NOT_OWNED means the app was sideloaded or built locally rather than installed
     * from Play — an expected situation, so it is logged at debug level instead of warning.
     */
    private fun logRefreshFailure(error: Exception) {
        if ((error as? InstallException)?.errorCode == InstallErrorCode.ERROR_APP_NOT_OWNED) {
            Timber.d("In-app update unavailable: app not installed from Play")
        } else {
            Timber.w(error, "Could not refresh in-app update status")
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (activityHolder.activity === owner) detach()
    }

    override suspend fun check(): Answer<InAppUpdateInfo?, Failure> =
        coRunCatching {
            val info = manager.appUpdateInfo.await()
            mutableState.value = info.toState()
            tracker.checked(
                available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
                versionCode = info.availableVersionCode().toLong(),
                priority = info.updatePriority().toLong(),
                stalenessDays = info.clientVersionStalenessDays()?.toLong(),
            )
            info.takeIf { it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE }
                ?.toInfo()
        }.thenFailure {
            Failure.Unknown("In-app update status could not be checked: ${it.message}")
        }.onFailure {
            tracker.failed(InAppUpdateTracker.Reason.ERROR)
        }

    override suspend fun launch(): Answer<InAppUpdateLaunchResult, Failure> =
        withContext(mainDispatcher) {
            val activity = activityHolder.activity
                ?: return@withContext Answer.Failure(
                    Failure.ResourceNotFound("No activity attached")
                ).also { tracker.failed(InAppUpdateTracker.Reason.NO_ACTIVITY) }
            coRunCatching { start(activity) }
                .thenFailure { Failure.Unknown("In-app update could not be launched: ${it.message}") }
                .onSuccess { tracker.launched(it) }
                .onFailure { tracker.failed(InAppUpdateTracker.Reason.ERROR) }
        }

    /**
     * Serializes launch attempts through [launching]: the flag stays set while Play's UI is up
     * and is cleared by the activity-result callback; when the flow never starts, `finally`
     * resets it so a later call can try again.
     */
    private suspend fun start(activity: ComponentActivity): InAppUpdateLaunchResult {
        check(activity.isResumed()) { "Activity is not resumed" }
        if (launching) return InAppUpdateLaunchResult.AlreadyInProgress
        launching = true
        var started = false
        try {
            val info = manager.appUpdateInfo.await()
            check(activityHolder.activity === activity && activity.isResumed()) {
                "Activity changed while checking for updates"
            }
            mutableState.value = info.toState()
            return resolveLaunchResult(info).also { started = it is InAppUpdateLaunchResult.Started }
        } finally {
            launching = started
        }
    }

    /**
     * `startUpdateFlowForResult` returning false means Play refused to launch the flow, so the
     * fall-through maps to [InAppUpdateLaunchResult.NotAllowed] rather than a failure.
     */
    private fun resolveLaunchResult(info: AppUpdateInfo): InAppUpdateLaunchResult = when {
        info.isInProgress() -> InAppUpdateLaunchResult.AlreadyInProgress
        info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE ->
            InAppUpdateLaunchResult.NotAvailable

        !info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> InAppUpdateLaunchResult.NotAllowed
        startFlow(info) -> InAppUpdateLaunchResult.Started
        else -> InAppUpdateLaunchResult.NotAllowed
    }

    private fun startFlow(info: AppUpdateInfo): Boolean =
        manager.startUpdateFlowForResult(
            info,
            checkNotNull(launcher) { "InAppUpdate not attached" },
            buildImmediateUpdateOptions(),
        )

    private fun buildImmediateUpdateOptions(): AppUpdateOptions =
        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()

    private fun detach() {
        activityHolder.activity?.let { activity ->
            activity.lifecycle.removeObserver(this)
            activityHolder.detach(activity)
        }
        launcher?.unregister()
        launcher = null
        launching = false
    }

    /**
     * Maps Play's status to UI state: in-progress trumps everything, and a pending update only
     * surfaces as [InAppUpdateState.Available] when the configured policy recommends it.
     */
    private fun AppUpdateInfo.toState(): InAppUpdateState = when {
        isInProgress() -> InAppUpdateState.InProgress
        updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE -> InAppUpdateState.Idle
        else -> toInfo().takeIf { it.isRecommended }
            ?.let { InAppUpdateState.Available(it) }
            ?: InAppUpdateState.Idle
    }

    private fun AppUpdateInfo.toInfo() = InAppUpdateInfo(
        availableVersionCode = availableVersionCode(),
        priority = updatePriority(),
        stalenessDays = clientVersionStalenessDays(),
        isRecommended = isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
                updatePriority() >= config.minPriority &&
                (clientVersionStalenessDays() ?: 0) >= config.minStalenessDays,
    )

    private fun LifecycleOwner.isResumed() =
        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    private fun AppUpdateInfo.isInProgress() =
        updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

    private companion object {
        const val LAUNCHER_KEY = "platform.device.inAppUpdate"
    }
}
