package de.umstieg.launcher.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Eindeutiger Schlüssel einer startbaren App: Paket + Activity + Seriennummer des Nutzerprofils
 * (Hauptprofil oder Arbeitsprofil). Reines Kotlin, damit Logik und Tests ohne Android laufen.
 */
@Serializable
data class AppKey(
    val packageName: String,
    val activityName: String,
    val userSerial: Long = 0,
) {
    val id: String get() = "$packageName/$activityName#$userSerial"
}

/** Ein Element auf dem Homescreen oder im Dock: eine App oder ein Ordner. */
@Serializable
sealed interface HomeItem {
    val id: String
}

@Serializable
@SerialName("app")
data class AppItem(val app: AppKey) : HomeItem {
    override val id: String get() = "app:" + app.id
}

@Serializable
@SerialName("folder")
data class FolderItem(
    val folderId: String,
    val name: String,
    val apps: List<AppKey>,
) : HomeItem {
    override val id: String get() = "folder:$folderId"
}

/** Element an einer Rasterzelle (x = Spalte, y = Reihe). */
@Serializable
data class Placed(val x: Int, val y: Int, val item: HomeItem)

/** Alle Seiten des Homescreens plus Dock (geordnete Liste ohne Lücken). */
@Serializable
data class HomeLayout(
    val pages: List<List<Placed>> = listOf(emptyList()),
    val dock: List<HomeItem> = emptyList(),
)

/** Größe des Rasters. */
data class GridSpec(val columns: Int, val rows: Int) {
    val cellsPerPage: Int get() = columns * rows
}

@Serializable
enum class IndicatorStyle { DOTS, BAR, NUMBER, NONE }

@Serializable
enum class LegacyBackground { WHITE, DOMINANT }

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class HapticStrength { LIGHT, MEDIUM, STRONG }

/** Einstellungen der Phase 1 mit Standardwerten, die ohne Einrichtung funktionieren. */
@Serializable
data class LauncherSettings(
    val columns: Int = 4,
    val rows: Int = 6,
    val sideMarginDp: Int = 16,
    val indicator: IndicatorStyle = IndicatorStyle.DOTS,
    val autoAdd: Boolean = true,
    val layoutLocked: Boolean = false,
    val iconRounding: Int = 62,
    val iconSize: Int = 100,
    val legacyBackground: LegacyBackground = LegacyBackground.WHITE,
    val labelsHome: Boolean = true,
    val labelsDock: Boolean = false,
    val labelsFolder: Boolean = true,
    val fontScale: Int = 100,
    val dockEnabled: Boolean = true,
    val dockCount: Int = 4,
    val folderByDrag: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val haptics: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val wobble: Boolean = true,
    val largeTouch: Boolean = false,
) {
    val grid: GridSpec get() = GridSpec(columns, rows)

    /** Dock-Kapazität; ausgeschaltetes Dock nimmt nichts auf. */
    val dockCapacity: Int get() = if (dockEnabled) dockCount else 0

    /** Werte in die erlaubten Bereiche zwingen (z. B. nach einem Import). */
    fun sanitized(): LauncherSettings = copy(
        columns = columns.coerceIn(COLUMNS),
        rows = rows.coerceIn(ROWS),
        sideMarginDp = sideMarginDp.coerceIn(SIDE_MARGIN),
        iconRounding = iconRounding.coerceIn(0, 100),
        iconSize = iconSize.coerceIn(ICON_SIZE),
        fontScale = fontScale.coerceIn(FONT_SCALE),
        dockCount = dockCount.coerceIn(DOCK_COUNT),
    )

    companion object {
        val COLUMNS = 3..7
        val ROWS = 4..9
        val SIDE_MARGIN = 0..32
        val ICON_SIZE = 70..130
        val FONT_SCALE = 80..140
        val DOCK_COUNT = 3..6
    }
}

/** Gesamter gespeicherter Zustand; dasselbe JSON dient für Export und Import. */
@Serializable
data class LauncherData(
    val version: Int = CURRENT_VERSION,
    val settings: LauncherSettings = LauncherSettings(),
    val layout: HomeLayout = HomeLayout(),
    /** Alle Apps, die der Launcher schon gesehen hat (auch per „–“ entfernte). */
    val knownApps: Set<AppKey> = emptySet(),
    /** Erstes Layout wurde angelegt. */
    val initialized: Boolean = false,
    val defaultHintDismissed: Boolean = false,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
