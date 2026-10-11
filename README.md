# Compose Toolkit

[![Android CI](https://github.com/Slion/ComposeToolkit/actions/workflows/android.yml/badge.svg)](https://github.com/Slion/ComposeToolkit/actions/workflows/android.yml)

[Preference](https://developer.android.com/develop/ui/views/components/settings) screens for [Jetpack Compose](https://developer.android.com/jetpack/compose) [Material 3](https://developer.android.com/jetpack/compose/designsystems/material3).

This is not an officially supported Google product.

> **Android only.** This library targets the Android platform exclusively. It was forked from a
> multiplatform (iOS/JVM/JS/Wasm) project, but those targets have been dropped to keep the
> dependency surface small and the build fast.

## Preview

<p><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="32%" /> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="32%" /></p>

## Integration

This project is consumed as a [git submodule](https://git-scm.com/docs/git-submodule) rather than a published Maven artifact. Add the submodule, then wire it into your build as a [composite build](https://docs.gradle.org/current/userguide/composite_builds.html).

1. Add the submodule:

   ```sh
   git submodule add https://github.com/Slion/ComposeToolkit.git third_party/ComposeToolkit
   git submodule update --init --recursive
   ```

2. Include the submodule as a composite build and substitute the `:lib` module for the library coordinate, in your root `settings.gradle.kts`:

   ```kotlin
   includeBuild("third_party/ComposeToolkit") {
       dependencySubstitution {
           substitute(module("net.slions.compose.toolkit:lib")).using(project(":lib"))
       }
   }
   ```

3. Depend on the library in the modules that use it:

   ```kotlin
   implementation("net.slions.compose.toolkit:lib")
   ```

The composite build ensures the `:lib` module is always used in place of any published artifact.

## Core concepts

The library is built from four concepts (plus one for grouping headers):

| Concept | Type(s) | Role |
|---|---|---|
| **Catalog** | `Catalog` | The top-level composable: an adaptive (single/two-pane) host for a whole page tree, with search, back handling, and breadcrumbs. |
| **Page** | `Page` | A named, nestable content node (`subPages` forms a tree of any depth); its `content` block declares items. |
| **Item** | `Item`, `ItemCheckbox`, `ItemSwitch`, … | One interactive element in a page's list; reads/writes its state through the store via its `key`. |
| **Section** | `Section` | A non-interactive grouping header between items. |
| **Store** | `Store`, `MutableStore` | The persisted key/value data source behind items; exposed to the composition as `LocalStore`. |

In one line: *a Catalog hosts Pages; Pages declare Items; Items persist through the Store.*

A class-level overview of the public API is maintained in [`scripts/api-class-diagram.mmd`](scripts/api-class-diagram.mmd); render it with `python scripts/render_diagram.py` and open the produced HTML (drag classes to rearrange).

## Design

There is no official and complete Material 3 UX specification for preference yet, so the UX design of this library mainly comes from the following sources:

- [Material Design 3](https://m3.material.io/)
- [Settings design guidelines](https://developer.android.com/design/ui/mobile/guides/patterns/settings)
- [Android settings design guidelines](https://source.android.com/docs/core/settings/settings-guidelines)
- [AndroidX Preference](https://developer.android.com/jetpack/androidx/releases/preference)
- [AOSP Settings](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/)

## Usage

This library is designed with both extensibility and ease-of-use in mind.

Basic usage of this library involves invoking the `ProvidePreferenceLocals` composable, and then calling the `*Item` helper functions in a `LazyColumn` composable:

```kotlin
AppTheme {
    ProvidePreferenceLocals {
        // Other composables wrapping the LazyColumn ...
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemSwitch(
                key = "switch_preference",
                defaultValue = false,
                title = "Switch preference",
                icon = { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) },
                summary = { if (it) "On" else "Off" }
            )
        }
    }
}
```

### Built-in items

Built-in item types include:

- [`Item`](lib/src/main/kotlin/Item.kt)
- [`Section`](lib/src/main/kotlin/Section.kt)
- [`ItemCheckbox`](lib/src/main/kotlin/ItemCheckbox.kt)
- [`ItemList`](lib/src/main/kotlin/ItemList.kt) (supports both alert dialog and dropdown menu)
- [`ItemMultiSelectList`](lib/src/main/kotlin/ItemMultiSelectList.kt)
- [`ItemRadio`](lib/src/main/kotlin/ItemRadio.kt)
- [`ItemSlider`](lib/src/main/kotlin/ItemSlider.kt)
- [`ItemSwitch`](lib/src/main/kotlin/ItemSwitch.kt)
- [`ItemTextField`](lib/src/main/kotlin/ItemTextField.kt)
- [`ItemActionIconButton`](lib/src/main/kotlin/ItemActionIconButton.kt)
- [`ItemActionsSwitch`](lib/src/main/kotlin/ItemActionsSwitch.kt)

Each built-in item type includes 4 kinds of APIs:

1. A `LazyListScope.*Item` extension function, which is the easiest way to use preferences in this library, and helps developers to avoid boilerplates like having to specify the key twice for the `LazyColumn` and the `Item`.
2. A `LazyListScope.*Item` extension function, which is an overload of the first extension function but accepts `value` and `onValueChange` instead.
3. A `*Item` composable that takes a `MutableState`, which allows developers to bring in any kind of state they currently have.
4. A `*Item` composable that takes `value` and `onValueChange`, which allows developers to use the preference without a state and even in non-preference scenarios.

### Theming

The visual appearance of the items can be customized by providing a custom [`ItemTheme`](lib/src/main/kotlin/ItemTheme.kt) with `itemTheme` to `ProvideCatalogLocals` or `ProvideItemTheme`.

Customizable values in the theme include most dimensions, colors and text styles used by the built-in items.

### Data source

The data source of the items can be customized by providing a custom `MutableStateFlow<Store>` to `ProvideCatalogLocals` or `ProvideStore`.

The [`Store`](lib/src/main/kotlin/Store.kt) interface defined in this library is similar to the AndroidX DataStore [`Preferences`](https://developer.android.com/reference/kotlin/androidx/datastore/preferences/core/Preferences) class, but:

- It can be implemented by other mechanisms like [`SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences), thanks to being a public interface instead of an abstract class with only an internal constructor.
- It doesn't have to be produced and updated via a [`DataStore`](https://developer.android.com/reference/kotlin/androidx/datastore/core/DataStore).
- It doesn't mandate a fixed set of types that an implementation has to support, so that implementations have the flexibility to support much more or less types. The implementations within this library supports most of the types supported by `SharedPreferences` **except for `Long`** by default. You can opt in to `Long` support by setting `isDefaultStoreAndroidLongSupportEnabled` to `true`.

The default data source provided by this library (`createDefaultStore()`) is implemented with [`SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences), because:

- `SharedPreferences` is available as part of the Android framework, and doesn't require external dependencies like AndroidX DataStore which [bundles its own copy of `protobuf-lite`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:datastore/datastore-preferences-core/build.gradle;l=108;drc=9fd0cda7bb963d41fd25645b0761776caa830ed7).
- `SharedPreferences` can actually be [10x faster](https://stackoverflow.com/q/71601343) than AndroidX DataStore, likely due to its existing optimizations and simple threading and persistence model (XML is simple enough to be faster than Protobuf).
- `SharedPreferences` has a synchronous API, but it is actually async except for the first (un-cached) read, and allows in-memory value change without waiting for the disk write to complete, which is good for the preference use case.
- Existing users of `SharedPreferences` can use this library directly with the default data source.

**There should only be at most one invocation of `createDefaultStore()`**, similar to creating `DataStore` in AndroidX DataStore. It is also only for usage within a single process due to being backed by `SharedPreferences`.

If AndroidX DataStore is considered more appropriate for your use case, e.g. you need multi-process support, you can also create an AndroidX DataStore backed implementation that provides a `MutableStateFlow<Store>` on your own.

## Settings screen

[`Catalog`](lib/src/main/kotlin/Catalog.kt) hosts a whole settings tree in a single adaptive screen: a list pane of pages with a search field, and a detail pane showing the selected page's items.

```kotlin
Catalog(
    title = "Settings",
    pages = pages,
    onBack = { dismiss() },
)
```

- **Single-pane** (narrow window, e.g. a phone in portrait): selecting a page navigates to the detail pane; the top bar shows the page title with a back arrow, and system back pops the detail before dismissing the screen.
- **Two-pane** (wide window, e.g. a tablet or an unfolded foldable): both panes are visible at once as a 50/50 split.
- The **list pane** shows an MD3-style search pill. While a query is entered, the page list is replaced by the matching pages and preference entries; selecting a result clears the query, opens the page, and scrolls to (briefly highlights) the matched item.
- The **root of the tree** hosts regular items in addition to the page items: pass `rootContent` to `Catalog` to draw them below the top-level pages (any `LazyListScope` item builder works, as in a page's `content`; the items are searchable). Bump a page's `contentVersion` or the screen's `rootContentVersion` when items change at runtime so the search index is rebuilt.

The screen follows the [Material 3 adaptive](https://developer.android.com/develop/ui/compose/m3/adaptive) layout, so the same code adapts across postures. It also handles **half-folded** foldables (the two panes stay exactly half each via `HingePolicy.NeverAvoid`) and **medium-width** windows (the detail pane activates there, not only at the 720dp "Expanded" class).

### Nested pages

A page can declare `subPages`, forming a tree of any depth:

```kotlin
Page(
    id = "display",
    title = "Display",
    subPages = listOf(
        Page(id = "display_brightness", title = "Brightness") { /* ... */ },
    ),
) { /* ... */ }
```

In two-pane mode the current trail is shown as a breadcrumb above the panes (tapping an ancestor jumps back to it); in single-pane mode the back arrow pops one level at a time. Page ids must be unique within the whole tree.

The tree and search helpers live in [`Page.kt`](lib/src/main/kotlin/Page.kt): `walkPages()`, `findPage()`, `findPagePath()`, and `searchPages()`. Search covers the entire tree: [`buildSearchIndex(pages)`](lib/src/main/kotlin/SearchIndex.kt) walks every page's content (including all `subPages`) and records a searchable entry for each item, so `Catalog` can filter both pages and items and scroll to a match.

### Hosting in a bottom sheet or dialog

`Catalog` reads the window's adaptive info by default. A host that is smaller than the window — a modal bottom sheet, a dialog, a split — should measure its own size, build a `WindowAdaptiveInfo` for it (with no hinges, since the fold does not apply to the host), and pass it via `adaptiveInfo`, so the screen adapts to the host rather than to the window:

```kotlin
Catalog(
    title = "Settings",
    pages = pages,
    adaptiveInfo = hostAdaptiveInfo, // host-measured, no hinges
    singlePaneOnly = true,           // a sheet never splits into list + detail
)
```

Set `singlePaneOnly = true` for hosts that must never split into a list + detail (a bottom sheet, a dialog): the screen stays single-pane at any width, and the list navigates to the detail and back.

## Device-UI tests

The `sample` app doubles as the test fixture for the library's screen behavior. A small device-UI suite, built on the [AutoTest](https://github.com/Slion/AutoTest) framework, drives the installed sample over adb and asserts on the live UI hierarchy — nested navigation, breadcrumbs, bottom-sheet single-pane hosting, and search. See [`tools/autotest/`](tools/autotest/README.md) for setup and how to run it.

## Credits

Forked from [zhanghai/ComposePreference](https://github.com/zhanghai/ComposePreference).