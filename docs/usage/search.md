# Search

`Catalog` searches the page tree from a right-aligned **search** affordance in the title
bar. Tapping it opens a full-screen overlay (it slides over the panes and covers them);
there is no search field inside a pane.

## Where the affordance is

The search icon lives in whichever title bar is on screen:

- **Two-pane:** in the breadcrumb bar that spans the top of both panes.
- **Single-pane, at the root level:** in the list pane's fixed header.
- **Single-pane, on a sub-page:** in the detail pane's compact bar (back chevron + title).
  A sub-page is shown in the detail pane in single-pane mode, so its compact bar is the
  only title bar on screen — this is how you reach search on a sub-page.

## The overlay

The overlay is a full-width search field in a fixed top bar (a back chevron on the left, a
clear button on the right once a query is entered), with the matching items listed below.
When it opens, the field takes focus and the software keyboard shows; the placeholder
names the page being searched (e.g. *Search Theme* on a sub-page, *Search Settings* at
the root level) so the scope is clear, until a query is typed.

## Scope: the current page and below

Search is scoped to the page the user is currently viewing — **that page and everything
below it**, not the whole tree:

- At the **root level** there is no open page, so the scope is the **whole tree**.
- In **single-pane**, on a sub-page only that page and its descendants are searched.
- In **two-pane**, the scope is the page of the **list pane (the left panel)**: a page
  open in the detail does not narrow the scope.

Each result still records its full trail from the screen root, so selecting a result can
navigate through the whole tree even though it was found in a scoped search.

## Results

A search matches **pages** (by title or summary) and **preference entries** (by title or
summary), case-insensitively. The results are ordered by relevance, then depth, then tree
order:

| Rank | Match |
| --- | --- |
| 0 | A page whose title **exactly** equals the query. |
| 1 | A page whose title **contains** the query. |
| 2 | A preference entry whose title **contains** the query. |
| 3 | A match that only hit a **summary** (a page's or an entry's). |

Within a rank the shallower page (closer to the root) comes first; tree order breaks the
last tie.

- A **page** result shows the page item (its icon, and its trail as the subtitle).
- An **entry** result shows the entry's title with the page's trail as its subtitle.
- The root page's items use the screen title as their path (the root is the screen
  itself, so it has no trail segment).
- An empty query shows a *Search items* hint; a query with no hits shows *No matching
  items*.

## Selecting a result

Tapping a result closes the overlay, clears the query, and hides the keyboard, then
locates the item:

- **Page-level match** ("search locates, it doesn't navigate"): the page's item is
  focused in the list pane — it scrolls into view and is highlighted briefly — rather than
  opening the page. (A match on the root page itself has no item to focus.)
- **Entry match**: the entry's page is opened if it is not already the one on screen (a
  page hosted from several places is opened through the trail the result was found
  under), then the entry is scrolled into view and highlighted. If the entry's page is the
  one the list pane already shows, the list is simply scrolled to the entry.

The matched item pulses and fades back out over ~3.6s (long enough for the highlight
animation to finish before the state is cleared).

## Dismissing

The overlay is closed by its **back chevron** or the **system back** button. The search
back handler is checked before the screen's own back handling, so back first dismisses the
overlay, then (on a second press) pops the page or leaves the screen as normal.

## Feeding the index

Every item registered in a page's `content` is searchable, as are the root page's regular
items. When a page's items change at runtime without a new `Page`, bump that page's
`contentVersion` so the index is rebuilt.

An item's **icon** in a search result comes from the `icon` parameter of its `LazyListScope`
builder. The specific builders (`itemSwitch`, `itemList`, …) record it automatically; but
when a generic `item(…)` draws custom content whose row carries its own icon
(`item(title = …) { MyRow(icon = …) }`), the index walk never sees the row's icon — pass
the icon to the builder too, or the result falls back to the page's icon.

## Searching without the UI

The tree and search helpers are public in [`Page.kt`](../../lib/src/main/kotlin/Page.kt)
and [`SearchIndex.kt`](../../lib/src/main/kotlin/SearchIndex.kt): `buildSearchIndex(root)`
walks every page's content (including all `subPages`) into an index, and
`searchPages(root, index, query)` filters it. These are the same functions the overlay
drives, so they can be reused for a custom search surface.
