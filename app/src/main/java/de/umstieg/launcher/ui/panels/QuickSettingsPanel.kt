package de.umstieg.launcher.ui.panels

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.R
import de.umstieg.launcher.core.QsItem
import de.umstieg.launcher.core.QsLayout
import de.umstieg.launcher.core.QsOps
import de.umstieg.launcher.core.QsPage
import de.umstieg.launcher.core.QsSize
import de.umstieg.launcher.core.QsTile
import de.umstieg.launcher.data.MediaInfo
import de.umstieg.launcher.data.NotificationCenter
import de.umstieg.launcher.data.Requirement
import de.umstieg.launcher.data.TileStatus
import de.umstieg.launcher.ui.home.HomeViewModel
import de.umstieg.launcher.ui.home.MinusBadge
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.glass
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.roundToInt

private val Label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.Center)
private val Small = TextStyle(fontFamily = Inter, fontSize = 13.sp, textAlign = TextAlign.Center)

@Composable
fun tileName(t: QsTile): String = stringResource(
    when (t) {
        QsTile.WIFI -> R.string.qs_wifi
        QsTile.BLUETOOTH -> R.string.qs_bluetooth
        QsTile.MOBILE_DATA -> R.string.qs_mobile
        QsTile.AIRPLANE -> R.string.qs_airplane
        QsTile.HOTSPOT -> R.string.qs_hotspot
        QsTile.NFC -> R.string.qs_nfc
        QsTile.LOCATION -> R.string.qs_location
        QsTile.MEDIA -> R.string.qs_media
        QsTile.BRIGHTNESS -> R.string.qs_brightness
        QsTile.VOLUME -> R.string.qs_volume
        QsTile.FLASHLIGHT -> R.string.qs_flashlight
        QsTile.DND -> R.string.qs_dnd
        QsTile.ROTATION -> R.string.qs_rotation
        QsTile.BATTERY_SAVER -> R.string.qs_battery_saver
        QsTile.CALCULATOR -> R.string.qs_calculator
        QsTile.CAMERA -> R.string.qs_camera
        QsTile.ALARM -> R.string.qs_alarm
        QsTile.QR -> R.string.qs_qr
        QsTile.SYSTEM_PANEL -> R.string.qs_system
    },
)

fun tileIcon(t: QsTile): Int = when (t) {
    QsTile.WIFI -> R.drawable.ic_lucide_wifi
    QsTile.BLUETOOTH -> R.drawable.ic_lucide_bluetooth
    QsTile.MOBILE_DATA -> R.drawable.ic_lucide_signal_high
    QsTile.AIRPLANE -> R.drawable.ic_lucide_plane
    QsTile.HOTSPOT -> R.drawable.ic_lucide_radio_tower
    QsTile.NFC -> R.drawable.ic_lucide_nfc
    QsTile.LOCATION -> R.drawable.ic_lucide_map_pin
    QsTile.MEDIA -> R.drawable.ic_lucide_music
    QsTile.BRIGHTNESS -> R.drawable.ic_lucide_sun
    QsTile.VOLUME -> R.drawable.ic_lucide_volume_2
    QsTile.FLASHLIGHT -> R.drawable.ic_lucide_flashlight
    QsTile.DND -> R.drawable.ic_lucide_moon
    QsTile.ROTATION -> R.drawable.ic_lucide_rotate_ccw
    QsTile.BATTERY_SAVER -> R.drawable.ic_lucide_battery_charging
    QsTile.CALCULATOR -> R.drawable.ic_lucide_calculator
    QsTile.CAMERA -> R.drawable.ic_lucide_camera
    QsTile.ALARM -> R.drawable.ic_lucide_alarm_clock
    QsTile.QR -> R.drawable.ic_lucide_scan_qr_code
    QsTile.SYSTEM_PANEL -> R.drawable.ic_lucide_sliders_horizontal
}

@Composable
private fun pageName(p: QsPage): String = p.name.ifBlank {
    stringResource(
        when (p.id) {
            "control" -> R.string.qs_page_control
            "connections" -> R.string.qs_page_connections
            "media" -> R.string.qs_page_media
            else -> R.string.qs_page_new
        },
    )
}

@Composable
private fun detailText(s: TileStatus): String? = when (s.detail) {
    TileStatus.Detail.CONNECTED -> stringResource(R.string.qs_connected)
    TileStatus.Detail.DISCONNECTED -> stringResource(R.string.qs_disconnected)
    TileStatus.Detail.ON -> stringResource(R.string.on)
    TileStatus.Detail.OFF -> stringResource(R.string.off)
    else -> null
}

/** Schnellzugriff (Wisch von oben rechts): Seiten als Tabs, Elemente in den Größen S/M/L, bearbeitbar. */
@Composable
fun QuickSettingsPanel(vm: HomeViewModel, layout: QsLayout) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var managePages by remember { mutableStateOf(false) }
    var addItem by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var needs by remember { mutableStateOf<Requirement?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            tick++
        }
    }
    // Seiten liegen untereinander und werden senkrecht geblättert; die letzte (leere) Seite fügt eine neue hinzu.
    val pager = androidx.compose.foundation.pager.rememberPagerState { layout.pages.size + 1 }
    val pageIndex = pager.currentPage.coerceIn(0, layout.pages.lastIndex)
    val page = layout.pages.getOrElse(pageIndex) { layout.pages.first() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val sys = vm.system
    val media by vm.media.sessions.collectAsStateWithLifecycle()
    val torch by sys.torch.collectAsStateWithLifecycle()

    fun act(tile: QsTile) {
        val req = sys.requirement(tile)
        val done = when (tile) {
            QsTile.FLASHLIGHT -> sys.setTorch(!torch)
            QsTile.DND -> if (!sys.hasPolicyAccess()) { needs = Requirement.NOTIFICATION_POLICY; true } else sys.setDnd(sys.status(tile).active != true)
            QsTile.ROTATION -> if (!sys.canWriteSettings()) { needs = Requirement.WRITE_SETTINGS; true } else sys.setRotationLock(sys.status(tile).active != true)
            QsTile.SYSTEM_PANEL -> sys.expandStatusBar(true)
            QsTile.MEDIA -> if (!NotificationCenter.isPermissionGranted(context)) { needs = Requirement.NOTIFICATION_LISTENER; true } else false
            else -> false
        }
        if (!done) sys.settingsIntent(tile)?.let { sys.start(context, it) }
        if (req == Requirement.SYSTEM_ONLY && tile != QsTile.SYSTEM_PANEL) Unit
        tick++
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (de.umstieg.launcher.ui.theme.LocalTosStyle.current.lightWallpaper) Color(0xD90B0F16) else Color(0x990B0F16))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp),
    ) {
        if (editing) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.qs_edit_title, pageName(page)), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Tos.Ink), modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.qs_edit_hint), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink2))
                }
                PillButton(stringResource(R.string.qs_pages), accent = false) { managePages = true }
            }
        } else {
            // Klein und ruhig: Seitenname links, Stift rechts. Keine Tabs.
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.qs_header, pageName(page)),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Tos.Ink2),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Tos.GlassControl).clickable(role = Role.Button) { editing = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(R.drawable.ic_lucide_pencil), stringResource(R.string.qs_edit), tint = Tos.Ink, modifier = Modifier.size(16.dp)) }
            }
        }

        val view = androidx.compose.ui.platform.LocalView.current
        Box(
            Modifier.weight(1f).fillMaxWidth()
                // Langes Drücken irgendwo auf der Seite startet das Bearbeiten (wie der Stift).
                .pointerInput(editing) {
                    if (editing) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        val ended = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val ev = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed || (c.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                            }
                            true
                        }
                        if (ended == null) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            editing = true
                        }
                    }
                },
        ) {
            androidx.compose.foundation.pager.VerticalPager(
                state = pager,
                modifier = Modifier.fillMaxSize().padding(end = 22.dp),
                userScrollEnabled = !editing,
                key = { it },
            ) { pi ->
                val pg = layout.pages.getOrNull(pi)
                if (pg == null) {
                    // Leere Seite am Ende: neue Seite anlegen.
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Row(
                            Modifier.fillMaxWidth().height(96.dp).glass(RoundedCornerShape(22.dp), Tos.Glass, Tos.GlassLine)
                                .clickable(role = Role.Button) {
                                    vm.controller.updateQuickSettings { QsOps.addPage(it, java.util.UUID.randomUUID().toString(), "") }
                                    editing = true
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_lucide_plus), null, tint = Tos.Ink2, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.qs_add_page), style = Label.copy(color = Tos.Ink2))
                        }
                    }
                    return@VerticalPager
                }
                // Kein inneres Scrollen: senkrechte Wische gehören dem Seiten-Pager.
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var index = 0
                    QsOps.packRows(pg.items).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            row.forEach { item ->
                                val i = index++
                                Box(Modifier.weight(item.size.units.toFloat()), contentAlignment = Alignment.TopCenter) {
                                    val status = remember(tick, item.tile, torch) { sys.status(item.tile) }
                                    QsItemView(item, status, media, vm, ::act) { needs = it }
                                    if (editing && pg.id == page.id) {
                                        Box(
                                            Modifier.matchParentSize()
                                                .border(2.dp, if (selected == i) Tos.Accent else Color.Transparent, RoundedCornerShape(20.dp))
                                                .pointerInput(i) { detectTapGestures { selected = if (selected == i) null else i } },
                                        )
                                        MinusBadge(stringResource(R.string.qs_remove, tileName(item.tile)), false, onClick = {
                                            vm.controller.updateQuickSettings { QsOps.removeItem(it, pg.id, i) }
                                            selected = null
                                        }, Modifier.offset((-10).dp, (-10).dp))
                                    }
                                }
                            }
                            val used = row.sumOf { it.size.units }
                            if (used < QsOps.ROW_UNITS) Spacer(Modifier.weight((QsOps.ROW_UNITS - used).toFloat()))
                        }
                    }
                    if (!editing) {
                        Text(stringResource(R.string.qs_system_hint), style = TextStyle(fontFamily = Inter, fontSize = 11.sp, lineHeight = 15.sp, color = Tos.Ink3), modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    // Unten frei bis über die Gestenleiste (Hinweis nicht abschneiden).
                    Spacer(Modifier.height(24.dp))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
            // Punkte rechts: eine pro Seite, dazu „+“; antippen springt zur Seite.
            Column(
                Modifier.align(Alignment.CenterEnd).width(16.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (i in 0..layout.pages.size) {
                    val on = i == pager.currentPage
                    val desc = if (i < layout.pages.size) pageName(layout.pages[i]) else stringResource(R.string.qs_add_page)
                    Box(
                        Modifier.size(width = 16.dp, height = if (on) 24.dp else 14.dp)
                            .clickable(role = Role.Tab, onClickLabel = desc) { scope.launch { pager.animateScrollToPage(i) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (i < layout.pages.size) {
                            val h by androidx.compose.animation.core.animateDpAsState(if (on) 18.dp else 6.dp, label = "qsDot")
                            Box(Modifier.size(width = 6.dp, height = h).clip(Tos.PillShape).background(if (on) Color.White else Tos.Ink3))
                        } else {
                            Icon(painterResource(R.drawable.ic_lucide_plus), desc, tint = if (on) Color.White else Tos.Ink3, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }

        if (editing) {
            selected?.let { i ->
                val item = page.items.getOrNull(i)
                if (item != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp).glass(Tos.PillShape, Tos.GlassControl, Tos.GlassLine).padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(stringResource(R.string.qs_size), style = Label.copy(color = Tos.Ink2), modifier = Modifier.padding(horizontal = 10.dp))
                        QsOps.allowedSizes(item.tile).forEach { sz ->
                            val on = sz == item.size
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(if (on) Tos.Accent else Color.Transparent)
                                    .clickable(role = Role.RadioButton) { vm.controller.updateQuickSettings { QsOps.setSize(it, page.id, i, sz) } },
                                contentAlignment = Alignment.Center,
                            ) { Text(sz.name, style = Label.copy(color = if (on) Tos.AccentInk else Tos.Ink)) }
                        }
                        Spacer(Modifier.weight(1f))
                        IconAction(R.drawable.ic_lucide_chevron_left, stringResource(R.string.qs_move_back)) {
                            vm.controller.updateQuickSettings { QsOps.moveItem(it, page.id, i, i - 1) }
                            selected = (i - 1).coerceAtLeast(0)
                        }
                        IconAction(R.drawable.ic_lucide_chevron_right, stringResource(R.string.qs_move_forward)) {
                            vm.controller.updateQuickSettings { QsOps.moveItem(it, page.id, i, i + 1) }
                            selected = (i + 1).coerceAtMost(page.items.lastIndex)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton("+ " + stringResource(R.string.qs_add_item), accent = false) { addItem = true }
                Spacer(Modifier.weight(1f))
                PillButton(stringResource(R.string.edit_done), accent = true) {
                    editing = false
                    selected = null
                }
            }
        }
    }

    if (addItem) {
        AlertDialog(
            onDismissRequest = { addItem = false },
            title = { Text(stringResource(R.string.qs_add_item)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    QsOps.available(layout, page.id).forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).clickable {
                                vm.controller.updateQuickSettings { QsOps.addItem(it, page.id, t) }
                                addItem = false
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(tileIcon(t)), null)
                            Spacer(Modifier.width(12.dp))
                            Text(tileName(t))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { addItem = false }) { Text(stringResource(R.string.close)) } },
        )
    }
    if (managePages) PagesDialog(vm, layout) { managePages = false }
    needs?.let { r -> PermissionDialog(r, vm) { needs = null } }
}

@Composable
private fun IconAction(icon: Int, description: String, small: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(if (small) 40.dp else 48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), description, tint = Tos.Ink, modifier = Modifier.size(if (small) 17.dp else 24.dp))
    }
}

/** Ein Element in seiner Größe; Regler und Medien haben eigene Darstellungen. */
@Composable
private fun QsItemView(item: QsItem, status: TileStatus, media: List<MediaInfo>, vm: HomeViewModel, act: (QsTile) -> Unit, need: (Requirement) -> Unit) {
    val sys = vm.system
    when (item.tile) {
        QsTile.BRIGHTNESS -> SliderPill(R.drawable.ic_lucide_sun, tileName(item.tile), sys.brightness()) { v ->
            if (!sys.setBrightness(v)) need(Requirement.WRITE_SETTINGS)
        }
        QsTile.VOLUME -> SliderPill(R.drawable.ic_lucide_volume_2, tileName(item.tile), sys.volume()) { sys.setVolume(it) }
        QsTile.MEDIA -> MediaCard(media.firstOrNull(), item.size == QsSize.L, vm) { act(QsTile.MEDIA) }
        else -> ToggleTile(item, status, act)
    }
}

@Composable
private fun ToggleTile(item: QsItem, status: TileStatus, act: (QsTile) -> Unit) {
    val name = tileName(item.tile)
    val active = status.active == true
    val detail = detailText(status)
    val bg = if (active) Tos.Accent else Color.White.copy(alpha = 0.12f)
    val fg = if (active) Tos.AccentInk else Tos.Ink
    val description = listOfNotNull(name, detail).joinToString(", ")
    if (item.size == QsSize.S) {
        // Kompakt: 64-dp-Kachel, kleine Beschriftung darunter.
        Column(Modifier.width(72.dp).semantics(mergeDescendants = true) { contentDescription = description }, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(bg).border(1.dp, Tos.GlassLine, RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button) { act(item.tile) },
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(tileIcon(item.tile)), null, tint = fg, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(5.dp))
            FitText(name, Label.copy(color = Tos.Ink, fontSize = 11.sp), 11)
            if (detail != null) FitText(detail, Small.copy(color = Tos.Ink3, fontSize = 10.sp), 10)
        }
    } else {
        Row(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(bg)
                .border(1.dp, Tos.GlassLine, RoundedCornerShape(18.dp))
                .clickable(role = Role.Button) { act(item.tile) }
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(tileIcon(item.tile)), null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                FitText(name, Label.copy(color = fg, textAlign = TextAlign.Start, fontSize = 13.sp), 13)
                if (detail != null) Text(detail, style = Small.copy(color = fg.copy(alpha = 0.75f), textAlign = TextAlign.Start, fontSize = 11.sp), maxLines = 1)
            }
            Icon(painterResource(R.drawable.ic_lucide_chevron_right), null, tint = fg.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Flacher Regler (48 dp, Helligkeit, Lautstärke): dezente Füllung mit schmalem Knopf an der Kante.
 * Symbol, Name und Prozent bleiben immer lesbar (nie vom Füllbereich oder Knopf überdeckt).
 */
@Composable
private fun SliderPill(icon: Int, name: String, initial: Float, onChange: (Float) -> Unit) {
    var value by remember { mutableFloatStateOf(initial) }
    var width by remember { mutableIntStateOf(1) }
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Tos.GlassLine, shape)
            .pointerInput(Unit) {
                width = size.width
                detectTapGestures { p ->
                    value = (p.x / width).coerceIn(0f, 1f)
                    onChange(value)
                }
            }
            .pointerInput(Unit) {
                width = size.width
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    value = (change.position.x / width).coerceIn(0f, 1f)
                    onChange(value)
                }
            }
            .semantics {
                contentDescription = name
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                setProgress { v ->
                    value = v.coerceIn(0f, 1f)
                    onChange(value)
                    true
                }
            }
            .drawBehind {
                val x = size.width * value
                drawRect(Color.White.copy(alpha = 0.20f), size = androidx.compose.ui.geometry.Size(x, size.height))
                // Knopf: schmaler, heller Balken an der Kante der Füllung.
                val kx = x.coerceIn(6.dp.toPx(), size.width - 6.dp.toPx())
                drawRoundRect(
                    Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset(kx - 2.dp.toPx(), 11.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height - 22.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
            },
    ) {
        Row(Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, tint = Tos.Ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(name, style = Label.copy(color = Tos.Ink, fontSize = 13.sp, textAlign = TextAlign.Start), modifier = Modifier.weight(1f), maxLines = 1)
            Text("${(value * 100).roundToInt()} %", style = Label.copy(color = Tos.Ink2, fontSize = 12.sp))
        }
    }
}

/**
 * Beschriftung ohne Abschneiden: mehrere Wörter dürfen auf zwei Zeilen umbrechen, sonst wird die
 * Schrift schrittweise verkleinert, bis der Text passt.
 */
@Composable
private fun FitText(text: String, style: TextStyle, maxSp: Int) {
    BasicText(
        text = text,
        style = style,
        maxLines = if (text.contains(' ')) 2 else 1,
        softWrap = text.contains(' '),
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = maxSp.sp, stepSize = 0.5.sp),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Mediensteuerung kompakt: ohne Wiedergabe eine Zeile („Nichts wird abgespielt“, Play für die letzte
 * Sitzung, falls vorhanden); mit Wiedergabe Cover links, Titel, Steuerung. L zeigt zusätzlich den Fortschritt.
 */
@Composable
internal fun MediaCard(info: MediaInfo?, large: Boolean, vm: HomeViewModel, onNoAccess: () -> Unit) {
    val context = LocalContext.current
    val granted = NotificationCenter.isPermissionGranted(context)
    val shape = RoundedCornerShape(18.dp)
    if (!granted || info == null) {
        Row(
            Modifier.fillMaxWidth().height(44.dp).glass(shape, Tos.Glass, Tos.GlassLine)
                .clickable(enabled = !granted, onClick = onNoAccess).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_lucide_music), null, tint = Tos.Ink2, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(if (!granted) R.string.qs_media_no_access else R.string.qs_media_none),
                style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, color = Tos.Ink2),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
        }
        return
    }
    val art = remember(info.art) { info.art?.asImageBitmap() }
    Column(Modifier.fillMaxWidth().glass(shape, Tos.Glass, Tos.GlassLine).padding(horizontal = 8.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (art != null) {
                Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)))
            } else {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_lucide_music), null, tint = Tos.Ink2, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(info.title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Tos.Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(info.artist, info.album).filter { it.isNotBlank() }.joinToString(" · "), style = TextStyle(fontFamily = Inter, fontSize = 11.sp, color = Tos.Ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconAction(R.drawable.ic_lucide_skip_back, stringResource(R.string.qs_media_previous), small = true) { info.previous() }
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Color.White).clickable(role = Role.Button) { info.playPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (info.playing) R.drawable.ic_lucide_pause else R.drawable.ic_lucide_play),
                    stringResource(if (info.playing) R.string.qs_media_pause else R.string.qs_media_play),
                    tint = Color(0xFF10141C), modifier = Modifier.size(15.dp),
                )
            }
            IconAction(R.drawable.ic_lucide_skip_forward, stringResource(R.string.qs_media_next), small = true) { info.next() }
            if (large) IconAction(R.drawable.ic_lucide_cast, stringResource(R.string.qs_media_output), small = true) { vm.media.showOutputSwitcher(context) }
        }
        if (large && info.durationMs > 0) {
            Spacer(Modifier.height(8.dp))
            val fraction = (info.positionMs.toFloat() / info.durationMs).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(3.dp).clip(Tos.PillShape).background(Color.White.copy(alpha = 0.2f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(Color.White, Tos.PillShape))
            }
        }
    }
}

/** Seiten verwalten: umbenennen, sortieren, löschen, hinzufügen. */
@Composable
private fun PagesDialog(vm: HomeViewModel, layout: QsLayout, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.qs_pages)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                layout.pages.forEachIndexed { i, p ->
                    var name by remember(p.id) { mutableStateOf(p.name) }
                    val fallback = pageName(p)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = name.ifEmpty { "" },
                            onValueChange = {
                                name = it.take(24)
                                vm.controller.updateQuickSettings { l -> QsOps.renamePage(l, p.id, name) }
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(Tos.Accent),
                            decorationBox = { inner -> Box { if (name.isEmpty()) Text(fallback, color = Color.Gray); inner() } },
                            modifier = Modifier.weight(1f).padding(8.dp),
                        )
                        TextButton(onClick = { vm.controller.updateQuickSettings { QsOps.movePage(it, i, i - 1) } }) { Text("↑") }
                        TextButton(onClick = { vm.controller.updateQuickSettings { QsOps.movePage(it, i, i + 1) } }) { Text("↓") }
                        if (layout.pages.size > 1) {
                            TextButton(onClick = { vm.controller.updateQuickSettings { QsOps.removePage(it, p.id) } }) {
                                Text(stringResource(R.string.qs_page_delete), color = Tos.Danger)
                            }
                        }
                    }
                }
                TextButton(onClick = { vm.controller.updateQuickSettings { QsOps.addPage(it, UUID.randomUUID().toString(), "") } }) {
                    Text("+ " + stringResource(R.string.qs_page_add))
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.edit_done)) } },
    )
}

/** Erklärt eine fehlende Berechtigung und führt zur Systemseite. */
@Composable
fun PermissionDialog(r: Requirement, vm: HomeViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val (title, text, intent) = when (r) {
        Requirement.WRITE_SETTINGS -> Triple(R.string.perm_write_settings_title, R.string.perm_write_settings_text, vm.system.writeSettingsIntent())
        Requirement.NOTIFICATION_POLICY -> Triple(R.string.perm_dnd_title, R.string.perm_dnd_text, vm.system.policyAccessIntent())
        else -> Triple(R.string.listener_title, R.string.listener_text, vm.system.listenerSettingsIntent())
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(text)) },
        confirmButton = {
            TextButton(onClick = {
                if (r == Requirement.WRITE_SETTINGS || r == Requirement.NOTIFICATION_POLICY) vm.system.start(context, Intent(intent)) else vm.requestListenerAccess(context)
                onClose()
            }) { Text(stringResource(R.string.perm_open_settings)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}
