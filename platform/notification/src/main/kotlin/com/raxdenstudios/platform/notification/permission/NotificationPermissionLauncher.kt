package com.raxdenstudios.platform.notification.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/**
 * Reads the notification permission and shows the system dialog that grants it (`POST_NOTIFICATIONS`, Android 13+).
 * The host attaches every activity when it is created; they are kept as a stack and the dialog is shown on the most
 * recently attached activity still alive, so destroying one on top falls back to the one below.
 *
 * Unlike `PermissionsManager` (commons-permissions), [launch] shows the system dialog even when the user already
 * denied it once (the second and last chance Android gives), so the caller decides when to ask after its own
 * pre-explanation.
 */
interface NotificationPermissionLauncher {

    /** Registers [activity] to show the dialog; it is forgotten when destroyed. Call it before the activity starts. */
    fun attach(
        activity: ComponentActivity,
    )

    /** Whether notifications can be posted right now (permission granted and not turned off). */
    fun areNotificationsEnabled(): Boolean

    /** Whether posting needs the runtime permission, i.e. Android 13+. */
    fun isRuntimePermission(): Boolean

    /**
     * Whether `POST_NOTIFICATIONS` itself is granted (always true below Android 13). Notifications can still be off
     * with it granted: the user turned them off for the app in the system settings.
     */
    fun isPermissionGranted(): Boolean

    /** Whether the system would still show its dialog after a first denial (the "rationale" state). */
    fun shouldShowRationale(): Boolean

    /**
     * Shows the system dialog and suspends until the user answers. Returns whether it was shown: false without a
     * runtime permission, with the permission already granted or with no attached activity alive, in which case
     * nothing is shown. Also false when the dialog could not be launched, when a newer [launch] replaces this one, or
     * when the activity showing it is finished (not recreated) before the user answers. Read
     * [areNotificationsEnabled] for the answer. Must be called from the main thread.
     */
    suspend fun launch(): Boolean
}

/**
 * Every member runs on the main thread (activity callbacks and [launch]), so the stack needs no lock. At most one
 * dialog is pending: its continuation resumes with `true` once answered and with `false` when it can no longer be.
 */
internal class NotificationPermissionLauncherImpl(
    private val context: Context,
) : NotificationPermissionLauncher {

    private val attachedActivities = mutableListOf<AttachedActivity>()
    private var pendingRequest: PendingRequest? = null

    override fun attach(
        activity: ComponentActivity,
    ) {
        activity.lifecycle.addObserver(AttachedActivityObserver(activity))
    }

    override fun areNotificationsEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    override fun isRuntimePermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun isPermissionGranted(): Boolean = isRuntimePermission().not() ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    override fun shouldShowRationale(): Boolean = isRuntimePermission() &&
        topActivity()?.activity?.get()?.shouldShowNotificationRationale() == true

    override suspend fun launch(): Boolean {
        val attached = topActivity()
        // A granted permission shows no dialog (Android answers "granted" at once): nothing to report.
        if (!isRuntimePermission() || isPermissionGranted() || attached == null) return false
        return awaitPermissionAnswer(attached)
    }

    /** Resumes with whether the dialog was shown and answered. */
    private suspend fun awaitPermissionAnswer(
        attached: AttachedActivity,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        completePendingRequest(isAnswered = false)
        val request = PendingRequest(continuation = continuation, owner = attached.activity)
        pendingRequest = request
        continuation.invokeOnCancellation { if (pendingRequest === request) pendingRequest = null }
        launchPermissionDialog(attached.launcher)
    }

    private fun launchPermissionDialog(
        launcher: ActivityResultLauncher<String>,
    ) {
        runCatching { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            .onFailure { error ->
                Timber.w(error, "Unable to launch the notification permission dialog")
                completePendingRequest(isAnswered = false)
            }
    }

    private fun completePendingRequest(
        isAnswered: Boolean,
    ) {
        val request = pendingRequest ?: return
        pendingRequest = null
        if (request.continuation.isActive) request.continuation.resume(isAnswered)
    }

    private fun ComponentActivity.shouldShowNotificationRationale(): Boolean =
        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

    /** The most recently attached activity that is still alive. */
    private fun topActivity(): AttachedActivity? = attachedActivities.lastOrNull { attached ->
        attached.activity.get()?.isDestroyed == false
    }

    private fun register(
        activity: ComponentActivity,
    ) {
        val reference = WeakReference(activity)
        attachedActivities += AttachedActivity(
            activity = reference,
            launcher = registerPermissionLauncher(activity),
        )
        adoptPendingRequest(reference)
    }

    // The activity recreated after a configuration change receives the pending result, so it becomes its owner.
    private fun adoptPendingRequest(
        activity: WeakReference<ComponentActivity>,
    ) {
        val request = pendingRequest ?: return
        if (request.owner == null) request.owner = activity
    }

    private fun registerPermissionLauncher(
        activity: ComponentActivity,
    ): ActivityResultLauncher<String> = activity.activityResultRegistry.register(
        REQUEST_KEY,
        activity,
        ActivityResultContracts.RequestPermission(),
    ) { _ -> completePendingRequest(isAnswered = true) }

    // Also drops the entries of activities already collected.
    private fun unregister(
        activity: ComponentActivity,
    ) {
        attachedActivities.removeAll { attached ->
            val attachedActivity = attached.activity.get()
            attachedActivity == null || attachedActivity === activity
        }
        releasePendingRequest(activity)
    }

    // A finished owner never delivers the answer; a recreated one does, through the registration of its new instance.
    private fun releasePendingRequest(
        activity: ComponentActivity,
    ) {
        val request = pendingRequest ?: return
        if (request.owner?.get() !== activity) return
        if (activity.isChangingConfigurations) {
            request.owner = null
        } else {
            completePendingRequest(isAnswered = false)
        }
    }

    private class AttachedActivity(
        val activity: WeakReference<ComponentActivity>,
        val launcher: ActivityResultLauncher<String>,
    )

    /** The dialog in progress; [owner] is null while its activity is being recreated. */
    private class PendingRequest(
        val continuation: CancellableContinuation<Boolean>,
        var owner: WeakReference<ComponentActivity>?,
    )

    /** Registers the dialog launcher while the activity is created (it must happen before it starts). */
    private inner class AttachedActivityObserver(
        private val activity: ComponentActivity,
    ) : DefaultLifecycleObserver {

        override fun onCreate(
            owner: LifecycleOwner,
        ) {
            register(activity)
        }

        override fun onDestroy(
            owner: LifecycleOwner,
        ) {
            unregister(activity)
            owner.lifecycle.removeObserver(this)
        }
    }

    private companion object {
        const val REQUEST_KEY = "platform_notification_permission"
    }
}
