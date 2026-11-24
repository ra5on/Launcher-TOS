package de.umstieg.launcher.ui.panels

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
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
    val media by vm.media.sessions.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { lastSeen = System.currentTimeMillis() } }
    val visible = remember(all) { NotificationCenter.visible(all) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val byPackage = remember(apps) { apps.values.groupBy { it.component.packageName } }
    var options by remember { mutableStateOf<StatusBarNotification?>(null) }

    // Nach App gestapelt (neueste zuerst); ohne Gruppierung jede Benachrichtigung einzeln.
    val groups: List<List<StatusBarNotification>> = remember(visible, d.settings.groupNotifications) {
        if (d.settings.groupNotifications) visible.groupBy { it.packageName }.values.toList() else visible.map { listOf(it) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (de.umstieg.launcher.ui.theme.LocalTosStyle.current.lightWallpaper) Color(0xD90B0F16) else Color(0x990B0F16))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp),
    ) {
        // Kopf: Datum und kleine Knöpfe in einer Zeile, darunter der Titel über die ganze Breite.
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            val date = remember { SimpleDateFormat("EEEE, d. MMMM", Locale.GERMAN).format(Date()) }
            Text(date, style = TextStyle(fontFamily = Inter, fontSize = 12.sp, color = Tos.Ink2), modifier = Modifier.weight(1f))
            if (granted && visible.isNotEmpty()) {
                SmallPill(R.drawable.ic_lucide_list_x, stringResource(R.string.notifications_clear_all)) { NotificationCenter.cancelAll() }
                Spacer(Modifier.width(6.dp))
            }
            // Android-Leiste (wichtig, wenn eigene Panels die ganze Breite oben belegen).
            SmallPill(R.drawable.ic_lucide_panel_bottom, stringResource(R.string.notifications_android)) {
                vm.showOverlay(Overlay.NONE)
                vm.openSystemNotifications()
            }
        }
        Text(
            stringResource(R.string.notifications_title),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Tos.Ink),
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp).semantics { heading() },
        )
        if (!granted) {
            ListenerExplanation(onAllow = { vm.requestListenerAccess(context) }, onSystem = {
                vm.showOverlay(Overlay.NONE)
                vm.system.expandStatusBar(false)
            })
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            // Medien ganz oben als kompakte Karte.
            media.firstOrNull()?.let { info ->
                item(key = "media") { MediaCard(info, false, vm) {} }
            }
            if (visible.isEmpty()) {
                item(key = "empty") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).glass(RoundedCornerShape(18.dp), Tos.Glass, Tos.GlassLine).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.ic_lucide_bell_off), null, tint = Tos.Ink2, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.notifications_empty), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink2))
                    }
                }
            }
            groups.forEach { group ->
                val first = group.first()
                val gkey = first.packageName + ":" + first.key
                val open = expanded[first.packageName] == true || group.size == 1
                val app = byPackage[first.packageName]?.firstOrNull()
                if (group.size > 1) {
                    item(key = "h$gkey") {
                        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val name = app?.label ?: fallbackAppName(context, first.packageName) ?: stringResource(R.string.notifications_system)
                            Text(
                                "${name.uppercase()} · ${group.size}",
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.6.sp, color = Tos.Ink3),
                                modifier = Modifier.weight(1f),
                            )
                            if (open) {
                                Text(
                                    stringResource(R.string.notifications_collapse),
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Tos.Ink2),
                                    modifier = Modifier.clip(Tos.PillShape).clickable { expanded[first.packageName] = false }.padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                            }
                            Text(
                                stringResource(R.string.notifications_clear_group),
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Tos.Ink2),
                                modifier = Modifier.clip(Tos.PillShape).clickable { NotificationCenter.cancel(group.filter { it.isClearable }.map { it.key }) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                item(key = "g$gkey") {
                    // Gestapelt: nur die neueste Karte mit angedeuteten Karten darunter; Antippen klappt auf (animiert).
                    Column(Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = 0.85f, stiffness = 500f)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (open) {
                            group.forEach { sbn ->
                                NotificationCard(vm, d, sbn, byPackage[sbn.packageName]?.firstOrNull(), shape, onOptions = { options = sbn }, onTap = null)
                            }
                        } else {
                            Box {
                                // Angedeutete Karten des Stapels.
                                Box(Modifier.matchParentSize().padding(horizontal = 18.dp).offset(y = 10.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.06f)))
                                Box(Modifier.matchParentSize().padding(horizontal = 9.dp).offset(y = 5.dp).clip(RoundedCornerShape(17.dp)).background(Color.White.copy(alpha = 0.09f)))
                                NotificationCard(vm, d, first, app, shape, count = group.size, onOptions = { options = first }) { expanded[first.packageName] = true }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.notifications_hint), style = TextStyle(fontFamily = Inter, fontSize = 11.sp, color = Tos.Ink3), modifier = Modifier.padding(6.dp)) }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
    options?.let { sbn -> NotificationOptions(vm, sbn) { options = null } }
}

/** Kleine Pille im Kopf (Alle löschen, Android). */
@Composable
private fun SmallPill(icon: Int, text: String, onClick: () -> Unit) {
    Row(
        Modifier.height(30.dp).glass(Tos.PillShape, Tos.GlassControl, Tos.GlassLine).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = Tos.Ink, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = Tos.Ink))
    }
}

/** Optionen (Wischen nach rechts oder langes Drücken): stummschalten, App-Einstellungen, entfernen. */
@Composable
private fun NotificationOptions(vm: HomeViewModel, sbn: StatusBarNotification, onClose: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.notifications_options)) },
        text = {
            Column {
                listOf(
                    R.string.notifications_snooze_1h to { NotificationCenter.snooze(sbn.key, 60 * 60_000L) },
                    R.string.notifications_app_settings to {
                        vm.system.start(
                            context,
                            android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, sbn.packageName)
                                .putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, sbn.notification.channelId),
                        )
                    },
                    R.string.notifications_remove to { NotificationCenter.cancel(sbn.key) },
                ).forEach { (label, run) ->
                    Text(
                        stringResource(label),
                        modifier = Modifier.fillMaxWidth().clickable { run(); onClose() }.padding(vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}

/** Lesbarer Name für Pakete ohne Launcher-Eintrag; System-Pakete → null (dann „System“). */
internal fun fallbackAppName(context: android.content.Context, pkg: String): String? {
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
    count: Int = 1,
    onOptions: () -> Unit,
    onTap: (() -> Unit)?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offset = remember(sbn.key) { Animatable(0f) }
    var width by remember { mutableStateOf(1) }
    val n = sbn.notification
    val extras = n.extras
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
    val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
    val appName = app?.label ?: remember(sbn.packageName) { fallbackAppName(context, sbn.packageName) } ?: stringResource(R.string.notifications_system)
    // Zeit der Benachrichtigung selbst (von der App gesetzt), sonst Zeitpunkt des Postens.
    val time = relativeTimeText(RelativeTime.of(n.`when`.takeIf { it > 0 } ?: sbn.postTime, System.currentTimeMillis()))
    val large: ImageBitmap? by produceState<ImageBitmap?>(null, sbn.key) {
        value = withContext(Dispatchers.IO) {
            runCatching { n.getLargeIcon()?.loadDrawable(context)?.toBitmap(120, 120)?.asImageBitmap() }.getOrNull()
        }
    }
    // Großes Bild (BigPicture).
    val picture: ImageBitmap? by produceState<ImageBitmap?>(null, sbn.key) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                val bmp = extras.getParcelable<android.graphics.Bitmap>(Notification.EXTRA_PICTURE)
                bmp?.asImageBitmap() ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(Notification.EXTRA_PICTURE_ICON, android.graphics.drawable.Icon::class.java)
                        ?.loadDrawable(context)?.toBitmap(720, 400)?.asImageBitmap()
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    @Suppress("DEPRECATION")
                    extras.getParcelable<android.graphics.drawable.Icon>(Notification.EXTRA_PICTURE_ICON)
                        ?.loadDrawable(context)?.toBitmap(720, 400)?.asImageBitmap()
                } else {
                    null
                }
            }.getOrNull()
        }
    }
    val actions = remember(sbn.key) { n.actions?.toList().orEmpty().take(3) }
    var replyTo by remember(sbn.key) { mutableStateOf<Notification.Action?>(null) }
    var reply by remember(sbn.key) { mutableStateOf("") }
    val removeLabel = stringResource(R.string.notifications_remove)
    val optionsLabel = stringResource(R.string.notifications_options)
    fun dismiss() = NotificationCenter.cancel(sbn.key)
    val cardShape = RoundedCornerShape(18.dp)

    Box(Modifier.fillMaxWidth()) {
        // Hintergrund beim Wischen: links „Entfernen“, rechts „Optionen“.
        if (offset.value != 0f) {
            val left = offset.value < 0
            Row(
                Modifier.matchParentSize().clip(cardShape).background(if (left) Tos.Danger else Color.White.copy(alpha = 0.18f)).padding(horizontal = 18.dp),
                horizontalArrangement = if (left) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(if (left) R.drawable.ic_lucide_trash_2 else R.drawable.ic_lucide_sliders_horizontal), null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (left) removeLabel else optionsLabel, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White))
            }
        }
        Column(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .glass(cardShape, Tos.Glass, Tos.GlassLine)
                .pointerInput(sbn.key) {
                    width = size.width
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offset.value < -width * 0.35f && sbn.isClearable -> {
                                        offset.animateTo(-width.toFloat())
                                        dismiss()
                                    }
                                    offset.value > width * 0.25f -> {
                                        offset.animateTo(0f)
                                        onOptions()
                                    }
                                    else -> offset.animateTo(0f)
                                }
                            }
                        },
                    ) { change, dx ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + dx) }
                    }
                }
                .combinedClickable(onLongClick = onOptions, onLongClickLabel = optionsLabel) {
                    if (onTap != null) {
                        onTap()
                    } else if (vm.system.send(n.contentIntent)) {
                        if (n.flags and Notification.FLAG_AUTO_CANCEL != 0) dismiss()
                        vm.showOverlay(Overlay.NONE)
                    }
                }
                .semantics {
                    customActions = listOfNotNull(
                        if (sbn.isClearable) CustomAccessibilityAction(removeLabel) { dismiss(); true } else null,
                        CustomAccessibilityAction(optionsLabel) { onOptions(); true },
                    )
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            Row {
                if (app != null) {
                    AppIconImage(vm.icons, app, d.settings.iconOptions(), shape, 30.dp)
                } else {
                    Box(Modifier.size(30.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_lucide_bell), null, tint = Tos.Ink, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(appName.uppercase(), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 0.5.sp, color = Tos.Ink2), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        if (count > 1) {
                            Spacer(Modifier.width(6.dp))
                            Text("$count", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, color = Tos.Ink), modifier = Modifier.background(Color.White.copy(alpha = 0.18f), Tos.PillShape).padding(horizontal = 6.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(time, style = TextStyle(fontFamily = Inter, fontSize = 10.5.sp, color = Tos.Ink3))
                    }
                    if (title.isNotEmpty()) Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Tos.Ink), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    if (text.isNotEmpty()) Text(text, style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp, color = Tos.Ink2), maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
                if (picture == null) {
                    large?.let {
                        Spacer(Modifier.width(10.dp))
                        Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                    }
                }
            }
            picture?.let {
                Spacer(Modifier.height(8.dp))
                Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)))
            }
            if (actions.isNotEmpty() && count <= 1) {
                Row(Modifier.padding(top = 8.dp, start = 40.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    actions.forEach { a ->
                        val label = a.title?.toString().orEmpty()
                        Text(
                            label,
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Tos.Ink),
                            maxLines = 1,
                            modifier = Modifier.clip(Tos.PillShape).background(Color.White.copy(alpha = 0.14f))
                                .clickable(role = Role.Button) {
                                    if (a.remoteInputs?.isNotEmpty() == true) {
                                        replyTo = a
                                    } else if (NotificationCenter.runAction(context, a) && n.flags and Notification.FLAG_AUTO_CANCEL != 0) {
                                        dismiss()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            // Direktantwort (RemoteInput).
            replyTo?.let { a ->
                Row(
                    Modifier.padding(top = 8.dp).fillMaxWidth().height(40.dp).clip(Tos.PillShape).background(Color.White.copy(alpha = 0.10f)).padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        reply, { reply = it },
                        textStyle = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Tos.Accent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (reply.isEmpty()) Text(stringResource(R.string.notifications_reply_hint), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink3))
                            inner()
                        },
                    )
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(Tos.Accent).clickable(role = Role.Button, enabled = reply.isNotBlank()) {
                            if (NotificationCenter.runAction(context, a, reply.trim())) {
                                reply = ""
                                replyTo = null
                            }
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(painterResource(R.drawable.ic_lucide_reply), stringResource(R.string.notifications_send), tint = Tos.AccentInk, modifier = Modifier.size(15.dp)) }
                }
            }
        }
    }
}
