package de.umstieg.launcher.ui.drawer

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.core.net.toUri
import androidx.compose.runtime.produceState
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
    val groups = remember(list, d.categoryOverrides) {
        val byKey = list.associateBy { it.key }
        Categories.group(list.map { AppMeta(it.key, it.label, d.categoryOverrides[it.key.packageName] ?: it.category, it.firstInstallTime) }).map { (cat, metas) ->
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
    // Weitere Quellen: Kurzbefehle einmal laden; Kontakte/Dateien je Eingabe im Hintergrund.
    val shortcuts by produceState(emptyList<de.umstieg.launcher.data.ShortcutEntry>(), s.searchShortcuts) {
        value = if (s.searchShortcuts) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { vm.searchSources.shortcuts() } else emptyList()
    }
    val shortcutHits = remember(query, shortcuts) {
        if (query.trim().length < 2) emptyList() else FuzzySearch.rank(shortcuts, { it.label }, query, s.searchFuzziness).take(4)
    }
    val androidSettings = remember(query, s.searchAndroidSettings) {
        if (s.searchAndroidSettings) de.umstieg.launcher.core.SearchExtras.matchAndroidSettings(query) else emptyList()
    }
    val contacts by produceState(emptyList<de.umstieg.launcher.data.ContactEntry>(), query, s.searchContacts) {
        value = if (s.searchContacts && query.trim().length >= 2) {
            kotlinx.coroutines.delay(150)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { vm.searchSources.contacts(query) }
        } else {
            emptyList()
        }
    }
    val files by produceState(emptyList<de.umstieg.launcher.data.FileEntry>(), query, s.searchFiles) {
        value = if (s.searchFiles && query.trim().length >= 2) {
            kotlinx.coroutines.delay(200)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { vm.searchSources.files(query) }
        } else {
            emptyList()
        }
    }
    val url = remember(query) { de.umstieg.launcher.core.SearchExtras.asUrl(query) }
    fun openUri(uri: String) {
        vm.showOverlay(Overlay.NONE)
        runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri.toUri()).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    fun webSearch() {
        vm.controller.recordSearch(query, null)
        openUri(de.umstieg.launcher.core.SearchExtras.webSearchUrl(s.searchEngine, query))
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
                        // Enter öffnet den ersten Treffer (Adresse, App, Kurzbefehl, Einstellung …, sonst Websuche).
                        keyboardActions = KeyboardActions(onGo = {
                            when {
                                query.isBlank() -> Unit
                                url != null -> openUri(url)
                                hits.isNotEmpty() -> open(hits.first().item)
                                shortcutHits.isNotEmpty() -> { vm.showOverlay(Overlay.NONE); vm.searchSources.startShortcut(shortcutHits.first().item) }
                                settingHits.isNotEmpty() -> { vm.showOverlay(Overlay.NONE); actions.openSettings(settingHits.first().item.first.section.name) }
                                androidSettings.isNotEmpty() -> { vm.showOverlay(Overlay.NONE); vm.system.start(context, android.content.Intent(androidSettings.first().action)) }
                                contacts.isNotEmpty() -> { vm.showOverlay(Overlay.NONE); vm.system.start(context, vm.searchSources.openContactIntent(contacts.first())) }
                                s.searchWeb -> webSearch()
                            }
                        }),
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
        val nothing = query.isNotBlank() && calc == null && settingHits.isEmpty() && url == null && shortcutHits.isEmpty() &&
            androidSettings.isEmpty() && contacts.isEmpty() && files.isEmpty() && !s.searchWeb
        val prefixCount = (if (url != null) 1 else 0) + (if (calc != null) 1 else 0) +
            (if (hits.isNotEmpty()) 2 else if (nothing) 1 else 0) +
            (if (shortcutHits.isNotEmpty()) 2 else 0) + (if (settingHits.isNotEmpty()) 2 else 0) +
            (if (androidSettings.isNotEmpty()) 2 else 0) + (if (contacts.isNotEmpty()) 2 else 0) + (if (files.isNotEmpty()) 2 else 0) +
            (if (query.isNotBlank() && s.searchWeb) 2 else 0) + (if (showRecent) 1 else 0) + 1
        val prefix: LazyListScope.() -> Unit = {
            if (url != null) {
                item(key = "url") {
                    ResultCard { ResultRow(R.drawable.ic_lucide_external_link, stringResource(R.string.search_open_url), url) { openUri(url) } }
                }
            }
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
            } else if (nothing) {
                item(key = "none") {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.search_none), style = TextStyle(fontFamily = Inter, fontSize = 16.sp, color = Tos.Ink2))
                        Text(stringResource(R.string.search_typo_hint), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink3), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (shortcutHits.isNotEmpty()) {
                item(key = "scHeader") { SectionHeader(stringResource(R.string.search_shortcuts), null) }
                item(key = "sc") {
                    ResultCard {
                        shortcutHits.forEach { h ->
                            val owner = list.firstOrNull { it.key.packageName == h.item.packageName }?.label ?: h.item.packageName
                            ResultRow(R.drawable.ic_lucide_link, h.item.label, owner) {
                                vm.showOverlay(Overlay.NONE)
                                vm.searchSources.startShortcut(h.item)
                            }
                        }
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
            if (androidSettings.isNotEmpty()) {
                item(key = "andHeader") { SectionHeader(stringResource(R.string.search_android_settings), null) }
                item(key = "and") {
                    ResultCard {
                        androidSettings.forEach { page ->
                            ResultRow(R.drawable.ic_lucide_settings, androidSettingTitle(page.id), stringResource(R.string.search_android_settings_sub)) {
                                vm.showOverlay(Overlay.NONE)
                                vm.system.start(context, android.content.Intent(page.action))
                            }
                        }
                    }
                }
            }
            if (contacts.isNotEmpty()) {
                item(key = "ctHeader") { SectionHeader(stringResource(R.string.search_contacts), null) }
                item(key = "ct") {
                    ResultCard {
                        contacts.forEach { ct ->
                            ResultRow(R.drawable.ic_lucide_contact, ct.name, null) {
                                vm.showOverlay(Overlay.NONE)
                                vm.system.start(context, vm.searchSources.openContactIntent(ct))
                            }
                        }
                    }
                }
            }
            if (files.isNotEmpty()) {
                item(key = "fiHeader") { SectionHeader(stringResource(R.string.search_files), null) }
                item(key = "fi") {
                    ResultCard {
                        files.forEach { f ->
                            ResultRow(R.drawable.ic_lucide_file_text, f.name, f.mime) {
                                vm.showOverlay(Overlay.NONE)
                                vm.system.start(context, vm.searchSources.openFileIntent(f))
                            }
                        }
                    }
                }
            }
            if (query.isNotBlank() && s.searchWeb) {
                item(key = "webHeader") { SectionHeader(stringResource(R.string.search_web), null) }
                item(key = "web") {
                    ResultCard {
                        ResultRow(R.drawable.ic_lucide_globe, stringResource(R.string.search_web_for, query.trim()), engineName(s.searchEngine)) { webSearch() }
                        ResultRow(R.drawable.ic_lucide_store, stringResource(R.string.search_play_store), null) {
                            openUri(de.umstieg.launcher.core.SearchExtras.playStoreUrl(query))
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

/** Glaskarte für eine Gruppe von Treffern. */
@Composable
private fun ResultCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), Tos.Glass, Tos.GlassLine).padding(vertical = 4.dp)) { content() }
}

/** Trefferzeile: Symbol, Titel, Untertitel. */
@Composable
private fun ResultRow(icon: Int, title: String, sub: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = Tos.Ink2, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontFamily = Inter, fontSize = 16.sp, color = Tos.Ink), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = TextStyle(fontFamily = Inter, fontSize = 12.sp, color = Tos.Ink3), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun engineName(e: de.umstieg.launcher.core.SearchEngine): String = when (e) {
    de.umstieg.launcher.core.SearchEngine.GOOGLE -> "Google"
    de.umstieg.launcher.core.SearchEngine.DUCKDUCKGO -> "DuckDuckGo"
    de.umstieg.launcher.core.SearchEngine.STARTPAGE -> "Startpage"
    de.umstieg.launcher.core.SearchEngine.ECOSIA -> "Ecosia"
}

/** Deutsche Titel der Android-Einstellungsseiten. */
@Composable
private fun androidSettingTitle(id: String): String = stringResource(
    when (id) {
        "wifi" -> R.string.android_setting_wifi
        "bluetooth" -> R.string.android_setting_bluetooth
        "battery" -> R.string.android_setting_battery
        "display" -> R.string.android_setting_display
        "sound" -> R.string.android_setting_sound
        "notifications" -> R.string.android_setting_notifications
        "apps" -> R.string.android_setting_apps
        "storage" -> R.string.android_setting_storage
        "location" -> R.string.android_setting_location
        "security" -> R.string.android_setting_security
        "privacy" -> R.string.android_setting_privacy
        "accessibility" -> R.string.android_setting_accessibility
        "mobile" -> R.string.android_setting_mobile
        "hotspot" -> R.string.android_setting_hotspot
        "airplane" -> R.string.android_setting_airplane
        "nfc" -> R.string.android_setting_nfc
        "datetime" -> R.string.android_setting_datetime
        "language" -> R.string.android_setting_language
        "dnd" -> R.string.android_setting_dnd
        "about" -> R.string.android_setting_about
        "update" -> R.string.android_setting_update
        "wallpaper" -> R.string.android_setting_wallpaper
        "vpn" -> R.string.android_setting_vpn
        "cast" -> R.string.android_setting_cast
        else -> R.string.search_android_settings
    },
)

@Composable
private fun SectionHeader(title: String, trailing: String?) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 8.dp)) {
        Text(title.uppercase(), style = SectionStyle.copy(color = Tos.Ink2), modifier = Modifier.weight(1f).semantics { heading() })
        if (trailing != null) Text(trailing, style = SectionStyle.copy(color = Tos.Ink3))
    }
}
