# Pane spacing (two-pane layout)

`Catalog`'s two-pane layout is built on material3-adaptive's
`ListDetailPaneScaffold`. The scaffold's directive carries a *partition spacer* — a fixed
gutter between the panes (`PaneScaffoldDirective.horizontalPartitionSpacerSize`). The
material3 recommended directives set it to **24dp** for every multi-pane width class, and
that spacer is the visible gap between the list pane and the detail pane.

## `CatalogOptions.paneGap`

The gutter is exposed as a tunable of `CatalogOptions`:

```kotlin
Catalog(
    title = "Settings",
    root = root,
    options = CatalogOptions(paneGap = 0.dp),
)
```

- **Default:** `24.dp` — the material3 recommended value; hosts that pass no options keep
  the stock look.
- **`0.dp`:** the panes sit flush against each other. Rows keep their own
  `horizontalSpacing` padding (from the preference theme), so content does not touch the
  seam.
- Any `Dp` in between works (e.g. `8.dp`) for a tighter but non-zero gutter.

The option only affects the two-pane layout: in single-pane mode there is no partition,
and `singlePaneOnly` hosts (sheets, dialogs) are untouched.

## Changing it at runtime

`paneGap` is just a `Dp` read from the options at composition time, so it can be driven by
any state: rebuild the `CatalogOptions` whenever the value changes and the scaffold
re-lays-out on the next frame. No navigation or activity recreation is involved.

The demo does exactly this:

- The value lives in the shared `SampleThemeValues` (persisted in the app's `Store` under
  `sample.theme.paneGap`), so it survives a relaunch.
- The Theme page surfaces it as a **Pane gap** slider (0–48dp, 4dp steps) under the
  *Spacing* section, alongside the other live theme controls.
- Both demo hosts — `SampleApp` (main activity) and `SettingsActivity` — build their
  `Catalog` from the same value, so dragging the slider in either activity updates both,
  live:

  ```kotlin
  options =
      CatalogOptions(
          paneGap = (themeValues.paneGapDp ?: DEFAULT_PANE_GAP_DP).dp,
      ),
  ```

## Trade-offs

- A non-zero gutter reads as a clearer separation between the two panes (the stock
  material3 look); the gap is filled by the screen's surface color.
- `0.dp` maximizes content width per pane — useful on medium-width windows where both
  panes are already narrow — at the cost of the two lists reading as one continuous
  surface.
