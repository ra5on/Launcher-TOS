package de.umstieg.launcher.ui.home

import android.app.ActivityOptions
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppItem
import de.umstieg.launcher.core.AppOpenAnimation
import de.umstieg.launcher.core.DropTarget
import de.umstieg.launcher.core.FolderItem
import de.umstieg.launcher.core.Gesture
import de.umstieg.launcher.core.GestureAction
import de.umstieg.launcher.core.GestureClassifier
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.LayoutOps
import de.umstieg.launcher.core.PageSide
import de.umstieg.launcher.core.PageTransition
import de.umstieg.launcher.core.Slot
import de.umstieg.launcher.core.effective
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.UpdateState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.umstieg.launcher.ui.drawer.DrawerPage
import de.umstieg.launcher.ui.components.tosHaptic
import de.umstieg.launcher.ui.icons.rememberSuperellipse
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.LocalTosStyle
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.TosTheme
import de.umstieg.launcher.ui.theme.isDark
import de.umstieg.launcher.ui.theme.styleFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Aktionen, die die Activity bereitstellt. */
class HomeActions(
    val openSettings: (section: String?) -> Unit,
    val requestDefault: () -> Unit,
    val setWindowBlur: (Boolean) -> Unit,
    val addWidget: (AppWidgetProviderInfo, WidgetTarget) -> Unit,
    val uninstall: (AppInfo) -> Unit,
    val unlockHidden: (onSuccess: () -> Unit) -> Unit,
    val wallpaperColor: Color?,
    val systemReduceMotion: Boolean,
    val lightWallpaper: Boolean = false,
)

/**
 * Zuordnung der Pager-Seiten: Zusatzseiten links, Homescreen-Seiten, Zusatzseiten rechts. Liegen
 * App-Übersicht und Widget-Seite auf derselben Seite, liegen die Widgets außen.
 */
class PageMap(widgetSide: PageSide, drawerSide: PageSide, val homePages: Int) {
    private val left = buildList {
        if (widgetSide == PageSide.LEFT) add(EXTRA_WIDGETS)
        if (drawerSide == PageSide.LEFT) add(EXTRA_DRAWER)
    }
    private val right = buildList {
        if (drawerSide == PageSide.RIGHT) add(EXTRA_DRAWER)
        if (widgetSide == PageSide.RIGHT) add(EXTRA_WIDGETS)
    }
    val firstHome: Int get() = left.size
    val count: Int get() = left.size + homePages + right.size
    val drawer: Boolean get() = EXTRA_DRAWER in left || EXTRA_DRAWER in right
    val drawerIndex: Int get() = when {
        EXTRA_DRAWER in left -> left.indexOf(EXTRA_DRAWER)
        EXTRA_DRAWER in right -> firstHome + homePages + right.indexOf(EXTRA_DRAWER)
        else -> -1
    }
    fun homeIndex(pagerIndex: Int): Int? = (pagerIndex - firstHome).takeIf { it in 0 until homePages }

    private companion object {
        const val EXTRA_WIDGETS = 0
        const val EXTRA_DRAWER = 1
    }
}

/** Homescreen: Seiten, Gesten, Panels, Bearbeitungsmodus mit Ziehen & Ablegen. */
@Composable
fun HomeScreen(vm: HomeViewModel, isDefault: Boolean, actions: HomeActions) {
    val data by vm.data.collectAsStateWithLifecycle()
    val d = data ?: return
    val s = d.settings.effective()
    val dark = isDark(s.theme)
    TosTheme(dark = dark, style = styleFor(s, actions.wallpaperColor, actions.systemReduceMotion, actions.lightWallpaper)) {
        HomeContent(vm, d.copy(settings = s), isDefault, actions)
    }
}

private class Hit(val item: de.umstieg.launcher.core.HomeItem, val slot: Slot, val center: Offset)

@Composable
private fun HomeContent(vm: HomeViewModel, d: LauncherData, isDefault: Boolean, actions: HomeActions) {
    val s = d.settings
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val tosStyle = LocalTosStyle.current
    val scope = rememberCoroutineScope()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    val editMode by vm.editMode.collectAsStateWithLifecycle()
    val openFolderId by vm.openFolder.collectAsStateWithLifecycle()
    val addSheet by vm.addSheet.collectAsStateWithLifecycle()
    val overlay by vm.overlay.collectAsStateWithLifecycle()
    val menuApp by vm.menuApp.collectAsStateWithLifecycle()
    val customize by vm.customize.collectAsStateWithLifecycle()
    val widgetPicker by vm.widgetPicker.collectAsStateWithLifecycle()
    val quietApp by vm.quietApp.collectAsStateWithLifecycle()
    val resizeWidget by vm.resizeWidget.collectAsStateWithLifecycle()
    val setupRequested by vm.setupRequested.collectAsStateWithLifecycle()
    val openCategory by vm.openCategory.collectAsStateWithLifecycle()
    val peek by vm.peek.collectAsStateWithLifecycle()
    val updateState by vm.controller.updater.state.collectAsStateWithLifecycle()

    val map = PageMap(
        widgetSide = s.widgetSide,
        drawerSide = if (editMode) PageSide.OFF else s.drawerSide,
        homePages = d.layout.pages.size + if (editMode) 1 else 0,
    )
    val pagerState = rememberPagerState(initialPage = map.firstHome) { map.count }
    val shape = rememberSuperellipse(s.iconRounding)
    val drag = remember { DragState() }
    var lockedHint by remember { mutableStateOf(false) }

    // Geometrie in Wurzel-Koordinaten.
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

    val current by rememberUpdatedState(d)
    val currentEdit by rememberUpdatedState(editMode)
    val currentFolder by rememberUpdatedState(openFolderId)
    val currentMap by rememberUpdatedState(map)
    val currentOverlay by rememberUpdatedState(overlay)
    val statusTop by rememberUpdatedState(WindowInsets.statusBars.getTop(density).toFloat())

    LaunchedEffect(pagerState) {
        vm.toFirstPage.collect { animate ->
            val target = currentMap.firstHome
            if (animate && !tosStyle.reduceMotion) pagerState.animateScrollToPage(target) else pagerState.scrollToPage(target)
        }
    }
    LaunchedEffect(pagerState) {
        vm.toDrawer.collect { if (currentMap.drawer) pagerState.animateScrollToPage(currentMap.drawerIndex) }
    }
    LaunchedEffect(map.count) {
        if (pagerState.currentPage >= map.count) pagerState.scrollToPage((map.count - 1).coerceAtLeast(0))
    }
    LaunchedEffect(lockedHint) {
        if (lockedHint) {
            delay(4_000)
            lockedHint = false
        }
    }
    // Im Bearbeitungsmodus nicht auf Widget-Seite oder Übersicht hängen bleiben.
    LaunchedEffect(editMode) {
        if (editMode && map.homeIndex(pagerState.currentPage) == null) pagerState.scrollToPage(map.firstHome)
    }
    BackHandler(enabled = true) { vm.onBack() }

    val blurOn = overlay != Overlay.NONE || openFolderId != null || openCategory != null || menuApp != null || customize != null ||
        addSheet || widgetPicker != null
    LaunchedEffect(blurOn) { actions.setWindowBlur(blurOn) }

    fun launch(app: AppInfo, bounds: Rect) {
        val r = android.graphics.Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
        val options = when (if (tosStyle.reduceMotion) AppOpenAnimation.FADE else s.appOpenAnimation) {
            AppOpenAnimation.ZOOM -> ActivityOptions.makeScaleUpAnimation(view, r.left, r.top, r.width(), r.height())
            AppOpenAnimation.FADE -> ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out)
            AppOpenAnimation.NONE -> ActivityOptions.makeCustomAnimation(context, 0, 0)
        }
        if (!vm.launch(app, r, options)) Toast.makeText(context, R.string.app_not_startable, Toast.LENGTH_SHORT).show()
    }
    // Für Kacheln und Kategorie-Panel: App mit Zoom aus dem angetippten Icon starten.
    SideEffect { vm.animatedLaunch = ::launch }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { vm.animatedLaunch = null } }

    fun perform(action: GestureAction, binding: de.umstieg.launcher.core.GestureBinding?) {
        when (action) {
            GestureAction.LOCK_SCREEN -> de.umstieg.launcher.data.EdgeGestureService.lockScreen()
            // Eigene Liste auch ohne Zugriff: das Panel erklärt dann, wie man ihn erteilt (kein stiller Wechsel).
            GestureAction.NOTIFICATIONS -> if (s.ownNotificationList || !vm.system.expandStatusBar(false)) vm.showOverlay(Overlay.NOTIFICATIONS)
            GestureAction.QUICK_SETTINGS -> vm.showOverlay(Overlay.QUICK_SETTINGS)
            GestureAction.SYSTEM_NOTIFICATIONS, GestureAction.SYSTEM_QUICK_SETTINGS -> {
                // Kein Bedienungshilfe-Dienst: klappt das Aufklappen nicht, nur ein Hinweis.
                if (!vm.system.expandStatusBar(action == GestureAction.SYSTEM_QUICK_SETTINGS)) {
                    Toast.makeText(context, R.string.system_panel_unavailable, Toast.LENGTH_LONG).show()
                }
            }
            GestureAction.SEARCH -> vm.showOverlay(Overlay.SEARCH)
            GestureAction.APP_DRAWER -> if (s.drawerEnabled) vm.goToDrawer() else vm.showOverlay(Overlay.SEARCH)
            GestureAction.OPEN_APP -> binding?.app?.let { apps[it] }?.let { launch(it, Rect.Zero) }
            GestureAction.ASSISTANT -> if (!vm.system.openAssistant(context)) {
                Toast.makeText(context, R.string.assistant_missing, Toast.LENGTH_SHORT).show()
            }
            GestureAction.NOTHING -> Unit
        }
    }
    // Wisch auf der Statusleiste (Bedienungshilfe): Aktion mit der Belegung der Zone ausführen.
    LaunchedEffect(Unit) {
        vm.externalActions.collect { a ->
            val edge = listOf(de.umstieg.launcher.core.Gesture.EDGE_LEFT, de.umstieg.launcher.core.Gesture.EDGE_RIGHT, de.umstieg.launcher.core.Gesture.EDGE)
                .map { GestureClassifier.binding(current.settings, it) }.firstOrNull { it.action == a }
            perform(a, edge)
            vm.actionHandled()
        }
    }

    fun gesture(g: Gesture) {
        val b = GestureClassifier.binding(s, g)
        if (b.action != GestureAction.NOTHING) view.tosHaptic(s)
        perform(b.action, b)
    }

    fun homePage(): Int? = currentMap.homeIndex(pagerState.currentPage)

    /** Panel zur Gestenaktion, das beim Wischen dem Finger folgt (sonst null: Aktion beim Schwellwert). */
    fun overlayFor(action: GestureAction): Overlay? = when (action) {
        GestureAction.NOTIFICATIONS -> if (current.settings.ownNotificationList) Overlay.NOTIFICATIONS else null
        GestureAction.QUICK_SETTINGS -> Overlay.QUICK_SETTINGS
        GestureAction.SEARCH -> Overlay.SEARCH
        else -> null
    }

    fun hitTest(p: Offset): Hit? {
        val layout = current.layout
        val folderId = currentFolder
        if (folderId != null) {
            val folder = LayoutOps.findFolder(layout, folderId) ?: return null
            val geo = folderGeo ?: return null
            if (!folderPanel.contains(p)) return null
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
        val page = homePage() ?: return null
        val (x, y) = geo.cellAt(p) ?: return null
        val placed = LayoutOps.placedAt(layout, page, x, y) ?: return null
        val center = if (placed.item is de.umstieg.launcher.core.WidgetItem) {
            geo.iconCenter(placed.x, placed.y)
        } else {
            geo.iconCenter(x, y)
        }
        return Hit(placed.item, Slot.Grid(page, placed.x, placed.y), center)
    }

    /**
     * Ziel unter dem gezogenen Icon. Liegt dessen Mitte über einem anderen Icon, wird erst verweilt
     * ([de.umstieg.launcher.core.MergeDwell], ca. 400 ms): solange weicht nichts aus, danach gilt es als
     * Ordner-Ziel (Vorschau + Haptik). Maßgeblich ist die Icon-Mitte, nicht der Finger.
     */
    fun targetAt(pointer: Offset): DropTarget? {
        val layout = current.layout
        val now = android.os.SystemClock.uptimeMillis()
        // Widgets: Ziel ist die linke obere Zelle (unter der gegriffenen Stelle verschoben).
        val p = if (drag.item is de.umstieg.launcher.core.WidgetItem) pointer - drag.grab else pointer
        val ic = pointer - drag.grab
        val mayMerge = drag.item is AppItem && current.settings.folderByDrag
        fun dwellFor(key: String?): Boolean {
            val ready = drag.dwell.update(key, now)
            drag.holdKey = if (key != null && !ready) key else null
            return ready
        }
        dockGeo?.let { geo ->
            if (geo.area.inflate(geo.iconPx * 0.2f).contains(p)) {
                val i = geo.indexAt(p)
                val over = mayMerge && layout.dock.getOrNull(i) != null && !(drag.from == Slot.Dock(i)) && geo.isOnIcon(ic, i)
                return DropTarget.Dock(i, merge = dwellFor(if (over) "d:$i" else null))
            }
        }
        val geo = gridGeo ?: return null.also { dwellFor(null) }
        val page = homePage() ?: return null.also { dwellFor(null) }
        // Zelle nach der Icon-Mitte (dort, wo das gezogene Icon sichtbar ist).
        val (x, y) = geo.cellAt(if (drag.item is AppItem || drag.item is FolderItem) ic else p) ?: geo.cellAt(p) ?: return null.also { dwellFor(null) }
        val occupant = LayoutOps.itemAt(layout, page, x, y)
        val self = drag.from == Slot.Grid(page, x, y)
        val over = mayMerge && occupant != null && !self && occupant !is de.umstieg.launcher.core.WidgetItem && geo.isOnIcon(ic, x, y)
        return DropTarget.Grid(page, x, y, merge = dwellFor(if (over) "g:$page:$x:$y" else null))
    }

    fun finishDrag() {
        val from = drag.from
        val item = drag.item
        if (from != null && item != null) {
            val folderId = currentFolder
            val fGeo = folderGeo
            if (from is Slot.InFolder && folderId != null && fGeo != null && folderPanel.contains(drag.pointer)) {
                val (x, y) = fGeo.cellAt(drag.pointer) ?: (0 to 0)
                vm.moveInFolder(folderId, from.index, y * fGeo.columns + x)
            } else {
                drag.target?.let { vm.drop(from, it) }
            }
            view.tosHaptic(current.settings)
        }
        drag.clear()
    }

    // Vorschau des Ergebnisses: andere Icons weichen aus.
    val preview by remember {
        derivedStateOf {
            val from = drag.from
            val t = drag.target
            val l = current.layout
            // Beim Verweilen über einem Icon nichts verschieben, sonst flieht das Ziel unter dem Finger weg.
            if (from == null || t == null || t.merge || drag.holdKey != null) {
                l
            } else {
                LayoutOps.drop(l, current.settings.grid, current.settings.dockCapacity, from, t, false) { a, b ->
                    FolderItem("preview", "", listOf(a, b))
                }
            }
        }
    }
    // Haptik, wenn das Ziel ein Ordner-Ziel wird.
    val mergeTarget by remember { derivedStateOf { drag.target?.merge == true } }
    LaunchedEffect(mergeTarget) { if (mergeTarget) view.tosHaptic(s) }
    // Auch ohne Fingerbewegung: nach der Verweilzeit das Ziel neu bewerten (wird zum Ordner-Ziel).
    LaunchedEffect(drag.holdKey) {
        if (drag.holdKey == null) return@LaunchedEffect
        delay(de.umstieg.launcher.core.MergeDwell.DEFAULT_MS + 20)
        if (drag.active) drag.target = targetAt(drag.pointer)
    }

    // Seitenwechsel, wenn das gezogene Icon am Rand verweilt.
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
            if (currentMap.homeIndex(target) == null) break
            pagerState.animateScrollToPage(target)
            view.tosHaptic(current.settings)
            drag.target = targetAt(drag.pointer)
        }
    }

    // Endlos blättern: am Ende weiter wischen springt an den Anfang (und umgekehrt).
    val wrap = remember(pagerState) {
        object : NestedScrollConnection {
            var acc = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!current.settings.infiniteScroll || currentEdit) return Offset.Zero
                acc += available.x
                val threshold = 120f
                val last = pagerState.pageCount - 1
                if (acc < -threshold && pagerState.currentPage == last) {
                    acc = 0f
                    scope.launch { pagerState.scrollToPage(0) }
                } else if (acc > threshold && pagerState.currentPage == 0) {
                    acc = 0f
                    scope.launch { pagerState.scrollToPage(last) }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                acc = 0f
                return Velocity.Zero
            }
        }
    }

    val env = GridEnv(d, s, apps, badges, shape, editMode, drag, vm, ::launch)
    val hasDoubleTap = s.gestures[Gesture.DOUBLE_TAP]?.action != GestureAction.NOTHING ||
        s.gestures[Gesture.DOUBLE_TAP_BOTTOM]?.action != GestureAction.NOTHING
    val blurRadius = if (blurOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (28 * tosStyle.blur).dp else 0.dp

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    drag.longPressed = false
                }
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { p ->
                        if (currentOverlay != Overlay.NONE) return@detectDragGesturesAfterLongPress
                        if (homePage() == null && currentFolder == null) return@detectDragGesturesAfterLongPress
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
                                vm.openFolder(null)
                            }
                            drag.target = if (currentFolder != null) null else targetAt(change.position)
                        }
                    },
                    onDragEnd = { finishDrag() },
                    onDragCancel = { drag.clear() },
                )
            }
            // Wischgesten (oberer Rand, Bildschirmmitte, nach oben). Läuft in der Initial-Phase, also vor Pager
            // und Icons: sobald die Bewegung den Touch-Slop überschreitet und eher senkrecht ist, gehört sie der
            // Geste (auch wenn sie auf einem Icon beginnt); eher waagerecht bleibt sie dem Pager.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val allowed = currentOverlay == Overlay.NONE && !currentEdit && currentFolder == null &&
                        homePage() != null && !drag.active
                    if (!allowed) return@awaitEachGesture
                    val slop = viewConfiguration.touchSlop
                    var total = Offset.Zero
                    var claimed = false
                    var peekTarget: Overlay? = null
                    val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                    try {
                    while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val ch = ev.changes.firstOrNull { it.id == down.id }
                            if (ch == null || !ch.pressed) {
                                // Losgelassen: nach Strecke oder Tempo öffnen, sonst zurückfedern.
                                val target = peekTarget
                                if (target != null) {
                                    val progress = total.y / (size.height * 0.4f)
                                    // Schon ab etwa 70 dp oder einem kurzen, schnellen Wisch öffnen (wie die frühere Auslöseschwelle).
                                    if (progress > 0.18f || tracker.calculateVelocity().y > 700f) {
                                        view.tosHaptic(current.settings)
                                        vm.showOverlay(target)
                                    } else {
                                        vm.setPeek(null)
                                    }
                                }
                                break
                            }
                            if (drag.longPressed || drag.active) {
                                vm.setPeek(null)
                                break
                            }
                            total += ch.positionChange()
                            if (!claimed) {
                                if (abs(total.x) > slop && abs(total.x) >= abs(total.y)) break
                                if (abs(total.y) > slop && abs(total.y) > abs(total.x)) claimed = true
                            }
                            if (!claimed) continue
                            ch.consume()
                            val edgeBottom = (statusTop + size.height * current.settings.edgeHeightPercent / 100f) / size.height
                            // Panels (eigene Benachrichtigungen, Schnellzugriff, Suche) folgen dem Finger.
                            if (peekTarget == null && total.y > 0f) {
                                val early = GestureClassifier.classify(
                                    down.position.x / size.width, down.position.y / size.height, total.x / size.height, 1f,
                                    edgeBottom, current.settings.edgeSplit, current.settings.swipeZoneLeft, current.settings.swipeZoneRight,
                                    minDistance = 0f, dominance = 0f,
                                )
                                peekTarget = early?.let { overlayFor(GestureClassifier.binding(current.settings, it).action) }
                            }
                            val target = peekTarget
                            if (target != null) {
                                tracker.addPosition(ch.uptimeMillis, ch.position)
                                vm.setPeek(target, total.y / (size.height * 0.4f))
                                continue
                            }
                            val g = GestureClassifier.classify(
                                down.position.x / size.width, down.position.y / size.height,
                                total.x / size.height, total.y / size.height,
                                edgeBottom, current.settings.edgeSplit,
                                current.settings.swipeZoneLeft, current.settings.swipeZoneRight,
                                minDistance = SWIPE_TRIGGER_DP.dp.toPx() / size.height,
                                dominance = 1f,
                            )
                            if (g != null) {
                                gesture(g)
                                do {
                                    val rest = awaitPointerEvent(PointerEventPass.Initial)
                                    rest.changes.forEach { it.consume() }
                                } while (rest.changes.any { it.pressed })
                                break
                            }
                        }
                    } finally {
                        // Abgebrochene Geste (z. B. Home-Taste mitten im Wisch): kein halb offenes Panel zurücklassen.
                        if (peekTarget != null && vm.overlay.value == Overlay.NONE) vm.setPeek(null)
                    }
                }
            }
            .pointerInput(hasDoubleTap) {
                detectTapGestures(
                    onDoubleTap = if (hasDoubleTap) {
                        { p ->
                            if (currentOverlay == Overlay.NONE && !currentEdit && homePage() != null) {
                                gesture(GestureClassifier.doubleTap(p.y / size.height))
                            }
                        }
                    } else {
                        null
                    },
                    onTap = { if (!drag.longPressed && currentEdit && currentFolder == null) vm.setEditMode(false) },
                )
            },
    ) {
        // Hintergrund abdunkeln (Einstellung).
        if (tosStyle.dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = tosStyle.dim)))

        Column(
            Modifier
                .fillMaxSize()
                .blur(blurRadius)
                .graphicsLayer { alpha = if (openFolderId != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) 0.35f else 1f }
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(vertical = (s.topBottomPaddingDp / 2).dp),
        ) {
            Box(Modifier.fillMaxWidth().height(TOP_BAR_HEIGHT)) {
                if (editMode) {
                    EditTopBar(vm, actions, if (s.widgetsOnHome) ({ vm.showWidgetPicker(WidgetTarget.Grid(map.homeIndex(pagerState.currentPage) ?: 0)) }) else null)
                }
            }
            // Pager über die volle Höhe; Punkte und Dock liegen unten darüber. Homeseiten halten deren Platz frei,
            // App-Übersicht und Widget-Seite nutzen die Fläche bis unten.
            var bottomHeight by remember { mutableStateOf(0) }
            val bottomDp = with(density) { bottomHeight.toDp() }
            Box(Modifier.weight(1f).fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(wrap)
                    .onGloballyPositioned { pagerBounds = it.boundsInRoot() },
                userScrollEnabled = !drag.active,
                beyondViewportPageCount = 1,
                key = { it },
            ) { index ->
                Box(Modifier.fillMaxSize().pageTransition(pagerState, index, if (tosStyle.reduceMotion) PageTransition.SLIDE else s.pageTransition)) {
                    val home = map.homeIndex(index)
                    when {
                        home != null -> Box(Modifier.padding(bottom = bottomDp)) {
                            PageGrid(home, if (drag.active) preview else d.layout, env) { dy, px -> gridMetrics = dy to px }
                        }
                        index == map.drawerIndex -> DrawerPage(vm, d, apps, shape, actions)
                        else -> WidgetPage(vm, d, actions)
                    }
                }
            }
            val homeNow = map.homeIndex(pagerState.currentPage)
            if (homeNow == null) {
                SideEffect { dockGeo = null }
            } else Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { bottomHeight = it.height }) {
            if (homeNow != null) {
                // Feste Höhe, damit Raster und Pager beim Bearbeiten/Ziehen nicht springen; der Hinweis liegt darüber.
                Box(Modifier.fillMaxWidth().height(INDICATOR_AREA), contentAlignment = Alignment.Center) {
                    // Punkte: im Ruhezustand ausgeblendet, beim Blättern sichtbar (bis 1,5 s danach), im
                    // Bearbeitungsmodus immer; wenn keine Geste die Suche öffnet, ebenfalls immer (Suche per Tipp).
                    var recentlyScrolled by remember { mutableStateOf(false) }
                    LaunchedEffect(pagerState.isScrollInProgress) {
                        if (pagerState.isScrollInProgress) {
                            recentlyScrolled = true
                        } else {
                            delay(1_500)
                            recentlyScrolled = false
                        }
                    }
                    val searchByGesture = GestureClassifier.searchReachableByGesture(s)
                    val dotsShown = editMode || !searchByGesture || when (s.dotsMode) {
                        de.umstieg.launcher.core.DotsMode.ALWAYS -> true
                        de.umstieg.launcher.core.DotsMode.SCROLL -> recentlyScrolled
                        de.umstieg.launcher.core.DotsMode.OFF -> false
                    }
                    val dotsAlpha by androidx.compose.animation.core.animateFloatAsState(
                        if (dotsShown && s.indicator != de.umstieg.launcher.core.IndicatorStyle.NONE) 1f else 0f,
                        androidx.compose.animation.core.tween(if (tosStyle.reduceMotion) 0 else 260),
                        label = "dots",
                    )
                    val maxDot = (map.homePages - 1).toFloat()
                    if (dotsAlpha > 0f) {
                        PageIndicator(
                            style = s.indicator,
                            pageCount = map.homePages,
                            position = { ((pagerState.currentPage - map.firstHome) + pagerState.currentPageOffsetFraction).coerceIn(0f, maxDot) },
                            hasNewPage = editMode,
                            modifier = Modifier.graphicsLayer { alpha = dotsAlpha },
                            onSearch = if (editMode || !dotsShown) null else ({ vm.showOverlay(Overlay.SEARCH) }),
                        )
                    }
                    if (editMode && drag.active && currentFolder == null) {
                        Text(
                            stringResource(R.string.edit_hint_edge),
                            style = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = Tos.Ink2, textAlign = TextAlign.Center),
                            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-26).dp).fillMaxWidth(),
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }
            if (s.dockEnabled) {
                Dock(env, if (drag.active) preview else d.layout) { dockGeo = it }
            } else {
                SideEffect { dockGeo = null }
            }
            }
            }
        }

        // Geöffneter Ordner.
        val folder = openFolderId?.let { LayoutOps.findFolder(d.layout, it) }
        if (openFolderId != null && folder == null) LaunchedEffect(openFolderId) { vm.openFolder(null) }
        if (folder != null) {
            FolderOverlay(folder, env) { panel, grid ->
                folderPanel = panel
                folderGeo = grid
            }
        } else {
            SideEffect { folderGeo = null }
        }

        // Hinweise oben: Einrichtung fortsetzen bzw. Update verfügbar (wegwischbar).
        val setupVisible = !d.onboardingDone || setupRequested
        var factsTick by remember { mutableStateOf(0) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { factsTick++ }
        val openSteps = remember(factsTick, d, isDefault) {
            de.umstieg.launcher.core.SetupLogic.open(setupFacts(vm, d, isDefault, context), d.setupDeclined)
        }
        val update = (updateState as? UpdateState.Available)?.release
        if (!setupVisible && !editMode && overlay == Overlay.NONE && peek == null && openFolderId == null && map.homeIndex(pagerState.currentPage) != null) {
            if (!d.setupCardDismissed && openSteps.isNotEmpty()) {
                SetupCard(openSteps.size, onOpen = { vm.showSetup(true) }, onDismiss = {
                    vm.controller.updateData { it.copy(setupCardDismissed = true, setupDeclined = it.setupDeclined + openSteps) }
                }, modifier = Modifier.align(Alignment.TopCenter))
            } else if (update != null && update.version.name != d.updateDismissed && d.settings.autoUpdateCheck) {
                UpdateCard(update.version.name, onOpen = { actions.openSettings("UPDATES") }, onDismiss = {
                    vm.controller.updateData { it.copy(updateDismissed = update.version.name) }
                }, modifier = Modifier.align(Alignment.TopCenter))
            }
        }

        AnimatedVisibility(lockedHint, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            LockedHintCard {
                lockedHint = false
                actions.openSettings(null)
            }
        }

        if (addSheet) {
            AddAppsSheet(d, apps, shape, vm) { key -> vm.addApp(key, map.homeIndex(pagerState.currentPage) ?: 0) }
        }

        // Gezogenes Icon über allem.
        drag.item?.let { item ->
            val geo = gridGeo
            val iconPx = geo?.iconPx ?: 150f
            val half = iconPx / 2f
            if (item is de.umstieg.launcher.core.WidgetItem) {
                val w = with(density) { ((geo?.cellW ?: 200f) * item.spanX).toDp() }
                val h = with(density) { ((geo?.cellH ?: 200f) * item.spanY).toDp() }
                val ws = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
                Box(
                    Modifier
                        .offset { IntOffset((drag.pointer.x - drag.grab.x - half).roundToInt(), (drag.pointer.y - drag.grab.y - half).roundToInt()) }
                        .size(w, h)
                        .padding(4.dp)
                        .background(Tos.Accent.copy(alpha = 0.25f), ws)
                        .border(2.dp, Tos.Accent, ws),
                )
            } else {
                val iconDp = with(density) { iconPx.toDp() }
                Box(
                    Modifier
                        .offset { IntOffset((drag.pointer.x - drag.grab.x - half).roundToInt(), (drag.pointer.y - drag.grab.y - half).roundToInt()) }
                        .graphicsLayer {
                            scaleX = 1.1f
                            scaleY = 1.1f
                        },
                ) {
                    HomeItemView(
                        item = item, apps = apps, style = iconStyle(vm.icons, s, shape, iconDp), showLabel = false,
                        editMode = false, wobble = false, removeDescription = null, highlighted = false, hidden = false,
                        onClick = {}, onRemove = {}, onStartEdit = null,
                    )
                }
            }
        }

        openCategory?.let { oc -> de.umstieg.launcher.ui.drawer.CategoryPanel(vm, d, apps, oc.category, oc.origin, shape) }

        // Vollbild-Panels.
        OverlayHost(overlay, vm, d, apps, shape, actions, ::launch)

        widgetPicker?.let { target -> WidgetPicker(vm, target, actions) }
        resizeWidget?.let { id -> ResizeWidgetDialog(vm, d, id) }
        menuApp?.let { app -> AppMenuSheet(vm, d, app, actions, map.homeIndex(pagerState.currentPage) ?: 0) }
        customize?.let { app -> CustomizeDialog(vm, d, app) }
        quietApp?.let { app -> QuietProfileDialog(vm, app) }

        if (setupVisible && !editMode) {
            SetupWizard(vm, d, isDefault, actions) { vm.showSetup(false) }
        }
    }
}

/** Seitenübergang beim Blättern: Schieben, Stapeln, Blenden, Würfel. */
private fun Modifier.pageTransition(state: PagerState, page: Int, transition: PageTransition): Modifier = graphicsLayer {
    if (transition == PageTransition.SLIDE) return@graphicsLayer
    val offset = (state.currentPage - page) + state.currentPageOffsetFraction
    if (abs(offset) >= 1f) return@graphicsLayer
    when (transition) {
        PageTransition.FADE -> {
            translationX = offset * size.width
            alpha = 1f - abs(offset)
        }
        PageTransition.STACK -> if (offset > 0f) {
            translationX = offset * size.width
            val sc = 1f - 0.12f * offset
            scaleX = sc
            scaleY = sc
            alpha = 1f - offset
        }
        PageTransition.CUBE -> {
            cameraDistance = 12f * density
            transformOrigin = TransformOrigin(if (offset < 0f) 0f else 1f, 0.5f)
            rotationY = -offset * 70f
        }
        PageTransition.SLIDE -> Unit
    }
}

val TOP_BAR_HEIGHT = 64.dp
private val INDICATOR_AREA = 76.dp
private const val EDGE_DWELL_MS = 600L

/** Strecke, ab der ein beanspruchter senkrechter Wisch auslöst. */
private const val SWIPE_TRIGGER_DP = 40
