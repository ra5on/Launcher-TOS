package de.umstieg.launcher.ui.drawer

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppCategory
import de.umstieg.launcher.core.AppMeta
import de.umstieg.launcher.core.BadgeCounter
import de.umstieg.launcher.core.Calculator
import de.umstieg.launcher.core.Categories
import de.umstieg.launcher.core.DrawerView
import de.umstieg.launcher.core.FuzzySearch
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.iconOptions
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.ui.home.CountBadge
import de.umstieg.launcher.ui.home.HomeActions
import de.umstieg.launcher.ui.home.HomeViewModel
import de.umstieg.launcher.ui.home.LabelLook
import de.umstieg.launcher.ui.home.Overlay
import de.umstieg.launcher.ui.home.BadgeLook
import de.umstieg.launcher.ui.home.labelTextStyle
import de.umstieg.launcher.ui.icons.AppIconImage
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.settings.SettingsIndex
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.glass

private val SectionStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.8.sp)

/** App-Übersicht (letzte Seite rechts): Kategorien, A–Z oder Raster; oben ein Suchfeld. */
@Composable
fun DrawerPage(vm: HomeViewModel, d: LauncherData, apps: Map<de.umstieg.launcher.core.AppKey, AppInfo>, shape: SuperellipseShape, actions: HomeActions) {
    val list = remember(apps, d.hiddenApps) { visibleApps(d, apps) }
    val view = d.settings.drawerView
    val viewName = stringResource(
        when (view) {
            DrawerView.TILES -> R.string.drawer_view_tiles
            DrawerView.CATEGORIES -> R.string.drawer_view_categories
            DrawerView.AZ -> R.string.drawer_view_az
            DrawerView.GRID -> R.string.drawer_view_grid
        },
    )
    Column(Modifier.fillMaxSize().wallpaperScrim().padding(horizontal = 16.dp)) {
        FakeSearchField { vm.showOverlay(Overlay.SEARCH) }
        if (view == DrawerView.TILES) Spacer(Modifier.height(16.dp))
        if (view != DrawerView.TILES) Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                stringResource(R.string.drawer_title),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Tos.Ink),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            val switchLabel = stringResource(R.string.drawer_switch_view)
            Text(
                pluralStringResource(R.plurals.app_count, list.size, list.size) + " · " + viewName,
                style = TextStyle(fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Tos.Ink2),
                modifier = Modifier
                    .clickable(onClickLabel = switchLabel, role = Role.Button) {
                        val next = DrawerView.entries[(view.ordinal + 1) % DrawerView.entries.size]
                        vm.controller.updateSettings { it.copy(drawerView = next) }
                    }
                    .padding(8.dp),
            )
        }
        when (view) {
            DrawerView.TILES -> TileGrid(vm, d, list, shape)
            DrawerView.CATEGORIES -> CategoryList(vm, d, list, shape)
            DrawerView.AZ -> AzList(vm, d, list, shape, Modifier.weight(1f), prefix = {})
            DrawerView.GRID -> AppGrid(vm, d, list, shape)
        }
    }
}

/** Abdunkelung hinter Seiten auf hellem Hintergrund, damit Titel und Texte lesbar bleiben. */
@Composable
fun Modifier.wallpaperScrim(): Modifier {
    if (!de.umstieg.launcher.ui.theme.LocalTosStyle.current.lightWallpaper) return this
    return this.background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x8C0B0F16), Color(0x590B0F16), Color(0x730B0F16))))
}

@Composable
private fun FakeSearchField(onClick: () -> Unit) {
    val label = stringResource(R.string.drawer_search_hint)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .glass(Tos.PillShape, Tos.Glass, Tos.GlassLine)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_lucide_search), null, tint = Tos.Ink2, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = TextStyle(fontFamily = Inter, fontSize = 17.sp, color = Tos.Ink2))
    }
}

private fun launchFrom(vm: HomeViewModel, app: AppInfo, query: String?, launch: ((AppInfo, Rect) -> Unit)?) {
    vm.controller.recordSearch(query, app.key)
    if (launch != null) {
        launch(app, Rect.Zero)
    } else {
        vm.launch(app, null, null)
    }
}

/** Icon mit Beschriftung und Badge; lange drücken öffnet das App-Menü. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(vm: HomeViewModel, d: LauncherData, app: AppInfo, shape: SuperellipseShape, size: androidx.compose.ui.unit.Dp, onLaunch: () -> Unit) {
    val badges by vm.badges.collectAsStateWithLifecycle()
    val count = if (d.settings.badgesEnabled) BadgeCounter.forApp(badges, app.key, d.badgeDisabled) else 0
    val menu = stringResource(R.string.app_menu_open)
    Column(
        Modifier
            .combinedClickable(role = Role.Button, onLongClickLabel = menu, onLongClick = { vm.showMenu(app) }, onClick = onLaunch)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            AppIconImage(vm.icons, app, d.settings.iconOptions(), shape, size, shadow = d.settings.iconShadow)
            CountBadge(count, BadgeLook(d.settings.badgesEnabled, d.settings.badgeStyle, d.settings.badgeColor), vm.icons.colorOf(app)?.let { Color(it) }, Modifier.align(Alignment.TopEnd))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            style = labelTextStyle(LabelLook(d.settings.fontScale, d.settings.labelWeight, 1, d.settings.labelShadow, d.settings.labelColor), null),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(size * 1.4f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryList(vm: HomeViewModel, d: LauncherData, list: List<AppInfo>, shape: SuperellipseShape) {
    val groups = remember(list) {
        val byKey = list.associateBy { it.key }
        Categories.group(list.map { AppMeta(it.key, it.label, it.category, it.firstInstallTime) }).map { (cat, metas) ->
            cat to metas.mapNotNull { byKey[it.key] }
        }
    }
    val expanded = remember { mutableStateMapOf<AppCategory, Boolean>() }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { (cat, apps) ->
            item(key = cat.name) {
                val open = expanded[cat] == true
                Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), Tos.Glass, Tos.GlassLine).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(painterResource(categoryIcon(cat)), null, tint = Tos.Ink, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(categoryName(cat), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Tos.Ink))
                        Spacer(Modifier.width(8.dp))
                        Text("${apps.size}", style = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Tos.Ink2), modifier = Modifier.weight(1f))
                        if (apps.size > 5) {
                            Text(
                                stringResource(if (open) R.string.drawer_less else R.string.drawer_all),
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Tos.Accent),
                                modifier = Modifier.clickable(role = Role.Button) { expanded[cat] = !open }.padding(8.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, maxItemsInEachRow = 5) {
                        (if (open) apps else apps.take(5)).forEach { app ->
                            AppTile(vm, d, app, shape, 52.dp) { launchFrom(vm, app, null, null) }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AppGrid(vm: HomeViewModel, d: LauncherData, list: List<AppInfo>, shape: SuperellipseShape) {
    LazyVerticalGrid(GridCells.Adaptive(84.dp), Modifier.fillMaxSize()) {
        items(list, key = { it.key.id }) { app -> AppTile(vm, d, app, shape, 54.dp) { launchFrom(vm, app, null, null) } }
    }
}

/** A–Z-Liste mit Buchstaben-Leiste; [prefix] fügt oben eigene Einträge ein (z. B. Suchtreffer). */
@Composable
fun AzList(
    vm: HomeViewModel,
    d: LauncherData,
    list: List<AppInfo>,
    shape: SuperellipseShape,
    modifier: Modifier = Modifier,
    query: String? = null,
    launch: ((AppInfo, Rect) -> Unit)? = null,
    prefixCount: Int = 0,
    prefix: LazyListScope.() -> Unit,
) {
    val state = rememberLazyListState()
    // Neue Treffer werden vor der A–Z-Liste eingefügt; die LazyColumn hält sonst den bisher obersten
    // Eintrag fest und die Treffer lägen unsichtbar darüber. Deshalb bei jeder Eingabe nach oben.
    LaunchedEffect(query) { state.scrollToItem(0) }
    val groups = remember(list) { list.groupBy { Categories.indexLetter(it.label) }.toSortedMap(compareBy { if (it == '#') '~' else it }) }
    val headerIndex = remember(groups, prefixCount) {
        var i = prefixCount
        groups.mapValues { (_, apps) -> i.also { i += 1 + apps.size } }
    }
    Row(modifier.fillMaxWidth()) {
        LazyColumn(Modifier.weight(1f).fillMaxSize(), state = state) {
            prefix()
            groups.forEach { (letter, apps) ->
                item(key = "h$letter") {
                    Text(
                        letter.toString(),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Tos.Ink),
                        modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 6.dp).semantics { heading() },
                    )
                }
                apps.forEach { app ->
                    item(key = "a" + app.key.id) {
                        AppRow(
                            app, vm.icons, d, shape, AnnotatedString(app.label), null, Tos.Ink, Tos.Ink2,
                            onClick = { launchFrom(vm, app, query, launch) },
                            onLongClick = { vm.showMenu(app) },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        LetterRail(groups.keys.toList(), state, { headerIndex[it] })
    }
}

/** Suche als Overlay: Treffer mit Hervorhebung, Rechner, Einstellungen, Verlauf, darunter alle Apps A–Z. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchOverlay(vm: HomeViewModel, d: LauncherData, apps: Map<de.umstieg.launcher.core.AppKey, AppInfo>, shape: SuperellipseShape, actions: HomeActions, launch: (AppInfo, Rect) -> Unit) {
    val context = LocalContext.current
    val s = d.settings
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val list = remember(apps, d.hiddenApps) { visibleApps(d, apps) }
    val recentBoost = remember(d.recentApps) { d.recentApps.withIndex().associate { (i, k) -> k to (40 - i * 4) } }
    val hits = remember(query, list, s.searchFuzziness) {
        if (query.isBlank()) emptyList() else FuzzySearch.rank(list, { it.label }, query, s.searchFuzziness) { recentBoost[it.key] ?: 0 }
    }
    val calc = remember(query, s.searchCalculator) { if (s.searchCalculator) Calculator.evaluate(query) else null }
    val settingTitles = SettingsIndex.entries.map { it to stringResource(it.title) }
    val settingHits = remember(query, s.searchSettings) {
        if (!s.searchSettings || query.trim().length < 2) emptyList() else FuzzySearch.rank(settingTitles, { it.second }, query, s.searchFuzziness).distinctBy { it.item.second }.take(4)
    }
    LaunchedEffect(Unit) { if (s.searchKeyboard) runCatching { focus.requestFocus() } }
    val searchHint = stringResource(R.string.drawer_search_hint)

    fun open(app: AppInfo) {
        vm.controller.recordSearch(query, app.key)
        vm.showOverlay(Overlay.NONE)
        launch(app, Rect.Zero)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xB30B0F16))
            .windowInsetsPadding(WindowInsets.systemBars)
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .glass(Tos.PillShape, Tos.Glass, Tos.GlassLine)
                    .border(1.5.dp, Tos.Accent.copy(alpha = 0.8f), Tos.PillShape)
                    .padding(start = 18.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_lucide_search), null, tint = Tos.Ink2, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.drawer_search_hint), style = TextStyle(fontFamily = Inter, fontSize = 17.sp, color = Tos.Ink3))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it.take(60) },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = Inter, fontSize = 17.sp, color = Tos.Ink),
                        cursorBrush = SolidColor(Tos.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { hits.firstOrNull()?.let { open(it.item) } }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = searchHint },
                    )
                }
                if (query.isNotEmpty()) RoundGlyph(R.drawable.ic_lucide_x, stringResource(R.string.search_clear)) { query = "" }
            }
            Text(
                stringResource(R.string.cancel),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Tos.Accent),
                modifier = Modifier.clickable(role = Role.Button) { vm.showOverlay(Overlay.NONE) }.padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
            )
        }
        val showRecent = query.isBlank() && s.searchHistory && (d.recentApps.isNotEmpty() || d.recentQueries.isNotEmpty())
        // Anzahl der Einträge vor der A–Z-Liste (Sprungziele der Buchstaben-Leiste).
        val prefixCount = (if (calc != null) 1 else 0) +
            (if (hits.isNotEmpty()) 2 else if (query.isNotBlank() && calc == null && settingHits.isEmpty()) 1 else 0) +
            (if (settingHits.isNotEmpty()) 2 else 0) + (if (showRecent) 1 else 0) + 1
        val prefix: LazyListScope.() -> Unit = {
            if (calc != null) {
                item(key = "calc") {
                    val result = Calculator.format(calc)
                    val copied = stringResource(R.string.search_copied)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp).glass(RoundedCornerShape(22.dp), Tos.Glass, Tos.GlassLine)
                            .clickable(role = Role.Button) {
                                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(query, result))
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.ic_lucide_calculator), null, tint = Tos.Accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(14.dp))
                        Text("= $result", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Tos.Ink), modifier = Modifier.weight(1f))
                    }
                }
            }
            if (hits.isNotEmpty()) {
                item(key = "hitsHeader") { SectionHeader(stringResource(R.string.search_hits), pluralStringResource(R.plurals.app_count, hits.size, hits.size)) }
                item(key = "hits") {
                    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), Tos.Glass, Tos.GlassLine).padding(vertical = 4.dp)) {
                        hits.take(8).forEach { h ->
                            val sub = if (h.similar) stringResource(R.string.search_similar) else categoryName(h.item.category)
                            AppRow(h.item, vm.icons, d, shape, highlighted(h.item.label, h.highlight), sub, Tos.Ink, Tos.Ink2, { open(h.item) }, { vm.showMenu(h.item) })
                        }
                    }
                }
            } else if (query.isNotBlank() && calc == null && settingHits.isEmpty()) {
                item(key = "none") {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.search_none), style = TextStyle(fontFamily = Inter, fontSize = 16.sp, color = Tos.Ink2))
                        Text(stringResource(R.string.search_typo_hint), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink3), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (settingHits.isNotEmpty()) {
                item(key = "setHeader") { SectionHeader(stringResource(R.string.search_settings), null) }
                item(key = "sets") {
                    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), Tos.Glass, Tos.GlassLine).padding(vertical = 4.dp)) {
                        settingHits.forEach { h ->
                            Row(
                                Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                                    .clickable(role = Role.Button) {
                                        vm.showOverlay(Overlay.NONE)
                                        actions.openSettings(h.item.first.section.name)
                                    }
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(painterResource(R.drawable.ic_lucide_settings), null, tint = Tos.Ink2, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(highlighted(h.item.second, h.highlight), style = TextStyle(fontFamily = Inter, fontSize = 17.sp, color = Tos.Ink))
                            }
                        }
                    }
                }
            }
            if (showRecent) {
                item(key = "recent") {
                    Column {
                        SectionHeader(stringResource(R.string.search_recent), null)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            d.recentApps.mapNotNull { apps[it] }.filter { it.key !in d.hiddenApps }.take(5).forEach { app ->
                                AppTile(vm, d, app, shape, 50.dp) { open(app) }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            d.recentQueries.forEach { q ->
                                Text(
                                    q,
                                    style = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Tos.Ink),
                                    modifier = Modifier.background(Color.White.copy(alpha = 0.12f), Tos.PillShape).clickable(role = Role.Button) { query = q }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                            Text(
                                stringResource(R.string.search_clear_history),
                                style = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Tos.Accent),
                                modifier = Modifier.clickable(role = Role.Button) { vm.controller.clearHistory() }.padding(horizontal = 10.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
            item(key = "azHeader") { SectionHeader(stringResource(R.string.search_all_apps), stringResource(R.string.drawer_view_az)) }
        }
        Box(Modifier.weight(1f)) {
            AzList(vm, d, list, shape, Modifier.fillMaxSize(), query, { app, _ -> open(app) }, prefixCount, prefix)
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String?) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 8.dp)) {
        Text(title.uppercase(), style = SectionStyle.copy(color = Tos.Ink2), modifier = Modifier.weight(1f).semantics { heading() })
        if (trailing != null) Text(trailing, style = SectionStyle.copy(color = Tos.Ink3))
    }
}
