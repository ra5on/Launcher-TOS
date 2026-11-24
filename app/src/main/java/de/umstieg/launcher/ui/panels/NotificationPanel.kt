package de.umstieg.launcher.ui.panels

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.RelativeTime
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.NotificationCenter
import de.umstieg.launcher.ui.home.HomeViewModel
import de.umstieg.launcher.ui.home.Overlay
import de.umstieg.launcher.ui.icons.AppIconImage
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.core.iconOptions
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Zeitpunkt, an dem die Liste zuletzt geschlossen wurde („Neu“ = danach gepostet). */
private var lastSeen = 0L

/** Eigene Benachrichtigungsliste (Wisch von oben links). */
@Composable
fun NotificationPanel(vm: HomeViewModel, d: LauncherData, apps: Map<AppKey, AppInfo>, shape: SuperellipseShape) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(NotificationCenter.isPermissionGranted(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = NotificationCenter.isPermissionGranted(context) }
    val all by NotificationCenter.notifications.collectAsStateWithLifecycle()
    val seenBefore = remember { lastSeen }
    DisposableEffect(Unit) { onDispose { lastSeen = System.currentTimeMillis() } }
    val visible = remember(all) { NotificationCenter.visible(all) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val byPackage = remember(apps) { apps.values.groupBy { it.component.packageName } }

    // Gruppen: je App die neueste Karte, darunter „+ n weitere“ (antippen klappt auf).
    val groups: List<List<StatusBarNotification>> = remember(visible, d.settings.groupNotifications) {
        if (d.settings.groupNotifications) visible.groupBy { it.packageName }.values.toList() else visible.map { listOf(it) }
    }
    val fresh = groups.filter { it.first().postTime > seenBefore }
    val older = groups.filter { it.first().postTime <= seenBefore }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (de.umstieg.launcher.ui.theme.LocalTosStyle.current.lightWallpaper) Color(0xD90B0F16) else Color(0x990B0F16))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val date = remember { SimpleDateFormat("EEEE, d. MMMM", Locale.GERMAN).format(Date()) }
            Text(date, style = TextStyle(fontFamily = Inter, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Tos.Ink2), modifier = Modifier.weight(1f))
            if (granted && visible.isNotEmpty()) {
                Row(
                    Modifier.glass(Tos.PillShape, Tos.GlassControl, Tos.GlassLine).clickable { NotificationCenter.cancelAll() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_lucide_list_x), null, tint = Tos.Ink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.notifications_clear_all), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Tos.Ink))
                }
            }
        }
        Text(
            stringResource(R.string.notifications_title),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = Tos.Ink),
            modifier = Modifier.padding(vertical = 10.dp).semantics { heading() },
        )
        if (!granted) {
            ListenerExplanation(onAllow = { vm.system.start(context, vm.system.listenerSettingsIntent()) }, onSystem = {
                vm.showOverlay(Overlay.NONE)
                vm.system.expandStatusBar(false)
            })
            return@Column
        }
        if (visible.isEmpty()) {
            Text(stringResource(R.string.notifications_empty), style = TextStyle(fontFamily = Inter, fontSize = 17.sp, color = Tos.Ink2), modifier = Modifier.padding(vertical = 24.dp))
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(R.string.notifications_new to fresh, R.string.notifications_earlier to older).forEach { (title, list) ->
                if (list.isEmpty()) return@forEach
                item(key = "h$title") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)) {
                        Text(stringResource(title).uppercase(), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.8.sp, color = Tos.Ink2), modifier = Modifier.weight(1f))
                        Text("${list.sumOf { it.size }}", style = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = Tos.Ink2))
                    }
                }
                items(list, key = { it.first().key }) { group ->
                    val open = expanded[group.first().key] == true
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (if (open) group else group.take(1)).forEach { sbn ->
                            NotificationCard(vm, d, sbn, byPackage[sbn.packageName]?.firstOrNull(), shape, if (!open && group.size > 1) group.size - 1 else 0) {
                                expanded[group.first().key] = !open
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

/** Lesbarer Name für Pakete ohne Launcher-Eintrag; System-Pakete → null (dann „System“). */
private fun fallbackAppName(context: android.content.Context, pkg: String): String? {
    if (pkg == "android" || pkg.startsWith("com.android.")) return null
    val pm = context.packageManager
    val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
    if (label != null && label != pkg) return label
    return pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}

/** Deutsche Zeitangabe unabhängig von der Systemsprache. */
@Composable
private fun relativeTimeText(t: RelativeTime): String = when (t) {
    RelativeTime.Now -> stringResource(R.string.time_now)
    is RelativeTime.Minutes -> stringResource(R.string.time_minutes, t.n)
    is RelativeTime.Hours -> stringResource(R.string.time_hours, t.n)
    RelativeTime.Yesterday -> stringResource(R.string.time_yesterday)
    is RelativeTime.Date -> t.date.format(java.time.format.DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN))
}

/** Erklärt, wofür der Benachrichtigungszugriff dient, und führt zu den Systemeinstellungen. */
@Composable
fun ListenerExplanation(onAllow: () -> Unit, onSystem: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), Tos.Glass, Tos.GlassLine).padding(20.dp)) {
        Icon(painterResource(R.drawable.ic_lucide_bell), null, tint = Tos.Accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.listener_title), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Tos.Ink))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.listener_text), style = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 21.sp, color = Tos.Ink2))
        Spacer(Modifier.height(14.dp))
        PillButton(stringResource(R.string.listener_allow), accent = true, onClick = onAllow)
        if (onSystem != null) {
            Spacer(Modifier.height(8.dp))
            PillButton(stringResource(R.string.listener_system), accent = false, onClick = onSystem)
        }
    }
}

@Composable
fun PillButton(text: String, accent: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(Tos.PillShape)
            .background(if (accent) Tos.Accent else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = if (accent) Tos.AccentInk else Tos.Ink))
    }
}

@Composable
private fun NotificationCard(
    vm: HomeViewModel,
    d: LauncherData,
    sbn: StatusBarNotification,
    app: AppInfo?,
    shape: SuperellipseShape,
    more: Int,
    onToggleMore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offset = remember(sbn.key) { Animatable(0f) }
    var width by remember { mutableStateOf(1) }
    val extras = sbn.notification.extras
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
    val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
    val appName = app?.label ?: remember(sbn.packageName) { fallbackAppName(context, sbn.packageName) } ?: stringResource(R.string.notifications_system)
    // Zeit der Benachrichtigung selbst (von der App gesetzt), sonst Zeitpunkt des Postens.
    val time = relativeTimeText(RelativeTime.of(sbn.notification.`when`.takeIf { it > 0 } ?: sbn.postTime, System.currentTimeMillis()))
    val large: ImageBitmap? by produceState<ImageBitmap?>(null, sbn.key) {
        value = withContext(Dispatchers.IO) {
            runCatching { sbn.notification.getLargeIcon()?.loadDrawable(context)?.toBitmap(160, 160)?.asImageBitmap() }.getOrNull()
        }
    }
    val removeLabel = stringResource(R.string.notifications_remove)
    fun dismiss() = NotificationCenter.cancel(sbn.key)

    Box(Modifier.fillMaxWidth()) {
        // Hintergrund beim Wischen: „Entfernen“.
        if (offset.value != 0f) {
            Row(
                Modifier.matchParentSize().clip(RoundedCornerShape(28.dp)).background(Tos.Danger).padding(horizontal = 24.dp),
                horizontalArrangement = if (offset.value < 0) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_lucide_trash_2), null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(removeLabel, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Color.White))
            }
        }
        Row(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .glass(RoundedCornerShape(28.dp), Color(0xCC1A2233), Tos.GlassLine)
                .pointerInput(sbn.key) {
                    width = size.width
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (sbn.isClearable && abs(offset.value) > width * 0.35f) {
                                    offset.animateTo(if (offset.value < 0) -width.toFloat() else width.toFloat())
                                    dismiss()
                                } else {
                                    offset.animateTo(0f)
                                }
                            }
                        },
                    ) { change, dx ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + dx) }
                    }
                }
                .clickable {
                    if (vm.system.send(sbn.notification.contentIntent)) {
                        if (sbn.notification.flags and Notification.FLAG_AUTO_CANCEL != 0) dismiss()
                        vm.showOverlay(Overlay.NONE)
                    }
                }
                .semantics { if (sbn.isClearable) customActions = listOf(CustomAccessibilityAction(removeLabel) { dismiss(); true }) }
                .padding(18.dp),
        ) {
            if (app != null) {
                AppIconImage(vm.icons, app, d.settings.iconOptions(), shape, 46.dp)
            } else {
                Box(Modifier.size(46.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_lucide_bell), null, tint = Tos.Ink)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(appName.uppercase(), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.6.sp, color = Tos.Ink2), modifier = Modifier.weight(1f), maxLines = 1)
                    Text(time, style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink3))
                }
                if (title.isNotEmpty()) Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Tos.Ink), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (text.isNotEmpty()) Text(text, style = TextStyle(fontFamily = Inter, fontSize = 16.sp, color = Tos.Ink), maxLines = 4, overflow = TextOverflow.Ellipsis)
                if (more > 0) {
                    Text(
                        stringResource(R.string.notifications_more, more),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Tos.Ink2),
                        modifier = Modifier.clickable(onClick = onToggleMore).padding(top = 8.dp, bottom = 4.dp),
                    )
                }
            }
            large?.let {
                Spacer(Modifier.width(12.dp))
                Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
            }
        }
    }
}
