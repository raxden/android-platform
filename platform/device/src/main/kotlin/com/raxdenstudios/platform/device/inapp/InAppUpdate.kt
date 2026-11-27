package com.raxdenstudios.platform.device.inapp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.raxdenstudios.commons.android.ActivityHolder
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.onFailure
import com.raxdenstudios.commons.core.ext.onSuccess
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.data.ext.awaitUnitAnswer
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
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
 * Google Play in-app updates, immediate or flexible.
 *
 * - [InAppUpdateType.IMMEDIATE]: Play shows its own full-screen UI, downloads and installs the
 *   update and restarts the app. If the user leaves the flow midway, it is resumed automatically
 *   the next time the attached activity resumes.
 * - [InAppUpdateType.FLEXIBLE]: the update downloads in the background while the app is used;
 *   [state] reports [InAppUpdateState.Downloading] and then [InAppUpdateState.Downloaded], at
 *   which point the app calls [completeUpdate] to install and restart.
 *
 * When Play cannot run the flow (app not installed from Play, flow not allowed), [openStoreListing]
 * sends the user to the store listing instead.
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
 * Optionally restrict when [check] and [state] treat an update as worth offering (default: any update Play
 * allows for an immediate flow):
 *
 * ```kotlin
 * single { InAppUpdateConfig(minPriority = 4, minStalenessDays = 0) }
 * ```
 *
 * ## Usage
 *
 * The app decides which flow to launch, e.g. from `GetAppUpdateRequirementUseCase`. [launch] does not check the
 * configured policy; calling it is an explicit decision.
 *
 * ```kotlin
 * // Required: Play's full-screen flow; it also takes over an update already in progress (e.g. after a
 * // process restart). If Play cannot run it or the user declines (state is Declined), show the app's own
 * // blocking screen. AlreadyInProgress means a flexible update is already downloaded: call completeUpdate().
 * safeLaunch {
 *     inAppUpdate.launch(InAppUpdateType.IMMEDIATE).onSuccess { result ->
 *         if (result == InAppUpdateLaunchResult.NotAvailable || result == InAppUpdateLaunchResult.NotAllowed) {
 *             showBlockingUpdateScreen(onUpdate = { inAppUpdate.openStoreListing() })
 *         }
 *     }
 * }
 *
 * // Optional: download in the background and install once it is ready.
 * safeLaunch { inAppUpdate.launch(InAppUpdateType.FLEXIBLE) }
 * inAppUpdate.state.collect { state ->
 *     if (state is InAppUpdateState.Downloaded) showRestartPrompt { safeLaunch { inAppUpdate.completeUpdate() } }
 * }
 * ```
 */
interface InAppUpdate : DefaultLifecycleObserver {

    /**
     * Current update state. Refreshed on every resume of the attached activity and by [check] and [launch].
     * Terminal outcomes such as [InAppUpdateState.Declined] are replaced by the next refresh.
     */
    val state: StateFlow<InAppUpdateState>

    /**
     * Binds this helper to [activity]. Call in `Activity.onCreate` before STARTED, including
     * after recreation. The activity is held weakly and released when it is destroyed.
     */
    @MainThread
    fun attach(
        activity: ComponentActivity,
    )

    /**
     * Queries Play for a pending update without showing any UI, and refreshes [state] with the
     * result. Fails when the Play query fails.
     *
     * Success(null) means there is no update available. [InAppUpdateInfo.isRecommended] tells
     * whether the update satisfies the configured [InAppUpdateConfig] and is allowed by Play.
     */
    suspend fun check(): Answer<InAppUpdateInfo?, Failure>

    /**
     * Starts the [type] update flow on the attached activity, regardless of the configured
     * policy (calling it is an explicit decision). The attached activity must be RESUMED; otherwise
     * this returns a failure. Success only describes the outcome in [InAppUpdateLaunchResult]
     * (e.g. `NotAvailable` or `NotAllowed` mean Play did not start the flow); the user's decision
     * arrives through [state].
     */
    suspend fun launch(
        type: InAppUpdateType = InAppUpdateType.IMMEDIATE,
    ): Answer<InAppUpdateLaunchResult, Failure>

    /**
     * Installs a flexible update once [state] is [InAppUpdateState.Downloaded]. Play restarts the
     * app, so success is rarely observed. Fails when Play rejects the install request.
     */
    suspend fun completeUpdate(): Answer<Unit, Failure>

    /**
     * Opens the app's store listing from the attached activity — the fallback when Play cannot run
     * the in-app flow. Falls back to the web listing when the Play Store app is missing. Fails when
     * no activity is attached.
     */
    @MainThread
    fun openStoreListing(): Answer<Unit, Failure>
}

/** Kind of Play in-app update flow to launch. */
enum class InAppUpdateType {

    /** Full-screen flow that blocks the app until the update is installed. */
    IMMEDIATE,

    /** Background download; the app installs it later with [InAppUpdate.completeUpdate]. */
    FLEXIBLE,
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

    /**
     * Play accepted the request: its update UI is on screen for [InAppUpdateType.IMMEDIATE], or the
     * download has started in the background for [InAppUpdateType.FLEXIBLE].
     */
    data object Started : InAppUpdateLaunchResult

    /** No update is pending for this device. */
    data object NotAvailable : InAppUpdateLaunchResult

    /** A Play update is already running or already downloaded, so no new flow was started. */
    data object AlreadyInProgress : InAppUpdateLaunchResult

    /** Play does not allow the requested [InAppUpdateType] for this release, or refused to start the flow. */
    data object NotAllowed : InAppUpdateLaunchResult
}

/** Update status exposed to the UI; refreshed on every resume of the attached activity. */
sealed interface InAppUpdateState {

    /** Nothing to report: no pending update, or one the configured policy does not recommend. */
    data object Idle : InAppUpdateState

    /** An update is pending and satisfies the configured [InAppUpdateConfig]. */
    data class Available(
        val info: InAppUpdateInfo,
    ) : InAppUpdateState

    /** A Play immediate update is currently running. */
    data object InProgress : InAppUpdateState

    /**
     * A flexible update is downloading in the background.
     *
     * @property bytesDownloaded bytes downloaded so far.
     * @property totalBytes full size of the update as reported by Play.
     */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : InAppUpdateState

    /** A flexible update finished downloading; call [InAppUpdate.completeUpdate] to install it. */
    data object Downloaded : InAppUpdateState

    /**
     * The user cancelled Play's flow. The next refresh replaces this with the state Play reports,
     * usually [Available] again.
     */
    data object Declined : InAppUpdateState

    /** The update ended in error: Play's flow returned an error result, or a flexible download failed. */
    data object Failed : InAppUpdateState
}

/**
 * Debug builds are not installed from Play, so no update can ever be offered or launched; only
 * [openStoreListing] works, so a consumer's blocking update screen can be exercised.
 */
internal class DebugInAppUpdate : InAppUpdate {

    override val state: StateFlow<InAppUpdateState> =
        MutableStateFlow<InAppUpdateState>(InAppUpdateState.Idle).asStateFlow()
    private val activityHolder = ActivityHolder()

    override fun attach(
        activity: ComponentActivity,
    ) {
        activityHolder.attach(activity)
    }

    override suspend fun check(): Answer<InAppUpdateInfo?, Failure> = Answer.Success(null)

    override suspend fun launch(
        type: InAppUpdateType,
    ): Answer<InAppUpdateLaunchResult, Failure> = Answer.Success(InAppUpdateLaunchResult.NotAvailable)

    override suspend fun completeUpdate(): Answer<Unit, Failure> = Answer.Success(Unit)

    override fun openStoreListing(): Answer<Unit, Failure> = activityHolder.activity
        ?.let { activity -> openStoreListing(activity) }
        ?: Answer.Failure(Failure.ResourceNotFound("No activity attached"))
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
    private var launchedType: InAppUpdateType? = null
    private val installListener = InstallStateUpdatedListener { installState -> onInstallStateUpdated(installState) }

    override fun attach(
        activity: ComponentActivity,
    ) {
        if (activityHolder.activity === activity) return

        check(!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            "Attach InAppUpdate before STARTED"
        }
        launcher = registerInAppUpdateLauncher(activity)
        activityHolder.attach(activity)
        activity.lifecycle.addObserver(this)
        manager.registerListener(installListener)
    }

    /** Mirrors the progress of a flexible download into [state]. */
    private fun onInstallStateUpdated(
        installState: InstallState,
    ) {
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADING -> mutableState.value = InAppUpdateState.Downloading(
                bytesDownloaded = installState.bytesDownloaded(),
                totalBytes = installState.totalBytesToDownload(),
            )

            InstallStatus.DOWNLOADED -> {
                tracker.downloaded()
                mutableState.value = InAppUpdateState.Downloaded
            }

            InstallStatus.FAILED -> {
                launchedType = null
                tracker.flowFailed()
                mutableState.value = InAppUpdateState.Failed
            }

            InstallStatus.CANCELED, InstallStatus.INSTALLED -> {
                launchedType = null
                mutableState.value = InAppUpdateState.Idle
            }

            else -> Unit
        }
    }

    private fun registerInAppUpdateLauncher(
        activity: ComponentActivity,
    ): ActivityResultLauncher<IntentSenderRequest> = activity.activityResultRegistry.register(
        LAUNCHER_KEY,
        activity,
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        launching = false
        when (result.resultCode) {
            Activity.RESULT_OK -> tracker.accepted()

            Activity.RESULT_CANCELED -> {
                launchedType = null
                tracker.declined()
                mutableState.value = InAppUpdateState.Declined
            }

            else -> {
                tracker.flowFailed()
                mutableState.value = InAppUpdateState.Failed
            }
        }
    }

    override fun onResume(
        owner: LifecycleOwner,
    ) {
        manager.appUpdateInfo
            .addOnSuccessListener { info -> refreshState(owner, info) }
            .addOnFailureListener { error -> logRefreshFailure(error) }
    }

    /**
     * Publishes the new state and, when Play reports an immediate update interrupted mid-flow
     * (e.g. the user left the app during the download), relaunches it so the flow resumes where
     * it stopped instead of stalling in [InAppUpdateState.InProgress]. A flexible download keeps
     * running in the background, so it is never turned into an immediate flow.
     */
    private fun refreshState(
        owner: LifecycleOwner,
        info: AppUpdateInfo,
    ) {
        if (activityHolder.activity !== owner) return
        mutableState.value = info.toState()
        if (info.needsImmediateResume(launchedType) && !launching && owner.isResumed()) resumeInProgressUpdate(info)
    }

    private fun resumeInProgressUpdate(
        info: AppUpdateInfo,
    ) {
        runCatching {
            launching = startFlow(info, InAppUpdateType.IMMEDIATE)
            if (launching) tracker.resumed()
        }.onFailure { Timber.w(it, "Could not resume the in-progress update") }
    }

    override fun onDestroy(
        owner: LifecycleOwner,
    ) {
        if (activityHolder.activity === owner) detach()
    }

    override suspend fun check(): Answer<InAppUpdateInfo?, Failure> = coRunCatching {
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

    override suspend fun launch(
        type: InAppUpdateType,
    ): Answer<InAppUpdateLaunchResult, Failure> = withContext(mainDispatcher) {
        val activity = activityHolder.activity
            ?: return@withContext Answer.Failure(
                Failure.ResourceNotFound("No activity attached"),
            ).also { tracker.failed(InAppUpdateTracker.Reason.NO_ACTIVITY) }
        coRunCatching { start(activity, type) }
            .thenFailure { Failure.Unknown("In-app update could not be launched: ${it.message}") }
            .onSuccess { tracker.launched(type, it) }
            .onFailure { tracker.failed(InAppUpdateTracker.Reason.ERROR) }
    }

    override suspend fun completeUpdate(): Answer<Unit, Failure> = manager.completeUpdate()
        .awaitUnitAnswer(errorMessage = "In-app update could not be completed")
        .onSuccess { tracker.completed() }
        .onFailure { tracker.failed(InAppUpdateTracker.Reason.ERROR) }

    override fun openStoreListing(): Answer<Unit, Failure> {
        val activity = activityHolder.activity
            ?: return Answer.Failure(Failure.ResourceNotFound("No activity attached"))
        return openStoreListing(activity).onSuccess { tracker.storeOpened() }
    }

    /**
     * Serializes launch attempts through [launching]: the flag stays set while Play's UI is up
     * and is cleared by the activity-result callback; when the flow never starts, `finally`
     * resets it so a later call can try again.
     */
    private suspend fun start(
        activity: ComponentActivity,
        type: InAppUpdateType,
    ): InAppUpdateLaunchResult {
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
            return resolveLaunchResult(info, type).also { started = it is InAppUpdateLaunchResult.Started }
        } finally {
            launching = started
        }
    }

    /**
     * `startUpdateFlowForResult` returning false means Play refused to launch the flow, so the
     * fall-through maps to [InAppUpdateLaunchResult.NotAllowed] rather than a failure.
     */
    private fun resolveLaunchResult(
        info: AppUpdateInfo,
        type: InAppUpdateType,
    ): InAppUpdateLaunchResult = when {
        info.isDownloaded() -> InAppUpdateLaunchResult.AlreadyInProgress

        info.isInProgress() && type == InAppUpdateType.IMMEDIATE -> resumeAsImmediate(info)

        info.isInProgress() -> InAppUpdateLaunchResult.AlreadyInProgress

        info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE ->
            InAppUpdateLaunchResult.NotAvailable

        !info.isUpdateTypeAllowed(type.toPlayType()) -> InAppUpdateLaunchResult.NotAllowed

        startFlow(info, type) -> InAppUpdateLaunchResult.Started

        else -> InAppUpdateLaunchResult.NotAllowed
    }

    /** An update already in progress (e.g. started before a process restart) is taken over by the immediate flow. */
    private fun resumeAsImmediate(
        info: AppUpdateInfo,
    ): InAppUpdateLaunchResult = when {
        startFlow(info, InAppUpdateType.IMMEDIATE) -> InAppUpdateLaunchResult.Started
        else -> InAppUpdateLaunchResult.NotAllowed
    }

    private fun startFlow(
        info: AppUpdateInfo,
        type: InAppUpdateType,
    ): Boolean = manager.startUpdateFlowForResult(
        info,
        checkNotNull(launcher) { "InAppUpdate not attached" },
        buildUpdateOptions(type),
    ).also { started -> if (started) launchedType = type }

    private fun detach() {
        activityHolder.activity?.let { activity ->
            activity.lifecycle.removeObserver(this)
            activityHolder.detach(activity)
        }
        manager.unregisterListener(installListener)
        launcher?.unregister()
        launcher = null
        launching = false
    }

    /**
     * Maps Play's status to UI state: a downloaded flexible update trumps everything, then an
     * update in progress, and a pending update only surfaces as [InAppUpdateState.Available] when
     * the configured policy recommends it.
     */
    private fun AppUpdateInfo.toState(): InAppUpdateState = when {
        isDownloaded() -> InAppUpdateState.Downloaded

        isInProgress() && launchedType != InAppUpdateType.IMMEDIATE && isDownloading() -> InAppUpdateState.Downloading(
            bytesDownloaded = bytesDownloaded(),
            totalBytes = totalBytesToDownload(),
        )

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
        isRecommended = (isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) || isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) &&
            updatePriority() >= config.minPriority &&
            (clientVersionStalenessDays() ?: 0) >= config.minStalenessDays,
    )

    private companion object {
        const val LAUNCHER_KEY = "platform.device.inAppUpdate"
    }
}

private const val STORE_APP_URI = "market://details?id="
private const val STORE_WEB_URI = "https://play.google.com/store/apps/details?id="

/**
 * ERROR_APP_NOT_OWNED means the app was sideloaded or built locally rather than installed
 * from Play — an expected situation, so it is logged at debug level instead of warning.
 */
private fun logRefreshFailure(
    error: Exception,
) {
    if ((error as? InstallException)?.errorCode == InstallErrorCode.ERROR_APP_NOT_OWNED) {
        Timber.d("In-app update unavailable: app not installed from Play")
    } else {
        Timber.w(error, "Could not refresh in-app update status")
    }
}

private fun openStoreListing(
    activity: Activity,
): Answer<Unit, Failure> = runCatching { startStoreListing(activity) }
    .fold(
        onSuccess = { Answer.Success(Unit) },
        onFailure = { error -> Answer.Failure(error.toUnknownError("Store listing could not be opened")) },
    )

private fun startStoreListing(
    activity: Activity,
) {
    try {
        activity.startActivity(buildStoreListingIntent(STORE_APP_URI, activity.packageName))
    } catch (_: ActivityNotFoundException) {
        activity.startActivity(buildStoreListingIntent(STORE_WEB_URI, activity.packageName))
    }
}

private fun buildStoreListingIntent(
    baseUri: String,
    packageName: String,
): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(baseUri + packageName))

private fun buildUpdateOptions(
    type: InAppUpdateType,
): AppUpdateOptions = AppUpdateOptions.newBuilder(type.toPlayType()).build()

private fun InAppUpdateType.toPlayType(): Int = when (this) {
    InAppUpdateType.IMMEDIATE -> AppUpdateType.IMMEDIATE
    InAppUpdateType.FLEXIBLE -> AppUpdateType.FLEXIBLE
}

private fun LifecycleOwner.isResumed() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

private fun AppUpdateInfo.isInProgress() =
    updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

private fun AppUpdateInfo.isDownloaded() = installStatus() == InstallStatus.DOWNLOADED

private fun AppUpdateInfo.isDownloading() = installStatus() == InstallStatus.DOWNLOADING

/**
 * Only an immediate flow this process launched is resumed automatically: after a process restart
 * the type of an in-progress update is unknown, and relaunching a flexible download as immediate
 * would be wrong. Consumers resume it explicitly with `launch(IMMEDIATE)` on app start.
 */
private fun AppUpdateInfo.needsImmediateResume(
    launchedType: InAppUpdateType?,
) = isInProgress() && !isDownloaded() && launchedType == InAppUpdateType.IMMEDIATE
