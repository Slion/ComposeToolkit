/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.toolkit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Style of a card.
 *
 * A card is a *style*, not a container: it is applied to an item ([item] with [style]) or to the
 * items of a group ([group] with [style]), never built as a separate logical thing.
 */
public sealed class CardStyle {
    /** A filled card (Material Design 3 [Card]) with [CardDefaults.cardColors]. */
    public object Filled : CardStyle()

    /** An elevated card (Material Design 3 [ElevatedCard]) with
     * [CardDefaults.elevatedCardColors]. */
    public object Elevated : CardStyle()

    /** An outlined card (Material Design 3 [OutlinedCard]) with
     * [CardDefaults.outlinedCardColors]. */
    public object Outlined : CardStyle()

    /** No card at all: the items are drawn plainly (a group keeps its spacing and outer
     * clearance, and [item] keeps its standard rendering). */
    public object None : CardStyle()
}

/**
 * Renders [content] in a Material Design 3 card of [style].
 *
 * @param style Style of the card.
 * @param shape Shape of the card. If null, `MaterialTheme.shapes.medium` is used.
 * @param cardColor Card background color. If null, the default container color of [style] is
 * used.
 * @param cardElevation Card elevation. Only applies to [CardStyle.Elevated]. If null,
 * [CardDefaults.elevatedCardElevation] is used.
 * @param cardBorder Card border. Only applies to [CardStyle.Outlined]. If null,
 * [CardDefaults.outlinedCardBorder] is used.
 */
@Composable
internal fun CardSurface(
    style: CardStyle,
    shape: Shape? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val s = shape ?: MaterialTheme.shapes.medium
    when (style) {
        CardStyle.Filled -> Card(
            modifier = Modifier.fillMaxWidth(),
            shape = s,
            colors =
                cardColor?.let { CardDefaults.cardColors(containerColor = it) }
                    ?: CardDefaults.cardColors(),
        ) {
            content()
        }

        CardStyle.Elevated -> ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = s,
            colors =
                cardColor?.let { CardDefaults.elevatedCardColors(containerColor = it) }
                    ?: CardDefaults.elevatedCardColors(),
            elevation = cardElevation ?: CardDefaults.elevatedCardElevation(),
        ) {
            content()
        }

        CardStyle.Outlined -> OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = s,
            colors =
                cardColor?.let { CardDefaults.outlinedCardColors(containerColor = it) }
                    ?: CardDefaults.outlinedCardColors(),
            border = cardBorder ?: CardDefaults.outlinedCardBorder(),
        ) {
            content()
        }

        CardStyle.None -> error("CardSurface is not used with CardStyle.None")
    }
}

/** A single item of a [group], as built with [GroupScope.item]. */
public data class GroupItem(
    /** The title of the item; also the search text of the item. */
    public val title: String,
    /** Optional summary text of the item, used to index it. */
    public val summary: String?,
    /** The page the item opens; when non-null the item registers the child-page reference
     * (and is not indexed for search, as the page itself is searchable). */
    public val page: Page? = null,
    /** The leading icon of a data item. If null, [page]'s icon is used. */
    public val icon: @Composable (() -> Unit)? = null,
    /** The trailing action icon of a data item. If null, [page]'s default is used (for a
     * page item, the navigation chevron). */
    public val actionIcon: @Composable (() -> Unit)? = null,
    /** The trailing widget of a data item (e.g. a switch or checkbox). */
    public val widgetContainer: @Composable (() -> Unit)? = null,
    /** Whether a data item is enabled. */
    public val enabled: Boolean = true,
    /**
     * The click action of a data item; for a page item, when null the item navigates into
     * [page], and when set the trailing chevron becomes the page's own tap target.
     */
    public val onClick: (() -> Unit)? = null,
    /**
     * The composable content drawn inside the item's card. When null, the item renders the
     * standard item from its data ([title], [summary], [page], [icon], [widgetContainer],
     * [enabled], [onClick]) — for a page item that is exactly the [item] rendering.
     */
    public val content: (@Composable () -> Unit)? = null,
)

/**
 * Scope for [group]. Use [item] to add an item; each item is drawn in its own card, and the
 * items are indexed for search.
 */
public class GroupScope {
    internal val items = mutableListOf<GroupItem>()

    /**
     * Adds an item to the group; it is drawn in its own card.
     *
     * With [page], the item is a page item, like [item] in a page's content: it registers the
     * child-page reference (so the page joins the tree), is not indexed for search (the page
     * itself is searchable in its own right), and [title]/[summary]/[icon] default to the
     * page's.
     *
     * When [content] is null the item renders the standard item from its data (the [item]
     * rendering); when set, [content] is drawn inside the card instead, and [title]/[summary]
     * describe the item for search indexing (the content is composable and not walked by
     * [buildSearchIndex], so the item's own text cannot be read automatically). Pass the same
     * title/summary the item displays.
     *
     * @param page The page the item opens; when null, the item is a regular preference item.
     * @param title Title of the item. If null, [page]'s title is used.
     * @param summary Summary of the item. If null, [page]'s summary is used.
     * @param icon Icon to draw next to the text. If null, [page]'s icon is used.
     * @param actionIcon The trailing action icon. If null, [page]'s default is used.
     * @param widgetContainer Container to draw at the end of the preference item.
     * @param enabled Whether the item is enabled.
     * @param onClick Callback invoked when the item is clicked; for a page item, when null the
     *   item navigates into [page].
     * @param content Composable content drawn inside the item's card. If null, the standard
     *   item is rendered from the item's data.
     */
    public fun item(
        page: Page? = null,
        title: String? = null,
        summary: String? = null,
        icon: @Composable (() -> Unit)? = null,
        actionIcon: @Composable (() -> Unit)? = null,
        widgetContainer: @Composable (() -> Unit)? = null,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        content: (@Composable () -> Unit)? = null,
    ) {
        items.add(
            GroupItem(
                title = title ?: page?.title ?: "",
                summary = summary ?: page?.summary,
                page = page,
                icon = icon ?: page?.icon,
                actionIcon = actionIcon,
                widgetContainer = widgetContainer,
                enabled = enabled,
                onClick = onClick,
                content = content,
            )
        )
    }
}

/**
 * Renders a group of items, each drawn in its own Material Design 3 card of [style],
 * separated by [itemSpacing]. The first item shows rounded top corners, the last item shows
 * rounded bottom corners, and middle items have square corners, giving the impression of a
 * single card split into individual items. With [CardStyle.None], the items are drawn
 * plainly, without a card.
 *
 * @param style Style of the individual cards.
 * @param modifier Modifier used to draw the group.
 * @param itemSpacing Gap between the individual cards.
 * @param shape Shape of the individual cards. If null, `MaterialTheme.shapes.medium` is used.
 * @param cardColor Card background color. If null, the default container color of [style] is
 * used.
 * @param outerPadding Clearance between the group and its container. If null,
 * `ItemTheme.horizontalSpacing` is used on all sides.
 * @param items The pre-built items of the group, as produced by [GroupScope].
 */
@Composable
public fun Group(
    style: CardStyle = CardStyle.Filled,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 4.dp,
    shape: Shape? = null,
    cardColor: Color? = null,
    outerPadding: PaddingValues? = null,
    items: List<GroupItem>,
) {
    val outer = outerPadding ?: PaddingValues(LocalItemTheme.current.horizontalSpacing)
    val last = items.size - 1
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(outer),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        items.forEachIndexed { index, item ->
            GroupCard(
                item = item,
                style = style,
                shape = itemShape(shape, index, last),
                cardColor = cardColor,
            )
        }
    }
}

/**
 * The shape of the card of the [index]-th item of a group of [last + 1] items: rounded top
 * corners on the first, rounded bottom corners on the last, and square corners in between.
 */
@Composable
private fun itemShape(shape: Shape?, index: Int, last: Int): Shape {
    val cardShape = shape ?: MaterialTheme.shapes.medium
    val cornerSize =
        (cardShape as? RoundedCornerShape ?: RoundedCornerShape(12.dp)).topStart
    val zero = CornerSize(0f)
    return when {
        last <= 0 -> cardShape
        index == 0 -> RoundedCornerShape(cornerSize, cornerSize, zero, zero)
        index == last -> RoundedCornerShape(zero, zero, cornerSize, cornerSize)
        else -> RoundedCornerShape(zero)
    }
}

/**
 * Draws a single group item: [item]'s content (or its standard item) in a card of [style] and
 * [shape] — or plainly, without a card, when [style] is [CardStyle.None].
 */
@Composable
internal fun GroupCard(
    item: GroupItem,
    style: CardStyle,
    shape: Shape,
    cardColor: Color?,
) {
    val content: @Composable () -> Unit = {
        if (item.content != null) {
            item.content()
        } else {
            Item(
                title = item.title,
                modifier = Modifier.fillMaxWidth(),
                page = item.page,
                summary = item.summary,
                icon = item.icon,
                actionIcon = item.actionIcon,
                widgetContainer = item.widgetContainer,
                enabled = item.enabled,
                onClick = item.onClick,
            )
        }
    }
    if (style == CardStyle.None) {
        content()
    } else {
        CardSurface(style = style, shape = shape, cardColor = cardColor) { content() }
    }
}

/**
 * The position of the group being registered in the current page content build.
 *
 * Reset at the start of every build (composition or search index walk), so that an auto base
 * key is unique between the groups of a page, stable across recompositions, and identical
 * across the two builds (a search entry's key must match the composed item's key to
 * highlight it).
 */
internal object GroupKeys {
    private var position = 0

    internal fun next(): Int = position++

    internal fun reset() {
        position = 0
    }
}

/**
 * Adds a group of items to the lazy list, each drawn in its own card of [style], with the
 * first showing rounded top corners and the last rounded bottom corners.
 *
 * @param key Key used to identify the group in the lazy list. If null, no key is used.
 * @param style Style of the individual cards.
 * @param modifier Modifier used to draw the group.
 * @param itemSpacing Gap between the individual cards.
 * @param shape Shape of the individual cards. If null, `MaterialTheme.shapes.medium` is used.
 * @param cardColor Card background color. If null, the default container color of [style] is
 * used.
 * @param outerPadding Clearance between the group and its container. If null,
 * `ItemTheme.horizontalSpacing` is used on all sides.
 * @param content Content of the group. Use [GroupScope.item] to add items. The content is
 * built eagerly (not composed) so that [buildSearchIndex] can read each item's title/summary
 * and index the group's items.
 */
public fun LazyListScope.group(
    key: String? = null,
    style: CardStyle = CardStyle.Filled,
    modifier: Modifier = Modifier.fillMaxWidth(),
    itemSpacing: Dp = 4.dp,
    shape: Shape? = null,
    cardColor: Color? = null,
    outerPadding: PaddingValues? = null,
    content: GroupScope.() -> Unit,
) {
    // Runs the content (a plain data builder, not composable) before registering the items, so
    // each item is recorded with its own lazy list index — search can scroll to and highlight
    // the specific item, not just the group. A stable base key (generated when the caller
    // passes none) prefixes each item's key.
    val scope = GroupScope()
    scope.content()
    val items = scope.items
    val baseKey = key ?: "group:${GroupKeys.next()}"
    val last = items.size - 1
    items.forEachIndexed { index, item ->
        val itemKey = "$baseKey:$index"
        when {
            // A page card: registers the child-page reference (with the action that
            // replaces navigation, if any); no search entry, as the page itself is
            // searchable in its own right.
            item.page != null ->
                SearchIndexer.recordSubPage(item.page, itemKey, item.icon ?: item.page?.icon)
            // A preference card: searchable entry, keyed.
            else -> SearchIndexer.record(itemKey, item.title, item.summary, item.icon)
        }
        val isLast = index == last
        item(key = itemKey, contentType = "GroupItem") {
            // Read in the composable scope: the theme and layout direction are composition locals.
            val outer = outerPadding ?: PaddingValues(LocalItemTheme.current.horizontalSpacing)
            val layoutDirection = LocalLayoutDirection.current
            val itemPadding =
                PaddingValues(
                    start = outer.calculateLeftPadding(layoutDirection),
                    top = if (index == 0) outer.calculateTopPadding() else 0.dp,
                    end = outer.calculateRightPadding(layoutDirection),
                    bottom =
                        (if (isLast) outer.calculateBottomPadding() else 0.dp) +
                            (if (!isLast) itemSpacing else 0.dp),
                )
            Column(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .then(highlightedKeyModifier(itemKey))
                        .padding(itemPadding),
            ) {
                GroupCard(
                    item = item,
                    style = style,
                    shape = itemShape(shape, index, last),
                    cardColor = cardColor,
                )
            }
        }
    }
}
