package de.umstieg.launcher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.R
import de.umstieg.launcher.core.HapticStrength
import de.umstieg.launcher.core.IndicatorStyle
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.core.LegacyBackground
import de.umstieg.launcher.core.ThemeMode
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Klar
import de.umstieg.launcher.ui.theme.LocalPanelColors
import kotlin.math.max

enum class Section { OVERVIEW, GRID, ICONS, LABELS, DOCK, DESIGN, HAPTICS, ACCESSIBILITY, BACKUP, ABOUT }

/** Aktionen, die die Activity ausführt (Dateiauswahl, Rollen-Anfrage). */
class SettingsActions(
    val requestDefault: () -> Unit,
    val export: () -> Unit,
    val import: () -> Unit,
)

@Composable
fun SettingsScreen(vm: SettingsViewModel, isDefault: Boolean, versionName: String, licenses: () -> List<Pair<String, String>>, actions: SettingsActions) {
    val data by vm.data.collectAsStateWithLifecycle()
    val apps by vm.previewApps.collectAsStateWithLifecycle()
    val d = data ?: return
    var section by rememberSaveable { mutableStateOf(Section.OVERVIEW) }
    BackHandler(enabled = section != Section.OVERVIEW) { section = Section.OVERVIEW }
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
            when (section) {
                Section.OVERVIEW -> Overview(d, isDefault, versionName, actions) { section = it }
                Section.GRID -> GridPage(vm, d.settings, apps, back)
                Section.ICONS -> IconsPage(vm, d.settings, apps, back)
                Section.LABELS -> LabelsPage(vm, d.settings, apps, back)
                Section.DOCK -> DockPage(vm, d.settings, apps, back)
                Section.DESIGN -> DesignPage(vm, d.settings, back)
                Section.HAPTICS -> HapticsPage(vm, d.settings, back)
                Section.ACCESSIBILITY -> AccessibilityPage(vm, d.settings, back)
                Section.BACKUP -> BackupPage(vm, actions, back)
                Section.ABOUT -> AboutPage(versionName, licenses, back)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun onOff(b: Boolean) = stringResource(if (b) R.string.on else R.string.off)

@Composable
private fun indicatorName(i: IndicatorStyle) = stringResource(
    when (i) {
        IndicatorStyle.DOTS -> R.string.indicator_dots
        IndicatorStyle.BAR -> R.string.indicator_bar
        IndicatorStyle.NUMBER -> R.string.indicator_number
        IndicatorStyle.NONE -> R.string.indicator_none
    },
)

@Composable
private fun hapticName(h: HapticStrength) = stringResource(
    when (h) {
        HapticStrength.LIGHT -> R.string.haptics_light
        HapticStrength.MEDIUM -> R.string.haptics_medium
        HapticStrength.STRONG -> R.string.haptics_strong
    },
)

@Composable
private fun Overview(d: LauncherData, isDefault: Boolean, versionName: String, actions: SettingsActions, open: (Section) -> Unit) {
    val s = d.settings
    val c = LocalPanelColors.current
    Text(
        stringResource(R.string.settings_title),
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = c.content),
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp).semantics { heading() },
    )
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
        NavRow(R.drawable.ic_lucide_sun_moon, Color(0xFF2FB8D6), stringResource(R.string.settings_section_design),
            stringResource(themeName(s.theme))) { open(Section.DESIGN) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_vibrate, Color(0xFFA35CF0), stringResource(R.string.settings_section_haptics),
            stringResource(R.string.summary_haptics, if (s.haptics) hapticName(s.hapticStrength) else onOff(false), onOff(s.wobble))) { open(Section.HAPTICS) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_accessibility, Color(0xFF3FB86A), stringResource(R.string.settings_section_accessibility),
            stringResource(R.string.setting_large_touch) + " · " + onOff(s.largeTouch)) { open(Section.ACCESSIBILITY) }
    }
    SettingsCard {
        NavRow(R.drawable.ic_lucide_house, Color(0xFF2E9C87), stringResource(R.string.settings_section_default),
            stringResource(if (isDefault) R.string.summary_default_yes else R.string.summary_default_no)) { actions.requestDefault() }
        CardDivider()
        NavRow(R.drawable.ic_lucide_download, Color(0xFF5C6B85), stringResource(R.string.settings_section_backup),
            stringResource(R.string.summary_backup)) { open(Section.BACKUP) }
        CardDivider()
        NavRow(R.drawable.ic_lucide_info, Color(0xFF7C8799), stringResource(R.string.settings_section_about),
            stringResource(R.string.summary_about, versionName)) { open(Section.ABOUT) }
    }
}

private fun themeName(t: ThemeMode) = when (t) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun GridPage(vm: SettingsViewModel, s: LauncherSettings, apps: List<AppInfo>, back: () -> Unit) {
    var preview by remember(s) { mutableStateOf(s) }
    SubpageHeader(stringResource(R.string.settings_section_grid), back)
    GridPreview(apps, vm.icons, preview)
    SettingsCard {
        SliderBlock(stringResource(R.string.setting_columns), s.columns, LauncherSettings.COLUMNS, { "$it" },
            onPreview = { preview = preview.copy(columns = it) }, onCommit = { v -> vm.update { it.copy(columns = v) } })
        CardDivider()
        SliderBlock(stringResource(R.string.setting_rows), s.rows, LauncherSettings.ROWS, { "$it" },
            onPreview = { preview = preview.copy(rows = it) }, onCommit = { v -> vm.update { it.copy(rows = v) } })
        CardDivider()
        val dpFormat = stringResource(R.string.dp_value)
        SliderBlock(stringResource(R.string.setting_side_margin), s.sideMarginDp, LauncherSettings.SIDE_MARGIN, { String.format(dpFormat, it) },
            onPreview = { preview = preview.copy(sideMarginDp = it) }, onCommit = { v -> vm.update { it.copy(sideMarginDp = v) } }, stepSize = 4)
    }
    SettingsCard {
        SegmentedRow(
            stringResource(R.string.setting_indicator),
            IndicatorStyle.entries.map { it to indicatorName(it) },
            s.indicator,
        ) { v -> vm.update { it.copy(indicator = v) } }
    }
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_auto_add), stringResource(R.string.setting_auto_add_summary), s.autoAdd) { v -> vm.update { it.copy(autoAdd = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_lock), stringResource(R.string.setting_lock_summary), s.layoutLocked) { v -> vm.update { it.copy(layoutLocked = v) } }
    }
    ResetSection { vm.update { it.copy(columns = 4, rows = 6, sideMarginDp = 16, indicator = IndicatorStyle.DOTS, autoAdd = true, layoutLocked = false) } }
}

@Composable
private fun IconsPage(vm: SettingsViewModel, s: LauncherSettings, apps: List<AppInfo>, back: () -> Unit) {
    val percentFormat = rememberPercentFormat()
    var preview by remember(s) { mutableStateOf(s) }
    SubpageHeader(stringResource(R.string.settings_section_icons), back)
    IconRowPreview(apps, vm.icons, preview.copy(labelsHome = true), caption = true)
    SettingsCard {
        SliderBlock(
            stringResource(R.string.setting_rounding), s.iconRounding, 0..100, percentFormat,
            onPreview = { preview = preview.copy(iconRounding = it) },
            onCommit = { v -> vm.update { it.copy(iconRounding = v) } },
            start = { SideGlyph(R.drawable.ic_lucide_layout_grid) },
            end = { SideGlyph(R.drawable.ic_lucide_circle_check) },
        )
        val quick = listOf(
            0 to stringResource(R.string.rounding_square),
            30 to stringResource(R.string.rounding_soft),
            62 to stringResource(R.string.rounding_squircle),
            100 to stringResource(R.string.rounding_circle),
        )
        ChipRow(quick, quick.firstOrNull { it.first == preview.iconRounding }?.first) { v -> vm.update { it.copy(iconRounding = v) } }
        Spacer(Modifier.height(8.dp))
    }
    SettingsCard {
        SliderBlock(
            stringResource(R.string.setting_icon_size), s.iconSize, LauncherSettings.ICON_SIZE, percentFormat,
            onPreview = { preview = preview.copy(iconSize = it) },
            onCommit = { v -> vm.update { it.copy(iconSize = v) } },
            stepSize = 5,
            start = { SideGlyph(R.drawable.ic_lucide_minus) },
            end = { SideGlyph(R.drawable.ic_lucide_plus) },
        )
    }
    SettingsCard {
        SegmentedRow(
            stringResource(R.string.setting_legacy_bg),
            listOf(LegacyBackground.WHITE to stringResource(R.string.legacy_bg_white), LegacyBackground.DOMINANT to stringResource(R.string.legacy_bg_dominant)),
            s.legacyBackground,
        ) { v -> vm.update { it.copy(legacyBackground = v) } }
        Text(
            stringResource(R.string.setting_legacy_bg_summary),
            style = SummaryStyle.copy(color = LocalPanelColors.current.secondary),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        )
    }
    ResetSection { vm.update { it.copy(iconRounding = 62, iconSize = 100, legacyBackground = LegacyBackground.WHITE) } }
}

/** Formatiert „62 %“ aus der String-Ressource (auch in Lambdas nutzbar). */
@Composable
private fun rememberPercentFormat(): (Int) -> String {
    val format = stringResource(R.string.percent_value)
    return remember(format) { { v: Int -> String.format(format, v) } }
}

@Composable
private fun LabelsPage(vm: SettingsViewModel, s: LauncherSettings, apps: List<AppInfo>, back: () -> Unit) {
    val percentFormat = rememberPercentFormat()
    var preview by remember(s) { mutableStateOf(s) }
    SubpageHeader(stringResource(R.string.settings_section_labels), back)
    IconRowPreview(apps, vm.icons, preview, caption = false)
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_labels_home), checked = s.labelsHome) { v -> vm.update { it.copy(labelsHome = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_labels_dock), checked = s.labelsDock) { v -> vm.update { it.copy(labelsDock = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_labels_folder), checked = s.labelsFolder) { v -> vm.update { it.copy(labelsFolder = v) } }
    }
    SettingsCard {
        SliderBlock(
            stringResource(R.string.setting_font_size), s.fontScale, LauncherSettings.FONT_SCALE, percentFormat,
            onPreview = { preview = preview.copy(fontScale = it) },
            onCommit = { v -> vm.update { it.copy(fontScale = v) } },
            stepSize = 5,
        )
    }
    ResetSection { vm.update { it.copy(labelsHome = true, labelsDock = false, labelsFolder = true, fontScale = 100) } }
}

@Composable
private fun DockPage(vm: SettingsViewModel, s: LauncherSettings, apps: List<AppInfo>, back: () -> Unit) {
    var preview by remember(s) { mutableStateOf(s) }
    SubpageHeader(stringResource(R.string.settings_section_dock), back)
    GridPreview(apps, vm.icons, preview)
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_dock), checked = s.dockEnabled) { v -> vm.update { it.copy(dockEnabled = v) } }
        if (s.dockEnabled) {
            CardDivider()
            SliderBlock(stringResource(R.string.setting_dock_count), s.dockCount, LauncherSettings.DOCK_COUNT, { "$it" },
                onPreview = { preview = preview.copy(dockCount = it) }, onCommit = { v -> vm.update { it.copy(dockCount = v) } })
        }
    }
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_folder_by_drag), stringResource(R.string.setting_folder_by_drag_summary), s.folderByDrag) { v ->
            vm.update { it.copy(folderByDrag = v) }
        }
    }
    ResetSection { vm.update { it.copy(dockEnabled = true, dockCount = 4, folderByDrag = true) } }
}

@Composable
private fun DesignPage(vm: SettingsViewModel, s: LauncherSettings, back: () -> Unit) {
    SubpageHeader(stringResource(R.string.settings_section_design), back)
    SettingsCard {
        SegmentedRow(
            stringResource(R.string.setting_theme),
            ThemeMode.entries.map { it to stringResource(themeName(it)) },
            s.theme,
        ) { v -> vm.update { it.copy(theme = v) } }
        Text(
            stringResource(R.string.setting_theme_summary),
            style = SummaryStyle.copy(color = LocalPanelColors.current.secondary),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun HapticsPage(vm: SettingsViewModel, s: LauncherSettings, back: () -> Unit) {
    SubpageHeader(stringResource(R.string.settings_section_haptics), back)
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_haptics), stringResource(R.string.setting_haptics_summary), s.haptics) { v -> vm.update { it.copy(haptics = v) } }
        if (s.haptics) {
            CardDivider()
            SegmentedRow(
                stringResource(R.string.setting_haptics_strength),
                HapticStrength.entries.map { it to hapticName(it) },
                s.hapticStrength,
            ) { v -> vm.update { it.copy(hapticStrength = v) } }
        }
    }
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_wobble), checked = s.wobble) { v -> vm.update { it.copy(wobble = v) } }
    }
    ResetSection { vm.update { it.copy(haptics = true, hapticStrength = HapticStrength.MEDIUM, wobble = true) } }
}

@Composable
private fun AccessibilityPage(vm: SettingsViewModel, s: LauncherSettings, back: () -> Unit) {
    SubpageHeader(stringResource(R.string.settings_section_accessibility), back)
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_large_touch), stringResource(R.string.setting_large_touch_summary), s.largeTouch) { v ->
            vm.update { it.copy(largeTouch = v) }
        }
    }
}

@Composable
private fun BackupPage(vm: SettingsViewModel, actions: SettingsActions, back: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    SubpageHeader(stringResource(R.string.settings_section_backup), back)
    SettingsCard {
        ActionRow(stringResource(R.string.backup_export), stringResource(R.string.backup_export_summary), onClick = actions.export)
        CardDivider()
        ActionRow(stringResource(R.string.backup_import), stringResource(R.string.backup_import_summary), onClick = actions.import)
    }
    SettingsCard {
        ActionRow(stringResource(R.string.backup_reset), stringResource(R.string.backup_reset_summary), color = Klar.Danger) { confirm = true }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.backup_reset_confirm_title)) },
            text = { Text(stringResource(R.string.backup_reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    vm.resetLayout()
                }) { Text(stringResource(R.string.backup_reset_confirm), color = Klar.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun AboutPage(versionName: String, licenses: () -> List<Pair<String, String>>, back: () -> Unit) {
    val c = LocalPanelColors.current
    var shown by remember { mutableStateOf<String?>(null) }
    val texts = remember { licenses() }
    SubpageHeader(stringResource(R.string.settings_section_about), back)
    SettingsCard {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(stringResource(R.string.app_name), style = TitleStyle.copy(color = c.content))
            Text(stringResource(R.string.about_version, versionName), style = SummaryStyle.copy(color = c.secondary))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.about_text), style = SummaryStyle.copy(color = c.content, fontSize = 15.sp, lineHeight = 21.sp))
        }
    }
    SettingsCard {
        Text(
            stringResource(R.string.about_licenses),
            style = TitleStyle.copy(color = c.content),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp).semantics { heading() },
        )
        Text(
            stringResource(R.string.about_licenses_summary),
            style = SummaryStyle.copy(color = c.secondary),
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        texts.forEach { (name, _) ->
            CardDivider()
            ActionRow(name) { shown = if (shown == name) null else name }
            if (shown == name) {
                Text(
                    texts.first { it.first == name }.second,
                    style = SummaryStyle.copy(color = c.secondary, fontSize = 12.sp, lineHeight = 16.sp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ResetSection(onReset: () -> Unit) {
    SettingsCard {
        ActionRow(stringResource(R.string.settings_reset_section), onClick = onReset)
    }
}
