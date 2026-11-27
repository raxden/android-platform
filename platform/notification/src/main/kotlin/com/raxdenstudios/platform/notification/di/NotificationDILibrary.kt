package com.raxdenstudios.platform.notification.di

import com.raxdenstudios.platform.core.di.DILibrary
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.notification.di.module.NotificationDIModule

/**
 * DI entry point of the notification module: the `POST_NOTIFICATIONS` permission (state, system dialog) and the FCM
 * topic of the device's UTC offset. It binds the permission launcher and use cases, the topic sync use case and the
 * syncer. Topic sync stays disabled until the app binds a `NotificationTopicPolicy`.
 *
 * Needs `CoreDILibrary` (it provides the `CoroutineScopeProvider` used by the time zone receiver) and Firebase
 * configured in the app (`google-services.json`). The manifest of this module declares `POST_NOTIFICATIONS` and the
 * `NotificationTopicTimeZoneReceiver`, so the app adds nothing to its manifest.
 *
 * ## Setup
 *
 * Load this library after `CoreDILibrary`:
 *
 * ```kotlin
 * override fun provideLibs(): List<DILibrary> = listOf(
 *     CoreDILibrary(),
 *     NotificationDILibrary(),
 *     // ...
 * )
 * ```
 *
 * Attach the launcher to every activity, e.g. in `Application.ActivityLifecycleCallbacks`. It must run before the
 * activity is started, and it is forgotten when the activity is destroyed:
 *
 * ```kotlin
 * override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
 *     if (activity is ComponentActivity) notificationPermissionLauncher.attach(activity)
 * }
 * ```
 *
 * Bind the topic policy in an app module, and optionally a `NotificationTopicSyncListener` that is told about topic
 * changes (e.g. to track them). Skip this step if the app uses no topics. The module resolves them when a sync runs,
 * so the order of the modules does not matter:
 *
 * ```kotlin
 * module {
 *     factory {
 *         NotificationTopicPolicy(
 *             prefix = "radio_daily_utc_",
 *             legacyTopics = setOf(NotificationTopic("radio_daily")),
 *         )
 *     }
 *     factory<NotificationTopicSyncListener> {
 *         val tracker: NotificationTopicTracker = get()
 *         NotificationTopicSyncListener { sync -> tracker.topicChanged(sync) }
 *     }
 * }
 * ```
 *
 * The `prefix` names the topics of the UTC offsets (`radio_daily_utc_p1`, `radio_daily_utc_m5`, `radio_daily_utc_0`);
 * the `legacyTopics` are the topics older app versions subscribed to, each left once.
 *
 * ## Usage
 *
 * Sync the topic at the app start, from a background scope. The receiver runs the same sync on every time zone change,
 * so the app does not need to call it again:
 *
 * ```kotlin
 * coroutineScopeProvider.default.launch { notificationTopicSyncer.sync() }
 * ```
 *
 * Read the permission state from a ViewModel to decide what to show. `Blocked` and `Disabled` can only be fixed in the
 * system settings:
 *
 * ```kotlin
 * safeLaunch {
 *     getNotificationPermissionStateUseCase(GetNotificationPermissionStateUseCase.Params)
 *         .onSuccess { state -> /* show the matching UI for state */ }
 * }
 * ```
 *
 * Show the system dialog only after your own explanation, when the user chose to turn notifications on: it is shown
 * even after a first denial, the last chance Android gives. Report the answer only when `isDialogShown` is `true`:
 *
 * ```kotlin
 * safeLaunch {
 *     launchNotificationPermissionDialogUseCase(LaunchNotificationPermissionDialogUseCase.Params)
 *         .onSuccess { result -> if (result.isDialogShown) { /* track result.state */ } }
 * }
 * ```
 *
 * Inside a background handler (e.g. an FCM message), where no activity is alive, use `AreNotificationsEnabledUseCase`
 * instead, which reads the application context only.
 *
 * To tell "never asked" from "asked and denied" (e.g. prompt only users never asked), read
 * `WasNotificationPermissionRequestedUseCase` instead of the module's preferences file:
 *
 * ```kotlin
 * safeLaunch {
 *     wasNotificationPermissionRequestedUseCase(WasNotificationPermissionRequestedUseCase.Params)
 *         .onSuccess { requested -> if (!requested) { /* show your prompt */ } }
 * }
 * ```
 */
class NotificationDILibrary : DILibrary {

    override fun initialize() = Unit

    override fun getModules(): List<DIModule> = listOf(
        NotificationDIModule(),
    )
}
