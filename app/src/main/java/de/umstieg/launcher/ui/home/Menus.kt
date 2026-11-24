package de.umstieg.launcher.ui.home

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.LayoutOps
import de.umstieg.launcher.core.WidgetItem
import de.umstieg.launcher.core.iconOptions
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.ui.drawer.SearchOverlay
import de.umstieg.launcher.ui.icons.AppIconImage
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.panels.NotificationPanel
import de.umstieg.launcher.ui.panels.PillButton
import de.umstieg.launcher.ui.panels.QuickSettingsPanel
import de.umstieg.launcher.ui.settings.SwitchRow
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.LocalPanelColors
import de.umstieg.launcher.ui.theme.LocalTosStyle
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.glass

/** Vollbild-Panels mit Einblenden/Ausblenden (Dauer folgt der Animationsgeschwindigkeit). */
@Composable
fun OverlayHost(
    overlay: Overlay,
    vm: HomeViewModel,
    d: LauncherData,
    apps: Map<AppKey, AppInfo>,
    shape: SuperellipseShape,
    actions: HomeActions,
    launch: (AppInfo, Rect) -> Unit,
) {
    val style = LocalTosStyle.current
    val ms = if (style.reduceMotion || style.motionScale == 0f) 0 else (220 / style.motionScale.coerceAtLeast(0.5f)).toInt()
    Overlay.entries.filter { it != Overlay.NONE }.forEach { o ->
        AnimatedVisibility(
            visible = overlay == o,
            enter = fadeIn(tween(ms)) + slideInVertically(tween(ms)) { -it / 12 },
            exit = fadeOut(tween(ms)) + slideOutVertically(tween(ms)) { -it / 12 },
        ) {
            when (o) {
                Overlay.SEARCH -> SearchOverlay(vm, d, apps, shape, actions, launch)
                Overlay.NOTIFICATIONS -> NotificationPanel(vm, d, apps, shape)
                Overlay.QUICK_SETTINGS -> QuickSettingsPanel(vm, d.quickSettings)
                Overlay.NONE -> Unit
            }
        }
    }
}

/** Menü nach langem Drücken in Übersicht oder Suche. */
@Composable
fun AppMenuSheet(vm: HomeViewModel, d: LauncherData, app: AppInfo, actions: HomeActions, page: Int) {
    val c = LocalPanelColors.current
    val placed = remember(d.layout) { LayoutOps.placedApps(d.layout) }
    val hidden = app.key in d.hiddenApps
    val badgeOff = app.key.packageName in d.badgeDisabled
    fun close() = vm.showMenu(null)
    Box(
        Modifier.fillMaxSize().background(c.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(if (c.dark) Color(0xF2141B27) else Color(0xF7F2F4F7))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconImage(vm.icons, app, d.settings.iconOptions(c.dark), SuperellipseIconShape, 52.dp)
                Spacer(Modifier.width(14.dp))
                Text(app.label, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.content), modifier = Modifier.semantics { heading() })
            }
            Spacer(Modifier.height(12.dp))
            if (app.key !in placed) MenuRow(R.drawable.ic_lucide_house, stringResource(R.string.menu_to_home)) { vm.addApp(app.key, page); close() }
            MenuRow(R.drawable.ic_lucide_square_pen, stringResource(R.string.menu_customize)) { close(); vm.showCustomize(app) }
            MenuRow(R.drawable.ic_lucide_info, stringResource(R.string.menu_app_info)) { vm.controller.apps.showAppDetails(app, null); close() }
            MenuRow(if (badgeOff) R.drawable.ic_lucide_bell else R.drawable.ic_lucide_bell_off, stringResource(if (badgeOff) R.string.menu_badge_on else R.string.menu_badge_off)) {
                vm.controller.setBadgeDisabled(app.key.packageName, !badgeOff); close()
            }
            MenuRow(R.drawable.ic_lucide_eye_off, stringResource(if (hidden) R.string.menu_unhide else R.string.menu_hide)) {
                vm.controller.setHidden(app.key, !hidden); close()
            }
            MenuRow(R.drawable.ic_lucide_trash_2, stringResource(R.string.menu_uninstall), Tos.Danger) { actions.uninstall(app); close() }
        }
    }
}

/** Gemeinsame Squircle-Form für kleine Icons in Menüs. */
private val SuperellipseIconShape = SuperellipseShape(0.62f)

@Composable
private fun MenuRow(icon: Int, text: String, color: Color? = null, onClick: () -> Unit) {
    val c = LocalPanelColors.current
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = color ?: c.content, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, style = TextStyle(fontFamily = Inter, fontSize = 17.sp, color = color ?: c.content))
    }
}

/** Name und Icon-Stil einer App ändern, Badge an/aus. */
@Composable
fun CustomizeDialog(vm: HomeViewModel, d: LauncherData, app: AppInfo) {
    val current = d.appOverrides[app.key.id]
    var name by remember(app.key) { mutableStateOf(current?.label ?: "") }
    var mono by remember(app.key) { mutableStateOf(current?.monochrome ?: false) }
    var badge by remember(app.key) { mutableStateOf(app.key.packageName !in d.badgeDisabled) }
    AlertDialog(
        onDismissRequest = { vm.showCustomize(null) },
        title = { Text(stringResource(R.string.customize_title, app.originalLabel)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text(stringResource(R.string.customize_name)) },
                    placeholder = { Text(app.originalLabel) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    stringResource(R.string.customize_mono),
                    stringResource(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) R.string.customize_mono_summary else R.string.customize_mono_unsupported),
                    mono,
                ) { mono = it }
                SwitchRow(stringResource(R.string.customize_badge), null, badge) { badge = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.controller.setOverride(app.key, name, mono)
                vm.controller.setBadgeDisabled(app.key.packageName, !badge)
                vm.showCustomize(null)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    vm.controller.setOverride(app.key, null, false)
                    vm.showCustomize(null)
                }) { Text(stringResource(R.string.customize_reset)) }
                TextButton(onClick = { vm.showCustomize(null) }) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** Pausiertes Arbeitsprofil: fragen, ob es fortgesetzt werden soll. */
@Composable
fun QuietProfileDialog(vm: HomeViewModel, app: AppInfo) {
    AlertDialog(
        onDismissRequest = { vm.showQuietDialog(null) },
        title = { Text(stringResource(R.string.quiet_title)) },
        text = { Text(stringResource(R.string.quiet_text, app.label)) },
        confirmButton = {
            TextButton(onClick = {
                vm.controller.apps.resumeProfile(app.user)
                vm.showQuietDialog(null)
            }) { Text(stringResource(R.string.quiet_resume)) }
        },
        dismissButton = { TextButton(onClick = { vm.showQuietDialog(null) }) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Größe eines Widgets im Raster ändern (Spalten × Reihen). */
@Composable
fun ResizeWidgetDialog(vm: HomeViewModel, d: LauncherData, widgetId: Int) {
    val item = LayoutOps.allItems(d.layout).filterIsInstance<WidgetItem>().firstOrNull { it.widgetId == widgetId }
    if (item == null) {
        vm.showResize(null)
        return
    }
    var w by remember(widgetId) { mutableIntStateOf(item.spanX) }
    var h by remember(widgetId) { mutableIntStateOf(item.spanY) }
    AlertDialog(
        onDismissRequest = { vm.showResize(null) },
        title = { Text(stringResource(R.string.widget_resize)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(stringResource(R.string.widget_width), w, 1..d.settings.columns) { w = it }
                Stepper(stringResource(R.string.widget_height), h, 1..d.settings.rows) { h = it }
                Text(stringResource(R.string.widget_resize_hint))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.controller.resizeGridWidget(widgetId, w, h)
                vm.showResize(null)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = { vm.showResize(null) }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = { onChange((value - 1).coerceIn(range)) }) { Text("−") }
        Text("$value")
        TextButton(onClick = { onChange((value + 1).coerceIn(range)) }) { Text("+") }
    }
}

/** Kurze Einführung beim ersten Start (über „Anleitung erneut zeigen“ wiederholbar). */
@Composable
fun Onboarding(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val steps = listOf(
        Triple(R.drawable.ic_lucide_bell, R.string.intro_notifications_title, R.string.intro_notifications_text),
        Triple(R.drawable.ic_lucide_sliders_horizontal, R.string.intro_quick_title, R.string.intro_quick_text),
        Triple(R.drawable.ic_lucide_search, R.string.intro_search_title, R.string.intro_search_text),
        Triple(R.drawable.ic_lucide_hand, R.string.intro_edit_title, R.string.intro_edit_text),
    )
    val (icon, title, text) = steps[step]
    Box(
        Modifier.fillMaxSize().background(Color(0xB30B0F16))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(24.dp).fillMaxWidth().glass(RoundedCornerShape(30.dp), Color(0xE6182233), Tos.GlassLine).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(Tos.Accent, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, tint = Tos.AccentInk, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(title), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Tos.Ink), modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(stringResource(text), style = TextStyle(fontFamily = Inter, fontSize = 16.sp, lineHeight = 22.sp, color = Tos.Ink2))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.intro_step, step + 1, steps.size), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink3))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(stringResource(R.string.intro_skip), accent = false, onClick = onDone)
                PillButton(stringResource(if (step == steps.lastIndex) R.string.intro_done else R.string.intro_next), accent = true) {
                    if (step == steps.lastIndex) onDone() else step++
                }
            }
        }
    }
}
