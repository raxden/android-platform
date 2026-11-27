# platform-featureflag

Turns parts of the app **on or off remotely**, without a release, with boolean parameters in **Firebase Remote Config**.

The app declares its flags; the library registers their defaults, fetches the remote values once per process and answers reads without ever failing.

```kotlin
isFeatureFlagEnabledUseCase(IsFeatureFlagEnabledUseCase.Params(AppFeatureFlag.PODCASTS))
```

---

## 1. The idea in 30 seconds

| Concept | What it is | Example |
|---|---|---|
| **Flag** (`FeatureFlag`) | A Remote Config boolean parameter: its `key` and the `defaultValue` used until a remote value is activated. Declared by the app, usually as an enum. | `PODCASTS(key = "podcasts_enabled", defaultValue = false)` |
| **Fetch** (`FetchFeatureFlagsUseCase`) | Sets the in-app defaults, applies the fetch settings and fetches and activates the remote values. Once per process, at app start. | Called by `FeatureFlagInitializer` (androidx.startup) at app start |
| **Read** (`IsFeatureFlagEnabledUseCase`) | Whether a flag is on. Always succeeds: on any error it answers the flag's default. | `Answer.Success(true)` |
| **Minimum fetch interval** | How long fetched values stay cached before a new fetch reaches the backend. Passed by the app. | `0` in debug, `1 h` in release |

---

## 2. What a read answers

| Situation | Answer |
|---|---|
| A remote value was activated (now or in an earlier process) | The remote value |
| The key was never fetched and has no in-app default (`VALUE_SOURCE_STATIC`) | The flag's `defaultValue` |
| The key is registered but no remote value was ever fetched | The in-app default, i.e. the flag's `defaultValue` |
| Reading the value throws (e.g. the console value is not a boolean) | The flag's `defaultValue` |

### Waiting for the first fetch

The first fetch of a process can still be in flight when a screen reads a flag. To avoid showing a stale value right after a console change:

- Each read made while the first activation is in flight waits for it, **at most 2 seconds** counted from that read.
- Once one read has timed out, or the activation has finished (successfully or not), **no later read waits** in that process.

### The fetch

| | Value |
|---|---|
| Timeouts | 10 seconds for each step (setting the defaults, applying the settings, the fetch), so a full fetch can take up to about 30 seconds. This never delays reads beyond 2 seconds. |
| Minimum fetch interval | The one passed to `FeatureFlagDILibrary` |
| On failure (offline, timeout, Firebase error) | `Answer.Failure`. Not fatal: reads keep answering the last activated values or the defaults, and the next process start fetches again. |

---

## 3. Integrating it into an app

The app must have Firebase configured (`google-services.json` and the Google Services plugin).

**1. Add the dependency.** The library exposes `Answer` and `SuspendUseCase` in its API but does not export their modules, so the app also needs platform-core and commons-core on its classpath:

```kotlin
implementation(platform(libs.platform.bom))
implementation(libs.platform.featureflag) // com.raxdenstudios:platform-featureflag
implementation(libs.platform.core) // com.raxdenstudios:platform-core
implementation(platform(libs.commons.bom))
implementation(libs.commons.core) // com.raxdenstudios:commons-core
```

**2. Declare the flags** with an enum implementing `FeatureFlag`. Keys must be unique and match the console parameters:

```kotlin
enum class AppFeatureFlag(
    override val key: String,
    override val defaultValue: Boolean,
) : FeatureFlag {

    PODCASTS(key = "podcasts_enabled", defaultValue = false),
}
```

**3. Register the library** with the flags and the minimum fetch interval. Pass the app's own `BuildConfig.DEBUG`: the library's is always `false` for consumers.

```kotlin
override fun provideLibs(): List<DILibrary> = listOf(
    FeatureFlagDILibrary(
        flags = AppFeatureFlag.entries,
        minimumFetchInterval = if (BuildConfig.DEBUG) Duration.ZERO else 1.hours,
    ),
    // ...
)
```

A zero interval in debug makes a console change show up on the next launch. The interval must not be negative (the constructor throws `IllegalArgumentException`) and is truncated to whole seconds.

**4. Fetch once at app start** by registering the platform `FeatureFlagInitializer` in the app's androidx.startup provider. The app does not write its own initializer:

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="com.raxdenstudios.platform.featureflag.initializer.FeatureFlagInitializer"
        android:value="androidx.startup" />
</provider>
```

`com.raxdenstudios.platform.featureflag.initializer.FeatureFlagInitializer` launches `FetchFeatureFlagsUseCase` on `CoroutineScopeProvider.io` and only logs a failure (Timber warning, message only, no throwable).

- `dependencies()` = `DIInitializer` (platform-core), so Koin is started first. The app's `Application` must implement `DIAppProvider` (see [Starting DI](../core/README.md#starting-di-diinitializer)).
- It resolves `FetchFeatureFlagsUseCase` (this library) and `CoroutineScopeProvider` from Koin. `CoroutineScopeProvider` is bound by platform-core's `CoreDILibrary`; an app that does not load `CoreDILibrary` must bind it itself.
- It does not depend on platform-device's `TimberInitializer`, because this module does not depend on platform-device. The warning is written after a network round trip, so the app's tree is planted by then in practice.
- It is opt-in: the library manifest does not declare it.

**Migrating from the app copy.** Delete the app's `FeatureFlagInitializer` (`com.raxdenstudios.app.initializer`) and its test, and point its `<meta-data>` entry to the platform class. Do not keep both entries: the flags would be fetched twice per process. Remove any reference to the app class from other initializers' `dependencies()`.

**5. Read the flags** wherever they are needed:

```kotlin
val enabled = isFeatureFlagEnabledUseCase(
    IsFeatureFlagEnabledUseCase.Params(AppFeatureFlag.PODCASTS),
).getValueOrDefault(AppFeatureFlag.PODCASTS.defaultValue)
```

### Adding a flag

1. Add an entry to the app's enum with a unique key and the default that keeps the feature off.
2. Create the boolean parameter in the Firebase console with the same key.
3. Read it with `IsFeatureFlagEnabledUseCase`.

No change is needed in this library.

### Public API

| Type | Purpose |
|---|---|
| `FeatureFlag` | Interface the app's flags implement (`key`, `defaultValue`) |
| `FetchFeatureFlagsUseCase` | Fetches and activates the remote values (`Answer<Unit, Failure>`) |
| `IsFeatureFlagEnabledUseCase` | Reads a flag (`Answer<Boolean, Failure>`, always `Success`) |
| `FeatureFlagDILibrary(flags, minimumFetchInterval)` | Koin library that binds both use cases |
| `FeatureFlagInitializer` | androidx.startup initializer that runs `FetchFeatureFlagsUseCase` once per process |

Firebase types never leave the library. `FirebaseRemoteConfig` is not bound in Koin, so it does not clash with an app binding of it.
