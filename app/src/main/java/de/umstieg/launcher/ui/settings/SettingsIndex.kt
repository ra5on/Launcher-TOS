package de.umstieg.launcher.ui.settings

import androidx.annotation.StringRes
import de.umstieg.launcher.R

/** Seiten der Einstellungen (Name = Schlüssel für Sprünge aus der Suche). */
enum class Section {
    OVERVIEW, GRID, ICONS, LABELS, DOCK, DESIGN, GLASS, GESTURES, BADGES, QUICK, SEARCH, DRAWER, WIDGETS,
    HAPTICS, ACCESSIBILITY, PERMISSIONS, HIDDEN, BACKUP, ABOUT, UPDATES,
}

/** Ein auffindbarer Eintrag der Einstellungen. */
class SettingsEntry(@StringRes val title: Int, val section: Section)

/** Alle Einstellungen für die Suche (in den Einstellungen und in der globalen Suche). */
object SettingsIndex {
    val entries: List<SettingsEntry> = listOf(
        SettingsEntry(R.string.settings_section_grid, Section.GRID),
        SettingsEntry(R.string.setting_columns, Section.GRID),
        SettingsEntry(R.string.setting_rows, Section.GRID),
        SettingsEntry(R.string.setting_side_margin, Section.GRID),
        SettingsEntry(R.string.setting_vertical_padding, Section.GRID),
        SettingsEntry(R.string.setting_page_count, Section.GRID),
        SettingsEntry(R.string.setting_indicator, Section.GRID),
        SettingsEntry(R.string.setting_page_transition, Section.GRID),
        SettingsEntry(R.string.setting_infinite_scroll, Section.GRID),
        SettingsEntry(R.string.setting_auto_add, Section.GRID),
        SettingsEntry(R.string.setting_lock, Section.GRID),
        SettingsEntry(R.string.setting_hide_status_bar, Section.GRID),
        SettingsEntry(R.string.settings_section_icons, Section.ICONS),
        SettingsEntry(R.string.setting_rounding, Section.ICONS),
        SettingsEntry(R.string.setting_icon_size, Section.ICONS),
        SettingsEntry(R.string.setting_shadow, Section.ICONS),
        SettingsEntry(R.string.setting_legacy_bg, Section.ICONS),
        SettingsEntry(R.string.setting_themed_icons, Section.ICONS),
        SettingsEntry(R.string.settings_section_labels, Section.LABELS),
        SettingsEntry(R.string.setting_font_size, Section.LABELS),
        SettingsEntry(R.string.setting_font_weight, Section.LABELS),
        SettingsEntry(R.string.setting_label_lines, Section.LABELS),
        SettingsEntry(R.string.setting_label_shadow, Section.LABELS),
        SettingsEntry(R.string.setting_label_color, Section.LABELS),
        SettingsEntry(R.string.settings_section_dock, Section.DOCK),
        SettingsEntry(R.string.setting_dock_background, Section.DOCK),
        SettingsEntry(R.string.setting_folder_preview, Section.DOCK),
        SettingsEntry(R.string.setting_folder_columns, Section.DOCK),
        SettingsEntry(R.string.settings_section_design, Section.DESIGN),
        SettingsEntry(R.string.setting_accent, Section.DESIGN),
        SettingsEntry(R.string.setting_contrast, Section.DESIGN),
        SettingsEntry(R.string.settings_section_glass, Section.GLASS),
        SettingsEntry(R.string.setting_glass_tint, Section.GLASS),
        SettingsEntry(R.string.setting_blur, Section.GLASS),
        SettingsEntry(R.string.setting_dim, Section.GLASS),
        SettingsEntry(R.string.settings_section_gestures, Section.GESTURES),
        SettingsEntry(R.string.setting_swipe_zone, Section.GESTURES),
        SettingsEntry(R.string.gesture_presets, Section.GESTURES),
        SettingsEntry(R.string.setting_edge_split, Section.GESTURES),
        SettingsEntry(R.string.setting_edge_height, Section.GESTURES),
        SettingsEntry(R.string.settings_section_badges, Section.BADGES),
        SettingsEntry(R.string.setting_own_notifications, Section.BADGES),
        SettingsEntry(R.string.settings_section_quick, Section.QUICK),
        SettingsEntry(R.string.settings_section_search, Section.SEARCH),
        SettingsEntry(R.string.setting_search_fuzziness, Section.SEARCH),
        SettingsEntry(R.string.setting_search_history, Section.SEARCH),
        SettingsEntry(R.string.settings_section_drawer, Section.DRAWER),
        SettingsEntry(R.string.settings_section_hidden, Section.HIDDEN),
        SettingsEntry(R.string.settings_section_widgets, Section.WIDGETS),
        SettingsEntry(R.string.settings_section_haptics, Section.HAPTICS),
        SettingsEntry(R.string.setting_animation_speed, Section.HAPTICS),
        SettingsEntry(R.string.setting_open_animation, Section.HAPTICS),
        SettingsEntry(R.string.settings_section_accessibility, Section.ACCESSIBILITY),
        SettingsEntry(R.string.setting_simple_mode, Section.ACCESSIBILITY),
        SettingsEntry(R.string.setting_reduce_motion, Section.ACCESSIBILITY),
        SettingsEntry(R.string.settings_section_permissions, Section.PERMISSIONS),
        SettingsEntry(R.string.settings_section_backup, Section.BACKUP),
        SettingsEntry(R.string.backup_reset, Section.BACKUP),
        SettingsEntry(R.string.settings_section_about, Section.ABOUT),
        SettingsEntry(R.string.setup_restart, Section.ABOUT),
        SettingsEntry(R.string.settings_section_updates, Section.UPDATES),
        SettingsEntry(R.string.setting_auto_update, Section.UPDATES),
        SettingsEntry(R.string.setting_widget_side, Section.GRID),
        SettingsEntry(R.string.setting_drawer_side, Section.GRID),
    )
}
