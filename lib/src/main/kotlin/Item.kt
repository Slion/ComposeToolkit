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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds an item to the lazy list: a preference item, or — with [page] — an item that
 * opens a page. A page item is the basic way a page references its children: it can be
 * placed anywhere in a page's content, next to any other item, and it is styled exactly
 * like any other item (a host may give it a [CardStyle] or put it in a [group] like any
 * other item). It
 * only defaults its action: tapping a page item navigates into [page], unless [onClick]
 * is set, in which case the item content runs the action and the trailing chevron,
 * behind a separator, becomes the page's own tap target (a two-target item).
 *
 * @param key The lazy list key of the item, and the preference state key. If null,
 *   [page]'s id is used (page items are keyed by their page's id).
 * @param page The page the item opens; when null, the item is a regular preference item.
 * @param title The title of the item. Also used as the item's search text. If null,
 *   [page]'s title is used.
 * @param modifier Modifier applied to the item.
 * @param enabled Whether the item is enabled.
 * @param icon The leading icon. If null, [page]'s icon is used.
 * @param actionIcon The action icon, shown just before the widget. For a page item, a
 *   chevron if null.
 * @param summary The summary text, shown below the title. If null, [page]'s summary is
 *   used.
 * @param staticSummary A static summary used in the [buildSearchIndex] index. If null,
 *   [summary] is used.
 * @param widgetContainer The trailing widget (e.g. a switch or checkbox).
 * @param onClick Click handler; for a page item, when null the item navigates into [page];
 *   when set, the item content runs it and the trailing chevron (behind a separator)
 *   opens [page].
 * @param style When non-null, the item is drawn in its own card of this style (a card is a
 *   style, not a container); [CardStyle.None] is equivalent to null — the item is drawn
 *   without a card.
 * @param cardColor Card background color. Only applies with [style]; if null, the default
 * container color of [style] is used.
 * @param cardElevation Card elevation. Only applies to [CardStyle.Elevated]; if null,
 * [CardDefaults.elevatedCardElevation] is used.
 * @param cardBorder Card border. Only applies to [CardStyle.Outlined]; if null,
 * [CardDefaults.outlinedCardBorder] is used.
 * @param shape Card shape. Only applies with [style]; if null, `MaterialTheme.shapes.medium`
 * is used.
 * @param outerPadding Clearance between the card and its container. Only applies with [style];
 * if null, `ItemTheme.horizontalSpacing` is used on all sides.
 */
public fun LazyListScope.item(
    key: String? = null,
    page: Page? = null,
    title: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    val itemKey = key ?: page?.id
    val itemTitle = title ?: page?.title
    val itemSummary = summary ?: page?.summary
    when {
        // A page item: registers the page reference (for the tree walk and navigation).
        // No search entry: the child page is searchable as a page of the tree in its
        // own right, and an item entry would only duplicate it.
        page != null -> SearchIndexer.recordSubPage(page, itemKey, icon ?: page?.icon)
        // A preference item: searchable entry, keyed.
        itemKey != null ->
            SearchIndexer.record(
                itemKey,
                itemTitle ?: "",
                staticSummary ?: itemSummary,
                icon ?: page?.icon,
            )
    }
    item(key = itemKey, contentType = if (page != null) "PageItem" else "Item") {
        val composeItem: @Composable (Modifier) -> Unit = { m ->
            Item(
                title = itemTitle ?: "",
                page = page,
                modifier = m,
                enabled = enabled,
                icon = icon ?: page?.icon,
                actionIcon = actionIcon,
                summary = itemSummary,
                widgetContainer = widgetContainer,
                onClick = onClick,
            )
        }
        if (style != null && style != CardStyle.None) {
            // A carded item: the card is a style of the item, with the group's outer clearance.
            val outer = outerPadding ?: PaddingValues(LocalItemTheme.current.horizontalSpacing)
            Column(
                modifier =
                    modifier
                        .then(highlightedKeyModifier(itemKey))
                        .fillMaxWidth()
                        .padding(outer),
            ) {
                CardSurface(
                    style = style,
                    shape = shape,
                    cardColor = cardColor,
                    cardElevation = cardElevation,
                    cardBorder = cardBorder,
                ) {
                    composeItem(Modifier.fillMaxWidth())
                }
            }
        } else {
            composeItem(modifier.then(highlightedKeyModifier(itemKey)))
        }
    }
}

/**
 * The id of the page currently open in the detail pane, so a page item can highlight
 * itself; null when no page is open. Provided by the Catalog's panes.
 */
/**
 * The navigation callback of the pane hosting a page item: invoked with the page id to
 * navigate into it. Provided by the Catalog's panes.
 */
internal val LocalOnSelectPage: ProvidableCompositionLocal<(String) -> Unit> =
    compositionLocalOf { { } }

/**
 * One item: a preference item, or — with [page] — a page item that opens the page. A page
 * item is rendered exactly like any other item (styled like any other: a host may give it a
 * [CardStyle] or put it in a [group] like any other item); it only defaults its action — the
 * trailing chevron and the click (navigate into [page] unless [onClick] is set, in which
 * case the chevron, behind a separator, is the page's own tap target).
 */
@Composable
public fun Item(
    title: String,
    modifier: Modifier = Modifier,
    page: Page? = null,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    // Read in the composable scope: the click lambda is not.
    val navigate = LocalOnSelectPage.current
    // A page item whose click is taken by an action: the trailing action icon (the
    // chevron by default) becomes the page's own tap target (a two-target item), drawn
    // behind a separator in the widget slot, as in [ItemActionIconButton].
    val iconNavigates = page != null && onClick != null
    val chevron =
        @Composable {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
            )
        }
    val twoTargetIcon: @Composable (() -> Unit)? =
        if (iconNavigates) actionIcon ?: chevron else null
    // A page item's default action icon: the navigation chevron.
    val effectiveActionIcon =
        if (twoTargetIcon == null && page != null && actionIcon == null) {
            chevron
        } else {
            actionIcon
        }
    val effectiveOnClick =
        if (page != null) {
            onClick ?: { navigate(page.id) }
        } else {
            onClick
        }
    BasicItem(
        textContainer = {
            val theme = LocalItemTheme.current
            Column(
                modifier =
                    Modifier.padding(
                        theme.padding.copy(
                            start = if (icon != null) 0.dp else Dp.Unspecified,
                            end =
                                if (widgetContainer != null || effectiveActionIcon != null) {
                                    0.dp
                                } else {
                                    Dp.Unspecified
                                },
                        )
                    )
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides
                        theme.titleColor.let {
                            if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                        }
                ) {
                    ProvideTextStyle(value = theme.titleTextStyle) {
                        Text(text = title)
                    }
                }
                if (summary != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.summaryColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            }
                    ) {
                        ProvideTextStyle(value = theme.summaryTextStyle) {
                            Text(text = summary)
                        }
                    }
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        iconContainer = {
            if (icon != null) {
                val theme = LocalItemTheme.current
                Row(
                    modifier =
                        Modifier.widthIn(min = theme.iconContainerMinWidth)
                            .padding(theme.padding.copy(end = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.iconColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            },
                        content = icon,
                    )
                    // A fixed gap to the text regardless of the icon's own size, so a
                    // large icon (e.g. a game cover) is not flush against the title. The
                    // item grows past [iconContainerMinWidth] to keep the spacing intact.
                    Spacer(modifier = Modifier.width(theme.horizontalSpacing))
                }
            }
        },
        actionIconContainer = {
            if (effectiveActionIcon != null) {
                val theme = LocalItemTheme.current
                Box(
                    modifier =
                        Modifier.widthIn(min = theme.iconContainerMinWidth)
                            .padding(theme.padding.copy(start = 0.dp)),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.iconColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            },
                        content = effectiveActionIcon,
                    )
                }
            }
        },
        widgetContainer = {
            if (twoTargetIcon != null) {
                val theme = LocalItemTheme.current
                Row(
                    modifier = Modifier.padding(start = theme.horizontalSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier.size(DividerDefaults.Thickness, theme.dividerHeight)
                                .background(
                                    DividerDefaults.color.let {
                                        if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                                    }
                                ),
                    )
                    // The item's click runs the action, so the icon is the page's own
                    // entry point: a tap target of its own, behind a separator (as in
                    // [ItemActionIconButton]). Equal padding on both sides of the 48 dp
                    // button (whose 12 dp inner padding centers the icon) leaves the
                    // icon centered between the separator and the item's end.
                    IconButton(
                        onClick = { page?.id?.let(navigate) },
                        modifier = Modifier.padding(horizontal = theme.horizontalSpacing),
                        enabled = enabled,
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = theme.iconColor,
                                disabledContentColor =
                                    theme.iconColor.copy(alpha = theme.disabledOpacity),
                            ),
                        content = twoTargetIcon,
                    )
                }
            } else {
                widgetContainer?.invoke()
            }
        },
        onClick = effectiveOnClick,
    )
}

