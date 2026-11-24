package de.umstieg.launcher.ui.home

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.umstieg.launcher.BuildConfig
import de.umstieg.launcher.R
import de.umstieg.launcher.core.Gesture
import de.umstieg.launcher.core.GestureClassifier
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.SetupFacts
import de.umstieg.launcher.core.SetupLogic
import de.umstieg.launcher.core.SetupStep
import de.umstieg.launcher.data.NotificationCenter
import de.umstieg.launcher.data.PermissionHelp
import de.umstieg.launcher.ui.settings.actionName
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.glass

/** Liest den aktuellen Zustand aller Einrichtungs-Schritte vom Gerät. */
fun setupFacts(vm: HomeViewModel, d: LauncherData, isDefault: Boolean, context: android.content.Context) = SetupFacts(
    isDefault = isDefault,
    listener = NotificationCenter.isPermissionGranted(context),
    dnd = vm.system.hasPolicyAccess(),
    writeSettings = vm.system.canWriteSettings(),
    // Ohne automatische Updates braucht es „Unbekannte Apps installieren“ nicht.
    canInstall = vm.controller.updater.canInstall() || (d.updateChoiceMade && !d.settings.autoUpdateCheck),
    updateChoiceMade = d.updateChoiceMade,
    gesturesSeen = d.gesturesSeen,
    edgeService = de.umstieg.launcher.data.EdgeGestureService.isEnabled(context),
)

/**
 * Einrichtung beim ersten Start: führt Schritt für Schritt durch Standard-Startbildschirm,
 * Benachrichtigungszugriff (mit Hilfe zu „Eingeschränkten Einstellungen“), optional „Nicht stören“,
 * Helligkeit/Drehsperre, Updates und zeigt am Ende die Gesten. Erledigte Schritte werden übersprungen.
 */
@Composable
fun SetupWizard(vm: HomeViewModel, d: LauncherData, isDefault: Boolean, actions: HomeActions, onClose: () -> Unit) {
    val context = LocalContext.current
    val help = vm.controller.permissions
    var tick by remember { mutableIntStateOf(0) }
    // Fortschritt prozessweit merken: Android startet die Activity neu, wenn Launcher TOS Standard wird.
    var index by remember { mutableIntStateOf(vm.controller.setupIndex) }
    androidx.compose.runtime.SideEffect { vm.controller.setupIndex = index }
    var forceHelp by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        tick++
        // Zurück aus der App-Info: direkt weiter zur Zugriffs-Einstellung (nur einmal je Besuch).
        val back = help.appInfoOpenedFor
        if (back != null) {
            help.appInfoOpenedFor = null
            val granted = if (back == PermissionHelp.Kind.LISTENER) NotificationCenter.isPermissionGranted(context) else vm.system.hasPolicyAccess()
            // Schritt 2 erst, wenn die Freigabe erkennbar erteilt ist (oder sich nicht prüfen lässt) – sonst
            // käme wieder „Zugriff verweigert“. Ohne Freigabe bleibt die Hilfe (Schritt 1) stehen.
            if (!granted && (!help.restricted() || help.restrictedAllowed() == null)) {
                val intent = if (back == PermissionHelp.Kind.LISTENER) vm.system.listenerSettingsIntent() else vm.system.policyAccessIntent()
                vm.system.start(context, intent)
            }
        }
    }
    val facts = remember(tick, d, isDefault) { setupFacts(vm, d, isDefault, context) }
    val only = vm.controller.setupOnly
    val step = if (only != null) {
        only.takeIf { !SetupLogic.isDone(it, facts) && index <= it.ordinal }
    } else {
        SetupLogic.next(index, facts, d.setupDeclined, includeWelcome = index == 0)
    }

    fun finish() {
        vm.controller.setupIndex = 0
        vm.controller.setupOnly = null
        vm.controller.updateData { it.copy(onboardingDone = true) }
        onClose()
    }
    fun later() {
        if (step == null) return finish()
        index = step.ordinal + 1
        if (only != null || SetupLogic.next(index, facts, d.setupDeclined, includeWelcome = false) == null) finish()
    }
    if (step == null) {
        finish()
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F16))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (only == null) Dots(SetupStep.entries.size, step.ordinal)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (step) {
                    SetupStep.WELCOME -> StepBody(R.drawable.ic_launcher_foreground, R.string.setup_welcome_title, R.string.setup_welcome_text, bigIcon = true)
                    SetupStep.DEFAULT_HOME -> StepBody(R.drawable.ic_lucide_house, R.string.setup_default_title, R.string.setup_default_text)
                    SetupStep.NOTIFICATIONS -> {
                        val showHelp = forceHelp || help.showRestrictedHelp(PermissionHelp.Kind.LISTENER, facts.listener)
                        if (showHelp) {
                            RestrictedHelp()
                        } else {
                            // Nach der Freigabe: deutlich als zweiter Schritt kennzeichnen.
                            if (help.packageSource == SetupLogic.SOURCE_DOWNLOADED_FILE || help.packageSource == SetupLogic.SOURCE_LOCAL_FILE) {
                                Text(
                                    stringResource(R.string.restricted_step2),
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Tos.Accent),
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
                            }
                            StepBody(R.drawable.ic_lucide_bell, R.string.setup_notifications_title, R.string.setup_notifications_text)
                        }
                        if (BuildConfig.DEBUG && !showHelp) {
                            Text(
                                stringResource(R.string.setup_debug_show_help),
                                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = Tos.Ink3),
                                modifier = Modifier.padding(top = 16.dp).clickable { forceHelp = true }.padding(8.dp),
                            )
                        }
                    }
                    SetupStep.DND -> StepBody(R.drawable.ic_lucide_moon, R.string.setup_dnd_title, R.string.setup_dnd_text)
                    SetupStep.WRITE_SETTINGS -> StepBody(R.drawable.ic_lucide_sun, R.string.setup_write_title, R.string.setup_write_text)
                    SetupStep.EDGE_GESTURES -> StepBody(R.drawable.ic_lucide_arrow_down_to_line, R.string.setup_edge_title, if (help.restricted()) R.string.setup_edge_text_restricted else R.string.setup_edge_text)
                    SetupStep.UPDATES -> StepBody(R.drawable.ic_lucide_download, R.string.setup_updates_title, R.string.setup_updates_text)
                    SetupStep.INSTALL_UNKNOWN -> StepBody(R.drawable.ic_lucide_shield, R.string.setup_install_title, R.string.setup_install_text)
                    SetupStep.GESTURES -> GestureSketch(d)
                }
            }
            val listenerHelp = step == SetupStep.NOTIFICATIONS && (forceHelp || help.showRestrictedHelp(PermissionHelp.Kind.LISTENER, facts.listener))
            val primary = when (step) {
                SetupStep.WELCOME -> R.string.setup_start
                SetupStep.DEFAULT_HOME -> R.string.setup_default_action
                SetupStep.NOTIFICATIONS -> if (listenerHelp) R.string.setup_open_app_info else R.string.listener_allow
                SetupStep.DND, SetupStep.WRITE_SETTINGS, SetupStep.INSTALL_UNKNOWN -> R.string.setup_allow
                SetupStep.EDGE_GESTURES -> if (help.restricted()) R.string.setup_open_app_info else R.string.setup_edge_action
                SetupStep.UPDATES -> R.string.setup_updates_yes
                SetupStep.GESTURES -> R.string.intro_done
            }
            BigButton(stringResource(primary)) {
                when (step) {
                    SetupStep.WELCOME -> index = 1
                    SetupStep.DEFAULT_HOME -> actions.requestDefault()
                    SetupStep.NOTIFICATIONS -> if (listenerHelp) {
                        forceHelp = false
                        help.appInfoOpenedFor = PermissionHelp.Kind.LISTENER
                        vm.system.start(context, help.appDetailsIntent())
                    } else {
                        help.markAttempt(PermissionHelp.Kind.LISTENER)
                        vm.system.start(context, vm.system.listenerSettingsIntent())
                    }
                    SetupStep.DND -> {
                        help.markAttempt(PermissionHelp.Kind.DND)
                        vm.system.start(context, vm.system.policyAccessIntent())
                    }
                    SetupStep.WRITE_SETTINGS -> vm.system.start(context, vm.system.writeSettingsIntent())
                    SetupStep.EDGE_GESTURES -> {
                        // Einschalten vormerken; aktiv wird es, sobald der Dienst in den Bedienungshilfen läuft.
                        vm.controller.updateData { it.copy(settings = it.settings.copy(systemGestures = true)) }
                        vm.system.start(context, if (help.restricted()) help.appDetailsIntent() else de.umstieg.launcher.data.EdgeGestureService.settingsIntent())
                    }
                    SetupStep.UPDATES -> vm.controller.updateData { it.copy(updateChoiceMade = true, settings = it.settings.copy(autoUpdateCheck = true)) }
                    SetupStep.INSTALL_UNKNOWN -> vm.system.start(context, vm.controller.updater.allowInstallIntent())
                    SetupStep.GESTURES -> {
                        vm.controller.setupIndex = 0
                        vm.controller.updateData { it.copy(gesturesSeen = true, onboardingDone = true) }
                        onClose()
                    }
                }
            }
            if (listenerHelp) {
                // Android zeigt den Eintrag unter ⋮ oft erst nach einem ersten Versuch, den Zugriff einzuschalten.
                TextLink(stringResource(R.string.restricted_missing_entry)) {
                    help.markAttempt(PermissionHelp.Kind.LISTENER)
                    vm.system.start(context, vm.system.listenerSettingsIntent())
                }
            }
            if (step == SetupStep.UPDATES) {
                TextLink(stringResource(R.string.setup_updates_no)) {
                    vm.controller.updateData { it.copy(updateChoiceMade = true, settings = it.settings.copy(autoUpdateCheck = false)) }
                }
            } else if (step != SetupStep.WELCOME && step != SetupStep.GESTURES) {
                TextLink(stringResource(R.string.setup_later)) { later() }
            } else if (step == SetupStep.WELCOME) {
                TextLink(stringResource(R.string.setup_skip_all)) { finish() }
            }
        }
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    val desc = stringResource(R.string.intro_step, current + 1, count)
    Row(Modifier.padding(vertical = 12.dp).semantics { contentDescription = desc }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            Box(Modifier.size(width = if (i == current) 22.dp else 8.dp, height = 8.dp).background(if (i <= current) Tos.Accent else Tos.Ink3, CircleShape))
        }
    }
}

@Composable
private fun StepBody(icon: Int, title: Int, text: Int, bigIcon: Boolean = false) {
    Box(
        Modifier.size(if (bigIcon) 112.dp else 88.dp).background(if (bigIcon) Color(0xFF14243A) else Tos.Accent, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, tint = if (bigIcon) Color.Unspecified else Tos.AccentInk, modifier = Modifier.size(if (bigIcon) 112.dp else 44.dp))
    }
    Spacer(Modifier.height(28.dp))
    Text(
        stringResource(title),
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Tos.Ink, textAlign = TextAlign.Center),
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(text),
        style = TextStyle(fontFamily = Inter, fontSize = 18.sp, lineHeight = 25.sp, color = Tos.Ink2, textAlign = TextAlign.Center),
    )
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp).clip(Tos.PillShape).background(Tos.Accent)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Tos.AccentInk, textAlign = TextAlign.Center))
    }
}

@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Tos.Ink2),
        modifier = Modifier.padding(top = 8.dp).defaultMinSize(minHeight = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Hilfe zu „Eingeschränkten Einstellungen“: kurzer Text und Skizze der App-Info mit ⋮ oben rechts. */
@Composable
private fun RestrictedHelp() {
    Text(
        stringResource(R.string.restricted_title),
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Tos.Ink, textAlign = TextAlign.Center),
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(R.string.restricted_text),
        style = TextStyle(fontFamily = Inter, fontSize = 17.sp, lineHeight = 24.sp, color = Tos.Ink2, textAlign = TextAlign.Start), modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(20.dp))
    // Skizze: App-Info mit hervorgehobenem ⋮ und geöffnetem Menü.
    val sketchDesc = stringResource(R.string.restricted_sketch_description)
    Box(
        Modifier.fillMaxWidth(0.86f).aspectRatio(0.9f).clip(RoundedCornerShape(28.dp)).background(Color(0xFF1E2633))
            .border(2.dp, Tos.GlassLine, RoundedCornerShape(28.dp)).semantics { contentDescription = sketchDesc },
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_lucide_chevron_left), null, tint = Tos.Ink2)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.restricted_sketch_app_info), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Tos.Ink), modifier = Modifier.weight(1f))
                Box(Modifier.size(40.dp).background(Tos.Accent, CircleShape), contentAlignment = Alignment.Center) {
                    Text("⋮", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Tos.AccentInk))
                }
            }
            Spacer(Modifier.height(18.dp))
            repeat(4) {
                Box(Modifier.fillMaxWidth(if (it % 2 == 0) 0.7f else 0.5f).height(12.dp).background(Color.White.copy(alpha = 0.12f), CircleShape))
                Spacer(Modifier.height(14.dp))
            }
        }
        Column(
            Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = 64.dp).fillMaxWidth(0.82f)
                .background(Color(0xFF2B3444), RoundedCornerShape(16.dp)).padding(vertical = 6.dp),
        ) {
            Text(stringResource(R.string.restricted_sketch_uninstall), style = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Tos.Ink3), modifier = Modifier.padding(14.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp).border(2.dp, Tos.Accent, RoundedCornerShape(12.dp)).padding(10.dp)) {
                Text(stringResource(R.string.restricted_sketch_allow), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Tos.Ink))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        stringResource(R.string.restricted_after),
        style = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 21.sp, color = Tos.Ink2, textAlign = TextAlign.Center),
    )
}

/** Abschluss: die drei Gesten an einer Skizze (Statusleiste = Android, darunter Launcher TOS). */
@Composable
private fun GestureSketch(d: LauncherData) {
    val s = d.settings
    Text(
        stringResource(R.string.setup_gestures_title),
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Tos.Ink, textAlign = TextAlign.Center),
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(16.dp))
    val small = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Tos.Ink, textAlign = TextAlign.Center, hyphens = androidx.compose.ui.text.style.Hyphens.Auto, lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph)
    Box(
        Modifier.fillMaxWidth(0.72f).aspectRatio(0.5f).clip(RoundedCornerShape(32.dp)).glass(RoundedCornerShape(32.dp), Color(0xFF14243A), Tos.GlassLine),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Statusleiste: gehört Android.
            Box(Modifier.fillMaxWidth().height(36.dp).background(Color.White.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.setup_gestures_statusbar), style = small.copy(color = Tos.Ink2))
            }
            // Randzone darunter: Launcher TOS.
            Row(Modifier.fillMaxWidth().weight(s.edgeHeightPercent.toFloat().coerceAtLeast(10f))) {
                val zones = if (s.edgeSplit) listOf(Gesture.EDGE_LEFT, Gesture.EDGE_CENTER, Gesture.EDGE_RIGHT) else listOf(Gesture.EDGE)
                zones.forEachIndexed { i, g ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(4.dp).background(Tos.Accent.copy(alpha = if (i % 2 == 0) 0.30f else 0.20f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("↓\n" + actionName(GestureClassifier.binding(s, g).action), style = small.copy(fontSize = 12.sp), modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(100f - s.edgeHeightPercent), contentAlignment = Alignment.Center) {
                Text("↓\n" + actionName(GestureClassifier.binding(s, Gesture.SWIPE_DOWN).action), style = small.copy(fontSize = 15.sp))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        stringResource(R.string.setup_gestures_text),
        style = TextStyle(fontFamily = Inter, fontSize = 16.sp, lineHeight = 22.sp, color = Tos.Ink2, textAlign = TextAlign.Center),
    )
}

/** Kleine, wegwischbare Karte auf dem Homescreen: „Einrichtung fortsetzen (n offen)“. */
@Composable
fun SetupCard(open: Int, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .fillMaxWidth()
            .glass(Tos.CardShape, Color(0xE6182233), Tos.GlassLine)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_lucide_sparkles), null, tint = Tos.Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.setup_card, open),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Tos.Ink),
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onDismiss), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_lucide_x), stringResource(R.string.setup_card_dismiss), tint = Tos.Ink2)
        }
    }
}

/** Kleine Karte „Update verfügbar“. */
@Composable
fun UpdateCard(version: String, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .fillMaxWidth()
            .glass(Tos.CardShape, Color(0xE6182233), Tos.GlassLine)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_lucide_download), null, tint = Tos.Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.update_card, version),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Tos.Ink),
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onDismiss), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_lucide_x), stringResource(R.string.setup_card_dismiss), tint = Tos.Ink2)
        }
    }
}

