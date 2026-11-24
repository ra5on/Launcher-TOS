package de.umstieg.launcher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.umstieg.launcher.R
import de.umstieg.launcher.core.Gesture
import de.umstieg.launcher.core.GestureAction
import de.umstieg.launcher.core.GestureBinding
import de.umstieg.launcher.core.GestureClassifier
import de.umstieg.launcher.core.GesturePreset
import de.umstieg.launcher.core.GesturePresets
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.PageSide
import de.umstieg.launcher.core.ThemeMode
import de.umstieg.launcher.ui.theme.LocalPanelColors
import de.umstieg.launcher.ui.theme.Tos

/** Erklärende Sätze für alle Einstellungen (Titel → Satz), auch in der Ansicht „Alle“. */
@Composable
fun settingInfoMap(): Map<String, String> {
    val pairs = listOf(
        R.string.setting_columns to R.string.setting_columns_info,
        R.string.setting_rows to R.string.setting_rows_info,
        R.string.setting_side_margin to R.string.setting_side_margin_info,
        R.string.setting_vertical_padding to R.string.setting_vertical_padding_info,
        R.string.setting_page_count to R.string.setting_page_count_info,
        R.string.setting_fixed_pages to R.string.setting_fixed_pages_info,
        R.string.setting_indicator to R.string.setting_indicator_info,
        R.string.setting_dots_mode to R.string.setting_dots_mode_info,
        R.string.setting_page_transition to R.string.setting_page_transition_info,
        R.string.setting_rounding to R.string.setting_rounding_info,
        R.string.setting_icon_size to R.string.setting_icon_size_info,
        R.string.setting_shadow to R.string.setting_shadow_info,
        R.string.setting_legacy_bg to R.string.setting_legacy_bg_info,
        R.string.setting_labels_home to R.string.setting_labels_home_info,
        R.string.setting_labels_dock to R.string.setting_labels_dock_info,
        R.string.setting_labels_folder to R.string.setting_labels_folder_info,
        R.string.setting_font_size to R.string.setting_font_size_info,
        R.string.setting_font_weight to R.string.setting_font_weight_info,
        R.string.setting_label_lines to R.string.setting_label_lines_info,
        R.string.setting_label_shadow to R.string.setting_label_shadow_info,
        R.string.setting_label_color to R.string.setting_label_color_info,
        R.string.setting_dock to R.string.setting_dock_info,
        R.string.setting_dock_count to R.string.setting_dock_count_info,
        R.string.setting_dock_background to R.string.setting_dock_background_info,
        R.string.setting_dock_rounding to R.string.setting_dock_rounding_info,
        R.string.setting_folder_preview to R.string.setting_folder_preview_info,
        R.string.setting_folder_columns to R.string.setting_folder_columns_info,
        R.string.setting_folder_background to R.string.setting_folder_background_info,
        R.string.setting_theme to R.string.setting_theme_info,
        R.string.setting_accent to R.string.setting_accent_info,
        R.string.setting_glass_tint to R.string.setting_glass_tint_info,
        R.string.setting_blur to R.string.setting_blur_info,
        R.string.setting_dim to R.string.setting_dim_info,
        R.string.setting_swipe_zone_left to R.string.setting_swipe_zone_left_info,
        R.string.setting_swipe_zone_right to R.string.setting_swipe_zone_right_info,
        R.string.setting_badge_style to R.string.setting_badge_style_info,
        R.string.setting_badge_color to R.string.setting_badge_color_info,
        R.string.setting_group_notifications to R.string.setting_group_notifications_info,
        R.string.setting_search_fuzziness to R.string.setting_search_fuzziness_info,
        R.string.setting_search_keyboard to R.string.setting_search_keyboard_info,
        R.string.setting_search_calculator to R.string.setting_search_calculator_info,
        R.string.setting_search_settings to R.string.setting_search_settings_info,
        R.string.setting_drawer_view to R.string.setting_drawer_view_info,
        R.string.setting_tile_apps to R.string.setting_tile_apps_info,
        R.string.setting_widget_glass to R.string.setting_widget_glass_info,
        R.string.setting_haptics_strength to R.string.setting_haptics_strength_info,
        R.string.setting_wobble to R.string.setting_wobble_info,
        R.string.setting_animation_speed to R.string.setting_animation_speed_info,
        R.string.setting_open_animation to R.string.setting_open_animation_info,
        R.string.setting_reduce_motion to R.string.setting_reduce_motion_info,
        R.string.setting_themed_icons to R.string.setting_themed_icons_summary,
        R.string.setting_edge_height to R.string.setting_edge_height_info,
        R.string.gesture_edge_left to R.string.gesture_edge_left_info,
        R.string.gesture_edge_center to R.string.gesture_edge_center_info,
        R.string.gesture_edge_right to R.string.gesture_edge_right_info,
        R.string.gesture_edge to R.string.gesture_edge_info,
        R.string.gesture_down to R.string.gesture_down_info,
        R.string.gesture_up to R.string.gesture_up_info,
        R.string.gesture_double_tap to R.string.gesture_double_tap_info,
        R.string.gesture_double_tap_bottom to R.string.gesture_double_tap_bottom_info,
        R.string.quick_reset to R.string.quick_reset_info,
        R.string.search_clear_history to R.string.search_clear_history_info,
        R.string.settings_section_hidden to R.string.settings_section_hidden_info,
        R.string.settings_reset_section to R.string.settings_reset_section_info,
    )
    return pairs.associate { (t, i) -> stringResource(t) to stringResource(i) }
}

/** Zeile der einfachen Ansicht: Name, erklärender Satz, Wert rechts. */
@Composable
private fun SimpleRow(title: String, info: String, value: String?, onClick: () -> Unit) {
    val c = LocalPanelColors.current
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TitleStyle.copy(color = c.content))
            Text(info, style = SummaryStyle.copy(color = c.secondary))
        }
        if (value != null) {
            Spacer(Modifier.width(10.dp))
            Text(value, style = ValueStyle.copy(color = Tos.Accent))
        }
        Icon(painterResource(R.drawable.ic_lucide_chevron_right), null, tint = Tos.Accent, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text.uppercase(),
        style = SummaryStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.8.sp, color = LocalPanelColors.current.secondary),
        modifier = Modifier.padding(start = 6.dp, top = 8.dp).semantics { heading() },
    )
}

/** Auswahl in einem Dialog (einfache Ansicht). */
@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onDismiss: () -> Unit, onSelect: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (v, label) ->
                    Row(
                        Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).selectable(selected = v == selected, role = Role.RadioButton) {
                            onSelect(v)
                            onDismiss()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = v == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

private enum class Choice { COLUMNS, SIZE, WIDGETS, DRAWER, EDGE, MIDDLE, THEME }

/** Einstellungen „Einfach“: die wichtigsten Punkte in Alltagssprache, je mit einem erklärenden Satz. */
@Composable
fun SimpleOverview(d: LauncherData, vm: SettingsViewModel, actions: SettingsActions, open: (Section) -> Unit) {
    val s = d.settings
    var choice by remember { mutableStateOf<Choice?>(null) }
    val sideName: @Composable (PageSide) -> String = {
        stringResource(
            when (it) {
                PageSide.LEFT -> R.string.side_left
                PageSide.RIGHT -> R.string.side_right
                PageSide.OFF -> R.string.off
            },
        )
    }
    val sizeName = stringResource(
        when {
            s.iconSize < 92 -> R.string.size_small
            s.iconSize > 110 -> R.string.size_large
            else -> R.string.size_normal
        },
    )
    val cornerName = stringResource(
        when {
            s.iconRounding < 15 -> R.string.rounding_square
            s.iconRounding < 45 -> R.string.rounding_soft
            s.iconRounding < 85 -> R.string.rounding_squircle
            else -> R.string.rounding_circle
        },
    )
    Header(stringResource(R.string.simple_section_home))
    SettingsCard {
        SimpleRow(stringResource(R.string.simple_columns), stringResource(R.string.simple_columns_info), "${s.columns}") { choice = Choice.COLUMNS }
        CardDivider()
        SimpleRow(stringResource(R.string.simple_icon_size), stringResource(R.string.simple_icon_size_info), sizeName) { choice = Choice.SIZE }
        CardDivider()
        SimpleRow(stringResource(R.string.simple_corners), stringResource(R.string.simple_corners_info), cornerName) { open(Section.ICONS) }
        CardDivider()
        SimpleRow(stringResource(R.string.setting_widget_side), stringResource(R.string.setting_widget_page_summary), sideName(s.widgetSide)) { choice = Choice.WIDGETS }
        CardDivider()
        SimpleRow(stringResource(R.string.setting_drawer_side), stringResource(R.string.setting_drawer_summary), sideName(s.drawerSide)) { choice = Choice.DRAWER }
    }
    Header(stringResource(R.string.simple_section_swipe))
    SettingsCard {
        // Führt direkt auf die Gesten-Seite; der Untertitel zeigt die aktuelle Belegung.
        SimpleRow(stringResource(R.string.simple_edge), edgeSummary(s), null) { open(Section.GESTURES) }
        CardDivider()
        SimpleRow(stringResource(R.string.simple_middle), stringResource(R.string.simple_middle_info), actionName(GestureClassifier.binding(s, Gesture.SWIPE_DOWN).action)) { choice = Choice.MIDDLE }
    }
    Header(stringResource(R.string.simple_section_look))
    SettingsCard {
        SimpleRow(stringResource(R.string.simple_theme), stringResource(R.string.simple_theme_info), stringResource(themeName(s.theme))) { choice = Choice.THEME }
        CardDivider()
        SimpleRow(stringResource(R.string.simple_glass), stringResource(R.string.simple_glass_info), stringResource(R.string.percent_value, 100 - s.glassTint)) { open(Section.GLASS) }
        CardDivider()
        SwitchRow(stringResource(R.string.simple_badges), stringResource(R.string.simple_badges_info), s.badgesEnabled) { v -> vm.update { it.copy(badgesEnabled = v) } }
        CardDivider()
        SwitchRow(stringResource(R.string.setting_search_keyboard), stringResource(R.string.simple_keyboard_info), s.searchKeyboard) { v -> vm.update { it.copy(searchKeyboard = v) } }
    }
    Header(stringResource(R.string.simple_section_more))
    SettingsCard {
        SimpleRow(stringResource(R.string.settings_section_updates), stringResource(R.string.simple_updates_info), stringResource(if (s.autoUpdateCheck) R.string.on else R.string.off)) { open(Section.UPDATES) }
        CardDivider()
        SimpleRow(stringResource(R.string.setup_restart), stringResource(R.string.setup_restart_summary), null) { actions.restartSetup() }
    }

    val dismiss = { choice = null }
    when (choice) {
        Choice.COLUMNS -> ChoiceDialog(stringResource(R.string.simple_columns), (3..7).map { it to "$it" }, s.columns, dismiss) { v -> vm.update { it.copy(columns = v) } }
        Choice.SIZE -> ChoiceDialog(
            stringResource(R.string.simple_icon_size),
            listOf(85 to stringResource(R.string.size_small), 100 to stringResource(R.string.size_normal), 120 to stringResource(R.string.size_large)),
            listOf(85, 100, 120).minBy { kotlin.math.abs(it - s.iconSize) }, dismiss,
        ) { v -> vm.update { it.copy(iconSize = v) } }
        Choice.WIDGETS -> ChoiceDialog(stringResource(R.string.setting_widget_side), PageSide.entries.map { it to sideName(it) }, s.widgetSide, dismiss) { v -> vm.update { it.copy(widgetSide = v) } }
        Choice.DRAWER -> ChoiceDialog(stringResource(R.string.setting_drawer_side), PageSide.entries.map { it to sideName(it) }, s.drawerSide, dismiss) { v -> vm.update { it.copy(drawerSide = v) } }
        Choice.EDGE -> ChoiceDialog(stringResource(R.string.simple_edge), GesturePreset.entries.map { it to presetName(it) }, s.gesturePreset, dismiss) { v ->
            if (v == GesturePreset.CUSTOM) open(Section.GESTURES) else vm.update { GesturePresets.apply(it, v) }
        }
        Choice.MIDDLE -> ChoiceDialog(
            stringResource(R.string.simple_middle),
            listOf(GestureAction.SEARCH, GestureAction.SYSTEM_NOTIFICATIONS, GestureAction.NOTIFICATIONS, GestureAction.APP_DRAWER, GestureAction.NOTHING).map { it to actionName(it) },
            GestureClassifier.binding(s, Gesture.SWIPE_DOWN).action, dismiss,
        ) { v -> vm.update { GesturePresets.withGesture(it, Gesture.SWIPE_DOWN, GestureBinding(v)) } }
        Choice.THEME -> ChoiceDialog(stringResource(R.string.simple_theme), ThemeMode.entries.map { it to stringResource(themeName(it)) }, s.theme, dismiss) { v -> vm.update { it.copy(theme = v) } }
        null -> Unit
    }
}
