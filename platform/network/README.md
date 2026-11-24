# platform-network

## NetworkError to Failure

`NetworkError<*>.toFailure()` in `com.raxdenstudios.platform.network.ext` maps the commons `NetworkError` to `Failure.Network`, keeping the message and dropping the code and body:

| NetworkError | Failure.Network |
|---|---|
| `Client` | `Client` |
| `Server` | `Server` |
| `Network` | `Connection` |
| `Unknown` | `Unknown` |

Use it in a remote data source instead of a per-module mapper. When the error body carries information you need (an API error code), map that body yourself first. The app mappers (`NetworkErrorDtoToFailureMapper`, `NetworkErrorDtoToErrorMapper`) never read the body either, so replacing them does not change behaviour.
