# 009 — P1: Vocabulary & naming (library concepts, class renames)

**Status:** renames done 2026-10-09 (build + 28/28 tests green, 7/7 device tests
on A22 + G8X, sample pages renamed, `ThemeComponents.kt` moved to `:sample`,
diagram regenerated, README has a **Core concepts** section documenting
Catalog/Page/Item/Section/Store). Remaining: **package rename**
`net.slions.compose.preference` → `net.slions.compose.catalog` (deferred per
decision).

Reviewed against the public API diagram
(`scripts/api-class-diagram.mmd`, rendered by `scripts/render_diagram.py`).

## Concepts

The library is really four concepts; the current names blur them:

| Concept | Meaning | Current names |
|---|---|---|
| Catalog | top-level host composable (panes, search, back) | `PreferencePageScreen` |
| Page | named, nestable content node (`subPages` tree) | `PreferencePage` |
| Item | one interactive element in a page's list | `*Preference` (14 types) |
| Store | persisted key/value state behind items | `Preferences`, `PreferenceFlow`, `rememberPreferenceState` |

## Problems

1. **`Preference` has three meanings** — row widget, library prefix, and the state
   store. `Preferences` (a typed map) reads as "the settings" to most people;
   `PreferenceFlow` collides with Compose *navigation* vocabulary for what is a
   `StateFlow`-backed store — while `rememberPreferenceState` already names the same
   concept from the other side. Two names, one concept, one of them misleading.
2. **`PreferencePageScreen`** — "page screen" is a tautology; `PreferencePage` becomes
   wrong if the library ever hosts non-settings pages.
3. **`PreferenceCategory`** collides with androidx.preference, where that type is a
   *container*; ours is a *header row*.
4. **Sample code in the library**: `LiveSliderPreference`, `ColorPreference`,
   `AccentColorOption` (theme samples) live in `:preference`; they belong in `:sample`.
5. **`TwoTarget*`** (3 types) — "target" is undefined jargon.

## Proposed vocabulary (decided 2026-10-09)

Four neutral concepts: **Catalog** (top-level host composable), **Page** (named,
nestable content node), **Item** (interactive element, `Item*` prefix), **Store**
(persisted key/value state). No flavor prefix — the package
(`net.slions.compose.preference`) carries the flavor, keeping names neutral for
non-settings content. "Row" rejected (layout may become lines later).

| Now | Proposed | Note |
|---|---|---|
| `PreferencePageScreen` | `Catalog` | top-level host composable |
| `PreferencePageScreenOptions` | `CatalogOptions` | |
| `PreferencePage` | `Page` | |
| `BasicPreference` | `Item` | the generic item: `Item(title, key)` |
| `CheckboxPreference` | `ItemCheckbox` | `Item*` prefix family |
| `SwitchPreference` | `ItemSwitch` | |
| `RadioButtonPreference` | `ItemRadio` | |
| `SliderPreference` | `ItemSlider` | |
| `TextFieldPreference` | `ItemTextField` | |
| `ListPreference` | `ItemList` | |
| `MultiSelectListPreference` | `ItemMultiSelectList` | |
| `FooterPreference` | `ItemFooter` | |
| `TwoTarget*` (3 types) | `ItemActions*` | row tappable + extra action in the widget slot (M3 `ListItem.actions` vocabulary) |
| `twoTargetPreference` / `TwoTargetPreference` | `itemActions` / `ItemActions` | `actions: @Composable RowScope.() -> Unit` slot |
| `twoTargetIconButtonPreference` / `TwoTargetIconButtonPreference` | `itemActionIconButton` / `ItemActionIconButton` | |
| `twoTargetSwitchPreference` / `TwoTargetSwitchPreference` | `itemActionsSwitch` / `ItemActionsSwitch` | toggle + tap-to-settings pattern |
| `PreferenceCategory` | `Section` | non-interactive header — no `Item` prefix |
| `Preferences` / `MutablePreferences` | `Store` / `MutableStore` | |
| `MapPreferences` / `MutableMapPreferences` | `MapStore` / `MutableMapStore` | |
| `PreferenceFlow` + `create*PreferenceFlow` + `LocalPreferenceFlow` + `ProvidePreferenceFlow` | **delete as a concept** | `LocalStore` instead |
| `rememberPreferenceState` | `rememberValue(key)` | one per-key store handle |
| package `net.slions.compose.preference` | `net.slions.compose.catalog` | `:preference` module dir may stay; rename package + test packages |
| `PreferenceTheme` / `preferenceTheme` | `ItemTheme` / `itemTheme` (+`LocalItemTheme`, `ProvideItemTheme`) | superseded 2026-10-11: what is styled is the Item; `ProvidePreferenceLocals` → `ProvideCatalogLocals` as well; "row" wording in docs → "item" |
| `PreferenceCard*` (6 types) | `Card*` | `CardItem` now consistent with the Item concept |
| `searchPreferencePages` | `searchPages` | `walkPages`/`findPage*` already fine |
| `LiveSliderPreference`, `ColorPreference`, `AccentColorOption` | **move to `:sample`** | not library API |

Internal names (`ListPane`, `DetailPane`, `SearchPill`, `BreadcrumbBar`,
`SearchEntry`, `SearchIndexEntry`, `PageMatch`, panes in
`PreferencePageScreenPanes.kt`) are already clean — no change.

## Scope

~25 public renames across `:preference` + `:sample` + `:benchmark` + unit tests;
all mechanical (no behavior change). Binary compatibility is not a constraint.
Re-render the API diagram afterwards (`scripts/render_diagram.py`).

## Decisions (all resolved 2026-10-09)

1. ~~Flavor prefix~~ — neutral, package carries the flavor.
2. ~~Item term~~ — `Item*` prefix family; `Section` for the header.
3. **`TwoTarget*`** — the row is tappable (target 1) **and** carries a second
   interactive element in the widget slot (target 2: icon button / switch /
   arbitrary composable). Renamed to `ItemActions*` after M3 `ListItem.actions`.
4. **Package** — `net.slions.compose.catalog` (keep the `compose` segment).
5. **Per-key store handle** — `rememberValue(key)`.
