# platform-notification

Two building blocks every app with push notifications needs:

- **Permission**: reads the notification permission (`POST_NOTIFICATIONS` on Android 13+) as a single state and shows the system dialog when it can still be shown.
- **Topics**: keeps the device subscribed to the **FCM topic of its UTC offset** (`<prefix>p1`, `<prefix>m5`, `<prefix>0`…), so the server can send a push at the same local hour everywhere. The topic is synced at app start and whenever the time zone changes.

```kotlin
getNotificationPermissionStateUseCase(GetNotificationPermissionStateUseCase.Params) // Granted, Requestable, Blocked, Disabled
notificationTopicSyncer.sync() // subscribes to e.g. "radio_daily_utc_p1"
```

---

## 1. The idea in 30 seconds

| Concept | What it is | Example |
|---|---|---|
| **Launcher** (`NotificationPermissionLauncher`) | Single that keeps the attached activities and shows the system dialog on the top one. | `launcher.attach(activity)` in `ActivityLifecycleCallbacks` |
| **Permission state** (`NotificationPermissionState`) | What the app may do and, when it may not, what can still fix it. | `Requestable` → show your explanation, then the dialog |
| **Topic** (`NotificationTopic`) | An FCM topic, built from a prefix and the whole-hour UTC offset (clamped to -12..+14). | `NotificationTopic.utcOffset("radio_daily_utc_", 1)` = `radio_daily_utc_p1` |
| **Topic policy** (`NotificationTopicPolicy`) | Bound by the app: the topic prefix and the legacy topics to leave. **Without it, topic sync is disabled.** | `NotificationTopicPolicy(prefix = "radio_daily_utc_")` |
| **Syncer** (`NotificationTopicSyncer`) | Runs the sync, logs failures and tells the optional listener about changes. | Called at app start and by the time zone receiver |

---

## 2. Permission

| State | Meaning | What the app can do |
|---|---|---|
| `Granted` | Notifications can be posted. | Nothing. |
| `Requestable` | Android 13+, not granted, and the system dialog can still be shown (never asked, or denied once). | Show its own explanation, then `LaunchNotificationPermissionDialogUseCase`. |
| `Blocked` | Android 13+, not granted, and the dialog no longer shows (asked before and no rationale). | Send the user to the system settings. |
| `Disabled` | The app's notifications are turned off in the system settings: below Android 13, or on Android 13+ with the permission granted. | Send the user to the system settings. |

`isGranted` is `true` only for `Granted`; `isRuntimePermissionMissing` is `true` for `Requestable` and `Blocked`.

Android does not tell a permission never asked from one permanently denied, so the module remembers whether the dialog was ever shown (`permission_requested`, see [Persistence](#5-persistence)), readable through `WasNotificationPermissionRequestedUseCase`.

| Use case | Returns |
|---|---|
| `GetNotificationPermissionStateUseCase` | The current `NotificationPermissionState`. The rationale is read from the top attached activity. |
| `AreNotificationsEnabledUseCase` | Whether notifications can be posted now, from the application context only: safe in the background (e.g. while handling an FCM message). |
| `WasNotificationPermissionRequestedUseCase` | Whether this app ever showed the system dialog. Use it to tell "never asked" from "asked and denied" (e.g. prompt only users never asked) instead of reading the preferences file. |
| `LaunchNotificationPermissionDialogUseCase` | `NotificationPermissionDialogResult(state, isDialogShown)`. Shows the dialog only when the state is `Requestable` and an attached activity is alive; otherwise returns the current state with `isDialogShown = false`. Report an answer only when `isDialogShown` is `true`. Call it from the main thread. |

Unlike `PermissionsManager` (commons-permissions), the launcher shows the dialog even after a first denial (the second and last chance Android gives), so call it only after your own explanation, when the user chose to turn notifications on.

---

## 3. Topics

`SyncNotificationTopicUseCase` returns `NotificationTopicSync(topic, previousTopic, removedLegacyTopics)`; `hasChanges` is `true` when the topic changed or a legacy topic was left in this sync.

Rules:

- The offset is read from `Clock.systemDefaultZone()` (taken on every sync) at the current instant, so daylight saving time counts, and rounded to the nearest whole hour (+5:30 → +6, -3:30 → -3).
- Without a bound `NotificationTopicPolicy`, the use case fails without touching anything; the syncer and the receiver do nothing (debug log).
- Every sync runs under one shared lock, so the app-start sync and the receiver sync never interleave.
- If the saved topic cannot be read, the sync fails without subscribing or saving anything.
- The topic is subscribed on every sync, even when unchanged, so a subscription FCM lost is restored.
- When the topic changed, the new one is subscribed **before** the old one is left, and it is saved only once the old one is left, so a failure is retried on the next sync.
- Each `legacyTopics` entry is left once and remembered per topic. A failure is retried on the next sync and does not fail this one. A legacy topic equal to the current topic is never left.
- FCM calls give up after the platform-core task timeout (10 s), so a sync never hangs offline.

`NotificationTopicTimeZoneReceiver` is declared in this module's manifest (`TIMEZONE_CHANGED`, not exported). It runs the syncer with `goAsync` on `CoroutineScopeProvider.default` and abandons it after 8 seconds. Daylight saving transitions send no broadcast: the next process start picks them up, so until the app starts again the device keeps the topic of the previous offset.

---

## 4. Integrating it into an app

The app must have Firebase configured (`google-services.json` and the Google Services plugin). The manifest of this module declares `POST_NOTIFICATIONS` and the receiver; the app adds nothing to its manifest.

**1. Add the dependency.** The library exposes `Answer` and `SuspendUseCase` in its API but does not export their modules:

```kotlin
implementation(platform(libs.platform.bom))
implementation(libs.platform.notification) // com.raxdenstudios:platform-notification
implementation(libs.platform.core) // com.raxdenstudios:platform-core
implementation(platform(libs.commons.bom))
implementation(libs.commons.core) // com.raxdenstudios:commons-core
```

**2. Load the library.** `CoroutineScopeProvider` (used by the receiver) comes from platform-core's `CoreDILibrary`; an app that does not load it must bind it itself.

```kotlin
override fun provideLibs(): List<DILibrary> = listOf(
    CoreDILibrary(),          // CoroutineScopeProvider, used by the receiver
    NotificationDILibrary(),
    // ...
)
```

**3. Attach the launcher** to every activity, before it starts, e.g. in `Application.ActivityLifecycleCallbacks`:

```kotlin
override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    if (activity is ComponentActivity) notificationPermissionLauncher.attach(activity)
}
```

**4. Bind the topic policy** (and, optionally, a listener) in an app module. Skip this step if the app uses no topics:

```kotlin
module {
    factory {
        NotificationTopicPolicy(
            prefix = "radio_daily_utc_",
            legacyTopics = setOf(NotificationTopic("radio_daily")),
        )
    }
    factory<NotificationTopicSyncListener> {
        val tracker: NotificationTopicTracker = get()
        NotificationTopicSyncListener { sync -> tracker.topicChanged(sync) }
    }
}
```

**5. Sync at app start**, from an initializer or the `Application`, on a background scope:

```kotlin
coroutineScopeProvider.default.launch { notificationTopicSyncer.sync() }
```

**6. Use the permission use cases** from your ViewModels:

```kotlin
safeLaunch {
    launchNotificationPermissionDialogUseCase(LaunchNotificationPermissionDialogUseCase.Params)
        .onSuccess { result -> if (result.isDialogShown) tracker.permissionAnswered(result.state) }
}

safeLaunch {
    wasNotificationPermissionRequestedUseCase(WasNotificationPermissionRequestedUseCase.Params)
        .onSuccess { requested -> if (!requested) showNotificationsPrompt() }
}
```

---

## 5. Persistence

Every flag lives in the module's own `SharedPreferences` file, never in the app's default one. The names are public in `NotificationStorage`:

| Constant | Value | Type | Content |
|---|---|---|---|
| `PREFERENCES_NAME` | `platform_notification` | file (`MODE_PRIVATE`) | |
| `PERMISSION_REQUESTED_KEY` | `permission_requested` | Boolean | The system dialog was shown at least once |
| `DAILY_TOPIC_KEY` | `daily_topic` | String | The topic the device is subscribed to |
| `legacyTopicRemovedKey(topic)` | `legacy_topic_removed_<topic>` | Boolean | The device already left that legacy topic |

**Migrating from an app copy.** An app that had its own copy of this code kept these values under its own file and keys. Move them into `platform_notification` **once**, before the first sync or permission read (e.g. in a one-shot initializer guarded by its own flag), using the `NotificationStorage` constants. Without it, the module sees a first run: the permission of a user who denied it permanently reads as `Requestable`, and the old topic is never left, so the device could receive the push twice.

---

## 6. Public API

| Type | Purpose |
|---|---|
| `NotificationDILibrary` | Koin library that binds everything below |
| `NotificationPermissionLauncher` | `attach`, `areNotificationsEnabled`, `isRuntimePermission`, `isPermissionGranted`, `shouldShowRationale`, `suspend launch()` |
| `NotificationPermissionState` | `Granted`, `Requestable`, `Blocked`, `Disabled` |
| `NotificationPermissionDialogResult` | `state`, `isDialogShown` |
| `GetNotificationPermissionStateUseCase`, `AreNotificationsEnabledUseCase`, `WasNotificationPermissionRequestedUseCase`, `LaunchNotificationPermissionDialogUseCase` | Permission use cases |
| `NotificationTopic` | FCM topic value class, `utcOffset(prefix, utcOffsetHours)` |
| `NotificationTopicPolicy` | App-bound prefix and legacy topics |
| `NotificationTopicSync` | `topic`, `previousTopic`, `removedLegacyTopics`, `hasChanges` |
| `SyncNotificationTopicUseCase` | One topic sync |
| `NotificationTopicSyncer` | Sync + logging + listener, for the app start |
| `NotificationTopicSyncListener` | Optional app-bound callback on changes |
| `NotificationTopicTimeZoneReceiver` | Manifest receiver for `TIMEZONE_CHANGED` |
| `NotificationStorage` | Preferences file name and keys |

`FirebaseMessaging` and the preferences file are not bound in Koin under public types, so they never clash with app bindings.
