package de.umstieg.launcher.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.umstieg.launcher.R
import de.umstieg.launcher.core.Accent
import de.umstieg.launcher.core.AppOpenAnimation
import de.umstieg.launcher.core.AutoAddTarget
import de.umstieg.launcher.core.BadgeColor
import de.umstieg.launcher.core.BadgeStyle
import de.umstieg.launcher.core.DockBackground
import de.umstieg.launcher.core.DrawerView
import de.umstieg.launcher.core.FolderBackground
import de.umstieg.launcher.core.FontWeightSetting
import de.umstieg.launcher.core.Gesture
import de.umstieg.launcher.core.GestureAction
import de.umstieg.launcher.core.GestureBinding
import de.umstieg.launcher.core.GestureClassifier
import de.umstieg.launcher.core.GesturePreset
import de.umstieg.launcher.core.GesturePresets
import de.umstieg.launcher.core.HapticStrength
import de.umstieg.launcher.core.IndicatorStyle
import de.umstieg.launcher.core.LabelColor
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.core.LegacyBackground
import de.umstieg.launcher.core.PageCountMode
import de.umstieg.launcher.core.PageSide
import de.umstieg.launcher.data.UpdateState
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.core.PageTransition
import de.umstieg.launcher.core.QsLayout
import de.umstieg.launcher.core.SearchFuzziness
import de.umstieg.launcher.core.Shadow
import de.umstieg.launcher.core.ThemeMode
import de.umstieg.launcher.core.TriState
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.NotificationCenter
import de.umstieg.launcher.ui.theme.LocalPanelColors
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.accentColor

/** Was jede Unterseite braucht. */
class PageEnv(
    val vm: SettingsViewModel,
    val d: LauncherData,
    val apps: List<AppInfo>,
    val actions: SettingsActions,
    val back: () -> Unit,
    val open: (Section) -> Unit,
) {
    val s: LauncherSettings get() = d.settings
    fun set(block: (LauncherSettings) -> LauncherSettings) = vm.update(block)
}

internal val Defaults = LauncherSettings()

@Composable
internal fun percentFormat(): (Int) -> String {
    val format = stringResource(R.string.percent_value)
    return remember(format) { { v: Int -> String.format(format, v) } }
}

@Composable
internal fun ResetSection(onReset: () -> Unit) {
    SettingsCard { ActionRow(stringResource(R.string.settings_reset_section), onClick = onReset) }
}

/** „Links / Rechts / Aus“ für eine Zusatzseite. */
@Composable
fun PageSideRow(title: String, summary: String, value: PageSide, onChange: (PageSide) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = TitleStyle.copy(color = LocalPanelColors.current.content))
        Text(summary, style = SummaryStyle.copy(color = LocalPanelColors.current.secondary))
        PageSide.entries.forEach { side ->
            OptionCard(
                icon = null,
                title = stringResource(when (side) { PageSide.LEFT -> R.string.side_left; PageSide.RIGHT -> R.string.side_right; PageSide.OFF -> R.string.off }),
                info = stringResource(when (side) { PageSide.LEFT -> R.string.side_left_info; PageSide.RIGHT -> R.string.side_right_info; PageSide.OFF -> R.string.side_off_info }),
                selected = side == value,
                preview = { SidePreview(side) },
            ) { onChange(side) }
        }
    }
}

/** Mini-Vorschau: drei Seiten nebeneinander, die besondere Seite hervorgehoben. */
@Composable
private fun SidePreview(side: PageSide) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(PageSide.LEFT, null, PageSide.RIGHT).forEach { pos ->
            val special = pos != null && pos == side
            val home = pos == null
            Box(
                Modifier.size(16.dp, 28.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (special) Tos.Accent else if (home) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)),
            )
        }
    }
}

/** Mini-Vorschau der Seitenpunkte über dem Dock. */
@Composable
private fun DotsPreview(mode: de.umstieg.launcher.core.DotsMode) {
    Box(Modifier.size(30.dp, 52.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF1B2A4A)), contentAlignment = Alignment.BottomCenter) {
        Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val a = when (mode) { de.umstieg.launcher.core.DotsMode.ALWAYS -> 1f; de.umstieg.launcher.core.DotsMode.SCROLL -> 0.45f; de.umstieg.launcher.core.DotsMode.OFF -> 0f }
            Box(Modifier.size(7.dp, 3.dp).clip(Tos.PillShape).background(Color.White.copy(alpha = a)))
            Box(Modifier.size(3.dp).clip(Tos.PillShape).background(Color.White.copy(alpha = a * 0.5f)))
        }
        Box(Modifier.padding(bottom = 3.dp, start = 3.dp, end = 3.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f)))
    }
}

// ---------- Homescreen & Raster ----------

@Composable
fun GridPage(p: PageEnv) {
    val s = p.s
    var preview by remember(s) { mutableStateOf(s) }
    SubpageHeader(stringResource(R.string.settings_section_grid), p.back)
    SettingsCard {
        PageSideRow(stringResource(R.string.setting_widget_side), stringResource(R.string.setting_widget_page_summary), s.widgetSide) { v -> p.set { it.copy(widgetSide = v) } }
        CardDivider()
        PageSideRow(stringResource(R.string.setting_drawer_side), stringResource(R.string.setting_drawer_summary), s.drawerSide) { v -> p.set { it.copy(drawerSide = v) } }
    }
    GridPreview(p.apps, p.vm.icons, preview)
    val dpFormat = stringResource(R.string.dp_value)
    SettingsCard {
        SliderBlock(stringResource(R.string.setting_columns), s.columns, LauncherSettings.COLUMNS, { "$it" },
            onPreview = { preview = preview.copy(columns = it) }, onCommit = { v -> p.set { it.copy(columns = v) } })
        CardDivider()
        SliderBlock(stringResource(R.string.setting_rows), s.rows, LauncherSettings.ROWS, { "$it" },
            onPreview = { preview = preview.copy(rows = it) }, onCommit = { v -> p.set { it.copy(rows = v) } })
        CardDivider()
        SliderBlock(stringResource(R.string.setting_side_margin), s.sideMarginDp, LauncherSettings.SIDE_MARGIN, { String.format(dpFormat, it) },
            onPreview = { preview = preview.copy(sideMarginDp = it) }, onCommit = { v -> p.set { it.copy(sideMarginDp = v) } }, stepSize = 4)
        CardDivider()
        SliderBlock(stringResource(R.string.setting_vertical_padding), s.topBottomPaddingDp, 0..48, { String.format(dpFormat, it) },
            onPreview = {}, onCommit = { v -> p.set { it.copy(topBottomPaddingDp = v) } }, stepSize = 4)
    }
    SettingsCard {
        SegmentedRow(
            stringResource(R.string.setting_page_count),
            listOf(PageCountMode.AUTO to stringResource(R.string.page_count_auto), PageCountMode.FIXED to stringResource(R.string.page_count_fixed)),
            s.pageCountMode,
        ) { v -> p.set { it.copy(pageCountMode = v) } }
        if (s.pageCountMode == PageCountMode.FIXED) {
            SliderBlock(stringResource(R.string.setting_fixed_pages), s.fixedPages, LauncherSettings.FIXED_PAGES, { "$it" },
                onPreview = {}, onCommit = { v -> p.set { it.copy(fixedPages = v) } })
        }
        CardDivider()
        SegmentedRow(stringResource(R.string.setting_indicator), IndicatorStyle.entries.map { it to indicatorName(it) }, s.indicator) { v -> p.set { it.copy(indicator = v) } }
        CardDivider()
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.setting_dots_mode), style = TitleStyle.copy(color = LocalPanelColors.current.content))
            listOf(
                de.umstieg.launcher.core.DotsMode.SCROLL to (R.string.dots_scroll to R.string.dots_scroll_info),
                de.umstieg.launcher.core.DotsMode.ALWAYS to (R.string.dots_always to R.string.dots_always_info),
                de.umstieg.launcher.core.DotsMode.OFF to (R.string.off to R.string.dots_off_info),
            ).forEach { (mode, texts) ->
                OptionCard(null, stringResource(texts.first), stringResource(texts.second), s.dotsMode == mode, preview = { DotsPreview(mode) }) {
                    p.set { it.copy(dotsMode = mode) }
                }
            }
        }
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_page_transition),
            listOf(
                PageTransition.SLIDE to stringResource(R.string.transition_slide),
                PageTransition.STACK to stringResource(R.string.transition_stack),
                PageTransition.FADE to stringResource(R.string.transition_fade),
                PageTransition.CUBE to stringResource(R.string.transition_cube),
            ),
            s.pageTransition,
        ) { v -> p.set { it.copy(pageTransition = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_infinite_scroll), stringResource(R.string.setting_infinite_scroll_summary), s.infiniteScroll) { v -> p.set { it.copy(infiniteScroll = v) } }
    }
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_auto_add), stringResource(R.string.setting_auto_add_summary), s.autoAdd) { v -> p.set { it.copy(autoAdd = v) } }
        if (s.autoAdd) {
            SegmentedRow(
                null,
                listOf(AutoAddTarget.NEXT_FREE to stringResource(R.string.auto_add_next_free), AutoAddTarget.LAST_PAGE to stringResource(R.string.auto_add_last_page)),
                s.autoAddTarget,
            ) { v -> p.set { it.copy(autoAddTarget = v) } }
        }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_lock), stringResource(R.string.setting_lock_summary), s.layoutLocked) { v -> p.set { it.copy(layoutLocked = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_hide_status_bar), stringResource(R.string.setting_hide_status_bar_summary), s.hideStatusBar) { v -> p.set { it.copy(hideStatusBar = v) } }
    }
    ResetSection {
        p.set {
            it.copy(
                columns = Defaults.columns, rows = Defaults.rows, sideMarginDp = Defaults.sideMarginDp, topBottomPaddingDp = Defaults.topBottomPaddingDp,
                pageCountMode = Defaults.pageCountMode, fixedPages = Defaults.fixedPages, indicator = Defaults.indicator,
                pageTransition = Defaults.pageTransition, infiniteScroll = Defaults.infiniteScroll, autoAdd = Defaults.autoAdd,
                autoAddTarget = Defaults.autoAddTarget, layoutLocked = Defaults.layoutLocked, hideStatusBar = Defaults.hideStatusBar,
            )
        }
    }
}

// ---------- App-Icons ----------

@Composable
fun IconsPage(p: PageEnv) {
    val s = p.s
    var preview by remember(s) { mutableStateOf(s) }
    val pct = percentFormat()
    SubpageHeader(stringResource(R.string.settings_section_icons), p.back)
    IconRowPreview(p.apps, p.vm.icons, preview.copy(labelsHome = true), caption = true)
    SettingsCard {
        SliderBlock(
            stringResource(R.string.setting_rounding), s.iconRounding, 0..100, pct,
            onPreview = { preview = preview.copy(iconRounding = it) }, onCommit = { v -> p.set { it.copy(iconRounding = v) } },
            start = { SideGlyph(R.drawable.ic_lucide_layout_grid) }, end = { SideGlyph(R.drawable.ic_lucide_circle_check) },
        )
        val quick = listOf(
            0 to stringResource(R.string.rounding_square), 30 to stringResource(R.string.rounding_soft),
            62 to stringResource(R.string.rounding_squircle), 100 to stringResource(R.string.rounding_circle),
        )
        ChipRow(quick, quick.firstOrNull { it.first == preview.iconRounding }?.first) { v -> p.set { it.copy(iconRounding = v) } }
        Spacer(Modifier.height(8.dp))
    }
    SettingsCard {
        SliderBlock(
            stringResource(R.string.setting_icon_size), s.iconSize, LauncherSettings.ICON_SIZE, pct,
            onPreview = { preview = preview.copy(iconSize = it) }, onCommit = { v -> p.set { it.copy(iconSize = v) } }, stepSize = 5,
            start = { SideGlyph(R.drawable.ic_lucide_minus) }, end = { SideGlyph(R.drawable.ic_lucide_plus) },
        )
    }
    SettingsCard {
        SegmentedRow(
            stringResource(R.string.setting_shadow),
            listOf(Shadow.NONE to stringResource(R.string.off), Shadow.LIGHT to stringResource(R.string.haptics_light), Shadow.STRONG to stringResource(R.string.haptics_strong)),
            s.iconShadow,
        ) { v -> p.set { it.copy(iconShadow = v) } }
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_legacy_bg),
            listOf(
                LegacyBackground.WHITE to stringResource(R.string.legacy_bg_white),
                LegacyBackground.DOMINANT to stringResource(R.string.legacy_bg_dominant),
                LegacyBackground.CUSTOM to stringResource(R.string.legacy_bg_custom),
            ),
            s.legacyBackground,
        ) { v -> p.set { it.copy(legacyBackground = v) } }
        if (s.legacyBackground == LegacyBackground.CUSTOM) {
            ColorChooser(Color(s.legacyColor.toInt())) { c -> p.set { it.copy(legacyColor = c.toArgb().toLong() and 0xFFFFFFFFL) } }
        }
        Note(stringResource(R.string.setting_legacy_bg_summary))
        CardDivider()
        SwitchRow(
            stringResource(R.string.setting_themed_icons),
            stringResource(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) R.string.setting_themed_icons_summary else R.string.customize_mono_unsupported),
            s.themedIcons,
        ) { v -> p.set { it.copy(themedIcons = v) } }
    }
    ResetSection {
        p.set {
            it.copy(
                iconRounding = Defaults.iconRounding, iconSize = Defaults.iconSize, iconShadow = Defaults.iconShadow,
                legacyBackground = Defaults.legacyBackground, legacyColor = Defaults.legacyColor, themedIcons = Defaults.themedIcons,
            )
        }
    }
}

@Composable
fun Note(text: String) {
    Text(text, style = SummaryStyle.copy(color = LocalPanelColors.current.secondary), modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
}

/** Farbwahl aus festen Tönen (eigene Werte, keine fremden Paletten). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorChooser(current: Color, onPick: (Color) -> Unit) {
    val colors = listOf(
        0xFFFFFFFF, 0xFFF2F4F7, 0xFF10141C, 0xFF2EC4A6, 0xFF4F8DF7, 0xFF9B7BF2, 0xFFF2785C, 0xFFF2B33D,
        0xFFE0457A, 0xFF3FB86A, 0xFF1D6FD8, 0xFF7C8799,
    ).map { Color(it) }
    val c = LocalPanelColors.current
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { col ->
            val on = col.toArgb() == current.toArgb()
            val desc = "#" + Integer.toHexString(col.toArgb()).uppercase().takeLast(6)
            androidx.compose.foundation.layout.Box(
                Modifier.size(44.dp).background(col, CircleShape).border(if (on) 3.dp else 1.dp, if (on) Tos.Accent else c.glassLine, CircleShape)
                    .clickable(role = Role.RadioButton) { onPick(col) }
                    .semantics {
                        contentDescription = desc
                        selected = on
                    },
            )
        }
    }
}

// ---------- Beschriftung ----------

@Composable
fun LabelsPage(p: PageEnv) {
    val s = p.s
    var preview by remember(s) { mutableStateOf(s) }
    val pct = percentFormat()
    SubpageHeader(stringResource(R.string.settings_section_labels), p.back)
    IconRowPreview(p.apps, p.vm.icons, preview, caption = false)
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_labels_home), checked = s.labelsHome) { v -> p.set { it.copy(labelsHome = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_labels_dock), checked = s.labelsDock) { v -> p.set { it.copy(labelsDock = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_labels_folder), checked = s.labelsFolder) { v -> p.set { it.copy(labelsFolder = v) } }
    }
    SettingsCard {
        SliderBlock(stringResource(R.string.setting_font_size), s.fontScale, LauncherSettings.FONT_SCALE, pct,
            onPreview = { preview = preview.copy(fontScale = it) }, onCommit = { v -> p.set { it.copy(fontScale = v) } }, stepSize = 5)
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_font_weight),
            listOf(FontWeightSetting.NORMAL to stringResource(R.string.weight_normal), FontWeightSetting.MEDIUM to stringResource(R.string.weight_medium), FontWeightSetting.BOLD to stringResource(R.string.weight_bold)),
            s.labelWeight,
        ) { v -> p.set { it.copy(labelWeight = v) } }
        CardDivider()
        SegmentedRow(stringResource(R.string.setting_label_lines), listOf(1 to "1", 2 to "2"), s.labelLines) { v -> p.set { it.copy(labelLines = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_label_shadow), checked = s.labelShadow) { v -> p.set { it.copy(labelShadow = v) } }
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_label_color),
            listOf(LabelColor.AUTO to stringResource(R.string.label_color_auto), LabelColor.LIGHT to stringResource(R.string.theme_light), LabelColor.DARK to stringResource(R.string.theme_dark)),
            s.labelColor,
        ) { v -> p.set { it.copy(labelColor = v) } }
    }
    ResetSection {
        p.set {
            it.copy(
                labelsHome = Defaults.labelsHome, labelsDock = Defaults.labelsDock, labelsFolder = Defaults.labelsFolder, fontScale = Defaults.fontScale,
                labelWeight = Defaults.labelWeight, labelLines = Defaults.labelLines, labelShadow = Defaults.labelShadow, labelColor = Defaults.labelColor,
            )
        }
    }
}

// ---------- Dock & Ordner ----------

@Composable
fun DockPage(p: PageEnv) {
    val s = p.s
    var preview by remember(s) { mutableStateOf(s) }
    val pct = percentFormat()
    SubpageHeader(stringResource(R.string.settings_section_dock), p.back)
    GridPreview(p.apps, p.vm.icons, preview)
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_dock), checked = s.dockEnabled) { v -> p.set { it.copy(dockEnabled = v) } }
        if (s.dockEnabled) {
            CardDivider()
            SliderBlock(stringResource(R.string.setting_dock_count), s.dockCount, LauncherSettings.DOCK_COUNT, { "$it" },
                onPreview = { preview = preview.copy(dockCount = it) }, onCommit = { v -> p.set { it.copy(dockCount = v) } })
            CardDivider()
            SegmentedRow(
                stringResource(R.string.setting_dock_background),
                listOf(DockBackground.GLASS to stringResource(R.string.bg_glass), DockBackground.SOLID to stringResource(R.string.bg_solid), DockBackground.TRANSPARENT to stringResource(R.string.bg_transparent)),
                s.dockBackground,
            ) { v -> p.set { it.copy(dockBackground = v) } }
            CardDivider()
            SliderBlock(stringResource(R.string.setting_dock_rounding), s.dockRounding, 0..100, pct, onPreview = {}, onCommit = { v -> p.set { it.copy(dockRounding = v) } }, stepSize = 10)
        }
    }
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_folder_by_drag), stringResource(R.string.setting_folder_by_drag_summary), s.folderByDrag) { v -> p.set { it.copy(folderByDrag = v) } }
        CardDivider()
        SegmentedRow(stringResource(R.string.setting_folder_preview), listOf(false to "2 × 2", true to "3 × 3"), s.folderPreview3x3) { v -> p.set { it.copy(folderPreview3x3 = v) } }
        CardDivider()
        SliderBlock(stringResource(R.string.setting_folder_columns), s.folderColumns, LauncherSettings.FOLDER_COLUMNS, { "$it" }, onPreview = {}, onCommit = { v -> p.set { it.copy(folderColumns = v) } })
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_folder_background),
            listOf(FolderBackground.GLASS to stringResource(R.string.bg_glass), FolderBackground.SOLID to stringResource(R.string.bg_solid)),
            s.folderBackground,
        ) { v -> p.set { it.copy(folderBackground = v) } }
    }
    ResetSection {
        p.set {
            it.copy(
                dockEnabled = Defaults.dockEnabled, dockCount = Defaults.dockCount, dockBackground = Defaults.dockBackground, dockRounding = Defaults.dockRounding,
                folderByDrag = Defaults.folderByDrag, folderPreview3x3 = Defaults.folderPreview3x3, folderColumns = Defaults.folderColumns,
                folderBackground = Defaults.folderBackground,
            )
        }
    }
}

// ---------- Design & Farben ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignPage(p: PageEnv) {
    val s = p.s
    SubpageHeader(stringResource(R.string.settings_section_design), p.back)
    SettingsCard {
        SegmentedRow(stringResource(R.string.setting_theme), ThemeMode.entries.map { it to stringResource(themeName(it)) }, s.theme) { v -> p.set { it.copy(theme = v) } }
        Note(stringResource(R.string.setting_theme_summary))
    }
    SettingsCard {
        Text(stringResource(R.string.setting_accent), style = TitleStyle.copy(color = LocalPanelColors.current.content), modifier = Modifier.padding(start = 18.dp, top = 12.dp))
        val names = mapOf(
            Accent.MINT to R.string.accent_mint, Accent.BLUE to R.string.accent_blue, Accent.VIOLET to R.string.accent_violet,
            Accent.CORAL to R.string.accent_coral, Accent.AMBER to R.string.accent_amber, Accent.WALLPAPER to R.string.accent_wallpaper,
            Accent.CUSTOM to R.string.accent_custom,
        )
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Accent.entries.forEach { a ->
                val on = a == s.accent
                val col = accentColor(s.copy(accent = a), null)
                Row(
                    Modifier.defaultMinSize(minHeight = 44.dp).background(if (on) col.copy(alpha = 0.25f) else LocalPanelColors.current.chip, Tos.PillShape)
                        .border(if (on) 2.dp else 0.dp, if (on) col else Color.Transparent, Tos.PillShape)
                        .clickable(role = Role.RadioButton) { p.set { it.copy(accent = a) } }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Box(Modifier.size(18.dp).background(col, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(names.getValue(a)), style = SummaryStyle.copy(color = LocalPanelColors.current.content))
                }
            }
        }
        if (s.accent == Accent.CUSTOM) ColorChooser(Color(s.accentCustom.toInt())) { c -> p.set { it.copy(accentCustom = c.toArgb().toLong() and 0xFFFFFFFFL) } }
        if (s.accent == Accent.WALLPAPER) Note(stringResource(R.string.accent_wallpaper_note))
    }
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_contrast), stringResource(R.string.setting_contrast_summary), s.highContrast) { v -> p.set { it.copy(highContrast = v) } }
    }
    ResetSection { p.set { it.copy(theme = Defaults.theme, accent = Defaults.accent, accentCustom = Defaults.accentCustom, highContrast = Defaults.highContrast) } }
}

// ---------- Hintergrund & Glas ----------

/** Mini-Vorschau einer Glasfläche auf hellem Foto. */
@Composable
private fun GlassPreview(tint: Int) {
    Box(
        Modifier.size(44.dp, 44.dp).clip(RoundedCornerShape(10.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFF0C08A), Color(0xFF8AB8F0)))),
        contentAlignment = Alignment.Center,
    ) {
        val fill = Color(0xFF0E121A).copy(alpha = de.umstieg.launcher.core.GlassLogic.fillAlpha(tint, true))
        Box(Modifier.size(32.dp, 18.dp).clip(RoundedCornerShape(6.dp)).background(fill), contentAlignment = Alignment.Center) {
            Text("Aa", style = SummaryStyle.copy(color = Color.White, fontSize = 10.sp))
        }
    }
}

@Composable
fun GlassPage(p: PageEnv) {
    val s = p.s
    val pct = percentFormat()
    SubpageHeader(stringResource(R.string.settings_section_glass), p.back)
    // Schnellwahl als Auswahlkarten; darunter fein einstellbar.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple(R.string.glass_clear, R.string.glass_clear_info, 20 to 40), Triple(R.string.glass_balanced, R.string.glass_balanced_info, 50 to 60), Triple(R.string.glass_strong, R.string.glass_strong_info, 75 to 80))
            .forEach { (t, i, v) ->
                OptionCard(null, stringResource(t), stringResource(i), s.glassTint == v.first && s.blurStrength == v.second, preview = { GlassPreview(v.first) }) {
                    p.set { it.copy(glassTint = v.first, blurStrength = v.second) }
                }
            }
    }
    SettingsCard {
        SliderBlock(
            // Angezeigt wird die Durchsichtigkeit (100 − Tönung): mehr Prozent = mehr Hintergrund sichtbar.
            stringResource(R.string.setting_glass_tint), 100 - s.glassTint, 0..100, pct, onPreview = {}, onCommit = { v -> p.set { it.copy(glassTint = 100 - v) } }, stepSize = 10,
            start = { SideGlyph(R.drawable.ic_lucide_sparkles) }, end = { SideGlyph(R.drawable.ic_lucide_sun_moon) },
        )
        Note(stringResource(R.string.setting_glass_tint_summary))
        CardDivider()
        SliderBlock(stringResource(R.string.setting_blur), s.blurStrength, 0..100, pct, onPreview = {}, onCommit = { v -> p.set { it.copy(blurStrength = v) } }, stepSize = 10)
        Note(stringResource(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.string.setting_blur_summary else R.string.setting_blur_unsupported))
        CardDivider()
        SliderBlock(stringResource(R.string.setting_dim), s.dimWallpaper, 0..60, pct, onPreview = {}, onCommit = { v -> p.set { it.copy(dimWallpaper = v) } }, stepSize = 5)
    }
    ResetSection { p.set { it.copy(glassTint = Defaults.glassTint, blurStrength = Defaults.blurStrength, dimWallpaper = Defaults.dimWallpaper) } }
}

// ---------- Gesten ----------

@Composable
fun gestureName(g: Gesture) = stringResource(
    when (g) {
        Gesture.EDGE_LEFT -> R.string.gesture_edge_left
        Gesture.EDGE_CENTER -> R.string.gesture_edge_center
        Gesture.EDGE_RIGHT -> R.string.gesture_edge_right
        Gesture.EDGE -> R.string.gesture_edge
        Gesture.SWIPE_DOWN -> R.string.gesture_down
        Gesture.SWIPE_UP -> R.string.gesture_up
        Gesture.DOUBLE_TAP -> R.string.gesture_double_tap
        Gesture.DOUBLE_TAP_BOTTOM -> R.string.gesture_double_tap_bottom
    },
)

@Composable
fun actionName(a: GestureAction) = stringResource(
    when (a) {
        GestureAction.NOTIFICATIONS -> R.string.action_notifications
        GestureAction.QUICK_SETTINGS -> R.string.action_quick
        GestureAction.SYSTEM_NOTIFICATIONS -> R.string.action_system_notifications
        GestureAction.SYSTEM_QUICK_SETTINGS -> R.string.action_system_quick
        GestureAction.SEARCH -> R.string.action_search
        GestureAction.APP_DRAWER -> R.string.action_drawer
        GestureAction.OPEN_APP -> R.string.action_open_app
        GestureAction.ASSISTANT -> R.string.action_assistant
        GestureAction.NOTHING -> R.string.action_nothing
        GestureAction.LOCK_SCREEN -> R.string.action_lock_screen
    },
)

@Composable
fun presetName(g: GesturePreset) = stringResource(
    when (g) {
        GesturePreset.TOS -> R.string.preset_tos
        GesturePreset.ANDROID -> R.string.preset_android
        GesturePreset.ANDROID_SEARCH -> R.string.preset_android_search
        GesturePreset.CUSTOM -> R.string.preset_custom
    },
)


internal fun presetSummary(p: GesturePreset) = when (p) {
    GesturePreset.TOS -> R.string.preset_tos_summary
    GesturePreset.ANDROID -> R.string.preset_android_summary
    GesturePreset.ANDROID_SEARCH -> R.string.preset_android_search_summary
    GesturePreset.CUSTOM -> R.string.preset_custom_summary
}

// ---------- Benachrichtigungen & Badges ----------

@Composable
fun BadgesPage(p: PageEnv) {
    val s = p.s
    val context = LocalContext.current
    var granted by remember { mutableStateOf(NotificationCenter.isPermissionGranted(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = NotificationCenter.isPermissionGranted(context) }
    SubpageHeader(stringResource(R.string.settings_section_badges), p.back)
    if (p.vm.showRestrictedHelp(granted)) RestrictedSettingsCard(p)
    SettingsCard {
        Text(stringResource(R.string.listener_title), style = TitleStyle.copy(color = LocalPanelColors.current.content), modifier = Modifier.padding(start = 18.dp, top = 12.dp, end = 18.dp))
        Note(stringResource(R.string.listener_text))
        ActionRow(
            stringResource(if (granted) R.string.listener_granted else R.string.listener_allow),
            value = stringResource(if (granted) R.string.perm_status_granted else R.string.perm_status_missing),
        ) {
            p.actions.openIntent(p.vm.listenerAccessIntent())
        }
    }
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_badges), stringResource(R.string.setting_badges_summary), s.badgesEnabled) { v -> p.set { it.copy(badgesEnabled = v) } }
        CardDivider()
        SegmentedRow(stringResource(R.string.setting_badge_style), listOf(BadgeStyle.NUMBER to stringResource(R.string.badge_number), BadgeStyle.DOT to stringResource(R.string.badge_dot)), s.badgeStyle) { v -> p.set { it.copy(badgeStyle = v) } }
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_badge_color),
            listOf(BadgeColor.RED to stringResource(R.string.badge_red), BadgeColor.ACCENT to stringResource(R.string.badge_accent), BadgeColor.ICON to stringResource(R.string.legacy_bg_dominant)),
            s.badgeColor,
        ) { v -> p.set { it.copy(badgeColor = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_own_notifications), stringResource(R.string.setting_own_notifications_summary), s.ownNotificationList) { v -> p.set { it.copy(ownNotificationList = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_group_notifications), checked = s.groupNotifications) { v -> p.set { it.copy(groupNotifications = v) } }
    }
    SettingsCard {
        Text(stringResource(R.string.badges_per_app), style = TitleStyle.copy(color = LocalPanelColors.current.content), modifier = Modifier.padding(start = 18.dp, top = 12.dp))
        p.apps.distinctBy { it.key.packageName }.sortedBy { it.label.lowercase() }.forEach { app ->
            SwitchRow(app.label, checked = app.key.packageName !in p.d.badgeDisabled) { on -> p.vm.setBadgeDisabled(app.key.packageName, !on) }
        }
    }
    ResetSection {
        p.set {
            it.copy(
                badgesEnabled = Defaults.badgesEnabled, badgeStyle = Defaults.badgeStyle, badgeColor = Defaults.badgeColor,
                ownNotificationList = Defaults.ownNotificationList, groupNotifications = Defaults.groupNotifications,
            )
        }
    }
}

// ---------- Schnellzugriff ----------

@Composable
fun QuickPage(p: PageEnv) {
    SubpageHeader(stringResource(R.string.settings_section_quick), p.back)
    SettingsCard {
        Note(stringResource(R.string.quick_info))
        Note(stringResource(R.string.qs_system_hint))
    }
    SettingsCard {
        ActionRow(stringResource(R.string.perm_dnd_title), stringResource(R.string.perm_dnd_text)) {
            p.actions.openIntent(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        CardDivider()
        ActionRow(stringResource(R.string.perm_write_settings_title), stringResource(R.string.perm_write_settings_text)) {
            p.actions.openIntent(p.vm.writeSettingsIntent())
        }
    }
    SettingsCard {
        ActionRow(stringResource(R.string.quick_reset)) { p.vm.updateData { it.copy(quickSettings = QsLayout.DEFAULT) } }
    }
}

// ---------- Suche ----------

@Composable
fun SearchPage(p: PageEnv) {
    val s = p.s
    val context = androidx.compose.ui.platform.LocalContext.current
    val sources = remember { de.umstieg.launcher.data.SearchSources(context) }
    // Kontakte/Dateien: Berechtigung erst beim Einschalten erfragen; abgelehnt bleibt der Abschnitt aus.
    val contactsPerm = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        p.set { it.copy(searchContacts = ok) }
    }
    val filesPerm = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { res ->
        p.set { it.copy(searchFiles = res.values.any { v -> v }) }
    }
    SubpageHeader(stringResource(R.string.settings_section_search), p.back)
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_search_keyboard), checked = s.searchKeyboard) { v -> p.set { it.copy(searchKeyboard = v) } }
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_search_fuzziness),
            listOf(SearchFuzziness.STRICT to stringResource(R.string.fuzzy_strict), SearchFuzziness.NORMAL to stringResource(R.string.fuzzy_normal), SearchFuzziness.GENEROUS to stringResource(R.string.fuzzy_generous)),
            s.searchFuzziness,
        ) { v -> p.set { it.copy(searchFuzziness = v) } }
    }
    Text(stringResource(R.string.search_sections).uppercase(), style = SummaryStyle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = LocalPanelColors.current.secondary), modifier = Modifier.padding(start = 6.dp, top = 6.dp))
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_search_shortcuts), stringResource(R.string.setting_search_shortcuts_summary), s.searchShortcuts) { v -> p.set { it.copy(searchShortcuts = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_search_settings), checked = s.searchSettings) { v -> p.set { it.copy(searchSettings = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_search_android), stringResource(R.string.setting_search_android_summary), s.searchAndroidSettings) { v -> p.set { it.copy(searchAndroidSettings = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_search_calculator), checked = s.searchCalculator) { v -> p.set { it.copy(searchCalculator = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_search_contacts), stringResource(R.string.setting_search_contacts_summary), s.searchContacts && sources.canReadContacts()) { v ->
            if (v && !sources.canReadContacts()) contactsPerm.launch(android.Manifest.permission.READ_CONTACTS) else p.set { it.copy(searchContacts = v) }
        }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_search_files), stringResource(R.string.setting_search_files_summary), s.searchFiles && sources.canReadFiles()) { v ->
            if (v && !sources.canReadFiles()) filesPerm.launch(sources.filePermissions()) else p.set { it.copy(searchFiles = v) }
        }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_search_web), stringResource(R.string.setting_search_web_summary), s.searchWeb) { v -> p.set { it.copy(searchWeb = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_search_history), stringResource(R.string.setting_search_history_summary), s.searchHistory) { v -> p.set { it.copy(searchHistory = v) } }
    }
    if (s.searchWeb) {
        Text(stringResource(R.string.setting_search_engine).uppercase(), style = SummaryStyle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = LocalPanelColors.current.secondary), modifier = Modifier.padding(start = 6.dp, top = 6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            de.umstieg.launcher.core.SearchEngine.entries.forEach { e ->
                OptionCard(
                    icon = R.drawable.ic_lucide_globe,
                    title = de.umstieg.launcher.ui.drawer.engineName(e),
                    info = stringResource(
                        when (e) {
                            de.umstieg.launcher.core.SearchEngine.GOOGLE -> R.string.engine_google_info
                            de.umstieg.launcher.core.SearchEngine.DUCKDUCKGO -> R.string.engine_ddg_info
                            de.umstieg.launcher.core.SearchEngine.STARTPAGE -> R.string.engine_startpage_info
                            de.umstieg.launcher.core.SearchEngine.ECOSIA -> R.string.engine_ecosia_info
                        },
                    ),
                    selected = s.searchEngine == e,
                ) { p.set { it.copy(searchEngine = e) } }
            }
        }
    }
    SettingsCard { ActionRow(stringResource(R.string.search_clear_history)) { p.vm.updateData { it.copy(recentQueries = emptyList(), recentApps = emptyList()) } } }
    ResetSection {
        p.set {
            it.copy(
                searchFuzziness = Defaults.searchFuzziness, searchKeyboard = Defaults.searchKeyboard, searchCalculator = Defaults.searchCalculator,
                searchSettings = Defaults.searchSettings, searchHistory = Defaults.searchHistory, searchShortcuts = Defaults.searchShortcuts,
                searchAndroidSettings = Defaults.searchAndroidSettings, searchContacts = false, searchFiles = false, searchWeb = Defaults.searchWeb,
                searchEngine = Defaults.searchEngine,
            )
        }
    }
}

// ---------- App-Übersicht & versteckte Apps ----------

@Composable
fun DrawerSettingsPage(p: PageEnv) {
    val s = p.s
    SubpageHeader(stringResource(R.string.settings_section_drawer), p.back)
    SettingsCard {
        PageSideRow(stringResource(R.string.setting_drawer_side), stringResource(R.string.setting_drawer_summary), s.drawerSide) { v -> p.set { it.copy(drawerSide = v) } }
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_drawer_view),
            listOf(DrawerView.TILES to stringResource(R.string.drawer_view_tiles), DrawerView.CATEGORIES to stringResource(R.string.drawer_view_categories), DrawerView.AZ to stringResource(R.string.drawer_view_az), DrawerView.GRID to stringResource(R.string.drawer_view_grid)),
            s.drawerView,
        ) { v -> p.set { it.copy(drawerView = v) } }
        if (s.drawerView == DrawerView.TILES) {
            CardDivider()
            SegmentedRow(stringResource(R.string.setting_tile_apps), LauncherSettings.TILE_APPS.map { it to "$it" }, s.tileApps) { v -> p.set { it.copy(tileApps = v) } }
        }
    }
    SettingsCard {
        ActionRow(stringResource(R.string.settings_section_hidden), value = "${p.d.hiddenApps.size}") {
            if (s.hiddenAppsLocked) p.actions.authenticate { p.open(Section.HIDDEN) } else p.open(Section.HIDDEN)
        }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_hidden_lock), stringResource(R.string.setting_hidden_lock_summary), s.hiddenAppsLocked) { v ->
            p.actions.authenticate { p.set { it.copy(hiddenAppsLocked = v) } }
        }
    }
    ResetSection { p.set { it.copy(drawerSide = Defaults.drawerSide, drawerView = Defaults.drawerView, tileApps = Defaults.tileApps) } }
}

@Composable
fun HiddenAppsPage(p: PageEnv) {
    SubpageHeader(stringResource(R.string.settings_section_hidden)) { p.open(Section.DRAWER) }
    SettingsCard {
        Note(stringResource(R.string.hidden_info))
        p.apps.sortedBy { it.label.lowercase() }.forEach { app ->
            SwitchRow(app.label, checked = app.key in p.d.hiddenApps) { on -> p.vm.setHidden(app.key, on) }
        }
    }
}

// ---------- Widgets ----------

@Composable
fun WidgetsPage(p: PageEnv) {
    val s = p.s
    SubpageHeader(stringResource(R.string.settings_section_widgets), p.back)
    SettingsCard {
        PageSideRow(stringResource(R.string.setting_widget_side), stringResource(R.string.setting_widget_page_summary), s.widgetSide) { v -> p.set { it.copy(widgetSide = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_widgets_home), stringResource(R.string.setting_widgets_home_summary), s.widgetsOnHome) { v -> p.set { it.copy(widgetsOnHome = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_widget_glass), checked = s.widgetGlass) { v -> p.set { it.copy(widgetGlass = v) } }
    }
    ResetSection { p.set { it.copy(widgetSide = Defaults.widgetSide, widgetsOnHome = Defaults.widgetsOnHome, widgetGlass = Defaults.widgetGlass) } }
}

// ---------- Haptik & Animationen ----------

@Composable
fun HapticsPage(p: PageEnv) {
    val s = p.s
    SubpageHeader(stringResource(R.string.settings_section_haptics), p.back)
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_haptics), stringResource(R.string.setting_haptics_summary), s.haptics) { v -> p.set { it.copy(haptics = v) } }
        if (s.haptics) {
            CardDivider()
            SegmentedRow(stringResource(R.string.setting_haptics_strength), HapticStrength.entries.map { it to hapticName(it) }, s.hapticStrength) { v -> p.set { it.copy(hapticStrength = v) } }
        }
    }
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_wobble), checked = s.wobble) { v -> p.set { it.copy(wobble = v) } }
        CardDivider()
        val off = stringResource(R.string.off)
        SliderBlock(
            stringResource(R.string.setting_animation_speed), s.animationSpeed, 0..200,
            { if (it == 0) off else String.format(java.util.Locale.GERMAN, "%.1f×", it / 100f) },
            onPreview = {}, onCommit = { v -> p.set { it.copy(animationSpeed = v) } }, stepSize = 25,
        )
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_open_animation),
            listOf(AppOpenAnimation.ZOOM to stringResource(R.string.open_zoom), AppOpenAnimation.FADE to stringResource(R.string.transition_fade), AppOpenAnimation.NONE to stringResource(R.string.off)),
            s.appOpenAnimation,
        ) { v -> p.set { it.copy(appOpenAnimation = v) } }
    }
    ResetSection {
        p.set { it.copy(haptics = Defaults.haptics, hapticStrength = Defaults.hapticStrength, wobble = Defaults.wobble, animationSpeed = Defaults.animationSpeed, appOpenAnimation = Defaults.appOpenAnimation) }
    }
}

// ---------- Barrierefreiheit ----------

@Composable
fun AccessibilityPage(p: PageEnv) {
    val s = p.s
    SubpageHeader(stringResource(R.string.settings_section_accessibility), p.back)
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_large_touch), stringResource(R.string.setting_large_touch_summary), s.largeTouch) { v -> p.set { it.copy(largeTouch = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_simple_mode), stringResource(R.string.setting_simple_mode_summary), s.simpleMode) { v -> p.set { it.copy(simpleMode = v) } }
        CardDivider()
        SegmentedRow(
            stringResource(R.string.setting_reduce_motion),
            listOf(TriState.SYSTEM to stringResource(R.string.theme_system), TriState.ON to stringResource(R.string.on), TriState.OFF to stringResource(R.string.off)),
            s.reduceMotion,
        ) { v -> p.set { it.copy(reduceMotion = v) } }
    }
    ResetSection { p.set { it.copy(largeTouch = Defaults.largeTouch, simpleMode = Defaults.simpleMode, reduceMotion = Defaults.reduceMotion) } }
}

// ---------- Berechtigungen ----------

@Composable
fun PermissionsPage(p: PageEnv) {
    val context = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val status = remember(tick) { p.vm.permissionStatus() }
    SubpageHeader(stringResource(R.string.settings_section_permissions), p.back)
    SettingsCard { Note(stringResource(R.string.perm_intro)) }
    if (p.vm.showRestrictedHelp(NotificationCenter.isPermissionGranted(context))) RestrictedSettingsCard(p)
    SettingsCard {
        PermissionRow(R.string.perm_listener_title, R.string.perm_listener_text, NotificationCenter.isPermissionGranted(context)) {
            p.actions.openIntent(p.vm.listenerAccessIntent())
        }
        CardDivider()
        PermissionRow(R.string.perm_dnd_title, R.string.perm_dnd_text, status.policy) {
            p.actions.openIntent(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        CardDivider()
        PermissionRow(R.string.perm_write_settings_title, R.string.perm_write_settings_text, status.writeSettings) { p.actions.openIntent(p.vm.writeSettingsIntent()) }
        CardDivider()
        PermissionRow(R.string.perm_statusbar_title, R.string.perm_statusbar_text, true, null)
        CardDivider()
        PermissionRow(R.string.perm_network_title, R.string.perm_network_text, true, null)
        CardDivider()
        PermissionRow(R.string.perm_biometric_title, R.string.perm_biometric_text, true, null)
        CardDivider()
        PermissionRow(R.string.perm_delete_title, R.string.perm_delete_text, true, null)
        CardDivider()
        PermissionRow(R.string.perm_internet_title, R.string.perm_internet_text, true, null)
        CardDivider()
        PermissionRow(R.string.perm_install_title, R.string.perm_install_text, p.vm.canInstall()) { p.actions.openIntent(p.vm.allowInstallIntent()) }
    }
    SettingsCard { Note(stringResource(R.string.perm_internet_only_github)) }
}

@Composable
private fun PermissionRow(title: Int, text: Int, granted: Boolean, onClick: (() -> Unit)?) {
    val c = LocalPanelColors.current
    Column(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier).padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(title), style = TitleStyle.copy(color = c.content), modifier = Modifier.weight(1f))
            Text(
                stringResource(if (onClick == null) R.string.perm_status_auto else if (granted) R.string.perm_status_granted else R.string.perm_status_missing),
                style = SummaryStyle.copy(color = if (granted) Tos.Accent else c.secondary),
            )
        }
        Text(stringResource(text), style = SummaryStyle.copy(color = c.secondary))
    }
}

// ---------- Updates ----------

@Composable
fun UpdatesPage(p: PageEnv) {
    val c = LocalPanelColors.current
    val state by p.vm.updateState.collectAsStateWithLifecycle()
    var canInstall by remember { mutableStateOf(p.vm.canInstall()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canInstall = p.vm.canInstall() }
    SubpageHeader(stringResource(R.string.settings_section_updates), p.back)
    SettingsCard {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(stringResource(R.string.updates_installed, p.vm.installedVersion), style = TitleStyle.copy(color = c.content))
            Spacer(Modifier.height(6.dp))
            val status = when (val st = state) {
                UpdateState.Idle -> null
                UpdateState.Checking -> stringResource(R.string.updates_checking)
                UpdateState.UpToDate -> stringResource(R.string.updates_up_to_date)
                is UpdateState.Available -> stringResource(R.string.updates_available, st.release.version.name)
                is UpdateState.Downloading -> stringResource(R.string.updates_downloading, if (st.total > 0) (st.done * 100 / st.total).toInt() else 0)
                is UpdateState.Verifying -> stringResource(R.string.updates_verifying)
                is UpdateState.Installing -> stringResource(R.string.updates_installing)
                is UpdateState.NeedsPermission -> stringResource(R.string.updates_needs_permission)
                is UpdateState.Failed -> stringResource(
                    when (st.reason) {
                        UpdateState.Reason.NETWORK -> R.string.updates_failed_network
                        UpdateState.Reason.CHECKSUM -> R.string.updates_failed_checksum
                        UpdateState.Reason.SIGNATURE -> R.string.updates_failed_signature
                        UpdateState.Reason.PACKAGE -> R.string.updates_failed_package
                        UpdateState.Reason.INSTALL -> R.string.updates_failed_install
                    },
                ) + (
                    // Prüfsumme/Signatur sind schon klar benannt; bei Netz/Paket/Installation hilft der Grund.
                    st.detail?.takeIf { st.reason != UpdateState.Reason.CHECKSUM && st.reason != UpdateState.Reason.SIGNATURE }?.let { " ($it)" } ?: ""
                )
            }
            if (status != null) Text(status, style = SummaryStyle.copy(color = c.secondary))
            val progress = (state as? UpdateState.Downloading)?.let { if (it.total > 0) it.done.toFloat() / it.total else 0f }
            if (progress != null) {
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Tos.Accent)
            }
        }
        CardDivider()
        ActionRow(stringResource(R.string.updates_check_now), stringResource(R.string.updates_check_now_summary)) { p.vm.checkForUpdate() }
        val release = when (val st = state) {
            is UpdateState.Available -> st.release
            is UpdateState.NeedsPermission -> st.release
            is UpdateState.Failed -> st.release
            else -> null
        }
        if (release != null) {
            CardDivider()
            ActionRow(stringResource(R.string.updates_install, release.version.name), stringResource(R.string.updates_install_summary), color = Tos.Accent) { p.vm.install(release) }
        }
        if (release != null && !canInstall) {
            CardDivider()
            ActionRow(stringResource(R.string.updates_allow_install), stringResource(R.string.updates_allow_install_summary)) { p.actions.openIntent(p.vm.allowInstallIntent()) }
        }
    }
    val notes = (state as? UpdateState.Available)?.release?.notes ?: (state as? UpdateState.NeedsPermission)?.release?.notes
    if (!notes.isNullOrBlank()) {
        SettingsCard {
            Text(stringResource(R.string.updates_changelog), style = TitleStyle.copy(color = c.content), modifier = Modifier.padding(start = 18.dp, top = 12.dp))
            Text(notes, style = SummaryStyle.copy(color = c.content), modifier = Modifier.padding(18.dp))
        }
    }
    SettingsCard {
        SwitchRow(stringResource(R.string.setting_auto_update), stringResource(R.string.setting_auto_update_summary), p.s.autoUpdateCheck) { v ->
            p.vm.updateData { it.copy(updateChoiceMade = true, settings = it.settings.copy(autoUpdateCheck = v)) }
        }
        Note(stringResource(R.string.updates_privacy))
    }
}

/** Hilfe zu „Eingeschränkten Einstellungen“ (Android 13+, Installation nicht aus einem Store). */
@Composable
fun RestrictedSettingsCard(p: PageEnv) {
    SettingsCard {
        Text(stringResource(R.string.restricted_title), style = TitleStyle.copy(color = LocalPanelColors.current.content), modifier = Modifier.padding(start = 18.dp, top = 12.dp, end = 18.dp))
        Note(stringResource(R.string.restricted_text))
        ActionRow(stringResource(R.string.setup_open_app_info), stringResource(R.string.restricted_after)) {
            p.vm.markAppInfoOpened()
            p.actions.openIntent(p.vm.appDetailsIntent())
        }
    }
}

// ---------- Sichern ----------

@Composable
fun BackupPage(p: PageEnv) {
    var confirm by remember { mutableStateOf(false) }
    SubpageHeader(stringResource(R.string.settings_section_backup), p.back)
    SettingsCard {
        ActionRow(stringResource(R.string.backup_export), stringResource(R.string.backup_export_summary), onClick = p.actions.export)
        CardDivider()
        ActionRow(stringResource(R.string.backup_import), stringResource(R.string.backup_import_summary), onClick = p.actions.import)
    }
    SettingsCard { ActionRow(stringResource(R.string.backup_reset), stringResource(R.string.backup_reset_summary), color = Tos.Danger) { confirm = true } }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.backup_reset_confirm_title)) },
            text = { Text(stringResource(R.string.backup_reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    p.vm.resetLayout()
                }) { Text(stringResource(R.string.backup_reset_confirm), color = Tos.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

// ---------- Über ----------

@Composable
fun AboutPage(p: PageEnv, versionName: String, licenses: () -> List<Pair<String, String>>) {
    val c = LocalPanelColors.current
    var shown by remember { mutableStateOf<String?>(null) }
    val texts = remember { licenses() }
    SubpageHeader(stringResource(R.string.settings_section_about), p.back)
    SettingsCard {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(stringResource(R.string.app_name), style = TitleStyle.copy(color = c.content))
            Text(stringResource(R.string.about_version, versionName), style = SummaryStyle.copy(color = c.secondary))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.about_text), style = SummaryStyle.copy(color = c.content))
        }
    }
    SettingsCard { ActionRow(stringResource(R.string.setup_restart), stringResource(R.string.setup_restart_summary)) { p.actions.restartSetup() } }
    SettingsCard {
        Text(stringResource(R.string.about_licenses), style = TitleStyle.copy(color = c.content), modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
        Note(stringResource(R.string.about_licenses_summary))
        texts.forEach { (name, text) ->
            CardDivider()
            ActionRow(name) { shown = if (shown == name) null else name }
            if (shown == name) {
                Text(text, style = SummaryStyle.copy(color = c.secondary, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)), modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            }
        }
    }
}
