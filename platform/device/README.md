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
