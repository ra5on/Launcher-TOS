package de.umstieg.launcher.core

/** Kategorien der App-Übersicht. */
enum class AppCategory { RECENT, SOCIAL, PRODUCTIVITY, IMAGES, VIDEO, AUDIO, GAMES, NEWS, MAPS, ACCESSIBILITY, OTHER }

/** Was die App-Übersicht über eine App wissen muss. */
data class AppMeta(val key: AppKey, val label: String, val category: AppCategory, val firstInstallTime: Long)

object Categories {
    // Werte von ApplicationInfo.CATEGORY_* (stabil seit API 26).
    private const val GAME = 0
    private const val AUDIO = 1
    private const val VIDEO = 2
    private const val IMAGE = 3
    private const val SOCIAL = 4
    private const val NEWS = 5
    private const val MAPS = 6
    private const val PRODUCTIVITY = 7
    private const val ACCESSIBILITY = 8

    /** Schlüsselwörter im Paketnamen, falls die App keine Kategorie angibt. */
    private val keywords: List<Pair<String, AppCategory>> = listOf(
        "game" to AppCategory.GAMES,
        "music" to AppCategory.AUDIO, "radio" to AppCategory.AUDIO, "podcast" to AppCategory.AUDIO, "audio" to AppCategory.AUDIO,
        "video" to AppCategory.VIDEO, "youtube" to AppCategory.VIDEO, "player" to AppCategory.VIDEO,
        "camera" to AppCategory.IMAGES, "gallery" to AppCategory.IMAGES, "photo" to AppCategory.IMAGES,
        "messag" to AppCategory.SOCIAL, "sms" to AppCategory.SOCIAL, "mms" to AppCategory.SOCIAL, "chat" to AppCategory.SOCIAL,
        "dialer" to AppCategory.SOCIAL, "contacts" to AppCategory.SOCIAL, "mail" to AppCategory.SOCIAL,
        "telegram" to AppCategory.SOCIAL, "whatsapp" to AppCategory.SOCIAL, "signal" to AppCategory.SOCIAL,
        "news" to AppCategory.NEWS,
        "maps" to AppCategory.MAPS, "navi" to AppCategory.MAPS, "transit" to AppCategory.MAPS,
        "calendar" to AppCategory.PRODUCTIVITY, "note" to AppCategory.PRODUCTIVITY, "office" to AppCategory.PRODUCTIVITY,
        "docs" to AppCategory.PRODUCTIVITY, "calc" to AppCategory.PRODUCTIVITY, "clock" to AppCategory.PRODUCTIVITY,
        "files" to AppCategory.PRODUCTIVITY, "documents" to AppCategory.PRODUCTIVITY, "todo" to AppCategory.PRODUCTIVITY,
        "tasks" to AppCategory.PRODUCTIVITY,
        "accessib" to AppCategory.ACCESSIBILITY,
    )

    fun fromAndroid(category: Int, packageName: String): AppCategory = when (category) {
        GAME -> AppCategory.GAMES
        AUDIO -> AppCategory.AUDIO
        VIDEO -> AppCategory.VIDEO
        IMAGE -> AppCategory.IMAGES
        SOCIAL -> AppCategory.SOCIAL
        NEWS -> AppCategory.NEWS
        MAPS -> AppCategory.MAPS
        PRODUCTIVITY -> AppCategory.PRODUCTIVITY
        ACCESSIBILITY -> AppCategory.ACCESSIBILITY
        else -> {
            val p = packageName.lowercase()
            keywords.firstOrNull { p.contains(it.first) }?.second ?: AppCategory.OTHER
        }
    }

    /**
     * Gruppen für die Übersicht: zuerst „Zuletzt installiert“ (die [recentCount] neuesten), dann alle
     * Kategorien mit mindestens einer App in fester Reihenfolge, „Sonstige“ zuletzt. Innerhalb: nach Name.
     */
    fun group(apps: List<AppMeta>, recentCount: Int = 5): List<Pair<AppCategory, List<AppMeta>>> {
        val byName = apps.sortedBy { it.label.lowercase() }
        val recent = apps.filter { it.firstInstallTime > 0 }.sortedByDescending { it.firstInstallTime }.take(recentCount)
        val groups = AppCategory.entries.filter { it != AppCategory.RECENT }.mapNotNull { cat ->
            byName.filter { it.category == cat }.takeIf { it.isNotEmpty() }?.let { cat to it }
        }
        return (if (recent.isNotEmpty()) listOf(AppCategory.RECENT to recent) else emptyList()) + groups
    }

    /** Anfangsbuchstabe für die A–Z-Liste (Umlaute zum Grundbuchstaben, Ziffern/Zeichen → „#“). */
    fun indexLetter(label: String): Char {
        val n = TextNormalizer.normalize(label, expand = false).text
        val c = n.firstOrNull()?.uppercaseChar() ?: return '#'
        return if (c in 'A'..'Z') c else '#'
    }
}
