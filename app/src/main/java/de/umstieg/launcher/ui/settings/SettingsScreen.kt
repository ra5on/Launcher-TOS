package de.umstieg.launcher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.R
import de.umstieg.launcher.core.FuzzySearch
import de.umstieg.launcher.core.HapticStrength
import de.umstieg.launcher.core.IndicatorStyle
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.ThemeMode
import de.umstieg.launcher.data.NotificationCenter
import de.umstieg.launcher.ui.drawer.highlighted
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.LocalPanelColors
import de.umstieg.launcher.ui.theme.glass
import kotlin.math.max

/** Angeforderte Einstellungsseite; [id] unterscheidet wiederholte Anforderungen derselben Seite. */
data class SectionRequest(val section: Section, val id: Long)

/** Aktionen, die die Activity ausführt (Dateiauswahl, Rollen-Anfrage, Geräte-Sperre). */
class SettingsActions(
    val requestDefault: () -> Unit,
    val export: () -> Unit,
    val import: () -> Unit,
    val authenticate: (onSuccess: () -> Unit) -> Unit,
    val restartSetup: () -> Unit,
    val openIntent: (android.content.Intent) -> Unit,
)

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    isDefault: Boolean,
    versionName: String,
    startSection: SectionRequest,
    licenses: () -> List<Pair<String, String>>,
    actions: SettingsActions,
) {
    val data by vm.data.collectAsStateWithLifecycle()
    val apps by vm.previewApps.collectAsStateWithLifecycle()
    val d = data ?: return
    var section by rememberSaveable(startSection.id) { mutableStateOf(startSection.section) }
    BackHandler(enabled = section != Section.OVERVIEW) { section = if (section == Section.HIDDEN) Section.DRAWER else Section.OVERVIEW }
    val c = LocalPanelColors.current
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(c.background)
            drawRect(Brush.radialGradient(listOf(c.backgroundTop, Color.Transparent), Offset(0f, 0f), max(size.width, size.height) * 0.7f))
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val back = { section = Section.OVERVIEW }
            val p = PageEnv(vm, d, apps, actions, back) { section = it }
            when (section) {
                Section.OVERVIEW -> Overview(d, isDefault, versionName, actions) { section = it }
                Section.GRID -> GridPage(p)
                Section.ICONS -> IconsPage(p)
                Section.LABELS -> LabelsPage(p)
                Section.DOCK -> DockPage(p)
                Section.DESIGN -> DesignPage(p)
                Section.GLASS -> GlassPage(p)
                Section.GESTURES -> GesturesPage(p)
                Section.BADGES -> BadgesPage(p)
                Section.QUICK -> QuickPage(p)
                Section.SEARCH -> SearchPage(p)
                Section.DRAWER -> DrawerSettingsPage(p)
                Section.HIDDEN -> HiddenAppsPage(p)
                Section.WIDGETS -> WidgetsPage(p)
                Section.HAPTICS -> HapticsPage(p)
                Section.ACCESSIBILITY -> AccessibilityPage(p)
                Section.PERMISSIONS -> PermissionsPage(p)
                Section.BACKUP -> BackupPage(p)
                Section.ABOUT -> AboutPage(p, versionName, licenses)
                Section.UPDATES -> UpdatesPage(p)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun onOff(b: Boolean) = stringResource(if (b) R.string.on else R.string.off)

@Composable
fun indicatorName(i: IndicatorStyle) = stringResource(
    when (i) {
        IndicatorStyle.DOTS -> R.string.indicator_dots
        IndicatorStyle.BAR -> R.string.indicator_bar
        IndicatorStyle.NUMBER -> R.string.indicator_number
        IndicatorStyle.NONE -> R.string.indicator_none
    },
)

@Composable
fun hapticName(h: HapticStrength) = stringResource(
    when (h) {
        HapticStrength.LIGHT -> R.string.haptics_light
        HapticStrength.MEDIUM -> R.string.haptics_medium
        HapticStrength.STRONG -> R.string.haptics_strong
    },
)

fun themeName(t: ThemeMode) = when (t) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun Overview(d: LauncherData, isDefault: Boolean, versionName: String, actions: SettingsActions, open: (Section) -> Unit) {
    val s = d.settings
    val c = LocalPanelColors.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    Text(
        stringResource(R.string.settings_title),
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = c.content),
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp).semantics { heading() },
    )
    // Suche über alle Einstellungen.
    val hint = stringResource(R.string.settings_search_hint)
    Row(
        Modifier.fillMaxWidth().height(52.dp).glass(de.umstieg.launcher.ui.theme.Tos.PillShape, c.glass, c.glassLine).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_lucide_search), null, tint = c.secondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(hint, style = SummaryStyle.copy(color = c.secondary, fontSize = 17.sp))
            BasicTextField(
                value = query,
                onValueChange = { query = it.take(40) },
                singleLine = true,
                textStyle = SummaryStyle.copy(color = c.content, fontSize = 17.sp),
                cursorBrush = SolidColor(de.umstieg.launcher.ui.theme.Tos.Accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
            )
        }
    }
    if (query.isNotBlank()) {
        val titled = SettingsIndex.entries.map { it to stringResource(it.title) }
        val hits = FuzzySearch.rank(titled, { it.second }, query).distinctBy { it.item.second }.take(12)
        SettingsCard {
            if (hits.isEmpty()) Text(stringResource(R.string.search_none), style = SummaryStyle.copy(color = c.secondary), modifier = Modifier.padding(18.dp))
            hits.forEachIndexed { i, h ->
                if (i > 0) CardDivider()
                Row(
                    Modifier.fillMaxWidth().padding(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionRowAnnotated(highlighted(h.item.second, h.highlight)) { open(h.item.first.section) }
                }
            }
        }
        return
    }
    SettingsCard {
        NavRow(R.drawable.ic_lucide_layout_grid, Color(0xFF26B597), stringResource(R.string.settings_section_grid),
            stringResource(R.string.summary_grid, s.columns, s.rows, indicatorName(s.indicator))) { open(Section.GRID) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_image, Color(0xFF4F7CF0), stringResource(R.string.settings_section_icons),
            stringResource(R.string.summary_icons, s.iconRounding, s.iconSize)) { open(Section.ICONS) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_type, Color(0xFF8E6CF0), stringResource(R.string.settings_section_labels),
            stringResource(R.string.summary_labels, onOff(s.labelsHome), s.fontScale)) { open(Section.LABELS) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_panel_bottom, Color(0xFFF07A5C), stringResource(R.string.settings_section_dock),
            if (s.dockEnabled) stringResource(R.string.summary_dock_on, s.dockCount) else stringResource(R.string.summary_dock_off)) { open(Section.DOCK) }
    }
    SettingsCard {
        NavRow(R.drawable.ic_lucide_palette, Color(0xFF2FB8D6), stringResource(R.string.settings_section_design), stringResource(themeName(s.theme))) { open(Section.DESIGN) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_sun_moon, Color(0xFF5C8DF0), stringResource(R.string.settings_section_glass), stringResource(R.string.summary_glass, s.glassTint, s.blurStrength)) { open(Section.GLASS) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_hand, Color(0xFFF2A93B), stringResource(R.string.settings_section_gestures), stringResource(R.string.summary_gestures)) { open(Section.GESTURES) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_bell, Color(0xFFF2545B), stringResource(R.string.settings_section_badges),
            stringResource(if (NotificationCenter.isPermissionGranted(context)) R.string.summary_badges_on else R.string.summary_badges_no_access)) { open(Section.BADGES) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_sliders_horizontal, Color(0xFF4F8DF7), stringResource(R.string.settings_section_quick),
            stringResource(R.string.summary_quick, d.quickSettings.pages.size, d.quickSettings.pages.sumOf { it.items.size })) { open(Section.QUICK) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_search, Color(0xFF6B7486), stringResource(R.string.settings_section_search), stringResource(R.string.summary_search)) { open(Section.SEARCH) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_grid_3x3, Color(0xFF26B597), stringResource(R.string.settings_section_drawer), onOff(s.drawerEnabled)) { open(Section.DRAWER) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_layout_dashboard, Color(0xFF9B7BF2), stringResource(R.string.settings_section_widgets), onOff(s.widgetPageEnabled)) { open(Section.WIDGETS) }
    }
    SettingsCard {
        NavRow(R.drawable.ic_lucide_vibrate, Color(0xFFA35CF0), stringResource(R.string.settings_section_haptics),
            stringResource(R.string.summary_haptics, if (s.haptics) hapticName(s.hapticStrength) else onOff(false), onOff(s.wobble))) { open(Section.HAPTICS) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_accessibility, Color(0xFF3FB86A), stringResource(R.string.settings_section_accessibility),
            stringResource(R.string.setting_simple_mode) + " · " + onOff(s.simpleMode)) { open(Section.ACCESSIBILITY) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_shield, Color(0xFF3D5A80), stringResource(R.string.settings_section_permissions), stringResource(R.string.summary_permissions)) { open(Section.PERMISSIONS) }
    }
    SettingsCard {
        NavRow(R.drawable.ic_lucide_house, Color(0xFF2E9C87), stringResource(R.string.settings_section_default),
            stringResource(if (isDefault) R.string.summary_default_yes else R.string.summary_default_no)) { actions.requestDefault() }
        CardDivider()
        NavRow(R.drawable.ic_lucide_download, Color(0xFF5C6B85), stringResource(R.string.settings_section_backup), stringResource(R.string.summary_backup)) { open(Section.BACKUP) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_download, Color(0xFF26B597), stringResource(R.string.settings_section_updates), stringResource(if (s.autoUpdateCheck) R.string.summary_updates_auto else R.string.summary_updates_manual)) { open(Section.UPDATES) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_info, Color(0xFF7C8799), stringResource(R.string.settings_section_about), stringResource(R.string.summary_about, versionName)) { open(Section.ABOUT) }
    }
}
