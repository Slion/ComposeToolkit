# Page transitions

When the page a pane shows changes — navigating into a sub-page, popping up, jumping via a
breadcrumb, or opening a page from a search result — the pane animates the swap instead of
switching instantly. Both panes animate, in single-pane and two-pane mode alike.

The transition is a `PageTransition`, set on `CatalogOptions`:

```kotlin
Catalog(
    title = "Settings",
    root = root,
    options = CatalogOptions(pageTransition = PageTransition.SlideFade),
)
```

## Direction

The animation direction is derived from the navigation, not chosen by the transition:

- **Forward** (navigating deeper into the tree): the incoming content enters from the
  right, the outgoing content exits to the left.
- **Backward** (popping up: back arrow, breadcrumb, system back): mirrored — the incoming
  content enters from the left.

A `PageTransition` only declares the *shape* of the motion; it receives the direction
(and the pane's width) and resolves the animation endpoints from them.

## Presets

| Preset | Motion |
| --- | --- |
| `PageTransition.None` | No animation — the content swaps instantly (the pre-transition behavior). |
| `PageTransition.Fade` | Cross-fade between the outgoing and incoming content. |
| `PageTransition.Slide` | Direction-aware horizontal slide (the classic list-detail push). **Default.** |
| `PageTransition.SlideFade` | The slide blended with a cross-fade (half opacity at the endpoints). |

The built-in presets run for `300ms` with `FastOutSlowInEasing`.

## Custom transitions

Any other motion can be supplied with `PageTransition.Custom`:

```kotlin
val halfSlide = PageTransition.Custom(
    durationMs = 240,
    easing = LinearOutSlowInEasing,
    transform = { direction, width ->
        val offset = width / 2 * (if (direction == PageDirection.Forward) 1f else -1f)
        PageTransitionEndpoints(offset, 0.25f, -offset, 0.25f)
    },
)
```

`transform` receives the navigation `PageDirection` and the pane's width, and returns a
`PageTransitionEndpoints`:

- `enterOffset` / `enterAlpha` — where the **incoming** content starts (it always ends at
  `0.dp` / `1f`);
- `exitOffset` / `exitAlpha` — where the **outgoing** content ends (it always starts at
  `0.dp` / `1f`);
- positive offsets are to the right.

## What stays visible

During the transition the outgoing content is drawn frozen on top of the pane's surface —
at the scroll position it had when the navigation happened — until the animation
finishes, so a pane never flickers empty and the two lists don't jump. The pane clips the
sliding content, so it never overflows onto the neighboring pane.

## Changing it at runtime

`pageTransition` is read from the options at composition time, so it can be driven by any
state: rebuild the `CatalogOptions` whenever the value changes. The demo does exactly
this — the Theme page surfaces a **Transition** row under the *Motion* section (persisted
in the app's `Store` under `sample.theme.pageTransition`), and both demo hosts build
their `Catalog` from the same value, so the change applies live.
