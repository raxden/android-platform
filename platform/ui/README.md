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

---

## Components

Package `com.raxdenstudios.platform.ui.composable`. Every text is a `StringResource` and every image an
`ImageResource`. Previews live next to each component.

| Component | What it is |
|---|---|
| `TopBar.Back` | Top app bar with a back button tinted with `colors.navigationIconContentColor` (primary). `backIcon` picks the icon, `backButtonModifier` applies to the button only; `onNavigationIconClick` is required. |
| `TopBar.Default` | Top app bar with optional title and description, `actions` and a `navigationIcon` slot. |

Both top bars also take `windowInsets` (Material default). They expose no experimental Material3 types, so callers need no `@OptIn(ExperimentalMaterial3Api::class)`.
| `IconButton.Default` | Primary icon button for any action; `contentDescription` is required. |
| `IconButton.Back` / `Close` | Back (small `Icons.BackArrow` chevron by default) and close buttons described by the platform strings; both take `enabled`. |
| `IconButton.BackCircle` | 36dp primary circle with the back chevron, for content drawn under the top bar; takes `enabled`. |
| `Button.Default` / `Transparent` | Material button with a centred `TitleLarge` label; `Transparent` has no container nor elevation. |
| `Button.Primary` / `Secondary` | Full-width 56dp actions: bold filled, and outlined. `Primary` plays `appearShine` by default (`appearShine = true`); pass `false` to turn it off. |
| `Button.Card` | Selectable card with a primary icon over a label, announced as a radio button; takes `enabled`. |
| `Button.TextArrow` | Clickable `LabelMedium` followed by a right arrow ("See all"), announced as a button. |
| `BottomNavigationBar` + `BottomNavigationBarItem` | 64dp tab bar with a pill indicator, optional badge and a show/hide animation driven by `visible`. |
| `Modifier.selectableTab` | Tab semantics that stay clickable while selected, so UiAutomator keeps the test tag. |
| `EmptyState.Default` | Centred illustration (`EmptyStateDefaults.ImageSize`, 200dp, by default), bold title, description and optional tip. `tintVectors = true` tints vector images with the primary colour. |
| `Header.Section` / `Subsection` | Section title after a double arrow, with an optional `trailing` slot, or `trailingText` + `onTrailingClick` for a `TextArrow`; primary subheader. |

`modifier` always applies to the component itself, never to its label: callers can pass sizes, paddings and test
tags safely. Every button, icon button, `TopBar.Back` and `Header.Section` with a trailing arrow requires its click
callback.

### Icons

`com.raxdenstudios.platform.ui.theme.Icons` holds the images the components use:

| Icon | What it is |
|---|---|
| `BackArrow` | Small 8x13dp back chevron (drawable, mirrored in right-to-left layouts); default of `IconButton.Back`, `BackCircle` and `TopBar.Back`. |
| `ArrowBack` | Material arrow back (auto-mirrored); pass it as `icon`/`backIcon` for the Material look. |
| `Close` | Material close cross used by `IconButton.Close`. |
| `DoubleArrow` | Double chevron (drawable, mirrored in right-to-left layouts) at the start of `Header.Section`. |
| `KeyboardArrowRight` | Material right arrow (auto-mirrored) at the end of `Button.TextArrow`. |
| `ChevronLeft`, `ChevronRight`, `LocationOn` | Existing Material icons. |

### Press feedback

`Button.Default`, `Transparent`, `Primary`, `Secondary` and every `IconButton` read `LocalPressFeedback`;
`Button.Card` and `Button.TextArrow` ignore it. The default,
`PressFeedback.None`, adds no feedback. To shrink buttons while pressed and tap the haptics on every click, provide it
once at the theme root:

```kotlin
CompositionLocalProvider(
    LocalPressFeedback provides PressFeedback(
        buttonPressedScale = 0.96f,
        iconButtonPressedScale = 0.88f,
        hapticsEnabled = true,
    ),
) {
    content()
}
```

`PressFeedback` is a plain class with value equality, not a data class, so new fields can be added without
breaking binary compatibility: build a new instance instead of `copy`.

### Bottom navigation

The app maps its own tab model to items; the bar holds no state:

```kotlin
BottomNavigationBar(visible = model.isVisible) {
    model.items.forEach { item ->
        BottomNavigationBarItem(
            modifier = Modifier.testTag(item.testTag),
            label = item.label,
            icon = item.icon,
            selectedIcon = item.selectedIcon,
            selected = item == model.selectedItem,
            badge = item.badge,
            onClick = { onItemClick(item) },
        )
    }
}
```

`BottomNavigationBarDefaults` holds the sizes, the enter/exit transitions and the 250ms selection change spec. Pass
`indicatorSpacing`, `labelSpacing`, `colorAnimationSpec` and `indicatorAnimationSpec` to change the item layout and
motion. While the system has animations off, the bar fades in and out in 150ms instead of sliding and items snap
between states.

### Strings

| Key | English | Used by |
|---|---|---|
| `platform_ui_back` | Back | `IconButton.Back`, `IconButton.BackCircle`, `TopBar.Back` |
| `platform_ui_close` | Close | `IconButton.Close` |
| `platform_ui_tab_selected` | Selected | `Modifier.selectableTab` state description |

---

## Motion and haptics

| API | Package | What it is |
|---|---|---|
| `rememberReducedMotion()` | `motion` | True when the system turned animations off; previews always animate. Read once per composition, not observed live. |
| `isReducedMotion(context)` | `motion` | The same check outside composition (modifier nodes, navigation transitions), read when called. |
| `Modifier.pressScale(interactionSource, pressedScale, hapticOnPress, animationSpec)` | `motion` | Shrinks the element while pressed with a spring; still with reduced motion. |
| `Modifier.appearShine(shape)` | `motion` | A soft light sweeping once across the element after it appears; it replays on every attach (lazy list items, re-entered destinations) and is skipped with reduced motion. |
| `AppHaptics` / `rememberAppHaptics()` | `haptic` | `tap`, `softTap`, `tick`, `confirm`, `error`, `reject`, mapped to the best `HapticFeedbackConstants` per API level. |

Motion durations and springs of each app stay in the app; only the infrastructure is shared.
