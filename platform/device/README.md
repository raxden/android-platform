# platform-device

## Logging

### TimberCoroutineLogger

`com.raxdenstudios.platform.device.logging.TimberCoroutineLogger` is a commons `CoroutineLogger` that logs coroutine errors with Timber at error level, under the tag it receives. `TimberCoroutineLogger.install()` sets it as commons' `defaultCoroutineLogger`, used by `defaultExceptionHandler` and `safeLaunch`. Call it once at app start. Errors go to the trees planted when they happen, so the order of planting and `install()` does not matter. This replaces the `CoroutineLoggerInitializer` / `CoroutineSDK` copies in the apps; their ordering dependency on the Timber initializer is no longer needed.

```kotlin
Timber.plant(FirebaseCrashlyticsTree())
TimberCoroutineLogger.install()
```
