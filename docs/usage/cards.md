# Cards

A card in this library is a **style, not a container**: it is applied to an item
(`item` with `style`) or to the items of a `group` (`group` with `style`), and is never
built as a separate logical thing. Items without a card are drawn as plain rows.

## `CardStyle`

| Style | MD3 component | Notes |
| --- | --- | --- |
| `CardStyle.Filled` | `Card` | Tonal surface, no elevation. **Default for groups.** |
| `CardStyle.Elevated` | `ElevatedCard` | Surface with shadow. |
| `CardStyle.Outlined` | `OutlinedCard` | Surface with a 1dp outline. |
| `CardStyle.None` | — | No card: plain rows (a group keeps its spacing and clearance). |

## A group: several items, one card

`group` draws each item in its own card, with the **first item's top corners rounded, the
last item's bottom corners rounded, and the middle items square** — the visual impression
of a single card split into tappable rows:

```kotlin
Page(id = "display", title = "Display") {
    section(key = "display_general", title = "General")
    group(key = "display_general_group") {   // Filled, by default
        item(title = "Night light", summary = "Off", icon = { /* ... */ })
        item(title = "Auto brightness", icon = { /* ... */ })
    }
    // A different style for another group:
    group(key = "display_advanced", style = CardStyle.Elevated) {
        item(title = "Advanced display")
    }
}
```

The tunables of `group`:

- `itemSpacing` — the gap between the individual cards (default `4.dp`);
- `shape` — the card shape (default `MaterialTheme.shapes.medium`);
- `cardColor` — overrides the style's default container color;
- `outerPadding` — clearance between the group and the pane edges (default the theme's
  `horizontalSpacing` on all sides).

## A single carded row

`item` draws its own card when given a `style` (or `CardStyle.None`, which is a no-op):

```kotlin
item(
    title = "Card row",
    summary = "Static row drawn in a card.",
    style = CardStyle.Filled,
)
```

The same `shape` / `cardColor` / `cardElevation` / `cardBorder` / `outerPadding`
parameters as `group` apply, with the style-specific ones taking effect only for the
matching style (`cardElevation` only for `Elevated`, `cardBorder` only for `Outlined`).

### Preference builders

Every built-in preference builder (`itemList`, `itemSlider`, `itemColorList`,
`itemCheckbox`, `itemSwitch`, `itemTextField`, `itemMultiSelectList`, `itemRadio`,
`itemActionsSwitch`, `itemActionIconButton`, `itemActions`) accepts the same card
parameters. The card style is just another argument of the single call — the row's title,
summary and icon are never restated:

```kotlin
itemSlider(
    key = "tint",
    value = tint,
    onValueChange = { tint = it },
    title = "Tint",
    style = CardStyle.Filled,
)
```

A standalone carded row clears the pane edges horizontally and sits a small gap (2.dp)
above/below its neighbors, so consecutive carded rows read like a group's rows.

### Preference builders in a group

The same builders are also available on `GroupScope`: inside `group { }` a call to one
resolves to the group overload, and the row joins the group — drawn in the group's card,
with the group's `style`, `shape`, corner and spacing rules (the group overloads take no
`modifier` and no card parameters):

```kotlin
group(key = "display_general", style = CardStyle.Filled) {
    itemSwitch(key = "night_light", value = nightLight, onValueChange = { nightLight = it }, title = "Night light")
    itemSlider(key = "brightness", value = brightness, onValueChange = { brightness = it }, title = "Brightness")
}
```

Each row is still a single call: its title/summary/icon feed the search index through the
group's items.

## Custom content in a card

A group item (or a carded `item`) can draw **any composable** inside its card instead of
the standard row. The title/summary then describe the item for search indexing — the
content is composable, so it is not walked by the index:

```kotlin
group(key = "gp_content") {
    item(title = "Stateful row", summary = "A full switch row drawn as content.") {
        val state = rememberValue("gp_content_switch", true)
        ItemSwitch(
            state = state,
            title = "Stateful row",
            summary = "A full switch row drawn as content.",
        )
    }
    item(title = "Any layout") {
        Column(Modifier.padding(16.dp)) {
            Text("A card can hold any layout.")
        }
    }
}
```
