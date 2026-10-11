# 012 — Row action rows: caller misuse, not an API defect

**Status:** resolved (callers) · **Priority:** P2 (optional guard) · **Area:**
callers (`demo`, azahar `SettingsPages`), optional toolkit guard

## Problem

The demo's **Others** rows ("Settings", "Bottom sheet") — and the azahar app's
**Reset to Default** settings row — rendered a two-target separator + trailing
chevron even though each has a **single** action and no second target. Tapping
the chevron navigated into an **empty placeholder page** (a dead end).

## Root cause

All three rows were built as a *placeholder page + click*:

```kotlin
item(page = Page(id = "...", title = "...", ...) {}, onClick = { ... })
```

`Item`'s documented rule `page != null && onClick != null` → *two-target row*
then fires: the widget slot renders `[separator][chevron]` that opens the empty
page, and `actionIcon` (if set) becomes the button glyph instead of a trailing
indicator. The placeholder page is also registered in the tree/search, polluting
the index with a page that is never meant to be opened.

The toolkit API is **not** at fault: an action row is a plain item
(`item(key, title, icon, onClick)`, optionally `actionIcon`) — exactly what the
app's *working* home Settings row already does. The buggy rows just wrapped their
title/icon in a pointless empty `Page` to get a row, which is what tripped the
derivation.

## Fix (applied)

Make each an action row — a plain `item` with no `page`:

- Demo `SamplePages` Others rows → `item(key, title, summary, icon,
  actionIcon = OpenInNew, onClick)`.
- Azahar `SettingsPages` Reset-to-Default → `item(key, title, icon, onClick)`
  (no `actionIcon`: it shows an in-place confirm dialog, not a new surface).

Result: single-action rows, no separator, no dead-end chevron; the placeholder
pages are gone from the tree and the search index.

## Optional hardening (defer)

- **Guard:** a debug `check`/`warn` for a page with no items (in
  `walkPageContent`), i.e. the "empty action-page" smell. Cheap to add later if
  the pattern recurs.
- **Doc:** a one-line KDoc note on `Item`/`item`: *"for an action row, do not
  pass a `page`; use a plain item (optionally with `actionIcon`)."*

## Decisions

- **D1 — No toolkit API change needed.** The plain-item pattern is the intended
  way to build an action row. The earlier idea of an explicit `itemTwoTarget` /
  `secondTargetIcon` parameter is **not** pursued.
- **D2 — `actionIcon` keeps its current behavior** (trailing indicator on
  plain/page rows; the two-target button glyph on `page + onClick` rows). The
  two-target case is now only the app's game rows.

## Done

- Demo Others + azahar Reset-to-Default are plain action rows: no separator, no
  dead-end chevron; no placeholder pages in the tree or search.
- Verified on hardware (demo + azahar, G8X / Duo): rows show the leading icon +
  (open-in-new) indicator only; tapping runs the single action.
