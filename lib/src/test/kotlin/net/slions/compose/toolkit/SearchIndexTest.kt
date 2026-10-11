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
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchIndexTest {
    private val settings = Page(
        id = "settings",
        title = "Settings",
        content = {
            itemSwitch("dark", value = false, onValueChange = {}, title = "Dark theme", summary = "Use dark theme")
            itemSwitch("sound", value = true, onValueChange = {}, title = "Sounds")
        },
    )
    private val nestedChild = Page(
        id = "nested-child",
        title = "Nested child",
        content = {
            itemSwitch("wifi", value = false, onValueChange = {}, title = "Wi-Fi", summary = "Connected")
        },
    )
    private val nested =
        Page(id = "nested", title = "Nested") { item(page = nestedChild) }
    private val root =
        Page(id = "root", title = "Root") {
            item(page = settings)
            item(page = nested)
            itemSwitch("root-row", value = false, onValueChange = {}, title = "Root row")
        }

    @Test
    fun `buildSearchIndex records every row with its key, text and item index`() {
        val index = buildSearchIndex(root)
        // Every page of the tree is indexed, including the root and sub-pages.
        assertEquals(listOf("root", "settings", "nested", "nested-child"), index.keys.toList())
        assertEquals(
            listOf(
                SearchIndexEntry("dark", "Dark theme", "Use dark theme", 0),
                SearchIndexEntry("sound", "Sounds", null, 1),
            ),
            index["settings"],
        )
        assertEquals(
            listOf(SearchIndexEntry("wifi", "Wi-Fi", "Connected", 0)),
            index["nested-child"],
        )
        // The root's own rows are indexed; page rows are not (the child pages are
        // searchable in their own right).
        assertEquals(
            listOf(SearchIndexEntry("root-row", "Root row", null, 2)),
            index["root"],
        )
        // A page without searchable rows still gets an (empty) index entry.
        assertTrue(index["nested"]!!.isEmpty())
    }

    @Test
    fun `group preference builders join the group with group-prefixed keys`() {
        // Inside group { } the preference builders must resolve to their GroupScope
        // overloads (the row joins the group), not the enclosing LazyListScope — the
        // group-prefixed keys are the proof.
        val page =
            Page(
                id = "grouped",
                title = "Grouped",
                content = {
                    group(key = "grp") {
                        itemSwitch(key = "grp_switch", value = true, onValueChange = {}, title = "Group switch")
                        itemSlider(key = "grp_slider", value = 0.5f, onValueChange = {}, title = "Group slider")
                    }
                },
            )
        val index = buildSearchIndex(page)
        assertEquals(
            listOf(
                SearchIndexEntry("grp:0", "Group switch", null, 0),
                SearchIndexEntry("grp:1", "Group slider", null, 1),
            ),
            index["grouped"],
        )
    }

    @Test
    fun `the structure records page rows in content order with their row index`() {
        assertEquals(listOf("settings", "nested"), root.structure.subPages.map { it.page.id })
        assertEquals(listOf(0, 1), root.structure.subPages.map { it.index })
        assertEquals(listOf("nested-child"), nested.structure.subPages.map { it.page.id })
    }

    @Test
    fun `a blank query matches only the root page`() {
        val matches = searchPages(root, buildSearchIndex(root), "   ")
        assertEquals(listOf("root"), matches.map { it.page.id })
        assertTrue(matches.all { it.matches.isEmpty() })
    }

    @Test
    fun `search is case-insensitive on titles, summaries and page titles`() {
        val index = buildSearchIndex(root)
        assertEquals(listOf("settings"), searchPages(root, index, "DARK").map { it.page.id })
        assertEquals(listOf("settings"), searchPages(root, index, "use dark").map { it.page.id })
        assertEquals(listOf("nested-child"), searchPages(root, index, "connected").map { it.page.id })
        assertEquals(listOf("settings"), searchPages(root, index, "sEtTiNgS").map { it.page.id })
        assertEquals(listOf("root"), searchPages(root, index, "root row").map { it.page.id })
    }

    @Test
    fun `search matches nested pages by their rows`() {
        val matches = searchPages(root, buildSearchIndex(root), "wi-fi")
        assertEquals(listOf("nested-child"), matches.map { it.page.id })
        assertEquals(listOf("wifi"), matches.single().matches.map { it.key })
    }

    @Test
    fun `search returns no match for an unknown query`() {
        assertTrue(searchPages(root, buildSearchIndex(root), "zzz").isEmpty())
    }

    @Test
    fun `results are ranked by relevance, exact page title first`() {
        val exactPage = Page(id = "exact", title = "Group") {
            itemSwitch("g-row", value = false, onValueChange = {}, title = "Group row")
        }
        val partialPage = Page(id = "partial", title = "Group settings") {}
        val entryPage = Page(id = "entry", title = "Other") {
            itemSwitch("e-row", value = false, onValueChange = {}, title = "The group")
        }
        val summaryPage = Page(id = "summary", title = "Misc", summary = "About groups") {}
        val tree =
            Page(id = "root", title = "Root") {
                item(page = entryPage)
                item(page = summaryPage)
                item(page = exactPage)
                item(page = partialPage)
            }
        val entries =
            buildSearchEntries(
                tree,
                searchPages(tree, buildSearchIndex(tree), "Group"),
                "Group",
                "Root",
            )
        // Exact page title first, then the page title containing the query, then the
        // entry titles containing it (walk order kept: the "entry" page precedes the
        // "exact" page), and the summary-only match last. The page row is shown even
        // though the "exact" page also has a matching entry.
        assertEquals(
            listOf(
                "exact" to null,
                "partial" to null,
                "entry" to "e-row",
                "exact" to "g-row",
                "summary" to null,
            ),
            entries.map { it.page.id to it.entry?.key },
        )
    }

    @Test
    fun `a page hosted from two places is indexed and searched once`() {
        // The same page (same id, same content) is hosted under the root and under
        // 'mid': one index entry, one search match, one result row.
        val aliasedDeep =
            Page(id = "aliased", title = "Switch") {
                itemSwitch("sw", value = false, onValueChange = {}, title = "A switch")
            }
        val aliased =
            Page(id = "aliased", title = "Switch") {
                itemSwitch("sw", value = false, onValueChange = {}, title = "A switch")
            }
        val tree =
            Page(id = "root", title = "Root") {
                item(page = Page(id = "mid", title = "Mid") { item(page = aliasedDeep) })
                item(page = aliased)
            }
        val index = buildSearchIndex(tree)
        assertEquals(listOf("root", "mid", "aliased"), index.keys.toList())
        val matches = searchPages(tree, index, "switch")
        assertEquals(listOf("aliased"), matches.map { it.page.id })
        val entries =
            buildSearchEntries(tree, matches, "switch", "Root")
        // One page row (the page title matched) and one entry row — not two of each.
        assertEquals(listOf("aliased" to null, "aliased" to "sw"), entries.map { it.page.id to it.entry?.key })
        // The rows carry the shallowest trail: the page is top-level, so it is just
        // its own id (the trail is below the root, page included).
        assertTrue(entries.all { it.trail == listOf("aliased") })
    }

    @Test
    fun `record outside a walk is a no-op`() {
        assertNull(SearchIndexer.record("k", "t"))
    }
}
