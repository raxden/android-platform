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
