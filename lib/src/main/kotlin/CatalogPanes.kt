/*
 * Copyright 2026 Stéphane Lenclud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.toolkit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

/**
 * The breadcrumb that spans both panes in two-pane mode: the screen title, a back chevron,
 * and the current page's trail. The chevron behaves exactly like the system back ([onBack]:
 * one level up when nested two or deeper, [onBack] otherwise). Tapping an ancestor segment
 * pops the trail back to it; the last (current) segment is plain text.
 */
@Composable
internal fun BreadcrumbBar(
    title: String,
    currentPath: List<Page>,
    pagePath: List<String>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onNavigateToPath: (List<String>) -> Unit,
) {
    // currentPath is the shown page's trail below the root: nested once it is
    // non-empty.
    val nested = currentPath.isNotEmpty()
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (nested || showBackButton) {
                // Same behavior as the system back: the host's back action pops one level
                // up when nested two or deeper and leaves the screen otherwise. The chevron
                // must not pop a level-1 page to the root on its own: in two-pane the list
                // pane already shows the root's items, so a level-1 back exits the screen.
                IconButton(onClick = onBack) {
                    BackChevron()
                }
            }
            Text(
                text = title,
                modifier =
                    Modifier.padding(
                        start = if (nested || showBackButton) 0.dp else 16.dp,
                    ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // One chevron-separated segment per level of the current page's trail.
            currentPath.forEachIndexed { i, page ->
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(horizontal = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (i == currentPath.lastIndex) {
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = page.title,
                        modifier =
                            Modifier.clickable { onNavigateToPath(pagePath.take(i + 1)) },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The list pane: the in-list search pill and the parent page's items — its sub-page items
 * and its regular items, in content order (the search results while querying) — and, in
 * single-pane mode, the fixed 56dp title bar the list scrolls under. At the root level
 * the parent is the root page, whose items are the list's top level. Owns the pane's
 * [LazyListState] and its scroll-to-selection effects.
 *
 * The search pill's text field is kept out of the focus tree while [fieldFocusEnabled]
 * is false (launch and pane transitions), so the focus system never restores focus (and
 * the keyboard) to it mid-transition.
 */
@Composable
internal fun ListPane(
    title: String,
    isTwoPane: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    parentPage: Page,
    selectedPageId: String?,
    onSelectPage: (String) -> Unit,
    isSearching: Boolean,
    searchEntries: List<SearchEntry>,
    onSearchEntryClick: (SearchEntry) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    fieldFocusEnabled: Boolean,
    highlightedKey: String?,
    fadingEdges: Boolean,
    listScrollToIndex: Int?,
    onListScrollConsumed: () -> Unit,
    onActive: () -> Unit,
) {
    val listState = remember { LazyListState() }
    // Keep the list pane showing the page currently open in the detail: when the parent
    // page's items change (popping up the tree in two-pane, opening a page from a search
    // result), the pane's scroll offset would otherwise be left stale — e.g. scrolled
    // past the end of a new, shorter list, with the selected item out of view. Keyed on
    // the parent page's id (not on selectedPageId: a tap does not swap the set at the
    // root level), so only a navigation that changes the level re-triggers it.
    LaunchedEffect(parentPage.id, isSearching) {
        if (isSearching || listScrollToIndex != null) return@LaunchedEffect
        val sel = selectedPageId ?: return@LaunchedEffect
        // Item 0 is the search pill; the parent's items follow from index 1. The
        // selected item's position in the parent's lazy list comes from the walk.
        val index =
            parentPage.structure.subPages.firstOrNull { it.page.id == sel }?.index
                ?: return@LaunchedEffect
        // Item 0 is the search pill; the parent's items follow from index 1.
        val target = 1 + index
        // The list is recomposed with the new items asynchronously; wait until the target
        // item exists before scrolling to it (same pattern as the detail pane).
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > target }
        // No-op when the item is already on screen, so a restart never fights a scroll
        // the user is in the middle of.
        if (listState.layoutInfo.visibleItemsInfo.any { it.index == target }) {
            return@LaunchedEffect
        }
        listState.animateScrollToItem(target)
    }
    // Scroll to a root item a search result selected, once it is laid out.
    LaunchedEffect(listScrollToIndex) {
        val target = listScrollToIndex ?: return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > target }
        listState.animateScrollToItem(target)
        onListScrollConsumed()
    }

    val edge = if (fadingEdges) PANE_FADING_EDGE_LENGTH else 0.dp
    // Items highlight on a search selection, and page items navigate via the callback
    // provided here: the items are composed within this scope, even though the
    // registration itself (the lazy list scope) is not a composable context.
    CompositionLocalProvider(
        LocalHighlightedItemKey provides highlightedKey,
        LocalOnSelectPage provides onSelectPage,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.isFocused) onActive() },
        ) {
            LazyColumn(
                state = listState,
                // In single-pane the fixed 56dp header overlays the top of the list, so
                // the top fade is offset down below it (and the content padded past it);
                // in two-pane the fade sits at the very top, just below the breadcrumb.
                modifier =
                    if (fadingEdges) {
                        Modifier
                            .fillMaxSize()
                            .paneFadingEdges(topOffset = if (isTwoPane) 0.dp else 56.dp)
                    } else {
                        Modifier.fillMaxSize()
                    },
                // Inset the content past the static fading edges so items scroll under
                // the fades instead of ending right at the pane boundary; the fixed
                // single-pane header bar adds to the top inset.
                contentPadding =
                    PaddingValues(
                        top =
                            if (isTwoPane) {
                                edge
                            } else {
                                56.dp + edge
                            },
                        bottom = edge,
                    ),
            ) {
                // The search pill: an in-list item at the top of the list pane.
                // In single-pane it sits below the fixed 56dp header bar (accounted for
                // in the content padding); in two-pane a little breathing room below
                // the breadcrumb.
                item {
                    SearchPill(
                        isTwoPane = isTwoPane,
                        query = query,
                        onQueryChange = onQueryChange,
                        focusEnabled = fieldFocusEnabled,
                    )
                }
                if (isSearching) {
                    items(searchEntries, key = { it.id }) { entry ->
                        SearchEntryItem(
                            // An entry match shows the preference's own title with the
                            // page's trail as its subtitle; a page match shows the
                            // page's title, and its parent's trail (or summary) as
                            // subtitle.
                            entry = entry.entry,
                            page = entry.page,
                            path = entry.path,
                            icon = entry.icon,
                            onClick = { onSearchEntryClick(entry) },
                        )
                    }
                    if (searchEntries.isEmpty()) {
                        item {
                            Text(
                                text = "No matching pages",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    // The parent page's items: its sub-page items and its regular items,
                    // in content order. Each sub-page item is its own lazy item — not one
                    // group item — so the pane can scroll to the selected item (e.g. after
                    // popping up the tree in two-pane, when the item set changes).
                    GroupKeys.reset()
                    parentPage.content(this)
                }
            }
            // The single persistent header: a fixed 56dp bar at the top that never
            // vanishes and never changes — just the title at a single size. The search
            // lives in the in-list pill, which simply scrolls behind the bar. In
            // two-pane mode it is not shown at all: the breadcrumb above the panes
            // carries the title, and the search pill is a plain in-list item there.
            if (!isTwoPane) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .height(56.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    if (showBackButton) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onBack) {
                                BackChevron()
                            }
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(
                            text = title,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The MD3-style search pill, an in-list item at the top of the list pane. Owns the text
 * field and its focus state; [query] mirrors the field's text in both directions (typing
 * updates the query; a query change from outside — e.g. a result selection clearing it —
 * empties the field).
 *
 * While [focusEnabled] is false the field is kept out of the focus tree entirely, so
 * the focus system does not restore focus (and show the keyboard) on it during a pane
 * transition.
 */
@Composable
private fun SearchPill(
    isTwoPane: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    focusEnabled: Boolean,
) {
    val textFieldState = rememberTextFieldState()
    // The query as seen by the long-lived collector below: a plain closure would capture
    // the value from the composition that started the effect, so read it live.
    val currentQuery by rememberUpdatedState(query)
    // Field -> query: the field is the source of truth while typing. The collector must
    // NOT restart when the query changes — restarting it mid-IME-commit would re-emit a
    // stale text and race the IME's composing region (the "last character keeps being
    // added and removed" flicker).
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collect { text ->
            if (text != currentQuery) onQueryChange(text)
        }
    }
    // Query -> field, one case only: the query is cleared from outside (after a search
    // result is selected or the clear icon is tapped). Never rewrite the field while it
    // equals the query or the IME is composing — that destroys the composing region and
    // the IME re-sends the text, looping.
    LaunchedEffect(query) {
        if (query.isEmpty() && textFieldState.text.isNotEmpty()) {
            textFieldState.edit { replace(0, length, "") }
        }
    }
    val focusManager = LocalFocusManager.current
    var fieldFocused by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        if (isTwoPane) {
            Spacer(Modifier.height(8.dp))
        }
        // The results are shown inline in this pane, so a plain text field is used
        // instead of the state-based SearchBarDefaults.InputField, whose touch-mode
        // focus coupling would force-expand the bar.
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = LocalItemTheme.current.horizontalSpacing),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    // A fixed-width tappable slot; the icon is centered in it, so its
                    // edge lands 16.dp from the pill edge (8.dp pill padding + 8.dp
                    // within the slot).
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (fieldFocused) {
                                    Modifier.clickable {
                                        focusManager.clearFocus(force = true)
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (fieldFocused) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Dismiss search",
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                    }
                    BasicTextField(
                        state = textFieldState,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (focusEnabled) {
                                        Modifier
                                    } else {
                                        Modifier.focusProperties { canFocus = false }
                                    },
                                )
                                .onFocusChanged { fieldFocused = it.isFocused },
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                        )
                    }
                }
            }
        }
    }
}

/**
 * The detail pane: the selected page's items — its sub-page items and its regular items,
 * in content order — and, in single-pane mode, the fixed 56dp compact bar (back arrow +
 * title) the list scrolls under. Owns the pane's [LazyListState] (shared across pages,
 * so a page change starts at the top) and the scroll-to-item effect for a selected search
 * result.
 */
@Composable
internal fun DetailPane(
    page: Page?,
    selectedPageId: String?,
    onSelectPage: (String) -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    highlightedKey: String?,
    fadingEdges: Boolean,
    scrollToIndex: Int?,
    onDetailScrollConsumed: () -> Unit,
    onActive: () -> Unit,
) {
    if (page == null) return
    val detailState = rememberLazyListState()
    LaunchedEffect(page.id) {
        detailState.scrollToItem(0)
    }
    // Scroll to the item a search result navigated to, once the page's list is composed
    // (the detail pane is composed asynchronously, so wait for it to be laid out with
    // enough items first).
    LaunchedEffect(page.id, scrollToIndex) {
        val target = scrollToIndex ?: return@LaunchedEffect
        snapshotFlow { detailState.layoutInfo.totalItemsCount }
            .first { it > target }
        detailState.animateScrollToItem(target)
        onDetailScrollConsumed()
    }
    val edge = if (fadingEdges) PANE_FADING_EDGE_LENGTH else 0.dp
    // Items highlight on a search selection, and page items navigate via the callback
    // provided here: the items are composed within this scope, even though the
    // registration itself (the lazy list scope) is not a composable context.
    CompositionLocalProvider(
        LocalHighlightedItemKey provides highlightedKey,
        LocalOnSelectPage provides onSelectPage,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.isFocused) onActive() },
        ) {
            LazyColumn(
                state = detailState,
                // In single-pane the compact bar overlays the top of the list, so the
                // top fade is offset down below it; in two-pane there is no bar, so the
                // fade sits at the very top, just below the breadcrumb.
                modifier =
                    if (fadingEdges) {
                        Modifier
                            .fillMaxSize()
                            .paneFadingEdges(topOffset = if (showBack) 56.dp else 0.dp)
                    } else {
                        Modifier.fillMaxSize()
                    },
                // The compact bar overlays the top of the list and is always visible in
                // single-pane (where the back arrow lives); inset the first item by the
                // bar's height so the page's first item — e.g. its top category header —
                // isn't hidden beneath it. The top and bottom insets also keep the
                // content clear of the static fading edges so items scroll under them.
                contentPadding =
                    PaddingValues(
                        top =
                            if (showBack) {
                                56.dp + edge
                            } else {
                                edge
                            },
                        bottom = edge,
                    ),
            ) {
                // The page's items: its sub-page items and its regular items, in content
                // order; tapping a sub-page item navigates deeper into the tree (the
                // list pane switches to that page's items).
                GroupKeys.reset()
                page.content(this)
            }
            // The compact bar: declared after the list so it draws above the content.
            // Static — always shown in full whenever the detail page is open in
            // single-pane, with the content scrolling under it (the pane's top fade
            // provides the separation). In two-pane mode it is never shown: the
            // breadcrumb above the panes already names the page, so the bar would only
            // duplicate it.
            if (showBack) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .height(56.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            BackChevron()
                        }
                        Text(
                            text = page.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One search result item, styled as an MD3 [ListItem] (as in the material3 search bar
 * samples): a leading search icon, the entry's title, and a supporting line naming where
 * the page lives in the tree. A page-level match (no [entry]) shows the page's own title
 * in the headline, so the supporting line is the trail of the page's *parent* (the
 * trail minus the page's own title) — pages that share a title with a page of another
 * branch (e.g. hosted from two places) stay distinguishable. A top-level page has no
 * parent; its summary is shown instead.
 */
@Composable
private fun SearchEntryItem(
    entry: SearchIndexEntry?,
    page: Page?,
    path: String,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = entry?.title ?: page?.title.orEmpty()) },
        supportingContent = {
            val supporting =
                if (entry != null) {
                    // The entry lives in [page]; the page's trail (e.g. "Theme > Colors")
                    // shows where. Top-level pages have a one-segment trail, equal to
                    // their own title — show just it.
                    path
                } else {
                    // A page match names the page in the headline, so the supporting
                    // line drops the trail's last segment (the page's own title):
                    // "Group > Switch" -> "Group". A top-level page has no parent
                    // segment left, so its summary is shown instead.
                    val parentTrail = path.substringBeforeLast(" > ")
                    if (parentTrail == path) page?.summary else parentTrail
                }
            if (!supporting.isNullOrEmpty()) {
                Text(text = supporting)
            }
        },
        leadingContent = {
            // The item's own leading icon (the page item's for a page match), so a
            // result looks like its item; the search icon for items without one.
            // Vertical placement is material3's: centered, or top-aligned for an
            // item whose supporting text wraps to several lines (a three-line
            // list item).
            val itemIcon = entry?.icon ?: icon ?: page?.icon
            if (itemIcon != null) {
                itemIcon()
            } else {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

/**
 * A left-pointing chevron used as the back affordance, matching the chevron style of the
 * breadcrumb separators (a right chevron rotated 180°) rather than the filled arrow.
 */
@Composable
private fun BackChevron() {
    Icon(
        imageVector = Icons.Filled.ChevronRight,
        contentDescription = "Back",
        modifier = Modifier.size(24.dp).rotate(180f),
    )
}

/** Length of the gradient that fades out the pane content at its top and bottom edges. */
private val PANE_FADING_EDGE_LENGTH = 32.dp

/**
 * Static fading edges for a vertical scroll area: short surface-colored gradients at the
 * top and bottom of the pane that are always present. The panes pair this with vertical
 * [androidx.compose.foundation.layout.PaddingValues] of the same length on their
 * [LazyColumn], so the content scrolls under the fades instead of the fades popping in
 * and out with the scroll position.
 *
 * [topOffset] pushes the top fade down below an opaque bar that overlays the top of the
 * list in single-pane mode (the fixed list header, or the detail compact bar); without it
 * the fade would be painted behind the bar and never visible. In two-pane there is no
 * such overlay, so it stays 0.
 */
@Composable
private fun Modifier.paneFadingEdges(topOffset: Dp = 0.dp): Modifier {
    // Captured in composition (theme/density are composable reads), then used from the
    // non-composable draw lambda. The whole modifier is remembered so the pane's
    // [LazyColumn] sees a stable modifier instance across recompositions (a fresh
    // drawWithContent each time would defeat the lazy list's skip). The brushes are
    // built in the draw cache, not on every draw pass.
    val base = this
    val surface = MaterialTheme.colorScheme.surface
    val edgePx = with(LocalDensity.current) { PANE_FADING_EDGE_LENGTH.toPx() }
    val topOffsetPx = with(LocalDensity.current) { topOffset.toPx() }
    return remember(base, surface, edgePx, topOffsetPx) {
        base.drawWithCache {
            // Gradient coordinates are in canvas space, so the bottom brush is built from
            // the cache's size (the cache is invalidated when it changes).
            val topBrush =
                Brush.verticalGradient(
                    colors = listOf(surface, surface.copy(alpha = 0f)),
                    startY = topOffsetPx,
                    endY = topOffsetPx + edgePx,
                )
            val bottomBrush =
                Brush.verticalGradient(
                    colors = listOf(surface.copy(alpha = 0f), surface),
                    startY = size.height - edgePx,
                    endY = size.height,
                )
            onDrawWithContent {
                drawContent()
                // Top edge, starting below any overlaying bar.
                drawRect(
                    brush = topBrush,
                    topLeft = Offset(0f, topOffsetPx),
                    size = Size(size.width, edgePx),
                )
                // Bottom edge.
                drawRect(
                    brush = bottomBrush,
                    topLeft = Offset(0f, size.height - edgePx),
                    size = Size(size.width, edgePx),
                )
            }
        }
    }
}
