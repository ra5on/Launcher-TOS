package de.umstieg.launcher.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppItem
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.FolderItem
import de.umstieg.launcher.core.HomeItem
import de.umstieg.launcher.core.LegacyBackground
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.IconLoader
import de.umstieg.launcher.ui.icons.AppIconImage
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.theme.Tos
import kotlin.math.abs

/** Alles, was ein Icon zum Zeichnen braucht (vermeidet lange Parameterlisten). */
@Immutable
data class IconStyle(
    val loader: IconLoader,
    val legacy: LegacyBackground,
    val shape: SuperellipseShape,
    val iconSize: Dp,
    val fontScale: Int,
    val largeTouch: Boolean,
)

/** Wackeln im Bearbeitungsmodus (je Element leicht versetzt). */
@Composable
fun Modifier.wobble(active: Boolean, seed: String): Modifier {
    if (!active) return this
    val h = abs(seed.hashCode())
    val transition = rememberInfiniteTransition(label = "wobble")
    val angle by transition.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 120 + h % 50, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobbleAngle",
    )
    val dir = if (h % 2 == 0) 1f else -1f
    return graphicsLayer { rotationZ = angle * dir }
}

/** „–“-Badge oben links am Icon mit 48-dp-Touchfläche und Beschreibung für TalkBack. */
@Composable
fun MinusBadge(description: String, large: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val visual = if (large) 30.dp else 22.dp
    Box(
        modifier
            .size(if (large) 56.dp else 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(visual)
                .background(Color(0xD1282C36), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_lucide_minus), null, tint = Color.White, modifier = Modifier.size(visual * 0.6f))
        }
    }
}

/** Ordner-Icon: Glas-Fläche in Icon-Form mit Mini-Icons der ersten vier Apps. */
@Composable
fun FolderIcon(folder: FolderItem, apps: Map<AppKey, AppInfo>, style: IconStyle, modifier: Modifier = Modifier) {
    val size = style.iconSize
    val mini = size * 0.32f
    val gap = size * 0.08f
    val shown = folder.apps.mapNotNull { apps[it] }.take(4)
    Box(
        modifier.size(size).background(Color.White.copy(alpha = 0.22f), style.shape),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0 until 2) {
                        val app = shown.getOrNull(row * 2 + col)
                        if (app != null) {
                            AppIconImage(style.loader, app, style.legacy, style.shape, mini)
                        } else {
                            Spacer(Modifier.size(mini))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ein Element auf dem Homescreen, im Dock oder im Ordner: Icon (App oder Ordner), Beschriftung,
 * im Bearbeitungsmodus wackelnd mit „–“-Badge.
 */
@Composable
fun HomeItemView(
    item: HomeItem,
    apps: Map<AppKey, AppInfo>,
    style: IconStyle,
    showLabel: Boolean,
    editMode: Boolean,
    wobble: Boolean,
    removeDescription: String?,
    highlighted: Boolean,
    hidden: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onStartEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
    labelColor: Color = Tos.Ink,
) {
    val app = (item as? AppItem)?.let { apps[it.app] }
    if (item is AppItem && app == null) return
    val label = when (item) {
        is AppItem -> app!!.label
        is FolderItem -> item.name
    }
    val description = when (item) {
        is AppItem -> if (app!!.isWorkProfile) stringResource(R.string.work_app_description, label) else label
        is FolderItem -> stringResource(R.string.folder_description, item.name, item.apps.size)
    }
    val editLabel = stringResource(R.string.edit_start_action)
    val scale by animateFloatAsState(if (highlighted) 1.12f else 1f, label = "hoverScale")
    Column(
        modifier
            .graphicsLayer { alpha = if (hidden) 0f else 1f }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = label,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                val actions = buildList {
                    if (editMode && removeDescription != null) add(CustomAccessibilityAction(removeDescription) { onRemove(); true })
                    if (!editMode && onStartEdit != null) add(CustomAccessibilityAction(editLabel) { onStartEdit(); true })
                }
                if (actions.isNotEmpty()) customActions = actions
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .wobble(editMode && wobble, item.id)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            when (item) {
                is AppItem -> AppIconImage(style.loader, app!!, style.legacy, style.shape, style.iconSize)
                is FolderItem -> FolderIcon(item, apps, style)
            }
            if (app?.isWorkProfile == true) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(style.iconSize * 0.36f)
                        .background(Color.White, CircleShape)
                        .padding(style.iconSize * 0.07f),
                ) {
                    Icon(painterResource(R.drawable.ic_lucide_briefcase), null, tint = Color(0xFF1D6FD8))
                }
            }
            if (editMode && removeDescription != null) {
                val badgeTouch = if (style.largeTouch) 56.dp else 48.dp
                MinusBadge(
                    description = removeDescription,
                    large = style.largeTouch,
                    onClick = onRemove,
                    modifier = Modifier.offset(x = -badgeTouch / 2 + 4.dp, y = -badgeTouch / 2 + 4.dp),
                )
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            IconLabel(label, style.fontScale, Modifier.width(style.iconSize * 1.45f), labelColor)
        }
    }
}

@Composable
fun folderCountText(count: Int): String = pluralStringResource(R.plurals.folder_app_count, count, count)
