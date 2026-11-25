# platform-device

## Logging

### FirebaseCrashlyticsTree

`com.raxdenstudios.platform.device.logging.FirebaseCrashlyticsTree` is a Timber tree for release builds that records logged throwables in Firebase Crashlytics as non-fatals.

- Only `INFO` and above are loggable.
- Only logs with a throwable are sent; plain messages are not.
- Connectivity errors (`UnknownHostException`, `ConnectException`, `SocketTimeoutException`) are skipped, whether they are the throwable, in its cause chain or named in the message: they are expected noise when the device is offline.

Migrating from the app copies (`com.raxdenstudios.app.util.FirebaseCrashlyticsTree`): those only checked the message, so an error whose cause is a connectivity error (e.g. `IllegalStateException("sync failed", UnknownHostException())`) used to be reported and is now skipped, as are subclasses of those exceptions. The app must depend on Timber and Firebase Crashlytics itself (it already does to plant the tree).

```kotlin
if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree()) else Timber.plant(FirebaseCrashlyticsTree())
```

### TimberCoroutineLogger

`com.raxdenstudios.platform.device.logging.TimberCoroutineLogger` is a commons `CoroutineLogger` that logs coroutine errors with Timber at error level, under the tag it receives. `TimberCoroutineLogger.install()` sets it as commons' `defaultCoroutineLogger`, used by `defaultExceptionHandler` and `safeLaunch`. Call it once at app start. Errors go to the trees planted when they happen, so the order of planting and `install()` does not matter. This replaces the `CoroutineLoggerInitializer` / `CoroutineSDK` copies in the apps; their ordering dependency on the Timber initializer is no longer needed.

```kotlin
Timber.plant(FirebaseCrashlyticsTree())
TimberCoroutineLogger.install()
```

## Activity initializers

Two androidx.startup initializers attach platform components to every created `ComponentActivity`, so apps no longer copy that wiring into `Application` or their own initializers. Both live in `com.raxdenstudios.platform.device.initializer`:

| Initializer | Attaches | Filter |
|---|---|---|
| `PermissionsInitializer` | commons `PermissionsManager` | none, every activity |
| `EngagementPromptsInitializer` | `InAppReview` and `InAppShare` | `EngagementPromptsActivityFilter` from Koin, every activity when none is bound |

Each one registers `Application.ActivityLifecycleCallbacks` (commons `ActivityLifecycleCallbacksAdapter`) and attaches in `onActivityCreated`, before the activity is STARTED, as those components require.

### Enabling them (opt-in)

The library manifest does not declare them: an app gets them only when it adds them to its androidx.startup provider. They need Koin bindings from `DeviceDILibrary`, so declaring them for every consumer would crash an app that uses `platform-device` without that setup on its first activity.

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="com.raxdenstudios.platform.device.initializer.PermissionsInitializer"
        android:value="androidx.startup" />
    <meta-data
        android:name="com.raxdenstudios.platform.device.initializer.EngagementPromptsInitializer"
        android:value="androidx.startup" />
</provider>
```

To stop using one, remove its `<meta-data>` entry. If a future version declares them in the library manifest, an app opts out with `tools:node="remove"` on the same entry.

### Koin

They do not depend on the app's Koin initializer (a library cannot name it). `create` only registers the callbacks; the components are resolved from Koin the first time an activity is created, which happens after every initializer and `Application.onCreate` ran. The app must start Koin, with `DeviceDILibrary` loaded, in its own initializer or in `Application.onCreate`. If Koin is not started when the first activity is created, the callback throws.

### Choosing the activities that host the engagement prompts

Bind an `EngagementPromptsActivityFilter` to keep the review and share prompts off screens where they never make sense. A milestone reached while such a screen is in front stays pending in the engagement engine.

```kotlin
module {
    single { EngagementPromptsActivityFilter { activity -> activity !is AlarmRingingActivity } }
}
```

### Ordering

`dependencies()` is empty in both, so App Startup gives no order between them, nor between them and the app's own activity callbacks. The components they attach do not depend on each other. Attach app-specific helpers (for example a notification permission launcher) from an initializer of the app.

### Migrating from the app copies

Replace the app's `PermissionsInitializer` / `EngagementPromptsInitializer` entries in the manifest with the platform ones, keep only the app-specific attaches (such as `NotificationPermissionLauncher`) in an app initializer, and turn an `isPartOfAppSession()`-style check into an `EngagementPromptsActivityFilter` binding. Do not keep the app's own attach initializers enabled together with the platform ones: every activity would be attached twice, so swap the manifest entries instead.
