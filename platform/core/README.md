# platform-core

## Await a Play Services Task as an Answer

`Task<T>.awaitAnswer(errorMessage, timeout)` in `com.raxdenstudios.platform.core.data.ext` suspends until a Google Play Services or Firebase `Task` completes and returns `Answer<T, Failure>`. It never hangs: after `timeout` (default `DEFAULT_TASK_TIMEOUT`, 10 s; pass `Duration.INFINITE` to wait forever) it returns `Failure.Unknown("<errorMessage>: timed out after <timeout>")`.

- A failed task maps its exception with `toUnknownError(errorMessage)`.
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
