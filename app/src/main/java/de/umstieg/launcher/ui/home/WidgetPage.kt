package de.umstieg.launcher.ui.home

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.R
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.WidgetOps
import de.umstieg.launcher.ui.panels.PillButton
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Widget-Seite (ganz links): Android-Widgets untereinander, Bearbeiten-Modus zum Sortieren, Größe, Entfernen. */
@Composable
fun WidgetPage(vm: HomeViewModel, d: LauncherData, actions: HomeActions) {
    val editing by vm.widgetEdit.collectAsStateWithLifecycle()
    val entries = d.widgetPage
    val shape = RoundedCornerShape(28.dp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (entries.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(top = 40.dp).glass(shape, Tos.Glass, Tos.GlassLine).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(painterResource(R.drawable.ic_lucide_layout_dashboard), null, tint = Tos.Ink2, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.widgets_empty), style = TextStyle(fontFamily = Inter, fontSize = 17.sp, color = Tos.Ink2))
                Spacer(Modifier.height(16.dp))
                PillButton(stringResource(R.string.widgets_add), accent = true) { vm.showWidgetPicker(WidgetTarget.Page) }
            }
        }
        entries.forEach { e ->
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val h = (e.rows * 80).dp
                val w = maxWidth
                Column {
                    Box(
                        Modifier.fillMaxWidth().height(h)
                            .then(if (d.settings.widgetGlass) Modifier.glass(shape, Tos.Glass, Tos.GlassLine) else Modifier)
                            .clip(shape),
                    ) {
                        WidgetView(vm.widgets, e.widgetId, w, h, Modifier.fillMaxSize()) { vm.setWidgetEdit(true) }
                    }
                    if (editing) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp).glass(Tos.PillShape, Tos.GlassControl, Tos.GlassLine).padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            SmallAction(R.drawable.ic_lucide_chevron_up, stringResource(R.string.widgets_up)) { vm.controller.updateWidgetPage { WidgetOps.move(it, e.widgetId, -1) } }
                            SmallAction(R.drawable.ic_lucide_chevron_down, stringResource(R.string.widgets_down)) { vm.controller.updateWidgetPage { WidgetOps.move(it, e.widgetId, 1) } }
                            SmallAction(R.drawable.ic_lucide_minus, stringResource(R.string.widgets_smaller)) { vm.controller.updateWidgetPage { WidgetOps.resize(it, e.widgetId, e.rows - 1) } }
                            SmallAction(R.drawable.ic_lucide_plus, stringResource(R.string.widgets_larger)) { vm.controller.updateWidgetPage { WidgetOps.resize(it, e.widgetId, e.rows + 1) } }
                            SmallAction(R.drawable.ic_lucide_trash_2, stringResource(R.string.widget_remove)) { vm.controller.updateWidgetPage { WidgetOps.remove(it, e.widgetId) } }
                        }
                    }
                }
            }
        }
        if (entries.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                if (editing) {
                    PillButton("+ " + stringResource(R.string.widgets_add), accent = false) { vm.showWidgetPicker(WidgetTarget.Page) }
                    Spacer(Modifier.width(10.dp))
                    PillButton(stringResource(R.string.edit_done), accent = true) { vm.setWidgetEdit(false) }
                } else {
                    PillButton(stringResource(R.string.widgets_edit), accent = false) { vm.setWidgetEdit(true) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SmallAction(icon: Int, description: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), description, tint = Tos.Ink, modifier = Modifier.size(20.dp))
    }
}

/** Auswahl-Liste aller Widget-Anbieter, nach App gruppiert, mit Vorschau. */
@Composable
fun WidgetPicker(vm: HomeViewModel, target: WidgetTarget, actions: HomeActions) {
    val context = LocalContext.current
    val pm = context.packageManager
    val groups = remember {
        vm.widgets.providers().groupBy { it.provider.packageName }
            .map { (pkg, list) ->
                val app = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                app to list.sortedBy { it.loadLabel(pm) }
            }
            .sortedBy { it.first.lowercase() }
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xE60B0F16)).windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.widgets_pick), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Tos.Ink), modifier = Modifier.weight(1f).semantics { heading() })
            Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { vm.showWidgetPicker(null) }, contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_lucide_x), stringResource(R.string.close), tint = Tos.Ink)
            }
        }
        if (groups.isEmpty()) Text(stringResource(R.string.widgets_none), style = TextStyle(fontFamily = Inter, fontSize = 16.sp, color = Tos.Ink2))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            groups.forEach { (app, list) ->
                item(key = "h$app") {
                    Text(app, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Tos.Ink2), modifier = Modifier.padding(top = 10.dp))
                }
                items(list, key = { it.provider.flattenToString() }) { info -> ProviderRow(info, pm) {
                    vm.showWidgetPicker(null)
                    actions.addWidget(info, target)
                } }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun ProviderRow(info: AppWidgetProviderInfo, pm: android.content.pm.PackageManager, onPick: () -> Unit) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val preview: ImageBitmap? by produceState<ImageBitmap?>(null, info.provider) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                (info.loadPreviewImage(context, 0) ?: info.loadIcon(context, 0))?.toBitmap(320, 200)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val w = (info.minWidth / density).toInt()
    val h = (info.minHeight / density).toInt()
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).glass(RoundedCornerShape(22.dp), Tos.Glass, Tos.GlassLine)
            .clickable(role = Role.Button, onClick = onPick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(110.dp).heightIn(max = 80.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            preview?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth()) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(info.loadLabel(pm), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Tos.Ink))
            Text(stringResource(R.string.widgets_size, w, h), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink2))
        }
    }
}
