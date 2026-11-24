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

/**
 * Android-Widget auf dem Homescreen-Raster; belegt [spanX] × [spanY] Zellen ab seiner Position.
 * [provider] ist der flach geschriebene ComponentName des Widget-Anbieters.
 */
@Serializable
@SerialName("widget")
data class WidgetItem(
    val widgetId: Int,
    val provider: String,
    val spanX: Int = 2,
    val spanY: Int = 2,
) : HomeItem {
    override val id: String get() = "widget:$widgetId"
}

/** Element an einer Rasterzelle (x = Spalte, y = Reihe); Widgets belegen mehrere Zellen ab hier. */
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
enum class LegacyBackground { WHITE, DOMINANT, CUSTOM }

@Serializable
enum class PageCountMode { AUTO, FIXED }

/** Lage einer Zusatzseite (App-Übersicht, Widget-Seite). */
@Serializable
enum class PageSide { LEFT, RIGHT, OFF }

@Serializable
enum class AutoAddTarget { NEXT_FREE, LAST_PAGE }

@Serializable
enum class PageTransition { SLIDE, STACK, FADE, CUBE }

@Serializable
enum class Shadow { NONE, LIGHT, STRONG }

@Serializable
enum class FontWeightSetting { NORMAL, MEDIUM, BOLD }

@Serializable
enum class LabelColor { AUTO, LIGHT, DARK }

@Serializable
enum class DockBackground { GLASS, SOLID, TRANSPARENT }

@Serializable
enum class FolderBackground { GLASS, SOLID }

@Serializable
enum class Accent { MINT, BLUE, VIOLET, CORAL, AMBER, WALLPAPER, CUSTOM }

@Serializable
enum class BadgeStyle { NUMBER, DOT }

@Serializable
enum class BadgeColor { RED, ACCENT, ICON }

@Serializable
enum class SearchFuzziness { STRICT, NORMAL, GENEROUS }

@Serializable
enum class DrawerView { TILES, CATEGORIES, AZ, GRID }

/** Seitenpunkte: nur beim Blättern (Standard), immer oder aus. */
@Serializable
enum class DotsMode { SCROLL, ALWAYS, OFF }

@Serializable
enum class AppOpenAnimation { ZOOM, FADE, NONE }

@Serializable
enum class TriState { SYSTEM, ON, OFF }

/**
 * Gesten auf dem Homescreen (Katalog §8). Der obere Rand ist geteilt (EDGE_LEFT/CENTER/RIGHT) oder
 * ungeteilt (EDGE); „Nach unten wischen“ in der Bildschirmmitte ist davon getrennt (SWIPE_DOWN).
 */
@Serializable
enum class Gesture { EDGE_LEFT, EDGE_CENTER, EDGE_RIGHT, EDGE, SWIPE_DOWN, SWIPE_UP, DOUBLE_TAP, DOUBLE_TAP_BOTTOM }

/** Was eine Geste auslöst (eigene Panels oder die des Systems). */
@Serializable
enum class GestureAction {
    NOTIFICATIONS, QUICK_SETTINGS, SYSTEM_NOTIFICATIONS, SYSTEM_QUICK_SETTINGS,
    SEARCH, APP_DRAWER, OPEN_APP, ASSISTANT, NOTHING,
    /** Bildschirm sperren (nur mit der Bedienungshilfe „Systemweite Wischgesten“). */
    LOCK_SCREEN,
}

/** Gesten-Vorlagen (Schnellwahl); jede einzelne Änderung führt zu [CUSTOM]. */
@Serializable
enum class GesturePreset { TOS, ANDROID, ANDROID_SEARCH, CUSTOM }

@Serializable
data class GestureBinding(val action: GestureAction, val app: AppKey? = null)

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
    // ---------- Phase 2 ----------
    val pageCountMode: PageCountMode = PageCountMode.AUTO,
    val fixedPages: Int = 3,
    val autoAddTarget: AutoAddTarget = AutoAddTarget.NEXT_FREE,
    val topBottomPaddingDp: Int = 8,
    val pageTransition: PageTransition = PageTransition.SLIDE,
    val infiniteScroll: Boolean = false,
    val hideStatusBar: Boolean = false,
    val iconShadow: Shadow = Shadow.LIGHT,
    val legacyColor: Long = 0xFFFFFFFF,
    val themedIcons: Boolean = false,
    val labelWeight: FontWeightSetting = FontWeightSetting.MEDIUM,
    val labelLines: Int = 1,
    val labelShadow: Boolean = true,
    val labelColor: LabelColor = LabelColor.AUTO,
    val dockBackground: DockBackground = DockBackground.GLASS,
    val dockRounding: Int = 60,
    val folderPreview3x3: Boolean = false,
    val folderColumns: Int = 3,
    val folderBackground: FolderBackground = FolderBackground.GLASS,
    val glassTint: Int = 50,
    val blurStrength: Int = 60,
    val dimWallpaper: Int = 0,
    val accent: Accent = Accent.MINT,
    val accentCustom: Long = 0xFF2EC4A6,
    val highContrast: Boolean = false,
    val gestures: Map<Gesture, GestureBinding> = DEFAULT_GESTURES,
    val gesturePreset: GesturePreset = GesturePreset.TOS,
    /** Oberer Rand geteilt (links/Mitte/rechts) oder eine Zone. */
    val edgeSplit: Boolean = true,
    /** Höhe der Randzone ab Unterkante Statusleiste in % der Bildschirmhöhe. */
    val edgeHeightPercent: Int = 25,
    val swipeZoneLeft: Int = 35,
    val swipeZoneRight: Int = 35,
    /** Systemweite Wischgesten über die Bedienungshilfe (Streifen auf der Statusleiste). */
    val systemGestures: Boolean = false,
    /** Auch in anderen Apps (sonst nur, wenn der Homescreen im Vordergrund ist). */
    val systemGesturesEverywhere: Boolean = true,
    /** Ganze Breite eigene Panels (sonst links + rechts, Mitte bleibt Android). */
    val systemGestureFull: Boolean = false,
    val badgesEnabled: Boolean = true,
    val badgeStyle: BadgeStyle = BadgeStyle.NUMBER,
    val badgeColor: BadgeColor = BadgeColor.RED,
    val ownNotificationList: Boolean = true,
    val groupNotifications: Boolean = true,
    val searchFuzziness: SearchFuzziness = SearchFuzziness.NORMAL,
    val searchKeyboard: Boolean = true,
    val searchCalculator: Boolean = true,
    val searchSettings: Boolean = true,
    val searchHistory: Boolean = true,
    /** Abschnitte der Suche einzeln an/aus. Kontakte und Dateien brauchen eine Berechtigung (Standard aus). */
    val searchShortcuts: Boolean = true,
    val searchAndroidSettings: Boolean = true,
    val searchContacts: Boolean = false,
    val searchFiles: Boolean = false,
    val searchWeb: Boolean = true,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    /** App-Übersicht: links, rechts oder aus. */
    val drawerSide: PageSide = PageSide.RIGHT,
    val drawerView: DrawerView = DrawerView.TILES,
    /** Sichtbare Apps je Kachel der App-Übersicht: 4, 6 oder 9. */
    val tileApps: Int = 4,
    val hiddenAppsLocked: Boolean = false,
    /** Widget-Seite: links, rechts oder aus. */
    val widgetSide: PageSide = PageSide.OFF,
    val widgetsOnHome: Boolean = true,
    val widgetGlass: Boolean = true,
    val animationSpeed: Int = 100,
    val appOpenAnimation: AppOpenAnimation = AppOpenAnimation.ZOOM,
    val simpleMode: Boolean = false,
    val reduceMotion: TriState = TriState.SYSTEM,
    // ---------- 1.1 ----------
    /** Wöchentlich auf GitHub nach einer neuen Version suchen. */
    val autoUpdateCheck: Boolean = true,
    val dotsMode: DotsMode = DotsMode.SCROLL,
    /** Einstellungen in der einfachen Ansicht öffnen. */
    val settingsSimple: Boolean = true,
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
        fixedPages = fixedPages.coerceIn(FIXED_PAGES),
        topBottomPaddingDp = topBottomPaddingDp.coerceIn(0, 48),
        labelLines = labelLines.coerceIn(1, 2),
        dockRounding = dockRounding.coerceIn(0, 100),
        folderColumns = folderColumns.coerceIn(FOLDER_COLUMNS),
        glassTint = glassTint.coerceIn(0, 100),
        blurStrength = blurStrength.coerceIn(0, 100),
        dimWallpaper = dimWallpaper.coerceIn(0, 60),
        swipeZoneLeft = swipeZoneLeft.coerceIn(SWIPE_ZONE),
        swipeZoneRight = swipeZoneRight.coerceIn(SWIPE_ZONE),
        edgeHeightPercent = edgeHeightPercent.coerceIn(EDGE_HEIGHT),
        tileApps = if (tileApps in TILE_APPS) tileApps else 4,
        animationSpeed = animationSpeed.coerceIn(0, 200),
        gestures = DEFAULT_GESTURES + gestures,
    )

    val drawerEnabled: Boolean get() = drawerSide != PageSide.OFF
    val widgetPageEnabled: Boolean get() = widgetSide != PageSide.OFF

    /** Mindestanzahl der Seiten (feste Seitenanzahl). */
    val minPages: Int get() = if (pageCountMode == PageCountMode.FIXED) fixedPages else 1

    companion object {
        val COLUMNS = 3..7
        val ROWS = 4..9
        val SIDE_MARGIN = 0..32
        val ICON_SIZE = 70..130
        val FONT_SCALE = 80..140
        val DOCK_COUNT = 3..6
        val FIXED_PAGES = 1..9
        val FOLDER_COLUMNS = 3..5
        val SWIPE_ZONE = 20..50
        val EDGE_HEIGHT = 5..40
        val TILE_APPS = listOf(4, 6, 9)

        /** Vorlage „Launcher TOS“ (Standard). */
        val DEFAULT_GESTURES: Map<Gesture, GestureBinding> = mapOf(
            Gesture.EDGE_LEFT to GestureBinding(GestureAction.NOTIFICATIONS),
            Gesture.EDGE_CENTER to GestureBinding(GestureAction.SEARCH),
            Gesture.EDGE_RIGHT to GestureBinding(GestureAction.QUICK_SETTINGS),
            Gesture.EDGE to GestureBinding(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.SWIPE_DOWN to GestureBinding(GestureAction.SEARCH),
            Gesture.SWIPE_UP to GestureBinding(GestureAction.NOTHING),
            Gesture.DOUBLE_TAP to GestureBinding(GestureAction.NOTHING),
            Gesture.DOUBLE_TAP_BOTTOM to GestureBinding(GestureAction.ASSISTANT),
        )
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
    // ---------- Phase 2 ----------
    /** Widgets der Widget-Seite (links), von oben nach unten. */
    val widgetPage: List<WidgetEntry> = emptyList(),
    /** Ausgeblendete Apps (nicht in App-Übersicht und Suche). */
    val hiddenApps: Set<AppKey> = emptySet(),
    /** Eigene Kategorie je App (Paketname → Kategorie), über „Kategorie ändern“. */
    val categoryOverrides: Map<String, AppCategory> = emptyMap(),
    /** Eigene Namen/Icon-Stil je App (Schlüssel: [AppKey.id]). */
    val appOverrides: Map<String, AppOverride> = emptyMap(),
    /** Pakete ohne Badge. */
    val badgeDisabled: Set<String> = emptySet(),
    /** Zuletzt gesucht (neueste zuerst). */
    val recentQueries: List<String> = emptyList(),
    /** Zuletzt aus der Suche/Übersicht gestartet (neueste zuerst). */
    val recentApps: List<AppKey> = emptyList(),
    val quickSettings: QsLayout = QsLayout.DEFAULT,
    /** Einrichtung wurde einmal gezeigt (bzw. vor 1.1 die Einführung). */
    val onboardingDone: Boolean = false,
    // ---------- 1.1 ----------
    /** Schritte der Einrichtung, die bewusst abgelehnt wurden. */
    val setupDeclined: Set<SetupStep> = emptySet(),
    /** Karte „Einrichtung fortsetzen“ weggewischt. */
    val setupCardDismissed: Boolean = false,
    /** Frage „Updates automatisch prüfen“ beantwortet. */
    val updateChoiceMade: Boolean = false,
    /** Gesten in der Einrichtung gezeigt. */
    val gesturesSeen: Boolean = false,
    val lastUpdateCheck: Long = 0,
    /** Version, deren Update-Hinweis weggewischt wurde. */
    val updateDismissed: String? = null,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** Widget auf der Widget-Seite; Höhe in Rasterzeilen (1 Zeile ≈ 80 dp). */
@Serializable
data class WidgetEntry(val widgetId: Int, val provider: String, val rows: Int = 2)

/** Eigene Anpassung einer App. */
@Serializable
data class AppOverride(val label: String? = null, val monochrome: Boolean = false)

/** Wie Icons gerendert werden (Teil des Cache-Schlüssels). */
data class IconOptions(
    val legacy: LegacyBackground = LegacyBackground.WHITE,
    val legacyColor: Long = 0xFFFFFFFF,
    /** Einfarbige Themen-Icons (ab API 33, sofern die App eine Monochrom-Ebene hat). */
    val themed: Boolean = false,
    val themeDark: Boolean = true,
)

fun LauncherSettings.iconOptions(dark: Boolean = true): IconOptions = IconOptions(legacyBackground, legacyColor, themedIcons, dark)

/**
 * Wirksame Einstellungen: der einfache Modus vergrößert Icons und Schrift, schaltet Beschriftungen an
 * und lässt nur „nach unten wischen = Suche“ als Geste. Das Raster selbst bleibt unverändert.
 */
fun LauncherSettings.effective(): LauncherSettings = if (!simpleMode) {
    this
} else {
    copy(
        iconSize = maxOf(iconSize, 120),
        fontScale = maxOf(fontScale, 120),
        labelsHome = true,
        labelsDock = true,
        labelsFolder = true,
        wobble = false,
        gestures = Gesture.entries.associateWith { GestureBinding(GestureAction.NOTHING) } +
            (Gesture.SWIPE_DOWN to GestureBinding(GestureAction.SEARCH)),
    )
}
