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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppItem
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.BadgeColor
import de.umstieg.launcher.core.BadgeCounter
import de.umstieg.launcher.core.BadgeStyle
import de.umstieg.launcher.core.FolderItem
import de.umstieg.launcher.core.FontWeightSetting
import de.umstieg.launcher.core.HomeItem
import de.umstieg.launcher.core.IconOptions
import de.umstieg.launcher.core.LabelColor
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.core.Shadow
import de.umstieg.launcher.core.WidgetItem
import de.umstieg.launcher.core.iconOptions
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.IconLoader
import de.umstieg.launcher.ui.icons.AppIconImage
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.LocalTosStyle
import de.umstieg.launcher.ui.theme.Tos
import kotlin.math.abs

/** Beschriftungs-Stil aus den Einstellungen (Stärke, Zeilen, Schatten, Farbe). */
@Immutable
data class LabelLook(
    val fontScale: Int = 100,
    val weight: FontWeightSetting = FontWeightSetting.MEDIUM,
    val lines: Int = 1,
    val shadow: Boolean = true,
    val color: LabelColor = LabelColor.AUTO,
)

/** Badge-Stil aus den Einstellungen. */
@Immutable
data class BadgeLook(val enabled: Boolean = true, val style: BadgeStyle = BadgeStyle.NUMBER, val color: BadgeColor = BadgeColor.RED)

/** Alles, was ein Icon zum Zeichnen braucht (vermeidet lange Parameterlisten). */
@Immutable
data class IconStyle(
    val loader: IconLoader,
    val legacy: IconOptions,
    val shape: SuperellipseShape,
    val iconSize: Dp,
    val fontScale: Int,
    val largeTouch: Boolean,
    val shadow: Shadow = Shadow.LIGHT,
    val label: LabelLook = LabelLook(fontScale),
    val badge: BadgeLook = BadgeLook(),
    val folder3x3: Boolean = false,
)

/** IconStyle aus den Einstellungen für eine bestimmte Icongröße. */
fun iconStyle(loader: IconLoader, s: LauncherSettings, shape: SuperellipseShape, size: Dp, dark: Boolean = true) = IconStyle(
    loader = loader,
    legacy = s.iconOptions(dark),
    shape = shape,
    iconSize = size,
    fontScale = s.fontScale,
    largeTouch = s.largeTouch,
    shadow = s.iconShadow,
    label = LabelLook(s.fontScale, s.labelWeight, s.labelLines, s.labelShadow, s.labelColor),
    badge = BadgeLook(s.badgesEnabled, s.badgeStyle, s.badgeColor),
    folder3x3 = s.folderPreview3x3,
)

/** Wackeln im Bearbeitungsmodus (je Element leicht versetzt). */
@Composable
fun Modifier.wobble(active: Boolean, seed: String): Modifier {
    if (!active || LocalTosStyle.current.reduceMotion) return this
    val h = abs(seed.hashCode())
    val transition = rememberInfiniteTransition(label = "wobble")
    // Geschmeidig (Sinus-ähnliche Kurve) und je Icon leicht anders: Dauer, Ausschlag und Startphase.
    val duration = 230 + h % 90
    val angle by transition.animateFloat(
        initialValue = -1.2f - (h % 5) * 0.12f,
        targetValue = 1.2f + (h % 7) * 0.1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = duration, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            RepeatMode.Reverse,
            initialStartOffset = androidx.compose.animation.core.StartOffset(h % duration),
        ),
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

/** Zahl oder Punkt oben rechts am Icon. */
@Composable
fun CountBadge(count: Int, look: BadgeLook, iconColor: Color?, modifier: Modifier = Modifier) {
    if (!look.enabled || count <= 0) return
    val bg = when (look.color) {
        BadgeColor.RED -> Tos.Danger
        BadgeColor.ACCENT -> Tos.Accent
        BadgeColor.ICON -> iconColor ?: Tos.Danger
    }
    val ink = if (look.color == BadgeColor.ACCENT) Tos.AccentInk else Color.White
    if (look.style == BadgeStyle.DOT) {
        Box(modifier.size(12.dp).background(bg, CircleShape).border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape))
    } else {
        Box(
            modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp).background(bg, CircleShape).padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(BadgeCounter.label(count), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ink))
        }
    }
}

/** Ordner-Icon: Glas-Fläche in Icon-Form mit Mini-Icons der ersten vier (oder neun) Apps. */
@Composable
fun FolderIcon(folder: FolderItem, apps: Map<AppKey, AppInfo>, style: IconStyle, modifier: Modifier = Modifier) {
    val n = if (style.folder3x3) 3 else 2
    val size = style.iconSize
    val gap = size * (if (n == 3) 0.05f else 0.08f)
    val mini = (size * 0.8f - gap * (n - 1)) / n
    val shown = folder.apps.mapNotNull { apps[it] }.take(n * n)
    Box(
        modifier.size(size).background(Color.White.copy(alpha = 0.22f), style.shape),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0 until n) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0 until n) {
                        val app = shown.getOrNull(row * n + col)
                        if (app != null) {
                            AppIconImage(style.loader, app, style.legacy, style.shape, mini, shadow = Shadow.NONE)
                        } else {
                            Spacer(Modifier.size(mini))
                        }
                    }
                }
            }
        }
    }
}

/** Beschriftungs-Textstil aus [LabelLook]. */
@Composable
fun labelTextStyle(look: LabelLook, override: Color?): TextStyle {
    // „Automatisch“: auf hellen Hintergrundbildern dunkle Schrift (mit hellem Schein), sonst helle.
    val color = override ?: when (look.color) {
        LabelColor.AUTO -> if (LocalTosStyle.current.lightWallpaper) Color(0xFF10141C) else Tos.Ink
        LabelColor.LIGHT -> Tos.Ink
        LabelColor.DARK -> Color(0xFF10141C)
    }
    val weight = when (look.weight) {
        FontWeightSetting.NORMAL -> FontWeight.Normal
        FontWeightSetting.MEDIUM -> FontWeight.Medium
        FontWeightSetting.BOLD -> FontWeight.Bold
    }
    val base = labelStyle(look.fontScale, color).copy(fontWeight = weight)
    val strong = LocalTosStyle.current.highContrast
    return when {
        !look.shadow -> base.copy(shadow = null)
        color.red < 0.3f -> base.copy(shadow = androidx.compose.ui.graphics.Shadow(Color.White.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(0f, 1f), 4f))
        strong -> base.copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(0f, 2f), 8f))
        else -> base
    }
}

/**
 * Ein Element auf dem Homescreen, im Dock oder im Ordner: Icon (App oder Ordner), Badge,
 * Beschriftung, im Bearbeitungsmodus wackelnd mit „–“-Badge.
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
    labelColor: Color? = null,
    badge: Int = 0,
) {
    if (item is WidgetItem) return
    val app = (item as? AppItem)?.let { apps[it.app] }
    if (item is AppItem && app == null) return
    val label = when (item) {
        is AppItem -> app!!.label
        is FolderItem -> item.name
        else -> ""
    }
    val base = when (item) {
        is AppItem -> if (app!!.isWorkProfile) stringResource(R.string.work_app_description, label) else label
        is FolderItem -> stringResource(R.string.folder_description, item.name, item.apps.size)
        else -> ""
    }
    val description = if (badge > 0 && style.badge.enabled) {
        base + ", " + pluralStringResource(R.plurals.badge_description, badge, badge)
    } else {
        base
    }
    val editLabel = stringResource(R.string.edit_start_action)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Antippen drückt das Icon leicht ein (Federphysik); Ordner-Ziel beim Ziehen vergrößert.
    val scale by animateFloatAsState(
        when {
            highlighted -> 1.12f
            pressed && !LocalTosStyle.current.reduceMotion -> 0.9f
            else -> 1f
        },
        androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "iconScale",
    )
    Column(
        modifier
            .graphicsLayer { alpha = if (hidden) 0f else 1f }
            .clickable(
                interactionSource = interaction,
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
        // Ordner-Vorschau beim Verweilen: Glasrahmen hinter dem vergrößerten Ziel-Icon.
        val frame by animateFloatAsState(if (highlighted) 1f else 0f, label = "folderFrame")
        Box(
            Modifier
                .wobble(editMode && wobble, item.id)
                .drawBehind {
                    if (frame > 0f) {
                        val pad = size.minDimension * 0.16f
                        val r = (size.minDimension + 2 * pad) * 0.3f
                        drawRoundRect(
                            Color.White.copy(alpha = 0.22f * frame),
                            topLeft = androidx.compose.ui.geometry.Offset(-pad, -pad),
                            size = androidx.compose.ui.geometry.Size(size.width + 2 * pad, size.height + 2 * pad),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                        )
                        drawRoundRect(
                            Color.White.copy(alpha = 0.55f * frame),
                            topLeft = androidx.compose.ui.geometry.Offset(-pad, -pad),
                            size = androidx.compose.ui.geometry.Size(size.width + 2 * pad, size.height + 2 * pad),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * density),
                        )
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            when (item) {
                is AppItem -> AppIconImage(style.loader, app!!, style.legacy, style.shape, style.iconSize, shadow = style.shadow)
                is FolderItem -> FolderIcon(item, apps, style)
                else -> Unit
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
            if (!editMode) {
                val iconColor = app?.let { style.loader.colorOf(it) }?.let { Color(it) }
                CountBadge(badge, style.badge, iconColor, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-5).dp))
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
            Text(
                text = label,
                style = labelTextStyle(style.label, labelColor),
                maxLines = style.label.lines,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.width(style.iconSize * 1.45f),
            )
        }
    }
}

@Composable
fun folderCountText(count: Int): String = pluralStringResource(R.plurals.folder_app_count, count, count)
