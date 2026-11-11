package com.raxdenstudios.platform.device.inapp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.raxdenstudios.commons.android.ActivityHolder
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.onFailure
import com.raxdenstudios.commons.core.ext.onSuccess
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.core.util.IntentFactoryImpl
import com.raxdenstudios.platform.core.util.IntentType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * In-app share through the system share sheet.
 *
 * Android has no Play-style "in-app share" API, so this helper wraps the system chooser and
 * reports which app the user picked, which a plain `ACTION_SEND` intent cannot do. The system
 * never tells whether the content was finally sent from the target app, so
 * [InAppShareResult.TargetChosen] only means the user picked a destination.
 *
 * ## Setup
 *
 * Bind the running activities once, e.g. in `Application.ActivityLifecycleCallbacks`:
 *
 * ```kotlin
 * override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
 *     if (activity !is ComponentActivity) return
 *     inAppShare.attach(activity)   // must run before the activity is STARTED
 * }
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * safeLaunch {
 *     inAppShare.launch(InAppShareContent(subject = "RadioSquare", text = "Try it: $url"))
 *         .onSuccess { result ->
 *             if (result is InAppShareResult.TargetChosen) markAsShared()
 *         }
 * }
 * ```
 *
 * [launch] first shows a native modal asking the user whether they want to share the app and
 * only opens the system share sheet if they accept; [InAppShareResult.Declined] means they
 * rejected or dismissed that modal. [share] skips the modal and opens the system share sheet
 * directly.
 */
interface InAppShare : DefaultLifecycleObserver {

    /**
     * Binds this helper to [activity]. Call in `Activity.onCreate` before STARTED, including
     * after recreation. The activity is held weakly and released when it is destroyed.
     */
    @MainThread
    fun attach(activity: ComponentActivity)

    /**
     * Shows a native modal asking the user whether they want to share the app and, only if they
     * accept, the system share sheet. Suspends until both are closed.
     *
     * [Answer.Success] with [InAppShareResult.Declined] means the user rejected or dismissed the
     * modal without opening the share sheet. [Answer.Failure] means the modal or the sheet could
     * not be shown (no activity attached, a share already in progress or the system rejected the
     * intent).
     */
    suspend fun launch(content: InAppShareContent): Answer<InAppShareResult, Failure>

    /**
     * Shows the system share sheet directly on the attached activity, skipping the modal shown
     * by [launch], and suspends until it is closed.
     *
     * [Answer.Failure] means the sheet could not be shown (no activity attached, a share already
     * in progress or the system rejected the intent).
     */
    suspend fun share(content: InAppShareContent): Answer<InAppShareResult, Failure>
}

/**
 * @property subject subject for targets that support it (e.g. email).
 * @property text plain text to share, typically including the Play Store link.
 * @property chooserTitle optional title of the share sheet.
 */
data class InAppShareContent(val subject: String, val text: String, val chooserTitle: String? = null)

/**
 * How the share sheet ended. The system never confirms the content was actually sent by the
 * chosen app, so [TargetChosen] only reports the destination the user picked.
 */
sealed interface InAppShareResult {

    /** The user picked a destination app. [packageName] is null when the system did not report it. */
    data class TargetChosen(val packageName: String?) : InAppShareResult

    /** The user closed the share sheet without picking anything. */
    data object Dismissed : InAppShareResult

    /** The user rejected or dismissed the modal asking to share, before the share sheet opened. */
    data object Declined : InAppShareResult
}

internal class InAppShareImpl(
    context: Context,
    private val tracker: InAppShareTracker,
    private val prompt: InAppSharePrompt,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : InAppShare {

    private val appContext = context.applicationContext
    private val intentFactory = IntentFactoryImpl(appContext)
    private val chosenAction = "${appContext.packageName}.$CHOSEN_ACTION_SUFFIX"
    private val activityHolder = ActivityHolder()
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var pending: PendingShare? = null

    override fun attach(activity: ComponentActivity) {
        if (activityHolder.activity === activity) return

        // A new activity replaces the previous one: release its launcher and close any
        // share still in flight before binding the new instance.
        detach()
        activityHolder.attach(activity)
        launcher = registerInAppShareLauncher(activity)
        activity.lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (activityHolder.activity === owner) detach()
    }

    override suspend fun launch(content: InAppShareContent): Answer<InAppShareResult, Failure> =
        withContext(mainDispatcher) {
            val activity = activityHolder.activity
            val currentLauncher = launcher
            when {
                activity == null || currentLauncher == null -> noActivityFailure()
                pending != null -> alreadyInProgressFailure()
                else -> launchWithPrompt(activity, currentLauncher, content)
            }
        }

    override suspend fun share(content: InAppShareContent): Answer<InAppShareResult, Failure> =
        withContext(mainDispatcher) {
            val currentLauncher = launcher
            when {
                activityHolder.activity == null || currentLauncher == null -> noActivityFailure()
                pending != null -> alreadyInProgressFailure()
                else -> shareWithTracking(currentLauncher, content)
            }
        }

    /**
     * Marks a share in progress for the whole modal + sheet flow so a concurrent [launch] or
     * [share] call is rejected while the modal is still visible, same as while the sheet is open.
     */
    private suspend fun launchWithPrompt(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<Intent>,
        content: InAppShareContent,
    ): Answer<InAppShareResult, Failure> {
        pending = PendingShare()
        return try {
            tracker.promptShown()
            val prompted = coRunCatching { prompt.show(activity) }
                .thenFailure { Failure.Unknown("In-app share prompt could not be shown: ${it.message}") }
                .onFailure { tracker.failed(InAppShareTracker.Reason.ERROR) }
            when (prompted) {
                is Answer.Failure -> prompted

                is Answer.Success -> if (prompted.value) {
                    tracker.promptAccepted()
                    pending = null
                    shareWithTracking(launcher, content)
                } else {
                    tracker.promptDeclined()
                    Answer.Success(InAppShareResult.Declined)
                }
            }
        } finally {
            pending = null
        }
    }

    private fun noActivityFailure(): Answer<InAppShareResult, Failure> {
        tracker.failed(InAppShareTracker.Reason.NO_ACTIVITY)
        return Answer.Failure(Failure.ResourceNotFound("No activity attached"))
    }

    private fun alreadyInProgressFailure(): Answer<InAppShareResult, Failure> {
        tracker.failed(InAppShareTracker.Reason.ALREADY_IN_PROGRESS)
        return Answer.Failure(Failure.Unknown("A share is already in progress"))
    }

    private suspend fun shareWithTracking(
        launcher: ActivityResultLauncher<Intent>,
        content: InAppShareContent,
    ): Answer<InAppShareResult, Failure> = coRunCatching { share(launcher, content) }
        .thenFailure { Failure.Unknown("In-app share could not be launched: ${it.message}") }
        .onSuccess(::trackShareResult)
        .onFailure { tracker.failed(InAppShareTracker.Reason.ERROR) }

    private fun trackShareResult(result: InAppShareResult) {
        when (result) {
            is InAppShareResult.TargetChosen -> tracker.targetChosen(result.packageName)

            InAppShareResult.Dismissed -> tracker.dismissed()

            // share() (invoked by shareWithTracking) never returns Declined: that result is only
            // produced by launch()'s prompt step, before the sheet is shown.
            InAppShareResult.Declined -> Unit
        }
    }

    private suspend fun share(launcher: ActivityResultLauncher<Intent>, content: InAppShareContent): InAppShareResult {
        val share = PendingShare().also { pending = it }
        val receiver = registerChosenTargetReceiver(share)
        try {
            launcher.launch(buildChooser(content))
            return share.awaitResult()
        } finally {
            appContext.unregisterReceiver(receiver)
            pending = null
        }
    }

    private fun registerInAppShareLauncher(activity: ComponentActivity): ActivityResultLauncher<Intent> =
        activity.activityResultRegistry.register(
            LAUNCHER_KEY,
            activity,
            ActivityResultContracts.StartActivityForResult(),
        ) { pending?.markClosed() }

    /**
     * Each launch registers a fresh receiver bound to its own [PendingShare]; NOT_EXPORTED so
     * only the system chooser can deliver the broadcast. It is unregistered when the sheet
     * closes.
     */
    private fun registerChosenTargetReceiver(share: PendingShare): BroadcastReceiver =
        ChosenTargetReceiver(share).also {
            ContextCompat.registerReceiver(
                appContext,
                it,
                IntentFilter(chosenAction),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }

    private fun buildChooser(content: InAppShareContent): Intent {
        val shareIntent = intentFactory.build(IntentType.ShareText(content.subject, content.text))
        return Intent.createChooser(shareIntent, content.chooserTitle, buildChosenTargetSender())
    }

    /**
     * The chooser reports the picked target by filling [Intent.EXTRA_CHOSEN_COMPONENT] into this
     * broadcast's extras before sending it — that is why the pending intent must be mutable.
     */
    private fun buildChosenTargetSender(): IntentSender = PendingIntent.getBroadcast(
        appContext,
        0,
        Intent(chosenAction).setPackage(appContext.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    ).intentSender

    private fun detach() {
        activityHolder.activity?.let { activity ->
            activity.lifecycle.removeObserver(this)
            activityHolder.detach(activity)
        }
        launcher?.unregister()
        launcher = null
        pending?.markClosed()
    }

    /**
     * Correlates the two signals that end a share: the activity-result callback when the sheet
     * closes and the chosen-component broadcast, which can arrive afterwards. [awaitResult]
     * waits a short grace period after the close before falling back to
     * [InAppShareResult.Dismissed].
     */
    private class PendingShare {

        private val closed = CompletableDeferred<Unit>()
        private val chosen = CompletableDeferred<InAppShareResult.TargetChosen>()

        fun markClosed() {
            closed.complete(Unit)
        }

        fun markChosen(packageName: String?) {
            chosen.complete(InAppShareResult.TargetChosen(packageName))
        }

        suspend fun awaitResult(): InAppShareResult {
            closed.await()
            // The chosen-component broadcast can land slightly after the sheet closes.
            return withTimeoutOrNull(CHOSEN_GRACE_MS.milliseconds) { chosen.await() } ?: InAppShareResult.Dismissed
        }
    }

    /** Delivers the chooser's [Intent.EXTRA_CHOSEN_COMPONENT] broadcast to the in-flight share. */
    private class ChosenTargetReceiver(private val share: PendingShare) : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            share.markChosen(intent.chosenComponent()?.packageName)
        }

        private fun Intent.chosenComponent(): ComponentName? =
            // The typed getParcelableExtra overload requires API 33; the deprecated call works
            // on every supported level.
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT)
    }

    private companion object {
        const val LAUNCHER_KEY = "platform.device.inAppShare"
        const val CHOSEN_ACTION_SUFFIX = "platform.device.inAppShare.CHOSEN"
        const val CHOSEN_GRACE_MS = 500L
    }
}
