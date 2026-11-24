package de.umstieg.launcher.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import de.umstieg.launcher.core.iconOptions
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.ui.icons.AppIconImage
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.LocalPanelColors
import de.umstieg.launcher.ui.theme.glass

private val ButtonText = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

/** Leiste im Bearbeitungsmodus: „+“ (Apps hinzufügen), Zahnrad (Einstellungen), „Fertig“. */
@Composable
fun EditTopBar(vm: HomeViewModel, actions: HomeActions) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val addLabel = stringResource(R.string.edit_add_apps)
        Row(
            Modifier
                .height(48.dp)
                .clip(Tos.PillShape)
                .glass(Tos.PillShape, Tos.GlassControl)
                .clickable(onClickLabel = addLabel, role = Role.Button) { vm.showAddSheet(true) }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_lucide_plus), null, tint = Tos.Ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(addLabel, style = ButtonText.copy(color = Tos.Ink))
        }
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .glass(CircleShape, Tos.GlassControl)
                .clickable(role = Role.Button) { actions.openSettings() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_lucide_settings), stringResource(R.string.edit_settings), tint = Tos.Ink, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .height(48.dp)
                .clip(Tos.PillShape)
                .background(Color.White)
                .clickable(role = Role.Button) { vm.setEditMode(false) }
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_lucide_check), null, tint = Color(0xFF10141C), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.edit_done), style = ButtonText.copy(color = Color(0xFF10141C)))
        }
    }
}

/** Hinweiskarte beim ersten Start, falls Launcher TOS nicht der Standard-Launcher ist. */
@Composable
fun DefaultHintCard(onSet: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalPanelColors.current
    Column(
        modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .fillMaxWidth()
            .glass(Tos.CardShape, if (c.dark) Color(0xE6182233) else Color(0xF2FFFFFF), c.glassLine)
            .padding(18.dp),
    ) {
        Text(stringResource(R.string.default_hint_title), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = c.content))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.default_hint_text), style = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = c.secondary))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.defaultMinSize(minHeight = 48.dp).clip(Tos.PillShape).background(Tos.Accent)
                    .clickable(role = Role.Button, onClick = onSet).padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.default_hint_action), style = ButtonText.copy(color = Tos.AccentInk)) }
            Box(
                Modifier.defaultMinSize(minHeight = 48.dp).clip(Tos.PillShape).background(c.chip)
                    .clickable(role = Role.Button, onClick = onLater).padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.default_hint_later), style = ButtonText.copy(color = c.content)) }
        }
    }
}

/** Kurzer Hinweis, wenn das Layout gesperrt ist. */
@Composable
fun LockedHintCard(onOpenSettings: () -> Unit) {
    val c = LocalPanelColors.current
    Row(
        Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(14.dp)
            .fillMaxWidth()
            .glass(Tos.CardShape, if (c.dark) Color(0xE6182233) else Color(0xF2FFFFFF), c.glassLine)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_lucide_lock_keyhole), null, tint = c.content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.locked_hint), style = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = c.content), modifier = Modifier.weight(1f))
        Box(
            Modifier.defaultMinSize(minHeight = 48.dp).clip(Tos.PillShape).clickable(role = Role.Button, onClick = onOpenSettings).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.locked_hint_settings), style = ButtonText.copy(color = Tos.Accent)) }
    }
}

/** „+“-Liste: alle Apps, die nirgends auf dem Homescreen liegen. */
@Composable
fun AddAppsSheet(d: LauncherData, apps: Map<AppKey, AppInfo>, shape: SuperellipseShape, vm: HomeViewModel, onAdd: (AppKey) -> Unit) {
    val c = LocalPanelColors.current
    val placed = remember(d.layout) { LayoutOps.placedApps(d.layout) }
    val missing = remember(placed, apps) { apps.values.filter { it.key !in placed } }
    Box(
        Modifier
            .fillMaxSize()
            .background(c.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.showAddSheet(false) },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxSize(0.75f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(if (c.dark) Color(0xF2141B27) else Color(0xF7F2F4F7))
                // Klicks im Panel nicht an den Hintergrund weitergeben.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.add_apps_title),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = c.content),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Box(
                    Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { vm.showAddSheet(false) },
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(R.drawable.ic_lucide_x), stringResource(R.string.close), tint = c.content) }
            }
            if (missing.isEmpty()) {
                Text(
                    stringResource(R.string.add_apps_empty),
                    style = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = c.secondary),
                    modifier = Modifier.padding(20.dp),
                )
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(missing, key = { it.key.id }) { app ->
                    val addLabel = stringResource(R.string.add_apps_add, app.label)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 64.dp)
                            .clickable(onClickLabel = addLabel, role = Role.Button) { onAdd(app.key) }
                            .semantics(mergeDescendants = true) { contentDescription = addLabel }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconImage(vm.icons, app, d.settings.iconOptions(), shape, 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = c.content), modifier = Modifier.weight(1f))
                        Box(Modifier.size(36.dp).background(Tos.Accent, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_lucide_plus), null, tint = Tos.AccentInk, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
