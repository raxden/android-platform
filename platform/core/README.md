# platform-core

## Generic DI bindings (`CoreDILibrary`)

`com.raxdenstudios.platform.core.di.CoreDILibrary` binds the app-agnostic definitions every app used to declare itself. All are unqualified singletons:

| Type | Implementation |
|---|---|
| `DispatcherProvider` (commons-coroutines) | `DefaultDispatcherProvider()` |
| `CoroutineScopeProvider` (commons-coroutines) | `DefaultCoroutineScopeProvider(dispatcherProvider = get())` |
| `WorkManager` | `WorkManager.getInstance(androidContext())` |

List it first in `provideLibs()`. Koin lets the definition loaded last win, so an app override
listed after it wins. Apps should still delete their copies (see below):

```kotlin
override fun provideLibs(): List<DILibrary> = listOf(
    CoreDILibrary(),
    UIDILibrary(),
    DeviceDILibrary(),
    // ...
)
```

Delete the app's own copies of these bindings: its `CoroutinesDIModule` (and its entry in the app `DILibrary`) and the `single { WorkManager.getInstance(get()) }` line. `KoinDIApp` keeps Koin's default `allowOverride = true`, so a duplicate does not fail at startup: the definition loaded last (its library listed later in `provideLibs()`) silently replaces the other one. Override on purpose only, from a module listed after `CoreDILibrary`.

Other generic bindings live in the library that owns their dependency: `FirebaseAnalytics` and `PermissionsManager` are bound by `DeviceDILibrary` (platform-device), so apps that load it must not bind them again either.

## Starting DI (`DIInitializer`)

`com.raxdenstudios.platform.core.initializer.DIInitializer` is an androidx.startup initializer that starts the app's DI. Library and app initializers that need Koin while they run list it in their `dependencies()`, so no library has to know the app's own `KoinInitializer`.

- `create` does nothing when Koin is already started (`GlobalContext.getOrNull() != null`).
- Otherwise it casts `context.applicationContext` to `com.raxdenstudios.platform.core.di.DIAppProvider` and calls `diApp.initialize(context)`.
- If the `Application` does not implement `DIAppProvider`, it fails with an `IllegalStateException` that names the class to implement.
- `dependencies()` is empty: starting Koin needs nothing else. The Timber and Crashlytics initializers live in platform-device and core does not depend on them.
- "Already started" means Koin's global context is registered. `startKoin` registers it before loading the modules, so a first start that throws after that point is treated as started and is not retried.
- It runs in every process that runs the androidx.startup `InitializationProvider`, so multi-process apps start DI in each of those processes. Library initializers that depend on it are opt-in too: none is enabled by a library manifest.

### Setup

1. The app's `Application` implements `DIAppProvider`:

    ```kotlin
    class App : Application(), DIAppProvider {
        override val diApp: DIApp = AppDIApp()
    }
    ```

2. Delete the app's own `KoinInitializer` and point its `<meta-data>` to the platform class:

    ```xml
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${applicationId}.androidx-startup"
        android:exported="false"
        tools:node="merge">
        <meta-data
            android:name="com.raxdenstudios.platform.core.initializer.DIInitializer"
            android:value="androidx.startup" />
    </provider>
    ```

    It is opt-in: the library manifest does not declare it. The `<meta-data>` entry is optional when another enabled initializer lists `DIInitializer` in its `dependencies()`: App Startup runs an initializer reached through another one's dependencies even without its own entry, and runs each one once. Keep the entry anyway when nothing else depends on it, or Koin is never started.

    **Coexisting with an app `KoinInitializer`.** Delete it (recommended). If the app keeps its own `KoinInitializer` while any enabled library or app initializer depends on `DIInitializer`, the order between the two is not defined and the app's `startKoin` can throw `KoinApplicationAlreadyStartedException`. A kept `KoinInitializer` must either list `DIInitializer::class.java` in its `dependencies()` (it then has nothing left to do, so delete it) or start Koin only when `GlobalContext.getOrNull() == null`.

3. App initializers that need Koin in `create` declare it in `dependencies()`, plus `TimberInitializer` (platform-device) if they log while they run, because `DIInitializer` no longer orders Timber before Koin:

    ```kotlin
    class FcmTokenInitializer : Initializer<Unit>, KoinComponent {

        override fun create(
            context: Context,
        ) {
            // get<...>() is safe here: Koin is already started.
        }

        override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
            DIInitializer::class.java,
            TimberInitializer::class.java,
        )
    }
    ```

    The app no longer controls `DIInitializer`'s own dependencies, so Timber is no longer guaranteed to be planted before Koin starts, as it was with an app `KoinInitializer` depending on `TimberInitializer`. Koin's `androidLogger()` writes to Logcat, not Timber, so starting Koin does not need it; App Startup gives no order between initializers that do not depend on each other, so every initializer that logs must list `TimberInitializer` itself.

4. Test `Application`s (Robolectric, instrumentation) that do not start the real DI must either implement `DIAppProvider` or remove the initializer from the merged manifest:

    ```xml
    <meta-data
        android:name="com.raxdenstudios.platform.core.initializer.DIInitializer"
        tools:node="remove" />
    ```

    `tools:node="remove"` only keeps `DIInitializer` from running when no other enabled initializer lists it in `dependencies()`: App Startup still runs it as a dependency. So test `Application`s should implement `DIAppProvider`, or also remove every enabled initializer that depends on it.

    A test that starts Koin itself before App Startup runs is fine: `DIInitializer` sees Koin started and does nothing.

## Failure

`com.raxdenstudios.platform.core.domain.model.Failure` is the domain error carried in `Answer<T, Failure>`. It is an open interface: apps add their own business failures as `sealed interface XFailure : Failure`.

- `message` is diagnostic text for logs, not user-facing copy. The UI picks the text by failure type.
- `cause` is the technical origin, kept only for diagnosis and reporting. `Failure.Unknown(message, cause)` and `Throwable.toUnknownError(errorMessage)` keep it; every other failure returns `null` unless it overrides it.
- `cause` is not part of equality, so `Failure.Unknown("x") == Failure.Unknown("x", exception)` and tests can compare against the message alone. `copy()` drops it.
- Never keep a `Failure` in UI state or `SavedStateHandle`: map it to an `ErrorModel` and drop it, so the throwable is not retained.

```kotlin
answer.onFailure { failure -> failure.cause?.let { cause -> Timber.e(cause, failure.message) } }
```

`Throwable.toUnknownError(errorMessage)` builds `"<errorMessage>: <throwable message>"`, falling back to the throwable's class name when it has no message.

## Await a Play Services Task as an Answer

`Task<T>.awaitAnswer(errorMessage, timeout)` in `com.raxdenstudios.platform.core.data.ext` suspends until a Google Play Services or Firebase `Task` completes and returns `Answer<T, Failure>`. It never hangs: after `timeout` (default `DEFAULT_TASK_TIMEOUT`, 10 s; pass `Duration.INFINITE` to wait forever) it returns `Failure.Unknown("<errorMessage>: timed out after <timeout>")`.

- A failed task maps its exception with `toUnknownError(errorMessage)`, which keeps it as `Failure.cause`.
- A cancelled task is a `Failure.Unknown`.
- A `Task` cannot be cancelled: when the wait is cancelled or times out, its late result is dropped.
- `Task<Void>.awaitUnitAnswer(...)` returns `Answer<Unit, Failure>` for tasks without a value.
- `Task` comes from Play Services Tasks (`com.google.android.gms:play-services-tasks`), which any Firebase or Play Services dependency brings in.

Migrating from the private helpers in the apps' `NotificationTopicRemoteDataSource`: rename `subscribeToTopic(...).awaitAnswer(msg)` to `awaitUnitAnswer(msg)` (the generic `awaitAnswer` on a `Task<Void>` returns `Answer<Void, Failure>`), and note the cancelled message is now `"<msg>: task cancelled"`.

```kotlin
suspend fun subscribe(topic: String): Answer<Unit, Failure> =
    firebaseMessaging.subscribeToTopic(topic).awaitUnitAnswer("Subscribe to $topic failed")

suspend fun token(): Answer<String, Failure> =
    firebaseMessaging.token.awaitAnswer("Get FCM token failed")
```
