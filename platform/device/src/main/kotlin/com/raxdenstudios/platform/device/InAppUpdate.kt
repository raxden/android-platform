package com.raxdenstudios.platform.device

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

data class InAppUpdateInfo(
    val availableVersionCode: Int,
    val priority: Int,
    val stalenessDays: Int?,
    /** True when the configured [InAppUpdateConfig] accepts the update and Play allows it. */
    val isRecommended: Boolean,
)

sealed interface InAppUpdateLaunchResult {
    data object Started : InAppUpdateLaunchResult
    data object NotAvailable : InAppUpdateLaunchResult
    data object AlreadyInProgress : InAppUpdateLaunchResult

    /** Play does not allow an immediate update for this release. */
    data object NotAllowed : InAppUpdateLaunchResult
}

sealed interface InAppUpdateState {
    data object Idle : InAppUpdateState

    /** An update is pending and satisfies the configured [InAppUpdateConfig]. */
    data class Available(val info: InAppUpdateInfo) : InAppUpdateState
    data object InProgress : InAppUpdateState

    /** The user left the Play flow; the next resume refresh reports it as available again. */
    data object Declined : InAppUpdateState
    data object Failed : InAppUpdateState
}

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
                Activity.RESULT_OK -> Unit
                Activity.RESULT_CANCELED -> mutableState.value = InAppUpdateState.Declined
                else -> mutableState.value = InAppUpdateState.Failed
            }
        }

    override fun onResume(owner: LifecycleOwner) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (activityHolder.activity !== owner) return@addOnSuccessListener
            mutableState.value = info.toState()
            if (info.isInProgress() && !launching &&
                owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) {
                runCatching { launching = startFlow(info) }
                    .onFailure { Timber.w(it, "Could not resume the in-progress update") }
            }
        }.addOnFailureListener { error ->
            if ((error as? InstallException)?.errorCode == InstallErrorCode.ERROR_APP_NOT_OWNED) {
                Timber.d("In-app update unavailable: app not installed from Play")
            } else {
                Timber.w(error, "Could not refresh in-app update status")
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (activityHolder.activity === owner) detach()
    }

    override suspend fun check(): Answer<InAppUpdateInfo?, Failure> =
        coRunCatching {
            val info = manager.appUpdateInfo.await()
            mutableState.value = info.toState()
            info.takeIf { it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE }
                ?.toInfo()
        }.thenFailure {
            Failure.Unknown("In-app update status could not be checked: ${it.message}")
        }

    override suspend fun launch(): Answer<InAppUpdateLaunchResult, Failure> =
        withContext(mainDispatcher) {
            val activity = activityHolder.activity
                ?: return@withContext Answer.Failure(
                    Failure.ResourceNotFound("No activity attached")
                )
            coRunCatching { start(activity) }.thenFailure {
                Failure.Unknown("In-app update could not be launched: ${it.message}")
            }
        }

    private suspend fun start(activity: ComponentActivity): InAppUpdateLaunchResult {
        check(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            "Activity is not resumed"
        }
        if (launching) return InAppUpdateLaunchResult.AlreadyInProgress
        launching = true
        var started = false
        try {
            val info = manager.appUpdateInfo.await()
            check(
                activityHolder.activity === activity &&
                        activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) { "Activity changed while checking for updates" }
            mutableState.value = info.toState()

            val result = when {
                info.isInProgress() -> InAppUpdateLaunchResult.AlreadyInProgress
                info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE ->
                    InAppUpdateLaunchResult.NotAvailable

                !info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                    InAppUpdateLaunchResult.NotAllowed

                startFlow(info) -> InAppUpdateLaunchResult.Started
                else -> InAppUpdateLaunchResult.NotAllowed
            }
            started = result is InAppUpdateLaunchResult.Started
            return result
        } finally {
            launching = started
        }
    }

    private fun startFlow(info: AppUpdateInfo): Boolean =
        manager.startUpdateFlowForResult(
            info,
            checkNotNull(launcher) { "InAppUpdate not attached" },
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
        )

    private fun detach() {
        activityHolder.activity?.let { activity ->
            activity.lifecycle.removeObserver(this)
            activityHolder.detach(activity)
        }
        launcher?.unregister()
        launcher = null
        launching = false
    }

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

    private fun AppUpdateInfo.isInProgress() =
        updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

    private companion object {
        const val LAUNCHER_KEY = "platform.device.inAppUpdate"
    }
}
