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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    onSearchClick: () -> Unit,
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
            // Right-aligned search affordance: opens the full-screen search overlay.
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Draws the content of the page a pane shows, animating the swap when [targetId]
 * changes: the outgoing page's content ([exitingContent]) animates out while the
 * incoming one ([enteringContent]) animates in, per [transition]. [direction] decides
 * which side the content comes from (right when navigating deeper, left when popping
 * up). A null [targetId] draws nothing (and animates the outgoing content out, e.g.
 * when the detail pane closes in single-pane).
 *
 * The outgoing content is drawn frozen on top of the pane's surface until the
 * transition finishes, so the swap never shows an empty pane mid-animation. The box
 * clips the sliding content, so it never overflows onto the neighboring pane.
 */
@Composable
internal fun AnimatedPageContent(
    targetId: String?,
    direction: PageDirection,
    transition: PageTransition,
    enteringContent: @Composable () -> Unit,
    exitingContent: @Composable (id: String) -> Unit,
) {
    // No animation: draw the content directly (and nothing when there is none).
    if (transition is PageTransition.None) {
        if (targetId != null) enteringContent()
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val width = maxWidth
        // The id fully shown (null = none); [exitingId] holds the outgoing page until
        // the transition finishes.
        var shownId by remember { mutableStateOf(targetId) }
        var exitingId by remember { mutableStateOf<String?>(null) }
        var currentDirection by remember { mutableStateOf(direction) }
        val progress = remember { Animatable(1f) }

        LaunchedEffect(targetId) {
            if (targetId == shownId) return@LaunchedEffect
            currentDirection = direction
            val old = shownId
            shownId = targetId
            exitingId = old
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(transition.durationMs, easing = transition.easing),
            )
            exitingId = null
        }

        val p = progress.value
        val endpoints = transition.endpoints(currentDirection, width)
        // Incoming: from (enterOffset, enterAlpha) to (0, 1); outgoing: from (0, 1) to
        // (exitOffset, exitAlpha).
        val enterOffset = endpoints.enterOffset * (1f - p)
        val enterAlpha = endpoints.enterAlpha + (1f - endpoints.enterAlpha) * p
        val exitOffset = endpoints.exitOffset * p
        val exitAlpha = 1f + (endpoints.exitAlpha - 1f) * p

        // The outgoing content first, so the incoming one draws on top of it.
        exitingId?.let { id ->
            Box(
                Modifier
                    .fillMaxSize()
                    .offset(x = exitOffset)
                    .alpha(exitAlpha),
            ) {
                exitingContent(id)
            }
        }
        if (targetId != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .offset(x = enterOffset)
                    .alpha(enterAlpha),
            ) {
                enteringContent()
            }
        }
    }
}

/**
 * The list pane: the parent page's items — its sub-page items and its regular items, in
 * content order — and, in single-pane mode, the fixed 56dp title bar the list scrolls
 * under. At the root level the parent is the root page, whose items are the list's top
 * level. Each level's list uses its own [LazyListState] from [scrollStates] (owned by
 * the screen), so a level always comes back at the scroll position it was left at; the
 * first time a level is shown, the list scrolls to the selected page's item instead.
 *
 * The single-pane title bar carries a right-aligned search affordance ([onSearchClick]);
 * search itself is a full-screen overlay hosted by the screen, not in the pane.
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
    highlightedKey: String?,
    fadingEdges: Boolean,
    listScrollToIndex: Int?,
    onListScrollConsumed: () -> Unit,
    onActive: () -> Unit,
    pageTransition: PageTransition,
    pageDirection: PageDirection,
    pageFor: (String) -> Page?,
    scrollStates: MutableMap<String, LazyListState>,
    visitedLevels: MutableSet<String>,
    onSearchClick: () -> Unit,
) {
    // The list of this level: each level keeps its own state in [scrollStates] (owned by
    // the screen, so it survives the pane leaving the composition in single-pane mode).
    // Composing the level's list with its state restores the level's scroll position when
    // the user comes back (clamped when the level's items shrank); a level never shown
    // before starts at the top.
    val listState =
        remember(parentPage.id) { scrollStates.getOrPut(parentPage.id) { LazyListState() } }
    // First visit to this level: keep the list pane showing the page currently open in
    // the detail — without this, the offset would be left stale (e.g. scrolled past the
    // end of a shorter list, with the selected item out of view). On a revisit the level's
    // state already holds the position the user left it at, so leave it alone. (The level
    // is marked visited even when a search result selected an item in it: that scroll
    // then owns the position.)
    LaunchedEffect(parentPage.id) {
        val firstVisit = visitedLevels.add(parentPage.id)
        if (!firstVisit || listScrollToIndex != null) return@LaunchedEffect
        val sel = selectedPageId ?: return@LaunchedEffect
        // The selected item's position in the level's lazy list comes from the walk.
        val target =
            parentPage.structure.subPages.firstOrNull { it.page.id == sel }?.index
                ?: return@LaunchedEffect
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
    // registration itself (the lazy list scope) is not a composable context. The selected
    // page id lets the item of the page open in the detail pane highlight itself (visible
    // in the two-pane layout, where both panes are on screen at once).
    CompositionLocalProvider(
        LocalHighlightedItemKey provides highlightedKey,
        LocalOnSelectPage provides onSelectPage,
        LocalSelectedPageId provides selectedPageId,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.isFocused) onActive() },
        ) {
            AnimatedPageContent(
                targetId = parentPage.id,
                direction = pageDirection,
                transition = pageTransition,
                enteringContent = {
                    ListContent(
                        state = listState,
                        parentPage = parentPage,
                        isTwoPane = isTwoPane,
                        fadingEdges = fadingEdges,
                        edge = edge,
                    )
                },
            ) { id ->
                // The outgoing level's items, frozen while they leave the pane, in the
                // level's own state — so they stay at the level's scroll position.
                val page = pageFor(id) ?: return@AnimatedPageContent
                ListContent(
                    state = scrollStates[id] ?: LazyListState(),
                    parentPage = page,
                    isTwoPane = isTwoPane,
                    fadingEdges = fadingEdges,
                    edge = edge,
                )
            }
            // The single-pane header: a fixed 56dp bar at the top (a back chevron when
            // [showBackButton] is on, then the title, then a right-aligned search
            // affordance). In two-pane mode it is not shown at all: the breadcrumb above
            // the panes carries the title and its own search affordance.
            if (!isTwoPane) {
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
                                .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showBackButton) {
                            IconButton(onClick = onBack) {
                                BackChevron()
                            }
                        }
                        Text(
                            text = title,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .then(
                                        if (showBackButton) {
                                            Modifier
                                        } else {
                                            Modifier.padding(start = 16.dp)
                                        },
                                    ),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // Right-aligned search affordance: opens the full-screen search
                        // overlay.
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The list pane's scrollable content: [parentPage]'s items — its sub-page items and its
 * regular items, in content order. Each sub-page item is its own lazy item, so the pane
 * can scroll to the selected item (e.g. after popping up the tree in two-pane, when the
 * item set changes).
 */
@Composable
private fun ListContent(
    state: LazyListState,
    parentPage: Page,
    isTwoPane: Boolean,
    fadingEdges: Boolean,
    edge: Dp,
) {
    LazyColumn(
        state = state,
        // In single-pane the fixed 56dp header overlays the top of the list, so the top
        // fade is offset down below it (and the content padded past it); in two-pane the
        // fade sits at the very top, just below the breadcrumb.
        modifier =
            if (fadingEdges) {
                Modifier
                    .fillMaxSize()
                    .paneFadingEdges(topOffset = if (isTwoPane) 0.dp else 56.dp)
            } else {
                Modifier.fillMaxSize()
            },
        // Inset the content past the static fading edges so items scroll under the fades
        // instead of ending right at the pane boundary; the fixed single-pane header bar
        // adds to the top inset.
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
        // The level's items: its sub-page items and its regular items, in content order.
        GroupKeys.reset()
        parentPage.content(this)
    }
}

/**
 * The full-screen search overlay, hosted by the screen on top of the panes. A full-width
 * search field sits in a fixed top bar (back chevron + field + clear); the matching pages
 * and preference entries are listed below. Selecting a result or the back chevron closes
 * the overlay (the screen navigates or dismisses). [query] mirrors the field's text in
 * both directions, and the field takes focus (and the keyboard shows) when the overlay
 * opens. [placeholder] is the field's hint while the query is empty; the screen passes a
 * value naming the page being searched (e.g. "Search Theme") so the scope is clear.
 */
@Composable
internal fun FullScreenSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    entries: List<SearchEntry>,
    onEntryClick: (SearchEntry) -> Unit,
    onClose: () -> Unit,
    placeholder: String,
) {
    val textFieldState = rememberTextFieldState()
    // The query as seen by the long-lived collector below: a plain closure would capture
    // the value from the composition that started the effect, so read it live.
    val currentQuery by rememberUpdatedState(query)
    val fieldRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
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
    // Take focus (and show the keyboard) when the overlay opens.
    LaunchedEffect(Unit) {
        fieldRequester.requestFocus()
        keyboard?.show()
    }

    Box(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize()) {
            // The fixed top bar: back chevron, the full-width search field, and (while a
            // query is entered) a clear button.
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        BackChevron()
                    }
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier =
                                    Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(horizontal = 8.dp),
                            )
                        }
                        BasicTextField(
                            state = textFieldState,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .focusRequester(fieldRequester)
                                    .padding(horizontal = 8.dp),
                            textStyle =
                                MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
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
            // The results: the matching pages and preference entries, or a hint while the
            // query is empty.
            LazyColumn(
                state = remember { LazyListState() },
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                if (query.isEmpty()) {
                    item {
                        Text(
                            text = "Search items",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(entries, key = { it.id }) { entry ->
                        SearchEntryItem(
                            // An entry match shows the preference's own title with the
                            // page's trail as its subtitle; a page match shows the
                            // page's title, and its parent's trail (or summary) as
                            // subtitle.
                            entry = entry.entry,
                            page = entry.page,
                            path = entry.path,
                            icon = entry.icon,
                            onClick = { onEntryClick(entry) },
                        )
                    }
                    if (entries.isEmpty()) {
                        item {
                            Text(
                                text = "No matching items",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The detail pane: the selected page's items — its sub-page items and its regular items,
 * in content order — and, in single-pane mode, the fixed 56dp compact bar (back arrow +
 * title + a right-aligned search affordance, [onSearchClick]) the list scrolls under. The
 * search affordance is what carries search on subpages: in single-pane a subpage is shown
 * in this pane, so its bar is the only one on screen. Each page's list uses its own
 * [LazyListState] from [scrollStates] (owned by the screen), so a page always comes back at the scroll
 * position it was left at (clamped when its items shrank; the top when it has not been
 * shown before), plus the scroll-to-item effect for a selected search result.
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
    pageTransition: PageTransition,
    pageDirection: PageDirection,
    pageFor: (String) -> Page?,
    scrollStates: MutableMap<String, LazyListState>,
    onSearchClick: () -> Unit,
) {
    // The list of the current page: each page keeps its own state in [scrollStates]
    // (owned by the screen, so it survives the pane leaving the composition in
    // single-pane mode). Composing the page's list with its state restores the page's
    // scroll position when the user comes back (clamped when the page's items shrank);
    // a page never shown before starts at the top.
    val pageId = page?.id
    val detailState =
        remember(pageId) { pageId?.let { scrollStates.getOrPut(it) { LazyListState() } } }
    // Scroll to the item a search result navigated to, once the page's list is composed
    // (the detail pane is composed asynchronously, so wait for it to be laid out with
    // enough items first).
    LaunchedEffect(page?.id, scrollToIndex) {
        val state = detailState ?: return@LaunchedEffect
        val target = scrollToIndex ?: return@LaunchedEffect
        snapshotFlow { state.layoutInfo.totalItemsCount }
            .first { it > target }
        state.animateScrollToItem(target)
        onDetailScrollConsumed()
    }
    val edge = if (fadingEdges) PANE_FADING_EDGE_LENGTH else 0.dp
    // Items highlight on a search selection, and page items navigate via the callback
    // provided here: the items are composed within this scope, even though the
    // registration itself (the lazy list scope) is not a composable context.
    // The detail pane does not provide a selected page id: its items are the pages
    // *below* the open one, so none of them should highlight themselves.
    CompositionLocalProvider(
        LocalHighlightedItemKey provides highlightedKey,
        LocalOnSelectPage provides onSelectPage,
        LocalSelectedPageId provides null,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.isFocused) onActive() },
        ) {
            AnimatedPageContent(
                targetId = page?.id,
                direction = pageDirection,
                transition = pageTransition,
                enteringContent = {
                    page?.let { p ->
                        detailState?.let { state ->
                            DetailListContent(p, state, showBack, fadingEdges, edge)
                        }
                    }
                },
            ) { id ->
                // The outgoing page's items, frozen while they leave the pane, in the
                // page's own state — so they stay at the page's scroll position.
                val p = pageFor(id) ?: return@AnimatedPageContent
                DetailListContent(p, scrollStates[id] ?: LazyListState(), showBack, fadingEdges, edge)
            }
            // The compact bar: declared after the list so it draws above the content.
            // Static — always shown in full whenever the detail page is open in
            // single-pane, with the content scrolling under it (the pane's top fade
            // provides the separation). In two-pane mode it is never shown: the
            // breadcrumb above the panes already names the page, so the bar would only
            // duplicate it.
            if (showBack && page != null) {
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
                                .padding(start = 8.dp, end = 4.dp),
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
                        // Right-aligned search affordance: opens the full-screen search
                        // overlay, scoped to this page and below.
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The detail pane's scrollable content: [page]'s items — its sub-page items and its
 * regular items, in content order; tapping a sub-page item navigates deeper into the tree
 * (the list pane switches to that page's items).
 */
@Composable
private fun DetailListContent(
    page: Page,
    state: LazyListState,
    showBack: Boolean,
    fadingEdges: Boolean,
    edge: Dp,
) {
    LazyColumn(
        state = state,
        // In single-pane the compact bar overlays the top of the list, so the top fade is
        // offset down below it; in two-pane there is no bar, so the fade sits at the very
        // top, just below the breadcrumb.
        modifier =
            if (fadingEdges) {
                Modifier
                    .fillMaxSize()
                    .paneFadingEdges(topOffset = if (showBack) 56.dp else 0.dp)
            } else {
                Modifier.fillMaxSize()
            },
        // The compact bar overlays the top of the list and is always visible in single-pane
        // (where the back arrow lives); inset the first item by the bar's height so the
        // page's first item — e.g. its top category header — isn't hidden beneath it. The
        // top and bottom insets also keep the content clear of the static fading edges so
        // items scroll under them.
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
        GroupKeys.reset()
        page.content(this)
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
