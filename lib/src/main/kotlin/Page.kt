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

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/**
 * A settings page: an id, a title, and its items.
 *
 * A page is just a collection of items: any of the library's `*Item` / `section` /
 * `card` builders, in any order, plus [item] items with a [Page] that reference child
 * pages (the way a page opens its children). The tree can nest to any depth. An id
 * names a page: the same page may be hosted from several places in the tree (its items
 * are aliases, and the page is indexed and searchable once), but two distinct pages
 * must have distinct ids (a collision is a programming error, checked when the tree is
 * walked).
 *
 * @property id Id of the page; it is used as the lazy list key and as the navigation
 * destination. The same id may appear in several places of the tree (the same page
 * hosted from several), but never names two different pages.
 * @property title Title of the page, shown in the list pane and in the detail top bar.
 * @property summary Optional summary shown below the title in the list pane.
 * @property icon Optional leading icon, shown to the left of the title in the list pane.
 * @property contentVersion Increment when the items [content] produces change at runtime
 * (items added or removed without a new [Page]) so the screen's search index is
 * rebuilt. It is declared before [content] so trailing-lambda call sites
 * (`Page(id, title) { ... }`) still bind their lambda to [content].
 * @property content The items of the page. It runs in a pane's lazy list scope, so it
 * cannot read the composition directly: capture any theme values (e.g.
 * `MaterialTheme.colorScheme`) in an enclosing `@Composable` scope before building the
 * page.
 */
public data class Page(
    public val id: String,
    public val title: String,
    public val summary: String? = null,
    public val icon: @Composable (() -> Unit)? = null,
    public val contentVersion: Int = 0,
    public val content: LazyListScope.() -> Unit,
) {
    /**
     * The structure of this page (searchable entries + child page references), derived
     * from a single walk of [content]; cached per instance.
     */
    internal val structure: PageStructure by lazy { walkPageContent(this) }

    /**
     * The child pages referenced by [content] via a page item ([item] with a [Page], or a
     * [group] item with a [Page]), in content order.
     */
    internal val childPages: List<Page>
        get() = structure.subPages.map { it.page }
}

/**
 * Depth-first traversal of the page tree rooted at this page, in declaration order: one
 * trail per page instance, each the chain of pages from the top level down to (and
 * including) that instance. A page id may appear in several trails when the same page is
 * hosted from several places (its items are aliases of one page); the ids of *different*
 * pages must be distinct, which is checked here (the same id always names the same page,
 * so its title must match across instances).
 */
internal fun Page.walkPageTrails(): List<List<Page>> {
    val trails = mutableListOf<List<Page>>()
    val titlesById = HashMap<String, String>()
    fun visit(page: Page, trail: List<Page>) {
        // The same id names one page: two instances with the same id must be the same
        // page, or a typo'd id would silently shadow one of them (the shadowed page
        // would never appear in search and its items would navigate to the other one).
        val seen = titlesById[page.id]
        if (seen == null) {
            titlesById[page.id] = page.title
        } else {
            require(seen == page.title) {
                "Page id '${page.id}' is used by two different pages ('$seen' and " +
                    "'${page.title}'). Page ids name a page: hosting one page from " +
                    "several places is fine, but two distinct pages need distinct ids."
            }
        }
        val current = trail + page
        trails += current
        page.childPages.forEach { visit(it, current) }
    }
    visit(this, emptyList())
    return trails
}

/**
 * The distinct pages of the tree rooted at this page, in first-occurrence walk order:
 * when the same page (the same id) is hosted from several places, its shallowest
 * instance is returned once.
 */
internal fun Page.walkDistinctPages(): List<Page> {
    val shallowest = LinkedHashMap<String, Pair<Int, Page>>()
    walkPageTrails().forEach { trail ->
        val id = trail.last().id
        val current = shallowest[id]
        if (current == null || trail.size < current.first) {
            shallowest[id] = trail.size to trail.last()
        }
    }
    return shallowest.values.map { it.second }
}

/**
 * Depth-first traversal of the page tree rooted at this page, in declaration order,
 * including every level of page references (a page hosted from several places appears
 * once per place; see [walkDistinctPages] for the deduplicated view).
 */
public fun Page.walkPages(): List<Page> =
    walkPageTrails().map { it.last() }

/** Finds the page with [id] anywhere in the tree rooted at [pages], or null. */
public fun findPage(root: Page, id: String): Page? =
    root.walkPages().firstOrNull { it.id == id }

/**
 * The preferred chain of pages from the top level down to (and including) the page with
 * [id], or null when no page in the tree has [id]. When the same page is hosted from
 * several places (the same id in several trails), the shallowest trail is returned (the
 * first walk-order trail on a depth tie) — the one closest to the root.
 */
public fun findPagePath(root: Page, id: String): List<Page>? =
    root.walkPageTrails().filter { it.last().id == id }.minByOrNull { it.size }

/**
 * A search result: a [page] whose title/summary or [matches] (entries of the preference
 * tree, see [buildSearchIndex]) contain the query.
 *
 * @property pageMatched True when the page's own title or summary contains the query. The
 * result can also come from matching entries alone, in which case the page itself is not
 * a match and its item must not be shown.
 */
public data class PageMatch(
    public val page: Page,
    public val matches: List<SearchIndexEntry>,
    public val pageMatched: Boolean = false,
)

/**
 * Case-insensitively searches the whole page tree rooted at [root] — including every level
 * of page references, in depth-first order — for pages whose title, summary, or
 * any entry of [index] contains [query] (a blank query matches only the root page, whose
 * items are the list itself). A page hosted from several places (the same id) is searched
 * once, through its shallowest instance, so it yields a single match.
 * The returned [PageMatch.matches] list only contains the entries that matched the query.
 *
 * @param index The search index built with [buildSearchIndex].
 */
public fun searchPages(
    root: Page,
    index: Map<String, List<SearchIndexEntry>>,
    query: String,
): List<PageMatch> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) {
        return listOf(PageMatch(root, emptyList()))
    }
    return root.walkDistinctPages().mapNotNull { page ->
        val pageMatched =
            page.title.lowercase().contains(q) || page.summary?.lowercase()?.contains(q) == true
        // Entries use the lowercase precomputed at index-build time.
        val matchingEntries = (index[page.id] ?: emptyList()).filter {
            it.titleLowercase.contains(q) || it.summaryLowercase?.contains(q) == true
        }
        if (pageMatched || matchingEntries.isNotEmpty()) {
            PageMatch(page = page, matches = matchingEntries, pageMatched = pageMatched)
        } else {
            null
        }
    }
}
