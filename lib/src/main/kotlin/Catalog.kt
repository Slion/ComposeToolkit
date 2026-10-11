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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * How long a search-selected preference item stays highlighted in the detail pane. This must be
 * long enough for the highlight animation (pulses + tint fade-out) to fully finish and return to
 * transparent before the key is cleared, otherwise the highlight is cut off mid-animation.
 */
private const val HIGHLIGHT_DURATION_MS = 3600L

/** The panes that can hold focus; the last one to do so decides single-pane landing. */
private enum class ActivePane { List, Detail }

/**
 * A search result item: a page (optionally nested; [path] names its trail as titles, e.g.
 * "Nested > Advanced" — the screen title for the root page's items) and, for an entry
 * match, the [entry] of the page. [trail] is the same trail as page ids below the root:
 * selecting the item enters the page through it (a page hosted from several places is
 * entered where the result found it).
 */
internal data class SearchEntry(
    val id: Int,
    val page: Page,
    val path: String,
    val trail: List<String>,
    val entry: SearchIndexEntry?,
    /**
     * The leading icon of the page's item, for a page-level match (a preference entry's
     * item icon lives on [entry]); null when the item has no icon.
     */
    val icon: @Composable (() -> Unit)? = null,
)

/**
 * Tunables for the adaptive-layout workarounds [Catalog] applies on
 * foldables; the defaults are the workarounds documented in `docs/known-issues.md`.
 */
public class CatalogOptions(
    /**
     * The hinge policy of the pane scaffold directive. [HingePolicy.NeverAvoid] (the
     * default) keeps the two panes exactly half each in every fold state; the material3
     * default (AvoidSeparating) inserts the separating fold as an excluded bound, so in
     * half-folded portrait the scaffold snaps the pane divider onto the crease and the
     * list pane grows past 50%.
     */
    public val hingePolicy: HingePolicy = HingePolicy.NeverAvoid,
    /**
     * Whether the two-pane layout also activates at the "medium" width class. The
     * material3 default only goes two-pane at the "expanded" width class (>= 720dp),
     * which unfolded foldables sit just below (e.g. 719dp); a settings list of short
     * page titles is comfortably usable at medium width, so the default is true.
     */
    public val twoPanesOnMediumWidth: Boolean = true,
    /**
     * Whether each pane draws the static surface fades at its top and bottom edges (with
     * the list content inset by their length so it scrolls under them).
     */
    public val fadingEdges: Boolean = true,
    /**
     * The gutter between the two panes in the two-pane layout. The material3 recommended
     * directives carry a 24dp spacer there; 0.dp makes the panes sit flush against each
     * other (the items keep their own 16dp edge padding).
     */
    public val paneGap: Dp = 24.dp,
    /**
     * How the panes animate when the shown page changes (the detail pane when another
     * page is opened, the list pane when the shown level changes, and the pane swap in
     * single-pane layouts). The direction of a direction-aware transition is derived
     * from the navigation trail: content slides in from the right when navigating
     * deeper, from the left when popping up. [PageTransition.None] restores the instant
     * swap.
     */
    public val pageTransition: PageTransition = PageTransition.Slide,
)

/**
 * The adaptive two-pane settings screen: a list pane of [pages], and a detail pane
 * showing the selected page's preferences.
 *
 * - Single-pane (e.g. a phone in portrait): selecting a page navigates to the detail
 *   pane; the top bar shows the page title with a back arrow, and system back pops the
 *   detail before dismissing the screen.
 * - Two-pane (e.g. wide windows): both panes are visible at once; the top bar shows the
 *   screen title and there is no navigation.
 *
 * The list pane shows the root page's items (its sub-page items and its regular items,
 * in content order); the detail pane shows the selected page's items the same way.
 * Search is a full-screen overlay opened from the search affordance in the title bar
 * (the breadcrumb in two-pane mode; the list header at the root, and the detail pane's
 * compact bar on a subpage, in single-pane). It is scoped to the page the user is
 * currently viewing — that page and everything below it (the whole tree at the root
 * level): in single-pane that is the open page, in two-pane the page of the list pane
 * (the left panel). While a query is entered it lists the matching pages and preference
 * entries within that scope, and selecting a result closes it and opens the page (or
 * scrolls the list to the entry's item).
 *
 * @param title Title of the screen, shown in the top bar.
 * @param root The root page: its items are the list pane's top level, and its page items
 *   ([item] with a [Page]) are the first-level pages. A page item whose click is taken
 *   by an action is a two-target item: the content runs the action, the trailing chevron
 *   opens the page.
 * @param modifier Modifier applied to the root surface.
 * @param onBack Called when the host should dismiss the screen: system back (or a tap
 * of the title-bar back button when [showBackButton] is on) at the root of the tree,
 * and — in a two-pane layout, where the list pane already shows the root's items —
 * also from a first-level page.
 * @param backEnabled Whether the screen's system-back handling is active. A host that
 * covers the screen with its own layer (e.g. a fragment or view shown over it) passes
 * false so that back goes to that layer — and its back stack — first.
 * @param showBackButton Whether to show a back chevron next to the screen title (the
 * single-pane title bar and, in two-pane, the breadcrumb). It behaves exactly like the
 * system back: in single-pane it pops the open page (back to the root list, or one
 * level up when nested deeper); in two-pane it pops one level up when nested two or
 * deeper, and calls [onBack] otherwise. A host that is not the app's root surface
 * (e.g. an activity opened from another screen) sets this — with [onBack] closing the
 * host — so the user can return without hunting the system back.
 * @param adaptiveInfo Adaptive layout info computed for the host of the screen instead
 * of for the window. The screen's adaptive layout (single- vs two-pane) follows this
 * value, which is what a host smaller than the window — e.g. a modal bottom sheet, a
 * dialog, or a split — passes: it measures its own size and builds a
 * [WindowAdaptiveInfo] for it (with no hinges, since the fold does not apply to the
 * host), so the screen adapts to the host rather than to the window. Null (the default)
 * uses the window.
 * @param singlePaneOnly Forces the single-pane layout regardless of the (host) width, so
 * a host that must never split into a list + detail (e.g. a bottom sheet, a dialog)
 * stays single-pane even when it is wide; the list navigates to the detail and back.
 * @param options Tunables for the fold/adaptive workarounds (hinge policy, two-pane
 * width class, pane fading edges, pane gap); see [CatalogOptions].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
public fun Catalog(
    title: String,
    root: Page,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    backEnabled: Boolean = true,
    showBackButton: Boolean = false,
    adaptiveInfo: WindowAdaptiveInfo? = null,
    singlePaneOnly: Boolean = false,
    options: CatalogOptions = CatalogOptions(),
) {
    // The window-derived adaptive state (quantized size class + scaffold directive);
    // see rememberScreenAdaptive for the fold-resize rationale.
    val adaptive = rememberScreenAdaptive(adaptiveInfo, singlePaneOnly, options)
    val isTwoPane = adaptive.isTwoPane

    val navigator =
        rememberListDetailPaneScaffoldNavigator<String>(
            scaffoldDirective = adaptive.directive,
            adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
            isDestinationHistoryAware = true,
        )
    val scope = rememberCoroutineScope()
    // Dismiss the search UI (field focus + software keyboard) when a search result is
    // selected, so focus and the keyboard don't linger on the search field.
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Whether the full-screen search overlay is showing (opened from the title-bar search
    // affordance). The query and its results live in the overlay, not in a pane.
    var searchOpen by remember { mutableStateOf(false) }

    // Opens the full-screen search overlay.
    fun openSearch() {
        searchOpen = true
    }

    // Closes the full-screen search overlay and dismisses its field (focus + keyboard).
    fun closeSearch() {
        searchOpen = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    // The pane whose content last had focus (e.g. the user was scrolling/interacting
    // with the detail list, or with the page list/search). Decides which pane a collapse
    // to single-pane lands on: the sub page if the detail was in use, the root list
    // otherwise. Null until focus is actually observed (fresh launch).
    var lastActivePane by rememberSaveable { mutableStateOf<ActivePane?>(null) }
    fun onPaneActive(pane: ActivePane) {
        if (lastActivePane != pane) lastActivePane = pane
    }

    // The scaffold's destination is the single source of truth for which page (if any)
    // the detail pane shows, so the top bar title and the list-pane selection highlight
    // follow the real navigation state (including across configuration changes such as
    // rotation).
    val destination = navigator.currentDestination
    val selectedPageId: String? =
        if (destination?.pane == ListDetailPaneScaffoldRole.Detail) {
            destination.contentKey
        } else {
            null
        }
    // The navigation trail within the page tree: the chain of page ids from the top
    // level down to the page currently shown in the detail pane (empty = none
    // selected). The detail shows the deepest page; the list pane shows the items of its
    // parent (the top-level pages while at the root). Tapping a sub-page item pushes its
    // id, tapping a breadcrumb segment (or back) pops to that ancestor.
    var pagePath by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    // True once the user explicitly opens a page (item tap, breadcrumb, search result).
    // Survives rotation so a later grow to two-pane can refill the detail pane; a pure
    // rotation round trip on a fresh launch must stay at the root level.
    var userSelectedPage by rememberSaveable { mutableStateOf(false) }
    // The direction of the navigation in flight (for the page transition): deeper (or a
    // same-level switch) reads as forward, popping up as backward.
    var pageDirection by remember { mutableStateOf(PageDirection.Forward) }
    // The ids from below the root down to the page (empty = the root level): the root
    // is the screen itself, so it is never part of the navigation trail.
    fun pathOf(pageId: String): List<String> =
        findPagePath(root, pageId)?.drop(1)?.map { it.id } ?: listOf(pageId)
    // Selects [pageId] in the detail pane with [trail] as its navigation trail. The
    // trail is the one of the place the page was selected from (an item knows where it
    // lives) rather than re-derived from the tree, so a page hosted from several
    // places is entered where the user tapped it.
    fun selectPageById(pageId: String, trail: List<String>) {
        // Deeper (or a same-level switch) is forward; a shorter trail pops up.
        pageDirection =
            if (trail.size >= pagePath.size) PageDirection.Forward else PageDirection.Backward
        userSelectedPage = true
        pagePath = trail
        scope.launch {
            navigator.navigateTo(
                pane = ListDetailPaneScaffoldRole.Detail,
                contentKey = pageId,
            )
        }
    }
    fun navigateToPath(id: List<String>) {
        pageDirection =
            if (id.size >= pagePath.size) PageDirection.Forward else PageDirection.Backward
        // An empty path means "back to the root list" (breadcrumb at the top level),
        // which is not a selection: forget the flag so size round trips stay at the
        // root.
        userSelectedPage = id.isNotEmpty()
        pagePath = id
        if (id.isNotEmpty()) {
            scope.launch {
                navigator.navigateTo(
                    pane = ListDetailPaneScaffoldRole.Detail,
                    contentKey = id.last(),
                )
            }
        }
    }
    // Pops the scaffold to the root list.
    suspend fun popToRootList() {
        navigator.navigateBack()
        pageDirection = PageDirection.Backward
        pagePath = emptyList()
        userSelectedPage = false
    }

    // Keep the pane layout in sync with the window size:
    // - Growing to two-pane with no detail destination opens a page right away (the last
    //   open one, or the first page) so the two-pane layout is not empty. The page is
    //   only a visual filler when the user never selected one: shrinking back pops to
    //   the root list, so a rotation round trip on a fresh launch ends where it
    //   started.
    // - Shrinking back to a single partition leaves the destination history pointing at
    //   the detail pane, which keeps the detail pane expanded (and the list hidden)
    //   until the user presses back. Pop back to the list so the layout collapses to
    //   single-pane immediately.
    LaunchedEffect(isTwoPane) {
        val onDetail =
            navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail
        when {
            isTwoPane && !onDetail -> {
                // The auto-open filler: the first sub-page of the root (a two-target item
                // still opens its page).
                val id =
                    navigator.currentDestination?.contentKey
                        ?: root.structure.subPages.firstOrNull()?.page?.id
                if (id != null) {
                    pagePath = pathOf(id)
                    navigator.navigateTo(
                        pane = ListDetailPaneScaffoldRole.Detail,
                        contentKey = id,
                    )
                }
            }
            isTwoPane && onDetail -> {
                // Material3's internal snapTo (in rememberThreePaneScaffoldNavigator)
                // does not reliably fire across a config change when the detail is
                // already open, so the scaffold state stays at the stale single-pane
                // value. Force it to the derived two-pane value.
                (navigator.scaffoldState as? MutableThreePaneScaffoldState)
                    ?.snapTo(navigator.scaffoldValue)
            }
            !isTwoPane && onDetail -> {
                if (!userSelectedPage) {
                    // The detail shows a page we auto-opened as a visual filler; a
                    // rotation round trip must end back at the root list.
                    popToRootList()
                } else if (lastActivePane != ActivePane.List) {
                    // The detail was in use (or focus was never observed, e.g. right
                    // after launch in two-pane), so keep it visible in single-pane too.
                    (navigator.scaffoldState as? MutableThreePaneScaffoldState)
                        ?.snapTo(navigator.scaffoldValue)
                } else {
                    navigator.navigateBack()
                }
            }
        }
    }

    var query by remember { mutableStateOf("") }

    // The preference item a search result navigated to is highlighted briefly, so the
    // user can find it in the opened page (mirrors the launcher settings behavior).
    var highlightedKey by remember { mutableStateOf<String?>(null) }
    // The scroll state of every page a pane has shown, keyed by page id (one map per
    // pane: the list levels' lists carry the search pill, so their item indices differ
    // from the detail pages'). Owned here — not by the panes — because in single-pane
    // mode the panes leave the composition when the other is shown, so a pane-local map
    // would lose the states on the very navigation they are meant to survive. Each page's
    // list is composed with its own state, so a page always comes back at the scroll
    // position it was left at (clamped when its items shrank).
    val listScrollStates = remember { mutableMapOf<String, LazyListState>() }
    val detailScrollStates = remember { mutableMapOf<String, LazyListState>() }
    // The list levels already shown: the first time a level is shown, the list scrolls
    // to the selected page's item; on a revisit the level's state already holds the
    // position the user left it at, so the list is left alone.
    val visitedListLevels = remember { mutableSetOf<String>() }
    LaunchedEffect(highlightedKey) {
        if (highlightedKey != null) {
            delay(HIGHLIGHT_DURATION_MS)
            highlightedKey = null
        }
    }

    // The lazy list index of the item a search result navigated to; the detail pane
    // scrolls to it when set.
    var scrollToIndex by remember { mutableStateOf<Int?>(null) }
    // The list-pane counterpart for root items: the search pill is item 0, the page
    // items follow, then the root items.
    var listScrollToIndex by remember { mutableStateOf<Int?>(null) }

    fun backAction() {
        val onDetail = destination?.pane == ListDetailPaneScaffoldRole.Detail
        when {
            // Nested in the tree: pop one level up (child -> parent) first.
            onDetail && pagePath.size > 1 -> navigateToPath(pagePath.dropLast(1))
            !isTwoPane && onDetail -> scope.launch { popToRootList() }
            else -> onBack()
        }
    }

    // System back: the scaffold pops detail -> list first, then the host leaves the
    // screen. A host that covers the screen with its own layer disables this so that
    // back goes to that layer (and its back stack) first.
    BackHandler(enabled = backEnabled, onBack = ::backAction)

    // System back closes the search overlay first (it sits on top of the panes), then
    // falls through to the screen's own back handling.
    BackHandler(enabled = searchOpen, onBack = ::closeSearch)

    // The page the detail pane shows (null at the root level, where the list itself is
    // shown).
    val currentPage = selectedPageId?.let { id -> findPage(root, id) }
    // The trail of the page the detail shows, from below the root (the pages of
    // [pagePath] resolved to instances for their titles). Derived from the stored trail
    // rather than re-walked, so a page hosted from several places keeps the trail it
    // was entered from (may be stale for one frame after a navigation).
    val currentPath: List<Page> =
        remember(root, pagePath) { pagePath.mapNotNull { findPage(root, it) } }
    // The page whose items fill the list pane: the selected page's parent, or the root
    // (whose items are the list itself) while no page is selected.
    val parentPage: Page =
        if (currentPath.isEmpty()) root else currentPath.getOrNull(currentPath.size - 2) ?: root
    // The search is scoped to the page the user is currently viewing — its subtree,
    // "the current level and below". In single-pane that is the open page (the root
    // level otherwise); in two-pane it is the page of the list pane (the left panel), so
    // a page open in the detail does not narrow the scope. Each result's trail is still
    // computed from the screen root (see buildSearchEntries), so selecting a result
    // navigates through the full tree.
    val searchScopePage: Page = if (isTwoPane) parentPage else currentPage ?: root

    // The structure (search entries + sub-page refs) is rebuilt when the tree changes or
    // when a page bumps its content version (items added or changed at runtime).
    val structures =
        remember(root, root.walkPages().map { it.contentVersion }) {
            buildPageStructure(root)
        }
    val index = remember(structures) { structures.mapValues { it.value.entries } }
    val matches =
        remember(searchScopePage.id, index, query) {
            searchPages(searchScopePage, index, query)
        }
    val searchEntries =
        remember(matches, query, title) { buildSearchEntries(root, matches, query, title) }
    val showBack =
        !isTwoPane &&
            destination?.pane == ListDetailPaneScaffoldRole.Detail &&
            currentPage != null

    // Make a pane show [owner]'s items, then scroll it to the item at [index] and
    // highlight the lazy list item [key] (null: the item cannot be highlighted). [trail]
    // is [owner]'s trail below the root, where the item was found: it is used when the
    // owner has to be opened (a page hosted from several places is entered through it).
    fun showListItem(owner: Page, trail: List<String>, key: String?, index: Int) {
        when {
            // The list pane already shows the owner's items.
            parentPage.id == owner.id -> Unit
            // Single-pane, the owner is the root, and the detail is open: back to the
            // root list, where the owner's items live.
            !isTwoPane &&
                owner.id == root.id &&
                destination?.pane == ListDetailPaneScaffoldRole.Detail ->
                scope.launch { popToRootList() }
            // Single-pane with a nested owner: the list only ever shows the root's
            // items, so open the owner in the detail and locate the item there.
            !isTwoPane -> {
                selectPageById(owner.id, trail)
                highlightedKey = key
                scrollToIndex = index
                return
            }
            // Two-pane, nested: move the selection to a child of the owner (the
            // current branch's child when the owner is an ancestor of the shown page,
            // otherwise the owner's first), so the list shows the owner's items.
            else -> {
                val ownerIndex = currentPath.indexOfFirst { it.id == owner.id }
                val childId =
                    (if (ownerIndex >= 0) currentPath.getOrNull(ownerIndex + 1)?.id else null)
                        ?: owner.structure.subPages.firstOrNull()?.page?.id
                childId?.let { navigateToPath(trail + listOf(it)) }
            }
        }
        highlightedKey = key
        listScrollToIndex = index
    }

    // An entry of a page the list pane should show: make sure it shows that page's
    // items, then scroll the list pane to the item and highlight it.
    fun showListEntry(entry: SearchIndexEntry, owner: Page, trail: List<String>) {
        query = ""
        showListItem(owner, trail, entry.key, entry.index)
    }

    // A page item matched: it is not a viewable page (it carries an action), so show the
    // page that owns the item in the list pane, then scroll to the item and highlight it.
    // [trail] is the matched page's trail below the root, so the owner is the item's
    // real parent rather than a re-walk of the tree.
    fun showSubPageItem(page: Page, trail: List<String>) {
        query = ""
        val ownerTrail = trail.dropLast(1)
        val owner = ownerTrail.lastOrNull()?.let { findPage(root, it) } ?: root
        val ref = owner.structure.subPages.firstOrNull { it.page.id == page.id } ?: return
        showListItem(owner, ownerTrail, ref.key, ref.index)
    }

    // A search result item was tapped.
    fun onSearchEntrySelected(entry: SearchEntry) {
        query = ""
        searchOpen = false
        // Clear the search field's focus and hide the keyboard up front: on Android the
        // IME is tied to the window, not to Compose focus, so moving focus alone does not
        // dismiss it. Doing this at the tap (synchronously) beats the item's own focus
        // request, which otherwise races the scroll animation.
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        val page = entry.page
        when {
            // An entry of the page the list pane shows: scroll the list to the item.
            entry.entry != null && page.id == parentPage.id ->
                showListEntry(entry.entry, page, pagePath.dropLast(1))
            // An entry of the root while nested (two-pane): back to the root level, then
            // scroll the list to the item.
            entry.entry != null && page.id == root.id ->
                showListEntry(entry.entry, root, emptyList())
            // The root page itself matched: its items are the list itself — nothing to
            // open.
            page.id == root.id -> Unit
            // A page-level match: focus the page's item (scroll + highlight) rather than
            // opening the page — search locates, it doesn't navigate.
            entry.entry == null -> showSubPageItem(page, entry.trail)
            else -> {
                // The entry's page: open it through the result's own trail (a page
                // hosted from several places is entered where the result found it).
                selectPageById(page.id, entry.trail)
                highlightedKey = entry.entry!!.key
                // The entry's index is relative to the page's own lazy list, sub-page
                // items included — no offset to compute.
                scrollToIndex = entry.entry.index
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        // systemBarsPadding (status + navigation bars) so an edge-to-edge host keeps
        // the content clear of both bars while the surface fills the whole window. A Box
        // wraps the panes so the full-screen search overlay can sit on top of them.
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // The breadcrumb spanning both panes (two-pane only: in single-pane the
            // per-pane bars with their back arrow already convey position).
            if (isTwoPane) {
                // The root is the screen itself (its title is the screen title), so it
                // is not a breadcrumb segment (currentPath is already root-exclusive).
                BreadcrumbBar(
                    title = title,
                    currentPath = currentPath,
                    pagePath = pagePath,
                    showBackButton = showBackButton,
                    onBack = ::backAction,
                    onNavigateToPath = ::navigateToPath,
                    onSearchClick = ::openSearch,
                )
            }
            ListDetailPaneScaffold(
                directive = adaptive.directive,
                scaffoldState = navigator.scaffoldState,
                listPane = {
                    // The scaffold only reads preferredWidth from the parentData of the
                    // pane's root node (the AnimatedPane); on an inner Box it is ignored
                    // and the pane falls back to the directive's 360dp default, letting
                    // the higher-priority detail pane absorb all the leftover width.
                    AnimatedPane(
                        Modifier
                            .preferredWidth(0.5f)
                            .fillMaxSize(),
                    ) {
                        ListPane(
                            title = title,
                            isTwoPane = isTwoPane,
                            showBackButton = showBackButton,
                            onBack = ::backAction,
                            parentPage = parentPage,
                            selectedPageId = selectedPageId,
                            // The list shows [parentPage]'s items: its trail is the
                            // shown page's trail minus the shown page (the root level
                            // while nothing is selected).
                            onSelectPage = { id ->
                                selectPageById(id, pagePath.dropLast(1) + listOf(id))
                            },
                            highlightedKey = highlightedKey,
                            fadingEdges = options.fadingEdges,
                            listScrollToIndex = listScrollToIndex,
                            onListScrollConsumed = { listScrollToIndex = null },
                            onActive = { onPaneActive(ActivePane.List) },
                            pageTransition = options.pageTransition,
                            pageDirection = pageDirection,
                            // Resolves the outgoing page while the transition draws it:
                            // the same canonical instance the panes show.
                            pageFor = { id -> findPage(root, id) },
                            scrollStates = listScrollStates,
                            visitedLevels = visitedListLevels,
                            onSearchClick = ::openSearch,
                        )
                    }
                },
                detailPane = {
                    // Mirror the list pane's 0.5f proportion on the AnimatedPane (the
                    // pane root) so both halves sum to the full width and the scaffold
                    // never has to scale them unequally (keeps the split 50/50 at any
                    // fold angle).
                    AnimatedPane(
                        Modifier
                            .preferredWidth(0.5f)
                            .fillMaxSize(),
                    ) {
                        DetailPane(
                            page = currentPage,
                            selectedPageId = selectedPageId,
                            // The detail shows the current page: its items extend the
                            // shown page's trail.
                            onSelectPage = { id -> selectPageById(id, pagePath + listOf(id)) },
                            onBack = ::backAction,
                            showBack = showBack,
                            highlightedKey = highlightedKey,
                            fadingEdges = options.fadingEdges,
                            scrollToIndex = scrollToIndex,
                            onDetailScrollConsumed = { scrollToIndex = null },
                            onActive = { onPaneActive(ActivePane.Detail) },
                            pageTransition = options.pageTransition,
                            pageDirection = pageDirection,
                            pageFor = { id -> findPage(root, id) },
                            scrollStates = detailScrollStates,
                            onSearchClick = ::openSearch,
                        )
                    }
                },
            )
            }

            // The full-screen search overlay, on top of the panes: it slides in and
            // covers them while open.
            AnimatedVisibility(
                visible = searchOpen,
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 16 },
                exit = fadeOut(tween(150)),
            ) {
                FullScreenSearch(
                    query = query,
                    onQueryChange = { query = it },
                    entries = searchEntries,
                    onEntryClick = ::onSearchEntrySelected,
                    onClose = ::closeSearch,
                    placeholder = stringResource(R.string.search_placeholder, searchScopePage.title),
                )
            }
        }
    }
}

/**
 * Builds a [WindowAdaptiveInfo] for a host measured at [size] px, so a surface smaller
 * than the window (a modal bottom sheet, a dialog, a split pane, …) can pass it to
 * [Catalog.adaptiveInfo] and have the screen adapt to *the host* rather
 * than to the window. The posture is empty (no hinges): the device's fold does not
 * apply to the host's own surface, and including it would make the sheet snap to the
 * crease.
 *
 * A pure function so it can be memoized (e.g. in `remember`); pass
 * [LocalDensity.current].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
public fun windowAdaptiveInfoFor(size: IntSize, density: Density): WindowAdaptiveInfo =
    with(density) {
        WindowAdaptiveInfo(
            // Built from the public WindowSizeClass constructor (the computeFromDpSize*
            // helpers are not exposed by the window-core on the compile classpath). The
            // constructor takes the min width/height in dp and classifies the size the
            // same way for the medium-width breakpoint the two-pane directive keys on.
            windowSizeClass =
                WindowSizeClass(
                    size.width.toDp().value.toInt(),
                    size.height.toDp().value.toInt(),
                ),
            windowPosture = Posture(),
        )
    }
