package de.umstieg.launcher.core

import kotlinx.serialization.Serializable

/** Suchmaschine für „Im Web suchen“. */
@Serializable
enum class SearchEngine(val template: String) {
    GOOGLE("https://www.google.com/search?q=%s"),
    DUCKDUCKGO("https://duckduckgo.com/?q=%s"),
    STARTPAGE("https://www.startpage.com/do/search?query=%s"),
    ECOSIA("https://www.ecosia.org/search?q=%s"),
}

/** Android-Einstellungsseite (Settings-Action) mit Suchbegriffen. */
data class AndroidSettingsPage(val id: String, val action: String, val keywords: List<String>)

/** Reine Logik der erweiterten Suche (getestet). */
object SearchExtras {
    private val TLD = Regex("^[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,24}(:\\d{1,5})?(/\\S*)?$", RegexOption.IGNORE_CASE)

    /** Eingabe als Adresse? „example.com“ → „https://example.com“; mit Schema unverändert; sonst null. */
    fun asUrl(query: String): String? {
        val q = query.trim()
        if (q.isEmpty() || q.contains(' ')) return null
        if (q.startsWith("http://", true) || q.startsWith("https://", true)) return q.takeIf { it.length > 8 && it.substringAfter("://").contains('.') }
        if (q.startsWith("www.", true) || TLD.matches(q)) {
            // Zahlen wie „3.14“ sind keine Adresse.
            if (q.replace(".", "").all { it.isDigit() }) return null
            return "https://$q"
        }
        return null
    }

    /** Such-URL der gewählten Suchmaschine (Begriff URL-kodiert). */
    fun webSearchUrl(engine: SearchEngine, query: String): String =
        engine.template.replace("%s", java.net.URLEncoder.encode(query.trim(), "UTF-8"))

    fun playStoreUrl(query: String): String = "market://search?q=" + java.net.URLEncoder.encode(query.trim(), "UTF-8") + "&c=apps"

    /** Häufige Android-Einstellungsseiten (Titel kommen aus den Ressourcen, Schlüssel = id). */
    val ANDROID_SETTINGS: List<AndroidSettingsPage> = listOf(
        AndroidSettingsPage("wifi", "android.settings.WIFI_SETTINGS", listOf("WLAN", "Wifi", "WLAN-Netz", "Internet")),
        AndroidSettingsPage("bluetooth", "android.settings.BLUETOOTH_SETTINGS", listOf("Bluetooth", "Kopfhörer", "Koppeln")),
        AndroidSettingsPage("battery", "android.intent.action.POWER_USAGE_SUMMARY", listOf("Akku", "Batterie", "Energie", "Energiesparen")),
        AndroidSettingsPage("display", "android.settings.DISPLAY_SETTINGS", listOf("Display", "Bildschirm", "Helligkeit", "Dunkel")),
        AndroidSettingsPage("sound", "android.settings.SOUND_SETTINGS", listOf("Ton", "Lautstärke", "Klingelton", "Vibration")),
        AndroidSettingsPage("notifications", "android.settings.NOTIFICATION_SETTINGS", listOf("Benachrichtigungen", "Mitteilungen")),
        AndroidSettingsPage("apps", "android.settings.MANAGE_APPLICATIONS_SETTINGS", listOf("Apps", "Anwendungen", "Standard-Apps")),
        AndroidSettingsPage("storage", "android.settings.INTERNAL_STORAGE_SETTINGS", listOf("Speicher", "Speicherplatz")),
        AndroidSettingsPage("location", "android.settings.LOCATION_SOURCE_SETTINGS", listOf("Standort", "GPS", "Ortung")),
        AndroidSettingsPage("security", "android.settings.SECURITY_SETTINGS", listOf("Sicherheit", "Displaysperre", "Fingerabdruck", "PIN")),
        AndroidSettingsPage("privacy", "android.settings.PRIVACY_SETTINGS", listOf("Datenschutz", "Berechtigungen")),
        AndroidSettingsPage("accessibility", "android.settings.ACCESSIBILITY_SETTINGS", listOf("Bedienungshilfen", "Barrierefreiheit")),
        AndroidSettingsPage("mobile", "android.settings.DATA_ROAMING_SETTINGS", listOf("Mobile Daten", "Roaming", "SIM")),
        AndroidSettingsPage("hotspot", "android.settings.TETHER_SETTINGS", listOf("Hotspot", "Tethering")),
        AndroidSettingsPage("airplane", "android.settings.AIRPLANE_MODE_SETTINGS", listOf("Flugmodus")),
        AndroidSettingsPage("nfc", "android.settings.NFC_SETTINGS", listOf("NFC", "Kontaktlos", "Bezahlen")),
        AndroidSettingsPage("datetime", "android.settings.DATE_SETTINGS", listOf("Datum", "Uhrzeit", "Zeitzone")),
        AndroidSettingsPage("language", "android.settings.LOCALE_SETTINGS", listOf("Sprache", "Tastatur")),
        AndroidSettingsPage("dnd", "android.settings.ZEN_MODE_SETTINGS", listOf("Nicht stören", "Modi", "Ruhe")),
        AndroidSettingsPage("about", "android.settings.DEVICE_INFO_SETTINGS", listOf("Über das Telefon", "Geräteinfo", "Android-Version")),
        AndroidSettingsPage("update", "android.settings.SYSTEM_UPDATE_SETTINGS", listOf("Systemupdate", "Update")),
        AndroidSettingsPage("wallpaper", "android.intent.action.SET_WALLPAPER", listOf("Hintergrund", "Hintergrundbild")),
        AndroidSettingsPage("vpn", "android.settings.VPN_SETTINGS", listOf("VPN")),
        AndroidSettingsPage("cast", "android.settings.CAST_SETTINGS", listOf("Streamen", "Bildschirm übertragen", "Cast")),
    )

    /** Passende Einstellungsseiten zu einer Eingabe (Präfix eines Suchbegriffs, ohne Groß/Klein). */
    fun matchAndroidSettings(query: String, limit: Int = 4): List<AndroidSettingsPage> {
        val q = TextNormalizer.normalize(query.trim(), expand = false).text.lowercase()
        if (q.length < 2) return emptyList()
        return ANDROID_SETTINGS.filter { page ->
            page.keywords.any { k ->
                val n = TextNormalizer.normalize(k, expand = false).text.lowercase()
                n.startsWith(q) || n.split(' ', '-').any { it.startsWith(q) }
            }
        }.take(limit)
    }
}
