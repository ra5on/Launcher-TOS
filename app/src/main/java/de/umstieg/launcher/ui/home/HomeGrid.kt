package de.umstieg.launcher.ui.home

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppItem
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.BadgeCounter
import de.umstieg.launcher.core.DockBackground
import de.umstieg.launcher.core.DropTarget
import de.umstieg.launcher.core.FolderItem
import de.umstieg.launcher.core.HomeItem
import de.umstieg.launcher.core.HomeLayout
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.core.LayoutOps
import de.umstieg.launcher.core.Slot
import de.umstieg.launcher.core.WidgetItem
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.WidgetHostHolder
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.LocalTosStyle
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.glass
import kotlin.math.roundToInt

/** Gemeinsame Werte für Raster, Dock und Ordner (vermeidet lange Parameterlisten). */
class GridEnv(
    val d: LauncherData,
    val settings: LauncherSettings,
    val apps: Map<AppKey, AppInfo>,
    val badges: Map<String, Int>,
    val shape: SuperellipseShape,
    val editMode: Boolean,
    val drag: DragState,
    val vm: HomeViewModel,
    val onLaunch: (AppInfo, Rect) -> Unit,
) {
    fun badgeOf(item: HomeItem): Int = BadgeCounter.forItem(badges, item, d.badgeDisabled)
}

/** Eine Seite des Rasters; während des Ziehens wird die Vorschau des Ergebnisses gezeigt (Icons weichen aus). */
@Composable
fun PageGrid(
    page: Int,
    layout: HomeLayout,
    env: GridEnv,
    onMetrics: (Float, Float) -> Unit,
) {
    val s = env.settings
    val items = layout.pages.getOrNull(page).orEmpty()
    val density = LocalDensity.current
    val motion = LocalTosStyle.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = s.sideMarginDp.dp)) {
        val cellW = maxWidth / s.columns
        val cellH = maxHeight / s.rows
        val wanted = if (s.labelsHome) (labelHeight(s.fontScale).value * density.fontScale * s.labelLines).dp + 6.dp else 0.dp
        // Zu enge Zellen (Querformat, viele Reihen): Beschriftung weglassen statt abschneiden.
        val showLabels = s.labelsHome && cellH - wanted >= 36.dp
        val labelSpace = if (showLabels) wanted else 0.dp
        val iconSize = iconDp(s.iconSize, minOf(cellW * 0.86f, cellH - labelSpace - 4.dp))
        val style = iconStyle(env.vm.icons, s, env.shape, iconSize)
        val iconPx = with(density) { iconSize.toPx() }
        val dy = with(density) { -labelSpace.toPx() / 2f }
        SideEffect { onMetrics(dy, iconPx) }
        val cellWPx = with(density) { cellW.toPx() }
        val cellHPx = with(density) { cellH.toPx() }

        // Zielzelle hervorheben (Umriss in Akzentfarbe), wo das gezogene Element landen wird.
        val drag = env.drag
        val target = drag.target
        if (drag.active && target is DropTarget.Grid && target.page == page && !target.merge) {
            Box(Modifier.offset(cellW * target.x, cellH * target.y).size(cellW, cellH), contentAlignment = Alignment.Center) {
                Box(Modifier.offset(y = -labelSpace / 2).size(iconSize).border(2.dp, Tos.Accent, env.shape))
            }
        }
        items.forEach { placed ->
            key(placed.item.id) {
                val offset by animateIntOffsetAsState(
                    IntOffset((cellWPx * placed.x).roundToInt(), (cellHPx * placed.y).roundToInt()),
                    animationSpec = if (motion.reduceMotion || motion.motionScale == 0f) snap() else spring(stiffness = 600f),
                    label = "cell",
                )
                val item = placed.item
                if (item is WidgetItem) {
                    if (s.widgetsOnHome) {
                        Box(Modifier.offset { offset }.size(cellW * item.spanX, cellH * item.spanY).padding(4.dp)) {
                            GridWidget(item, env, cellW * item.spanX - 8.dp, cellH * item.spanY - 8.dp)
                        }
                    }
                } else {
                    val slot = Slot.Grid(page, placed.x, placed.y)
                    Box(Modifier.offset { offset }.size(cellW, cellH), contentAlignment = Alignment.Center) {
                        ItemCell(
                            item = item, slot = slot, env = env, style = style, showLabel = showLabels,
                            highlighted = target is DropTarget.Grid && target.merge && target.page == page &&
                                LayoutOps.itemAt(env.d.layout, page, target.x, target.y)?.id == item.id,
                        )
                    }
                }
            }
        }
    }
}

/** Element mit Tipp-Verhalten: App starten bzw. Ordner öffnen; im Bearbeitungsmodus „–“ bzw. anpassen. */
@Composable
fun ItemCell(
    item: HomeItem,
    slot: Slot,
    env: GridEnv,
    style: IconStyle,
    showLabel: Boolean,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    labelColor: Color? = null,
    onLaunchOverride: ((AppInfo, Rect) -> Unit)? = null,
) {
    val bounds = remember { arrayOf(Rect.Zero) }
    val apps = env.apps
    val label = when (item) {
        is AppItem -> apps[item.app]?.label.orEmpty()
        is FolderItem -> item.name
        else -> ""
    }
    val removeDescription = when {
        item is FolderItem -> stringResource(R.string.edit_remove_folder, label)
        slot is Slot.InFolder -> stringResource(R.string.edit_remove_from_folder, label)
        slot is Slot.Dock -> stringResource(R.string.edit_remove_from_dock, label)
        else -> stringResource(R.string.edit_remove_app, label)
    }
    val vm = env.vm
    HomeItemView(
        item = item,
        apps = apps,
        style = style,
        showLabel = showLabel,
        editMode = env.editMode,
        wobble = env.settings.wobble,
        removeDescription = removeDescription,
        highlighted = highlighted,
        hidden = env.drag.item?.id == item.id,
        badge = env.badgeOf(item),
        onClick = {
            if (env.drag.longPressed) return@HomeItemView // Loslassen nach langem Drücken ist kein Tipp.
            when (item) {
                is AppItem -> apps[item.app]?.let { app ->
                    if (env.editMode) vm.showCustomize(app) else (onLaunchOverride ?: env.onLaunch)(app, bounds[0])
                }
                is FolderItem -> vm.openFolder(item.folderId)
                else -> Unit
            }
        },
        onRemove = { vm.remove(slot) },
        onStartEdit = { vm.setEditMode(true) },
        modifier = modifier.onGloballyPositioned { bounds[0] = it.boundsInWindow() },
        labelColor = labelColor,
    )
}

/** Festes Dock unten, Elemente zentriert; Hintergrund Glas/einfarbig/transparent, Rundung einstellbar. */
@Composable
fun Dock(env: GridEnv, layout: HomeLayout, onGeometry: (DockGeometry) -> Unit) {
    val s = env.settings
    val density = LocalDensity.current
    val dockDescription = stringResource(R.string.home_dock_description)
    val shape = RoundedCornerShape(percent = (s.dockRounding / 2).coerceIn(0, 50))
    val bg = when (s.dockBackground) {
        DockBackground.GLASS -> Modifier.glass(shape, Tos.Glass, Tos.GlassLine)
        DockBackground.SOLID -> Modifier.background(Color(0xE6141B27), shape)
        DockBackground.TRANSPARENT -> Modifier
    }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
            .then(bg)
            .semantics { contentDescription = dockDescription }
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        val slotW = maxWidth / s.dockCount
        val iconSize = iconDp(s.iconSize, slotW * 0.86f)
        val style = iconStyle(env.vm.icons, s, env.shape, iconSize)
        val labelSpace = if (s.labelsDock) (labelHeight(s.fontScale).value * density.fontScale).dp + 6.dp else 0.dp
        val iconPx = with(density) { iconSize.toPx() }
        val target = env.drag.target
        val realCount = env.d.layout.dock.size
        Row(
            Modifier
                .fillMaxWidth()
                .height(iconSize + labelSpace)
                .onGloballyPositioned { onGeometry(DockGeometry(it.boundsInRoot(), s.dockCount, realCount, iconPx)) },
            horizontalArrangement = Arrangement.Center,
        ) {
            layout.dock.forEachIndexed { i, item ->
                key(item.id) {
                    Box(Modifier.size(width = slotW, height = iconSize + labelSpace), contentAlignment = Alignment.TopCenter) {
                        ItemCell(
                            item = item, slot = Slot.Dock(i), env = env, style = style, showLabel = s.labelsDock,
                            highlighted = target is DropTarget.Dock && target.merge && env.d.layout.dock.getOrNull(target.index)?.id == item.id,
                        )
                    }
                }
            }
        }
    }
}

/** Widget im Homescreen-Raster; im Bearbeitungsmodus mit „–“ und Antippen zum Größe-Ändern. */
@Composable
private fun GridWidget(item: WidgetItem, env: GridEnv, width: Dp, height: Dp) {
    val glass = env.settings.widgetGlass
    val shape = RoundedCornerShape(22.dp)
    Box(Modifier.fillMaxSize().then(if (glass) Modifier.glass(shape, Tos.Glass, Tos.GlassLine) else Modifier).clip(shape)) {
        WidgetView(env.vm.widgets, item.widgetId, width, height, Modifier.fillMaxSize()) { env.vm.setEditMode(true) }
        if (env.editMode) {
            // Fängt Berührungen ab, damit das Widget gezogen statt bedient wird.
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, Tos.Accent.copy(alpha = 0.7f), shape)
                    .pointerInput(item.widgetId) {
                        detectTapGestures { if (!env.drag.longPressed) env.vm.showResize(item.widgetId) }
                    },
            )
            val label = stringResource(R.string.widget_remove)
            Box(Modifier.align(Alignment.TopStart)) {
                MinusBadge(label, env.settings.largeTouch, onClick = { env.vm.remove(locateSlot(env.d.layout, item) ?: return@MinusBadge) })
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color(0xD1282C36), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    "${item.spanX} × ${item.spanY}",
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp, color = Color.White),
                    modifier = Modifier.semantics { contentDescription = "" },
                )
            }
        }
    }
}

private fun locateSlot(layout: HomeLayout, item: WidgetItem): Slot? = LayoutOps.locate(layout, item.id)

/** Android-Widget (AppWidgetHostView) in Compose; meldet die Größe an den Anbieter. */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun WidgetView(holder: WidgetHostHolder, widgetId: Int, width: Dp, height: Dp, modifier: Modifier = Modifier, onLongPress: () -> Unit) {
    val info = remember(widgetId) { holder.info(widgetId) }
    if (info == null) {
        Box(modifier.background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.widget_unavailable), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink2))
        }
        return
    }
    LaunchedEffect(widgetId, width, height) { holder.updateSize(widgetId, width.value.toInt(), height.value.toInt()) }
    AndroidView(
        factory = { ctx ->
            holder.host.createView(ctx, widgetId, info).apply {
                setOnLongClickListener {
                    onLongPress()
                    true
                }
            }
        },
        modifier = modifier,
    )
}
