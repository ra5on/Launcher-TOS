package de.umstieg.launcher.core

import kotlinx.serialization.json.Json

/** JSON für Speicher (DataStore), Export und Import. */
object LauncherJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        classDiscriminator = "type"
    }

    fun encode(data: LauncherData): String = json.encodeToString(LauncherData.serializer(), data)

    /** Liest Daten und bringt sie in einen gültigen Zustand; wirft bei kaputtem JSON. */
    fun decode(text: String): LauncherData {
        val raw = json.decodeFromString(LauncherData.serializer(), text)
        val settings = raw.settings.sanitized()
        return raw.copy(
            settings = settings,
            // Ohne Seiten wäre der Schnellzugriff leer und nicht bedienbar.
            quickSettings = if (raw.quickSettings.pages.isEmpty()) QsLayout.DEFAULT else raw.quickSettings,
            layout = LayoutOps.normalize(raw.layout, settings.grid, settings.dockCapacity, settings.minPages),
        )
    }
}
