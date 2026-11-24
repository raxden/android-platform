# platform-core

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
