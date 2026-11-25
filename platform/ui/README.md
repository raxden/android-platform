# platform-ui

Jetpack Compose UI components, navigation utilities and the app theme.

---

## Theme

| Type | What it is |
|---|---|
| `Theme` | `Device` (follow the system) or `App(dark)` (the user's choice). |
| `ThemeProvider` | Holds the current `Theme` and applies it with `Apply { ... }`. |
| `ColorSchemeThemeProvider` | `ThemeProvider` built from one light and one dark `ColorScheme`. |
| `AbstractThemeProvider` | Base class for apps that need their own `getColorScheme(theme)`. |
| `ThemeManager` | Reads and updates the current `Theme`. |
| `ThemeSource` | App-implemented `Flow<Theme>` with the theme the user selected. |
| `ThemeDelegate` | Collects the `ThemeSource` and calls `ThemeManager.update` on every change. |

### Color schemes

Bind a `ColorSchemeThemeProvider` with the app's schemes; no subclass is needed:

```kotlin
single<ThemeProvider> {
    ColorSchemeThemeProvider(
        lightColorScheme = lightScheme,
        darkColorScheme = darkScheme,
    )
}
```

`Theme.isDarkTheme()` resolves `Theme.Device` with the system setting.

### Following the user's choice

`UIDIModule` binds `ThemeDelegate`. The app only binds a `ThemeSource` that maps its stored
setting to `Theme`, and calls the delegate from the ViewModel that lives as long as the UI:

```kotlin
factory<ThemeSource> {
    val observeSettingsUseCase: ObserveSettingsUseCase = get()
    ThemeSource {
        observeSettingsUseCase(ObserveSettingsUseCase.Params)
            .mapNotNull { result -> result.getValueOrNull()?.theme?.toTheme() }
    }
}
```

```kotlin
init {
    themeDelegate.observeTheme(viewModelScope)
}
```

Keep the app's own persisted theme model. `Theme` is not `@Serializable` on purpose: storing it
instead would change the polymorphic type name written to the stored JSON and break existing
settings.

### Migrating an app's own ThemeDelegate

Apps that had a local theme delegate must switch to the platform one. RadioSquare and
EuriborToday have a feature or landing `ThemeDelegate` with the same simple name, so:

1. Delete the app's local `ThemeDelegate` and its binding.
2. Bind a `ThemeSource` as shown above instead.
3. Change the `LandingViewModel` import to `com.raxdenstudios.platform.ui.theme.ThemeDelegate`.

An app that runs Koin `verify()` or `checkModules` must also bind a `ThemeSource`, or exclude
`ThemeDelegate` from the check. `UIDIModule` binds `ThemeDelegate`, and resolving it needs a
`ThemeSource`.
