package de.umstieg.launcher.ui.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppCategory
import de.umstieg.launcher.core.AppMeta
import de.umstieg.launcher.core.Categories
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.Shadow
import de.umstieg.launcher.core.iconOptions
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.ui.home.HomeViewModel
import de.umstieg.launcher.ui.home.LabelLook
import de.umstieg.launcher.ui.home.labelTextStyle
import de.umstieg.launcher.ui.icons.AppIconImage
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.LocalPanelColors
import de.umstieg.launcher.ui.theme.LocalTosStyle
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.glass
import kotlinx.coroutines.launch

/** Gruppen der App-Übersicht (Zuletzt installiert zuerst). */
fun categoryGroups(list: List<AppInfo>): List<Pair<AppCategory, List<AppInfo>>> {
    val byKey = list.associateBy { it.key }
    return Categories.group(list.map { AppMeta(it.key, it.label, it.category, it.firstInstallTime) }).map { (cat, metas) ->
        cat to metas.mapNotNull { byKey[it.key] }
    }
}

/** Spalten × Reihen der Kachel je Anzahl sichtbarer Apps (4 → 2×2, 6 → 3×2, 9 → 3×3). */
private fun tileGrid(n: Int): Pair<Int, Int> = when (n) {
    6 -> 3 to 2
    9 -> 3 to 3
    else -> 2 to 2
}

/** App-Übersicht als Kacheln: zwei Spalten, Kategoriename darunter; „+N“ öffnet die Kategorie. */
@Composable
fun TileGrid(vm: HomeViewModel, d: LauncherData, list: List<AppInfo>, shape: SuperellipseShape) {
    val groups = remember(list) { categoryGroups(list) }
    LazyVerticalGrid(
        GridCells.Fixed(2),
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(groups, key = { it.first.name }) { (cat, apps) -> CategoryTile(vm, d, cat, apps, shape) }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) { Spacer(Modifier.height(40.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryTile(vm: HomeViewModel, d: LauncherData, cat: AppCategory, apps: List<AppInfo>, shape: SuperellipseShape) {
    val n = d.settings.tileApps
    val (cols, rows) = tileGrid(n)
    val overflow = apps.size > n
    val shown = if (overflow) apps.take(n - 1) else apps.take(n)
    val rest = apps.size - shown.size
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val name = categoryName(cat)
    val openLabel = stringResource(R.string.drawer_open_category, name)
    val menu = stringResource(R.string.app_menu_open)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(cols.toFloat() / rows)
                .onGloballyPositioned { bounds = it.boundsInWindow() }
                .glass(RoundedCornerShape(30.dp), Tos.Glass, Tos.GlassLine)
                .padding(12.dp),
        ) {
            val cell: Dp = minOf(maxWidth / cols, maxHeight / rows)
            val icon = cell * 0.84f
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (c in 0 until cols) {
                            val i = r * cols + c
                            val app = shown.getOrNull(i)
                            Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
                                when {
                                    app != null -> {
                                        var iconBounds by remember { mutableStateOf(Rect.Zero) }
                                        AppIconImage(
                                        vm.icons, app, d.settings.iconOptions(), shape, icon,
                                        modifier = Modifier
                                            .onGloballyPositioned { iconBounds = it.boundsInWindow() }
                                            .semantics { contentDescription = app.label }
                                            .combinedClickable(role = Role.Button, onLongClickLabel = menu, onLongClick = { vm.showMenu(app) }) {
                                                vm.controller.recordSearch(null, app.key)
                                                vm.launchFrom(app, iconBounds)
                                            },
                                        shadow = d.settings.iconShadow,
                                    )
                                    }
                                    overflow && i == n - 1 -> MoreSlot(vm, d, apps.drop(shown.size).take(4), rest, shape, icon) {
                                        vm.openCategory(cat, bounds)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            style = labelTextStyle(LabelLook(d.settings.fontScale, d.settings.labelWeight, 1, true, d.settings.labelColor), null).copy(fontSize = 16.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClickLabel = openLabel, role = Role.Button) { vm.openCategory(cat, bounds) }.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Letzter Platz bei vielen Apps: Mini-Icons der übrigen und „+N“. */
@Composable
private fun MoreSlot(vm: HomeViewModel, d: LauncherData, apps: List<AppInfo>, rest: Int, shape: SuperellipseShape, size: Dp, onClick: () -> Unit) {
    val desc = pluralStringResource(R.plurals.drawer_more_apps, rest, rest)
    Box(
        Modifier.size(size).background(Color.White.copy(alpha = 0.14f), shape).clickable(onClickLabel = desc, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        // Wie im Entwurf: Mini-Icons füllen die Fläche, darüber eine dunkle Schicht mit „+N“.
        Column(verticalArrangement = Arrangement.spacedBy(size * 0.05f)) {
            for (r in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(size * 0.05f)) {
                    for (c in 0 until 2) {
                        val app = apps.getOrNull(r * 2 + c)
                        if (app != null) AppIconImage(vm.icons, app, d.settings.iconOptions(), shape, size * 0.38f, shadow = Shadow.NONE) else Spacer(Modifier.size(size * 0.38f))
                    }
                }
            }
        }
        Box(Modifier.matchParentSize().background(Color(0x730A0E14), shape), contentAlignment = Alignment.Center) {
            Text("+$rest", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White))
        }
    }
}

/**
 * Zoom aus der Herkunft ([origin], Fensterkoordinaten) in die endgültige Lage. Liefert den Modifier für
 * das Panel, den Fortschritt (für Abdunkelung) und eine Funktion zum Schließen mit Rück-Zoom.
 */
class ZoomState(val modifier: Modifier, val progress: () -> Float, val close: (() -> Unit) -> Unit)

@Composable
fun rememberZoomFrom(origin: Rect?): ZoomState {
    val reduce = LocalTosStyle.current.reduceMotion || LocalTosStyle.current.motionScale == 0f
    val anim = remember { Animatable(if (reduce) 1f else 0f) }
    val scope = rememberCoroutineScope()
    var panel by remember { mutableStateOf(Rect.Zero) }
    val stiffness = 420f * LocalTosStyle.current.motionScale.coerceIn(0.5f, 2f)
    LaunchedEffect(Unit) { if (!reduce) anim.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = stiffness)) }
    val mod = Modifier
        .onGloballyPositioned { panel = it.boundsInRoot() }
        .graphicsLayer {
            val p = anim.value
            if (origin != null && origin.width > 0f && panel.width > 0f) {
                val s0 = (origin.width / panel.width).coerceIn(0.12f, 1f)
                val sc = s0 + (1f - s0) * p
                scaleX = sc
                scaleY = sc
                translationX = (origin.center.x - panel.center.x) * (1f - p)
                translationY = (origin.center.y - panel.center.y) * (1f - p)
            }
            alpha = (0.2f + 0.8f * p).coerceIn(0f, 1f)
        }
    return ZoomState(mod, { anim.value }) { done ->
        if (reduce) {
            done()
        } else {
            scope.launch {
                anim.animateTo(0f, spring(dampingRatio = 1f, stiffness = stiffness * 1.4f))
                done()
            }
        }
    }
}

/** Geöffnete Kategorie als Panel wie ein Ordner (Zoom aus der Kachel). */
@Composable
fun CategoryPanel(vm: HomeViewModel, d: LauncherData, apps: Map<de.umstieg.launcher.core.AppKey, AppInfo>, cat: AppCategory, origin: Rect?, shape: SuperellipseShape) {
    val c = LocalPanelColors.current
    val list = remember(apps, d.hiddenApps) { visibleApps(d, apps) }
    val members = remember(list, cat) { categoryGroups(list).firstOrNull { it.first == cat }?.second.orEmpty() }
    val zoom = rememberZoomFrom(origin)
    val columns = 3
    val maxPanel = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    fun close() = zoom.close { vm.openCategory(null, null) }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = zoom.progress() }
            .background(c.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() }
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                zoom.modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .glass(Tos.PanelShape, Color(0xE6141B27), Tos.GlassLine)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(horizontal = 18.dp, vertical = 22.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(categoryName(cat), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Tos.Ink), modifier = Modifier.weight(1f).semantics { heading() })
                    Text(pluralStringResource(R.plurals.app_count, members.size, members.size), style = TextStyle(fontFamily = Inter, fontSize = 17.sp, color = Tos.Ink2))
                }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth().heightIn(max = maxPanel).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    members.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { app ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                    var iconBounds by remember { mutableStateOf(Rect.Zero) }
                                    Column(
                                        Modifier.onGloballyPositioned { iconBounds = it.boundsInWindow() }.clickable(role = Role.Button) {
                                            vm.openCategory(null, null)
                                            vm.controller.recordSearch(null, app.key)
                                            vm.launchFrom(app, iconBounds)
                                        },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        AppIconImage(vm.icons, app, d.settings.iconOptions(), shape, 60.dp, shadow = d.settings.iconShadow)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            app.label,
                                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Tos.Ink, textAlign = TextAlign.Center),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.width(92.dp),
                                        )
                                    }
                                }
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.category_panel_hint), style = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Tos.Ink2, textAlign = TextAlign.Center))
        }
    }
}
