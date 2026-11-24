package de.umstieg.launcher.ui.home

import android.app.ActivityOptions
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppItem
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.DropTarget
import de.umstieg.launcher.core.FolderItem
import de.umstieg.launcher.core.HomeItem
import de.umstieg.launcher.core.WidgetItem
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.LayoutOps
import de.umstieg.launcher.core.iconOptions
import de.umstieg.launcher.core.Slot
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.ui.components.tosHaptic
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.icons.rememberSuperellipse
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.TosTheme
import de.umstieg.launcher.ui.theme.glass
import de.umstieg.launcher.ui.theme.isDark
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Aktionen, die die Activity bereitstellt. */
class HomeActions(
    val openSettings: () -> Unit,
    val requestDefault: () -> Unit,
)

/** Homescreen: Seiten mit Raster, Seitenindikator, Dock, Bearbeitungsmodus mit Ziehen & Ablegen. */
@Composable
fun HomeScreen(vm: HomeViewModel, isDefault: Boolean, actions: HomeActions) {
    val data by vm.data.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val d = data ?: return
    TosTheme(dark = isDark(d.settings.theme)) {
        HomeContent(vm, d, apps, isDefault, actions)
    }
}

/** Ein Treffer bei der Trefferprüfung: Element, Herkunft, Icon-Mitte. */
private class Hit(val item: HomeItem, val slot: Slot, val center: Offset)

@Composable
private fun HomeContent(vm: HomeViewModel, d: LauncherData, apps: Map<AppKey, AppInfo>, isDefault: Boolean, actions: HomeActions) {
    val s = d.settings
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val editMode by vm.editMode.collectAsStateWithLifecycle()
    val openFolderId by vm.openFolder.collectAsStateWithLifecycle()
    val addSheet by vm.addSheet.collectAsStateWithLifecycle()
    val pageCount = d.layout.pages.size + if (editMode) 1 else 0
    val pagerState = rememberPagerState { pageCount }
    val shape = rememberSuperellipse(s.iconRounding)
    val drag = remember { DragState() }
    var lockedHint by remember { mutableStateOf(false) }

    // Geometrie in Wurzel-Koordinaten (wird beim Layout gemessen).
    var pagerBounds by remember { mutableStateOf(Rect.Zero) }
    var gridMetrics by remember { mutableStateOf(0f to 0f) }
    val marginPx = with(density) { s.sideMarginDp.dp.toPx() }
    val gridGeo: GridGeometry? by rememberUpdatedState(
        if (pagerBounds.isEmpty || gridMetrics.second <= 0f) {
            null
        } else {
            GridGeometry(
                Rect(pagerBounds.left + marginPx, pagerBounds.top, pagerBounds.right - marginPx, pagerBounds.bottom),
                s.columns, s.rows, gridMetrics.first, gridMetrics.second,
            )
        },
    )
    var dockGeo by remember { mutableStateOf<DockGeometry?>(null) }
    var folderGeo by remember { mutableStateOf<GridGeometry?>(null) }
    var folderPanel by remember { mutableStateOf(Rect.Zero) }

    // Für die Gesten-Lambdas immer die aktuellen Werte.
    val current by rememberUpdatedState(d)
    val currentEdit by rememberUpdatedState(editMode)
    val currentFolder by rememberUpdatedState(openFolderId)

    LaunchedEffect(pagerState) {
        vm.toFirstPage.collect { animate -> if (animate) pagerState.animateScrollToPage(0) else pagerState.scrollToPage(0) }
    }
    // Nach „Fertig“ verschwindet die leere Zusatzseite: nicht auf ihr stehen bleiben.
    LaunchedEffect(pageCount) {
        if (pagerState.currentPage >= pageCount) pagerState.scrollToPage((pageCount - 1).coerceAtLeast(0))
    }
    LaunchedEffect(lockedHint) {
        if (lockedHint) {
            delay(4_000)
            lockedHint = false
        }
    }
    BackHandler(enabled = true) { vm.onBack() }

    fun launch(app: AppInfo, bounds: Rect) {
        val r = android.graphics.Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
        val options = ActivityOptions.makeScaleUpAnimation(view, r.left, r.top, r.width(), r.height())
        if (!vm.launch(app, r, options)) Toast.makeText(context, R.string.app_not_startable, Toast.LENGTH_SHORT).show()
    }

    fun hitTest(p: Offset): Hit? {
        val layout = current.layout
        val folderId = currentFolder
        if (folderId != null) {
            val folder = LayoutOps.findFolder(layout, folderId) ?: return null
            val geo = folderGeo ?: return null
            val (x, y) = geo.cellAt(p) ?: return null
            val index = y * geo.columns + x
            val app = folder.apps.getOrNull(index) ?: return null
            return Hit(AppItem(app), Slot.InFolder(folderId, index), geo.iconCenter(x, y))
        }
        dockGeo?.let { geo ->
            if (geo.area.contains(p)) {
                val i = geo.indexAt(p)
                val item = layout.dock.getOrNull(i) ?: return null
                return Hit(item, Slot.Dock(i), geo.center(i))
            }
        }
        val geo = gridGeo ?: return null
        val (x, y) = geo.cellAt(p) ?: return null
        val page = pagerState.currentPage
        val item = LayoutOps.itemAt(layout, page, x, y) ?: return null
        return Hit(item, Slot.Grid(page, x, y), geo.iconCenter(x, y))
    }

    fun targetAt(p: Offset): DropTarget? {
        val layout = current.layout
        dockGeo?.let { geo ->
            if (geo.area.inflate(geo.iconPx * 0.2f).contains(p)) {
                val i = geo.indexAt(p)
                return DropTarget.Dock(i, merge = geo.isOnIcon(p, i) && layout.dock.getOrNull(i) != null)
            }
        }
        val geo = gridGeo ?: return null
        val (x, y) = geo.cellAt(p) ?: return null
        val page = pagerState.currentPage
        val occupied = LayoutOps.itemAt(layout, page, x, y) != null
        return DropTarget.Grid(page, x, y, merge = occupied && geo.isOnIcon(p, x, y))
    }

    fun finishDrag() {
        val from = drag.from
        val item = drag.item
        if (from != null && item != null) {
            val folderId = currentFolder
            val fGeo = folderGeo
            if (from is Slot.InFolder && folderId != null && fGeo != null && folderPanel.contains(drag.pointer)) {
                // Innerhalb des geöffneten Ordners umsortieren.
                val (x, y) = fGeo.cellAt(drag.pointer) ?: (0 to 0)
                vm.moveInFolder(folderId, from.index, y * fGeo.columns + x)
            } else {
                drag.target?.let { vm.drop(from, it) }
            }
            view.tosHaptic(current.settings)
        }
        drag.clear()
    }

    // Seitenwechsel, wenn das gezogene Icon am linken/rechten Rand verweilt.
    val edge by remember {
        derivedStateOf {
            val geo = gridGeo
            if (!drag.active || geo == null || currentFolder != null) {
                0
            } else {
                val zone = geo.cellW * 0.35f
                val p = drag.pointer
                when {
                    p.y !in geo.area.top..geo.area.bottom -> 0
                    p.x < geo.area.left + zone -> -1
                    p.x > geo.area.right - zone -> 1
                    else -> 0
                }
            }
        }
    }
    LaunchedEffect(edge) {
        if (edge == 0) return@LaunchedEffect
        while (true) {
            delay(EDGE_DWELL_MS)
            val target = pagerState.currentPage + edge
            if (target !in 0 until pagerState.pageCount) break
            pagerState.animateScrollToPage(target)
            drag.target = targetAt(drag.pointer)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Jede neue Berührung setzt die Markierung „langes Drücken“ zurück (vor allen Kindern, Initial-Phase).
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    drag.longPressed = false
                }
            }
            // Langes Drücken: Bearbeitungsmodus starten und (auf einem Element) direkt ziehen.
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { p ->
                        drag.longPressed = true
                        if (current.settings.layoutLocked) {
                            lockedHint = true
                            return@detectDragGesturesAfterLongPress
                        }
                        val hit = hitTest(p)
                        if (!currentEdit) vm.setEditMode(true)
                        view.tosHaptic(current.settings)
                        if (hit != null) {
                            drag.start(hit.item, hit.slot, p, hit.center)
                            drag.target = targetAt(p)
                        }
                    },
                    onDrag = { change, _ ->
                        if (drag.active) {
                            change.consume()
                            drag.pointer = change.position
                            if (currentFolder != null && !folderPanel.inflate(24f).contains(change.position)) {
                                vm.openFolder(null) // aus dem Ordner herausgezogen
                            }
                            drag.target = if (currentFolder != null) null else targetAt(change.position)
                        }
                    },
                    onDragEnd = { finishDrag() },
                    onDragCancel = { drag.clear() },
                )
            }
            // Tipp auf eine leere Fläche beendet den Bearbeitungsmodus.
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (!drag.longPressed && currentEdit && currentFolder == null) vm.setEditMode(false) },
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (openFolderId != null) 0.35f else 1f }
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            Box(Modifier.fillMaxWidth().height(TOP_BAR_HEIGHT)) {
                if (editMode) EditTopBar(vm, actions)
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { pagerBounds = it.boundsInRoot() },
                userScrollEnabled = !drag.active,
                beyondViewportPageCount = 1,
                key = { it },
            ) { page ->
                PageGrid(
                    page = page, d = d, apps = apps, shape = shape, editMode = editMode, drag = drag,
                    onMetrics = { dy, iconPx -> gridMetrics = dy to iconPx },
                    onLaunch = ::launch, vm = vm,
                )
            }
            if (editMode && drag.active && currentFolder == null) {
                Text(
                    stringResource(R.string.edit_hint_edge),
                    style = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = Tos.Ink2, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            PageIndicator(
                style = s.indicator,
                pageCount = pageCount,
                current = pagerState.currentPage,
                hasNewPage = editMode,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp),
            )
            if (s.dockEnabled) {
                Dock(d, apps, shape, editMode, drag, vm, ::launch) { dockGeo = it }
            } else {
                SideEffect { dockGeo = null }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Geöffneter Ordner (Glas-Panel).
        val folder = openFolderId?.let { LayoutOps.findFolder(d.layout, it) }
        if (openFolderId != null && folder == null) LaunchedEffect(openFolderId) { vm.openFolder(null) }
        if (folder != null) {
            FolderOverlay(
                folder = folder, apps = apps, settings = s, shape = shape, editMode = editMode, drag = drag, vm = vm,
                onLaunch = ::launch,
                onGeometry = { panel, grid ->
                    folderPanel = panel
                    folderGeo = grid
                },
            )
        } else {
            SideEffect { folderGeo = null }
        }

        // Hinweis: Standard-Launcher festlegen (beim ersten Start, falls nicht Standard).
        if (!isDefault && !d.defaultHintDismissed && !editMode) {
            DefaultHintCard(actions.requestDefault, vm::dismissDefaultHint, Modifier.align(Alignment.TopCenter))
        }

        AnimatedVisibility(lockedHint, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            LockedHintCard { lockedHint = false; actions.openSettings() }
        }

        if (addSheet) {
            AddAppsSheet(d, apps, shape, vm) { key -> vm.addApp(key, pagerState.currentPage.coerceAtMost(d.layout.pages.size)) }
        }

        // Gezogenes Icon über allem.
        drag.item?.let { item ->
            val geo = gridGeo
            val iconDp = with(density) { (geo?.iconPx ?: 150f).toDp() }
            val style = IconStyle(vm.icons, s.iconOptions(), shape, iconDp, s.fontScale, s.largeTouch)
            val half = (geo?.iconPx ?: 150f) / 2f
            Box(
                Modifier
                    .offset { IntOffset((drag.pointer.x - drag.grab.x - half).roundToInt(), (drag.pointer.y - drag.grab.y - half).roundToInt()) }
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                    },
            ) {
                HomeItemView(
                    item = item, apps = apps, style = style, showLabel = false, editMode = false, wobble = false,
                    removeDescription = null, highlighted = false, hidden = false, onClick = {}, onRemove = {}, onStartEdit = null,
                )
            }
        }
    }
}

/** Eine Seite des Rasters. */
@Composable
private fun PageGrid(
    page: Int,
    d: LauncherData,
    apps: Map<AppKey, AppInfo>,
    shape: SuperellipseShape,
    editMode: Boolean,
    drag: DragState,
    onMetrics: (Float, Float) -> Unit,
    onLaunch: (AppInfo, Rect) -> Unit,
    vm: HomeViewModel,
) {
    val s = d.settings
    val items = d.layout.pages.getOrNull(page).orEmpty()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = s.sideMarginDp.dp)) {
        val cellW = maxWidth / s.columns
        val cellH = maxHeight / s.rows
        val labelSpace = if (s.labelsHome) (labelHeight(s.fontScale).value * density.fontScale).dp + 6.dp else 0.dp
        val iconSize = iconDp(s.iconSize, minOf(cellW * 0.86f, cellH - labelSpace - 4.dp))
        val style = IconStyle(vm.icons, s.iconOptions(), shape, iconSize, s.fontScale, s.largeTouch)
        val iconPx = with(density) { iconSize.toPx() }
        val dy = with(density) { -labelSpace.toPx() / 2f }
        SideEffect { onMetrics(dy, iconPx) }
        // Ablageziel hervorheben (leere Zelle: Umriss in Akzentfarbe).
        val target = drag.target
        if (drag.active && target is DropTarget.Grid && target.page == page && !target.merge &&
            LayoutOps.itemAt(d.layout, page, target.x, target.y) == null
        ) {
            Box(Modifier.offset(cellW * target.x, cellH * target.y).size(cellW, cellH), contentAlignment = Alignment.Center) {
                Box(Modifier.offset(y = -labelSpace / 2).size(iconSize).border(2.dp, Tos.Accent, shape))
            }
        }
        items.forEach { placed ->
            key(placed.item.id) {
                val slot = Slot.Grid(page, placed.x, placed.y)
                Box(Modifier.offset(cellW * placed.x, cellH * placed.y).size(cellW, cellH), contentAlignment = Alignment.Center) {
                    ItemCell(
                        item = placed.item, slot = slot, apps = apps, style = style, showLabel = s.labelsHome,
                        editMode = editMode, wobble = s.wobble, drag = drag,
                        highlighted = target is DropTarget.Grid && target.merge && target.page == page && target.x == placed.x && target.y == placed.y,
                        onLaunch = onLaunch, vm = vm,
                    )
                }
            }
        }
    }
}

/** Element mit Tipp-Verhalten: App starten bzw. Ordner öffnen; im Bearbeitungsmodus „–“. */
@Composable
internal fun ItemCell(
    item: HomeItem,
    slot: Slot,
    apps: Map<AppKey, AppInfo>,
    style: IconStyle,
    showLabel: Boolean,
    editMode: Boolean,
    wobble: Boolean,
    drag: DragState,
    highlighted: Boolean,
    onLaunch: (AppInfo, Rect) -> Unit,
    vm: HomeViewModel,
    modifier: Modifier = Modifier,
    labelColor: androidx.compose.ui.graphics.Color = Tos.Ink,
) {
    val bounds = remember { arrayOf(Rect.Zero) }
    val label = when (item) {
        is AppItem -> apps[item.app]?.label.orEmpty()
        is FolderItem -> item.name
        is WidgetItem -> ""
    }
    val removeDescription = when {
        item is FolderItem -> stringResource(R.string.edit_remove_folder, label)
        slot is Slot.InFolder -> stringResource(R.string.edit_remove_from_folder, label)
        slot is Slot.Dock -> stringResource(R.string.edit_remove_from_dock, label)
        else -> stringResource(R.string.edit_remove_app, label)
    }
    HomeItemView(
        item = item,
        apps = apps,
        style = style,
        showLabel = showLabel,
        editMode = editMode,
        wobble = wobble,
        removeDescription = removeDescription,
        highlighted = highlighted,
        hidden = drag.item?.id == item.id,
        onClick = {
            when (item) {
                // Loslassen nach langem Drücken ist kein Tipp.
                is AppItem -> if (!editMode && !drag.longPressed) apps[item.app]?.let { onLaunch(it, bounds[0]) }
                is FolderItem -> if (!drag.longPressed) vm.openFolder(item.folderId)
                is WidgetItem -> Unit
            }
        },
        onRemove = { vm.remove(slot) },
        onStartEdit = { vm.setEditMode(true) },
        modifier = modifier.onGloballyPositioned { bounds[0] = it.boundsInWindow() },
        labelColor = labelColor,
    )
}

/** Festes Dock unten (Glas-Fläche), Elemente zentriert. */
@Composable
private fun Dock(
    d: LauncherData,
    apps: Map<AppKey, AppInfo>,
    shape: SuperellipseShape,
    editMode: Boolean,
    drag: DragState,
    vm: HomeViewModel,
    onLaunch: (AppInfo, Rect) -> Unit,
    onGeometry: (DockGeometry) -> Unit,
) {
    val s = d.settings
    val density = LocalDensity.current
    val dockDescription = stringResource(R.string.home_dock_description)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
            .glass(RoundedCornerShape(34.dp), Tos.Glass, Tos.GlassLine)
            .semantics { contentDescription = dockDescription }
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        val slotW = maxWidth / s.dockCount
        val iconSize = iconDp(s.iconSize, slotW * 0.86f)
        val style = IconStyle(vm.icons, s.iconOptions(), shape, iconSize, s.fontScale, s.largeTouch)
        val labelSpace = if (s.labelsDock) (labelHeight(s.fontScale).value * density.fontScale).dp + 6.dp else 0.dp
        val iconPx = with(density) { iconSize.toPx() }
        val target = drag.target
        Row(
            Modifier
                .fillMaxWidth()
                .height(iconSize + labelSpace)
                .onGloballyPositioned { onGeometry(DockGeometry(it.boundsInRoot(), s.dockCount, d.layout.dock.size, iconPx)) },
            horizontalArrangement = Arrangement.Center,
        ) {
            d.layout.dock.forEachIndexed { i, item ->
                key(item.id) {
                    Box(Modifier.size(width = slotW, height = iconSize + labelSpace), contentAlignment = Alignment.TopCenter) {
                        ItemCell(
                            item = item, slot = Slot.Dock(i), apps = apps, style = style, showLabel = s.labelsDock,
                            editMode = editMode, wobble = s.wobble, drag = drag,
                            highlighted = target is DropTarget.Dock && target.merge && target.index == i,
                            onLaunch = onLaunch, vm = vm,
                        )
                    }
                }
            }
        }
    }
}

val TOP_BAR_HEIGHT = 64.dp
private const val EDGE_DWELL_MS = 600L

