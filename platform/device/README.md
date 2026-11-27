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

`com.raxdenstudios.platform.device.logging.TimberCoroutineLogger` is a commons `CoroutineLogger` that logs coroutine errors with Timber at error level, under the tag it receives. `TimberCoroutineLogger.install()` sets it as commons' `defaultCoroutineLogger`, used by `defaultExceptionHandler` and `safeLaunch`. Call it once at app start. Errors go to the trees planted when they happen, so the order of planting and `install()` does not matter. Apps that use androidx.startup enable the platform `CoroutineLoggerInitializer` instead (see [Startup initializers](#startup-initializers)).

```kotlin
Timber.plant(FirebaseCrashlyticsTree())
TimberCoroutineLogger.install()
```

## Startup initializers

Three androidx.startup initializers in `com.raxdenstudios.platform.device.initializer` replace the identical copies the apps carried in `com.raxdenstudios.app.initializer`. None of them uses Koin.

| Initializer | Does | `dependencies()` |
|---|---|---|
| `TimberInitializer` | Plants `Timber.DebugTree` when the app is debuggable, `FirebaseCrashlyticsTree` otherwise | none |
| `CoroutineLoggerInitializer` | Calls `TimberCoroutineLogger.install()` | `TimberInitializer` |
| `CrashlyticsInitializer` | Sets the Crashlytics custom keys `app_version` (version name) and `app_version_code` (version code) | none |

They read the build from the running app, not from a `BuildConfig`: the library `BuildConfig` is baked into the AAR (`DEBUG` is always false for consumers). `TimberInitializer` checks `ApplicationInfo.FLAG_DEBUGGABLE`, and `CrashlyticsInitializer` reads the app's `PackageInfo` (`versionName`, and `PackageInfoCompat.getLongVersionCode`). The key names are the ones the app copies used, so existing Crashlytics filters keep working. Whether reports are collected is still set per build type by the app's `firebase_crashlytics_collection_enabled` manifest flag.

`TimberInitializer` picks the tree from `ApplicationInfo.FLAG_DEBUGGABLE`, so a release build marked
`debuggable=true` plants `Timber.DebugTree` and a debug build with `debuggable=false` plants
`FirebaseCrashlyticsTree`, a deliberate difference from `BuildConfig.DEBUG`.

### Enabling them (opt-in)

The library manifest does not declare them. Add them to the app's androidx.startup provider:

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="com.raxdenstudios.platform.device.initializer.TimberInitializer"
        android:value="androidx.startup" />
    <meta-data
        android:name="com.raxdenstudios.platform.device.initializer.CoroutineLoggerInitializer"
        android:value="androidx.startup" />
    <meta-data
        android:name="com.raxdenstudios.platform.device.initializer.CrashlyticsInitializer"
        android:value="androidx.startup" />
</provider>
```

An initializer reached through another one's `dependencies()` runs even without its own `<meta-data>`, so `CoroutineLoggerInitializer` alone already plants the Timber tree. Declaring both is harmless: App Startup runs each initializer once.

### Ordering with the app's Koin initializer

The app keeps its own `KoinInitializer` (it composes the app's DI graph). To keep starting Koin after logging is ready, list the platform initializers in its `dependencies()`:

```kotlin
import com.raxdenstudios.platform.device.initializer.CoroutineLoggerInitializer
import com.raxdenstudios.platform.device.initializer.TimberInitializer

class KoinInitializer : Initializer<Unit> {

    override fun create(
        context: Context,
    ) {
        AppDIApp().initialize(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        TimberInitializer::class.java,
        CoroutineLoggerInitializer::class.java,
    )
}
```

An app initializer may depend on library initializers; the reverse is impossible, because a library cannot name an app class. That is why initializers that need Koin while they run (such as an FCM token initializer that resolves a use case in `create`) stay in the app: they must depend on the app's `KoinInitializer`.

Apps can instead replace their `KoinInitializer` with platform-core's `DIInitializer` (see [Starting DI](../core/README.md#starting-di-diinitializer)): initializers that need Koin then depend on `DIInitializer::class.java`, including library ones, and list `TimberInitializer` too if they log at create time, because `DIInitializer` has no dependencies.

### Migrating from the app copies

1. Delete the app's `TimberInitializer`, `CoroutineLoggerInitializer` and `CrashlyticsInitializer`.
2. Point their `<meta-data>` entries to the `com.raxdenstudios.platform.device.initializer` classes. Remove the
   app's own `<meta-data>` entries for them, not only the classes: keeping both the app's and the platform's Timber
   entries plants two trees even though the class names differ.
3. Change the imports in the app's `KoinInitializer` (and any other app initializer that lists them) to the platform classes.

Do not keep the app's Timber initializer enabled together with the platform one: both would plant a tree and every log would be written, and every non-fatal recorded, twice. The same applies to Crashlytics keys and the coroutine logger, which are harmless but redundant.
Any `Timber.plant` still called in `Application.onCreate` is another source of double planting, so remove it too.

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
