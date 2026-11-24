package de.umstieg.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.umstieg.launcher.R
import de.umstieg.launcher.core.Gesture
import de.umstieg.launcher.core.GestureAction
import de.umstieg.launcher.core.GestureBinding
import de.umstieg.launcher.core.GestureClassifier
import de.umstieg.launcher.core.GesturePreset
import de.umstieg.launcher.core.GesturePresets
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.data.EdgeGestureService
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.LocalPanelColors
import de.umstieg.launcher.ui.theme.Tos

/** Symbol, Farbe und erklärender Satz je Aktion (für Handy-Bild und Auswahlkarten). */
fun actionIcon(a: GestureAction): Int = when (a) {
    GestureAction.NOTIFICATIONS -> R.drawable.ic_lucide_bell
    GestureAction.QUICK_SETTINGS -> R.drawable.ic_lucide_sliders_horizontal
    GestureAction.SYSTEM_NOTIFICATIONS -> R.drawable.ic_lucide_panel_bottom
    GestureAction.SYSTEM_QUICK_SETTINGS -> R.drawable.ic_lucide_layout_grid
    GestureAction.SEARCH -> R.drawable.ic_lucide_search
    GestureAction.APP_DRAWER -> R.drawable.ic_lucide_grid_3x3
    GestureAction.OPEN_APP -> R.drawable.ic_lucide_app_window
    GestureAction.ASSISTANT -> R.drawable.ic_lucide_bot
    GestureAction.LOCK_SCREEN -> R.drawable.ic_lucide_lock
    GestureAction.NOTHING -> R.drawable.ic_lucide_x
}

fun actionColor(a: GestureAction): Color = when (a) {
    GestureAction.NOTIFICATIONS -> Color(0xFF2EC4A6)
    GestureAction.QUICK_SETTINGS -> Color(0xFF7AA0FF)
    GestureAction.SYSTEM_NOTIFICATIONS, GestureAction.SYSTEM_QUICK_SETTINGS -> Color(0xFFB8C0CC)
    GestureAction.SEARCH -> Color(0xFFFFC178)
    GestureAction.APP_DRAWER -> Color(0xFFD678FF)
    GestureAction.OPEN_APP -> Color(0xFFFF8A7A)
    GestureAction.ASSISTANT -> Color(0xFF9BE15D)
    GestureAction.LOCK_SCREEN -> Color(0xFF8A94A6)
    GestureAction.NOTHING -> Color(0xFF5A6478)
}

@Composable
fun actionInfo(a: GestureAction): String = stringResource(
    when (a) {
        GestureAction.NOTIFICATIONS -> R.string.action_notifications_info
        GestureAction.QUICK_SETTINGS -> R.string.action_quick_info
        GestureAction.SYSTEM_NOTIFICATIONS -> R.string.action_system_notifications_info
        GestureAction.SYSTEM_QUICK_SETTINGS -> R.string.action_system_quick_info
        GestureAction.SEARCH -> R.string.action_search_info
        GestureAction.APP_DRAWER -> R.string.action_drawer_info
        GestureAction.OPEN_APP -> R.string.action_open_app_info
        GestureAction.ASSISTANT -> R.string.action_assistant_info
        GestureAction.LOCK_SCREEN -> R.string.action_lock_screen_info
        GestureAction.NOTHING -> R.string.action_nothing_info
    },
)

/** Kurzfassung für den Untertitel unter „Einfach“: „Links: … · Rechts: …“. */
@Composable
fun edgeSummary(s: LauncherSettings): String {
    if (!s.edgeSplit) return actionName(GestureClassifier.binding(s, Gesture.EDGE).action)
    return stringResource(
        R.string.gestures_summary,
        actionName(GestureClassifier.binding(s, Gesture.EDGE_LEFT).action),
        actionName(GestureClassifier.binding(s, Gesture.EDGE_RIGHT).action),
    )
}

/**
 * Gesten: großes Handy-Bild mit allen Zonen (antippen wählt die Aktion), darunter Vorlagen als Chips,
 * Regler für Breiten und Höhe (live im Bild) und die optionale Bedienungshilfe für Wische ab der Statusleiste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesPage(p: PageEnv) {
    val s = p.s
    val c = LocalPanelColors.current
    val context = LocalContext.current
    var zone by remember { mutableStateOf<Gesture?>(null) }
    var pickApp by remember { mutableStateOf<Gesture?>(null) }
    var serviceHelp by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val serviceOn = remember(tick) { EdgeGestureService.isEnabled(context) }
    // Live-Vorschau der Regler.
    var left by remember(s.swipeZoneLeft) { mutableIntStateOf(s.swipeZoneLeft) }
    var right by remember(s.swipeZoneRight) { mutableIntStateOf(s.swipeZoneRight) }
    var height by remember(s.edgeHeightPercent) { mutableIntStateOf(s.edgeHeightPercent) }
    val pct = percentFormat()

    SubpageHeader(stringResource(R.string.settings_section_gestures), p.back)
    Text(edgeSummary(s), style = SummaryStyle.copy(color = c.secondary, fontSize = 15.sp), modifier = Modifier.padding(horizontal = 6.dp))
    if (s.simpleMode) SettingsCard { Note(stringResource(R.string.gestures_simple_mode)) }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PhoneZones(s.copy(swipeZoneLeft = left, swipeZoneRight = right, edgeHeightPercent = height), s.systemGestures && serviceOn) { zone = it }
    }
    Text(
        stringResource(R.string.gestures_tap_zone),
        style = SummaryStyle.copy(color = c.secondary, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth(),
    )

    // Vorlagen als Schnellstart; „Eigen“ wird bei jeder Einzeländerung automatisch aktiv.
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GesturePreset.entries.forEach { preset ->
            val on = s.gesturePreset == preset
            Text(
                presetName(preset),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (on) Color(0xFF10141C) else c.content),
                modifier = Modifier.clip(Tos.PillShape).background(if (on) Color.White else c.chip)
                    .clickable(enabled = preset != GesturePreset.CUSTOM, role = Role.RadioButton) { p.set { GesturePresets.apply(it, preset) } }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
    Text(stringResource(presetSummary(s.gesturePreset)), style = SummaryStyle.copy(color = c.secondary), modifier = Modifier.padding(horizontal = 6.dp))

    SettingsCard {
        SwitchRow(stringResource(R.string.setting_edge_split), stringResource(R.string.setting_edge_split_summary), s.edgeSplit) { v ->
            p.set { GesturePresets.withSplit(it, v) }
        }
        if (s.edgeSplit) {
            CardDivider()
            SliderBlock(stringResource(R.string.setting_swipe_zone_left), s.swipeZoneLeft, LauncherSettings.SWIPE_ZONE, pct,
                onPreview = { left = it }, onCommit = { v -> p.set { it.copy(swipeZoneLeft = v) } }, stepSize = 5)
            CardDivider()
            SliderBlock(stringResource(R.string.setting_swipe_zone_right), s.swipeZoneRight, LauncherSettings.SWIPE_ZONE, pct,
                onPreview = { right = it }, onCommit = { v -> p.set { it.copy(swipeZoneRight = v) } }, stepSize = 5)
        }
        CardDivider()
        SliderBlock(stringResource(R.string.setting_edge_height), s.edgeHeightPercent, LauncherSettings.EDGE_HEIGHT, pct,
            onPreview = { height = it }, onCommit = { v -> p.set { it.copy(edgeHeightPercent = v) } }, stepSize = 5)
    }

    // Bedienungshilfe: Wische direkt auf der Statusleiste, auch in anderen Apps.
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_system_gestures), stringResource(R.string.setting_system_gestures_summary), s.systemGestures && serviceOn) { v ->
            p.set { it.copy(systemGestures = v) }
            if (v && !serviceOn) serviceHelp = true
        }
        if (s.systemGestures && !serviceOn) {
            ActionRow(stringResource(R.string.system_gestures_enable), stringResource(R.string.system_gestures_enable_summary)) { serviceHelp = true }
        }
        if (s.systemGestures) {
            CardDivider()
            SegmentedRow(
                stringResource(R.string.setting_system_gesture_mode),
                listOf(false to stringResource(R.string.system_gesture_split), true to stringResource(R.string.system_gesture_full)),
                s.systemGestureFull,
            ) { v -> p.set { it.copy(systemGestureFull = v) } }
            CardDivider()
            SegmentedRow(
                stringResource(R.string.setting_system_gesture_where),
                listOf(false to stringResource(R.string.system_gesture_home_only), true to stringResource(R.string.system_gesture_everywhere)),
                s.systemGesturesEverywhere,
            ) { v -> p.set { it.copy(systemGesturesEverywhere = v) } }
        }
    }
    if (!GestureClassifier.searchReachableByGesture(s)) SettingsCard { Note(stringResource(R.string.gestures_search_hint)) }
    SettingsCard { Note(stringResource(R.string.gestures_system_note)) }
    ResetSection {
        p.set { GesturePresets.apply(it, GesturePreset.TOS).copy(swipeZoneLeft = Defaults.swipeZoneLeft, swipeZoneRight = Defaults.swipeZoneRight, edgeHeightPercent = Defaults.edgeHeightPercent) }
    }

    zone?.let { g ->
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { zone = null }, sheetState = sheet, containerColor = if (c.dark) Color(0xFF151C28) else Color(0xFFF2F4F7)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(gestureName(g), style = TitleStyle.copy(color = c.content, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                Text(stringResource(R.string.gestures_pick_question), style = SummaryStyle.copy(color = c.secondary))
                val current = GestureClassifier.binding(s, g).action
                GestureAction.entries.forEach { a ->
                    val needsService = a == GestureAction.LOCK_SCREEN && !serviceOn
                    OptionCard(
                        icon = actionIcon(a),
                        title = actionName(a),
                        info = if (needsService) stringResource(R.string.action_needs_service) else actionInfo(a),
                        selected = a == current,
                        enabled = !needsService,
                        preview = { ActionPreview(a) },
                    ) {
                        if (a == GestureAction.OPEN_APP) pickApp = g else p.set { GesturePresets.withGesture(it, g, GestureBinding(a)) }
                        zone = null
                    }
                }
            }
        }
    }
    pickApp?.let { g ->
        AlertDialog(
            onDismissRequest = { pickApp = null },
            title = { Text(stringResource(R.string.action_open_app)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    p.apps.sortedBy { it.label.lowercase() }.forEach { app ->
                        Text(app.label, modifier = Modifier.fillMaxWidth().clickable {
                            p.set { GesturePresets.withGesture(it, g, GestureBinding(GestureAction.OPEN_APP, app.key)) }
                            pickApp = null
                        }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickApp = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (serviceHelp) {
        // Auch hier gelten „Eingeschränkte Einstellungen“: dann erst App-Info, danach Bedienungshilfen.
        val restricted = p.vm.restricted()
        AlertDialog(
            onDismissRequest = { serviceHelp = false },
            title = { Text(stringResource(R.string.system_gestures_enable)) },
            text = { Text(stringResource(if (restricted) R.string.system_gestures_restricted else R.string.system_gestures_steps)) },
            confirmButton = {
                TextButton(onClick = {
                    serviceHelp = false
                    p.actions.openIntent(if (restricted) p.vm.appDetailsIntent() else EdgeGestureService.settingsIntent())
                }) { Text(stringResource(if (restricted) R.string.setup_open_app_info else R.string.system_gestures_open)) }
            },
            dismissButton = { TextButton(onClick = { serviceHelp = false }) { Text(stringResource(R.string.setup_later)) } },
        )
    }
}

/** Handy-Bild mit allen Zonen; Farbe, Symbol und Kurzname zeigen die aktuelle Aktion. */
@Composable
private fun PhoneZones(s: LauncherSettings, onStatusBar: Boolean, onZone: (Gesture) -> Unit) {
    val w = 220.dp
    val h = 400.dp
    BoxWithConstraints(
        Modifier.size(w, h).clip(RoundedCornerShape(32.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1B2A4A), Color(0xFF14243A), Color(0xFF2A1E3A))))
            .border(2.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(32.dp)),
    ) {
        val pad = 5.dp
        val inner = w - pad * 2
        val top = pad + 14.dp
        val edgeH = ((h - top) * (s.edgeHeightPercent / 100f)).coerceAtLeast(56.dp)
        Text("08:15", style = TextStyle(fontFamily = Inter, fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f)), modifier = Modifier.offset(12.dp, 3.dp))
        if (s.edgeSplit) {
            val lw = inner * (s.swipeZoneLeft / 100f)
            val rw = inner * (s.swipeZoneRight / 100f)
            Zone(s, Gesture.EDGE_LEFT, pad, top, lw - 2.dp, edgeH, onZone)
            Zone(s, Gesture.EDGE_CENTER, pad + lw, top, inner - lw - rw, edgeH, onZone)
            Zone(s, Gesture.EDGE_RIGHT, pad + inner - rw + 2.dp, top, rw - 2.dp, edgeH, onZone)
        } else {
            Zone(s, Gesture.EDGE, pad, top, inner, edgeH, onZone)
        }
        val restTop = top + edgeH + 10.dp
        val rest = h - restTop - pad
        Zone(s, Gesture.SWIPE_DOWN, 30.dp, restTop, w - 60.dp, rest * 0.34f, onZone, prefix = stringResource(R.string.gesture_short_down))
        Zone(s, Gesture.SWIPE_UP, 30.dp, restTop + rest * 0.38f, w - 60.dp, rest * 0.26f, onZone, prefix = stringResource(R.string.gesture_short_up))
        Zone(s, Gesture.DOUBLE_TAP, 30.dp, restTop + rest * 0.68f, (w - 66.dp) / 2, rest * 0.30f, onZone, prefix = stringResource(R.string.gesture_short_double))
        Zone(s, Gesture.DOUBLE_TAP_BOTTOM, 36.dp + (w - 66.dp) / 2, restTop + rest * 0.68f, (w - 66.dp) / 2, rest * 0.30f, onZone, prefix = stringResource(R.string.gesture_short_double_bottom))
        // Bedienungshilfe aktiv: Streifen direkt auf der Statusleiste markieren.
        if (onStatusBar) {
            Row(Modifier.fillMaxWidth().height(4.dp)) {
                Box(Modifier.fillMaxHeight().weight(s.swipeZoneLeft.toFloat().coerceAtLeast(1f)).background(actionColor(GestureClassifier.binding(s, Gesture.EDGE_LEFT).action)))
                Spacer(Modifier.weight((100 - s.swipeZoneLeft - s.swipeZoneRight).toFloat().coerceAtLeast(1f)))
                Box(Modifier.fillMaxHeight().weight(s.swipeZoneRight.toFloat().coerceAtLeast(1f)).background(actionColor(GestureClassifier.binding(s, Gesture.EDGE_RIGHT).action)))
            }
        }
    }
}

@Composable
private fun Zone(s: LauncherSettings, g: Gesture, x: Dp, y: Dp, w: Dp, h: Dp, onZone: (Gesture) -> Unit, prefix: String? = null) {
    if (w <= 4.dp || h <= 4.dp) return
    val a = GestureClassifier.binding(s, g).action
    val col = actionColor(a)
    val name = actionName(a)
    val desc = gestureName(g) + ": " + name
    Column(
        Modifier.offset(x, y).size(w, h).clip(RoundedCornerShape(12.dp))
            .background(col.copy(alpha = if (a == GestureAction.NOTHING) 0.14f else 0.38f))
            .border(1.5.dp, col.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = gestureName(g)) { onZone(g) }
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(actionIcon(a)), null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(
            if (prefix != null) "$prefix: $name" else name,
            style = TextStyle(
                fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, lineHeight = 11.sp, color = Color.White, textAlign = TextAlign.Center,
                hyphens = androidx.compose.ui.text.style.Hyphens.Auto, lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph,
            ),
            maxLines = 3,
        )
    }
}

/** Mini-Vorschau einer Aktion: kleines Handy mit dem Ergebnis. */
@Composable
private fun ActionPreview(a: GestureAction) {
    Box(
        Modifier.size(30.dp, 52.dp).clip(RoundedCornerShape(7.dp)).background(if (a == GestureAction.LOCK_SCREEN) Color.Black else Color(0xFF1B2A4A))
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(7.dp)),
    ) {
        val (h, col) = when (a) {
            GestureAction.NOTIFICATIONS -> 30.dp to Color.White.copy(alpha = 0.35f)
            GestureAction.QUICK_SETTINGS -> 40.dp to Tos.Accent.copy(alpha = 0.55f)
            GestureAction.SYSTEM_NOTIFICATIONS -> 24.dp to Color.White
            GestureAction.SYSTEM_QUICK_SETTINGS -> 46.dp to Color.White
            GestureAction.SEARCH -> 8.dp to Color.White.copy(alpha = 0.5f)
            GestureAction.APP_DRAWER -> 46.dp to Color(0x99D678FF)
            else -> 0.dp to Color.Transparent
        }
        if (h > 0.dp) Box(Modifier.padding(2.dp).fillMaxWidth().height(h).clip(RoundedCornerShape(4.dp)).background(col))
    }
}

/** Große Auswahlkarte: Symbol, Name, ein Satz Erklärung, Mini-Vorschau. Für alle Auswahlseiten. */
@Composable
fun OptionCard(
    icon: Int?,
    title: String,
    info: String,
    selected: Boolean,
    enabled: Boolean = true,
    preview: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val c = LocalPanelColors.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) Tos.Accent.copy(alpha = 0.14f) else c.chip)
            .border(if (selected) 1.5.dp else 0.dp, if (selected) Tos.Accent else Color.Transparent, shape)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(if (selected) Tos.Accent else Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(icon), null, tint = if (selected) Tos.AccentInk else c.content, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = TitleStyle.copy(color = if (enabled) c.content else c.secondary, fontSize = 15.sp))
            Text(info, style = SummaryStyle.copy(color = c.secondary, fontSize = 13.sp))
        }
        if (preview != null) {
            Spacer(Modifier.width(10.dp))
            preview()
        }
    }
}
