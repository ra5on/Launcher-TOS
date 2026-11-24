package de.umstieg.launcher.core

/** Schritte der Einrichtung beim ersten Start (Reihenfolge = Ablauf). */
enum class SetupStep(val optional: Boolean) {
    WELCOME(false),
    DEFAULT_HOME(false),
    NOTIFICATIONS(false),
    DND(true),
    WRITE_SETTINGS(true),
    UPDATES(true),
    INSTALL_UNKNOWN(true),
    GESTURES(false),
}

/** Was die Einrichtung über den aktuellen Zustand weiß (vom Gerät gelesen). */
data class SetupFacts(
    val isDefault: Boolean,
    val listener: Boolean,
    val dnd: Boolean,
    val writeSettings: Boolean,
    val canInstall: Boolean,
    val updateChoiceMade: Boolean,
    val gesturesSeen: Boolean,
)

/** Reine Logik der Einrichtung: was ist erledigt, was ist offen, welcher Schritt kommt. */
object SetupLogic {
    fun isDone(step: SetupStep, f: SetupFacts): Boolean = when (step) {
        SetupStep.WELCOME -> false
        SetupStep.DEFAULT_HOME -> f.isDefault
        SetupStep.NOTIFICATIONS -> f.listener
        SetupStep.DND -> f.dnd
        SetupStep.WRITE_SETTINGS -> f.writeSettings
        SetupStep.UPDATES -> f.updateChoiceMade
        // Nur nötig, wenn automatische Updates gewünscht sind.
        SetupStep.INSTALL_UNKNOWN -> f.canInstall
        SetupStep.GESTURES -> f.gesturesSeen
    }

    /** Offene Schritte für die Karte „Einrichtung fortsetzen“ (ohne Willkommen und ohne abgelehnte). */
    fun open(f: SetupFacts, declined: Set<SetupStep>): List<SetupStep> =
        SetupStep.entries.filter { it != SetupStep.WELCOME && it !in declined && !isDone(it, f) }

    /** Nächster offener Schritt ab [from] (einschließlich); erledigte werden übersprungen. Null = fertig. */
    fun next(from: Int, f: SetupFacts, declined: Set<SetupStep>, includeWelcome: Boolean): SetupStep? =
        SetupStep.entries.drop(from.coerceAtLeast(0)).firstOrNull { step ->
            if (step == SetupStep.WELCOME) includeWelcome else step !in declined && !isDone(step, f)
        }

    /** Installationsquelle (InstallSourceInfo.getPackageSource, ab Android 13). */
    const val SOURCE_STORE = 2
    const val SOURCE_LOCAL_FILE = 3
    const val SOURCE_DOWNLOADED_FILE = 4

    /**
     * Gelten „Eingeschränkte Einstellungen“? Android 13+ sperrt Benachrichtigungszugriff und
     * Bedienungshilfe für Apps aus einer Datei bzw. einem Download, bis der Nutzer es in der App-Info
     * erlaubt. Deterministisch über die Paketquelle; [restrictedAllowed] ist der AppOps-Status, sofern
     * lesbar (true = bereits freigegeben). adb- und Store-Installationen sind nicht betroffen.
     */
    fun isRestricted(sdk: Int, packageSource: Int?, installer: String?, restrictedAllowed: Boolean?): Boolean {
        if (sdk < 33 || restrictedAllowed == true) return false
        return when (packageSource) {
            SOURCE_LOCAL_FILE, SOURCE_DOWNLOADED_FILE -> true
            SOURCE_STORE -> false
            // Quelle unbekannt: über den Installer entscheiden (Shell/adb und Stores sind frei).
            else -> installer != null && installer != "com.android.shell" && !isStore(installer)
        }
    }

    /** Hilfe vor dem Zugriff zeigen: eingeschränkt und noch nicht erteilt. */
    fun showRestrictedHelp(restricted: Boolean, granted: Boolean): Boolean = restricted && !granted

    /** Bekannte App-Stores (Installationsquelle); sonst gilt die Installation als „selbst geladen“. */
    val STORES = setOf(
        "com.android.vending", "org.fdroid.fdroid", "org.fdroid.basic", "com.aurora.store",
        "com.sec.android.app.samsungapps", "com.huawei.appmarket", "com.amazon.venezia",
    )

    fun isStore(installer: String?): Boolean = installer != null && installer in STORES
}
