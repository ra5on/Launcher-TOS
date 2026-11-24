package de.umstieg.launcher.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** JSON für Speicher (DataStore), Export und Import. */
object LauncherJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        classDiscriminator = "type"
    }

    fun encode(data: LauncherData): String = json.encodeToString(LauncherData.serializer(), data)

    /**
     * Übernimmt alte Felder (bis 1.0: Schalter „drawerEnabled“/„widgetPageEnabled“) in die neuen
     * Seitenwahlen „Links / Rechts / Aus“, sofern diese noch fehlen.
     */
    private fun migrate(text: String): String {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return text
        val settings = root["settings"]?.jsonObject ?: return text
        val extra = mutableMapOf<String, JsonElement>()
        if ("drawerSide" !in settings) settings["drawerEnabled"]?.jsonPrimitive?.booleanOrNull?.let { extra["drawerSide"] = JsonPrimitive(if (it) "RIGHT" else "OFF") }
        if ("widgetSide" !in settings) settings["widgetPageEnabled"]?.jsonPrimitive?.booleanOrNull?.let { extra["widgetSide"] = JsonPrimitive(if (it) "LEFT" else "OFF") }
        // Daten aus 1.0 (noch ohne Seitenwahl): die damalige Standardansicht „Kategorien“ wird zu den neuen Kacheln.
        if ("drawerSide" !in settings && settings["drawerView"]?.jsonPrimitive?.contentOrNull == "CATEGORIES") extra["drawerView"] = JsonPrimitive("TILES")
        // Ebenso die alte Glas-Tönung 30 → neuer, kräftigerer Standard 50 (besser lesbar auf hellen Fotos).
        if ("drawerSide" !in settings && settings["glassTint"]?.jsonPrimitive?.intOrNull == 30) extra["glassTint"] = JsonPrimitive(50)
        if (extra.isEmpty()) return text
        val newRoot = JsonObject(root + ("settings" to JsonObject(settings + extra)))
        return json.encodeToString(JsonElement.serializer(), newRoot)
    }

    /** Liest Daten und bringt sie in einen gültigen Zustand; wirft bei kaputtem JSON. */
    fun decode(text: String): LauncherData {
        val raw = json.decodeFromString(LauncherData.serializer(), migrate(text))
        val settings = raw.settings.sanitized()
        return raw.copy(
            settings = settings,
            // Ohne Seiten wäre der Schnellzugriff leer und nicht bedienbar.
            quickSettings = if (raw.quickSettings.pages.isEmpty()) QsLayout.DEFAULT else raw.quickSettings,
            layout = LayoutOps.normalize(raw.layout, settings.grid, settings.dockCapacity, settings.minPages),
        )
    }
}
