package de.umstieg.launcher.core

/** Kategorien der App-Übersicht. */
/** Kategorien der App-Übersicht; die Reihenfolge ist die Anzeige-Reihenfolge. */
enum class AppCategory {
    RECENT, SOCIAL, BROWSER, AI, PRODUCTIVITY, TOOLS, FINANCE, SHOPPING, IMAGES, VIDEO, AUDIO, GAMES, NEWS, MAPS,
    TRAVEL, SMART_HOME, HEALTH, ACCESSIBILITY, OTHER,
}

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

    /**
     * Eingebaute Zuordnung verbreiteter Apps nach Paketname (Präfix), Vorrang vor der groben Angabe der
     * App selbst – „Produktivität 44“ wird so feiner.
     */
    private val known: List<Pair<String, AppCategory>> = listOf(
        // Finanzen
        "com.starfinanz." to AppCategory.FINANCE, "de.sparkasse" to AppCategory.FINANCE, "com.bitpanda" to AppCategory.FINANCE,
        "com.paypal" to AppCategory.FINANCE, "de.number26" to AppCategory.FINANCE, "de.dkb" to AppCategory.FINANCE,
        "com.ing.mobile" to AppCategory.FINANCE, "de.ingdiba" to AppCategory.FINANCE, "de.comdirect" to AppCategory.FINANCE,
        "de.consorsbank" to AppCategory.FINANCE, "de.postbank" to AppCategory.FINANCE, "com.db.pwcc" to AppCategory.FINANCE,
        "de.commerzbanking" to AppCategory.FINANCE, "de.fiduciagad" to AppCategory.FINANCE, "de.traderepublic" to AppCategory.FINANCE,
        "com.revolut" to AppCategory.FINANCE, "com.klarna" to AppCategory.FINANCE, "com.coinbase" to AppCategory.FINANCE,
        "com.scalable" to AppCategory.FINANCE, "de.smartbroker" to AppCategory.FINANCE, "com.google.android.apps.walletnfcrel" to AppCategory.FINANCE,
        "de.finanzguru" to AppCategory.FINANCE, "com.outbank" to AppCategory.FINANCE, "de.lbbw" to AppCategory.FINANCE,
        // Browser
        "com.android.chrome" to AppCategory.BROWSER, "org.mozilla." to AppCategory.BROWSER, "com.brave.browser" to AppCategory.BROWSER,
        "com.duckduckgo.mobile" to AppCategory.BROWSER, "com.opera." to AppCategory.BROWSER, "com.microsoft.emmx" to AppCategory.BROWSER,
        "com.vivaldi.browser" to AppCategory.BROWSER, "com.sec.android.app.sbrowser" to AppCategory.BROWSER, "com.kiwibrowser" to AppCategory.BROWSER,
        "org.torproject" to AppCategory.BROWSER, "com.ecosia" to AppCategory.BROWSER,
        // KI-Assistenten
        "com.openai." to AppCategory.AI, "com.anthropic." to AppCategory.AI, "com.google.android.apps.bard" to AppCategory.AI,
        "com.microsoft.copilot" to AppCategory.AI, "ai.perplexity" to AppCategory.AI, "com.deepseek" to AppCategory.AI,
        "ai.x.grok" to AppCategory.AI, "com.mistral" to AppCategory.AI, "ai.mistral" to AppCategory.AI, "com.deepl" to AppCategory.AI,
        // Kommunikation
        "com.whatsapp" to AppCategory.SOCIAL, "org.telegram" to AppCategory.SOCIAL, "org.thoughtcrime.securesms" to AppCategory.SOCIAL,
        "com.google.android.apps.messaging" to AppCategory.SOCIAL, "com.google.android.dialer" to AppCategory.SOCIAL,
        "com.google.android.contacts" to AppCategory.SOCIAL, "com.google.android.gm" to AppCategory.SOCIAL, "ch.threema" to AppCategory.SOCIAL,
        "com.discord" to AppCategory.SOCIAL, "com.microsoft.teams" to AppCategory.SOCIAL, "us.zoom" to AppCategory.SOCIAL,
        "com.skype" to AppCategory.SOCIAL, "de.gmx" to AppCategory.SOCIAL, "de.web.mobile" to AppCategory.SOCIAL,
        "com.microsoft.office.outlook" to AppCategory.SOCIAL, "com.instagram" to AppCategory.SOCIAL, "com.facebook" to AppCategory.SOCIAL,
        "com.twitter" to AppCategory.SOCIAL, "com.zhiliaoapp.musically" to AppCategory.SOCIAL, "com.reddit" to AppCategory.SOCIAL,
        "com.snapchat" to AppCategory.SOCIAL, "org.joinmastodon" to AppCategory.SOCIAL, "com.linkedin" to AppCategory.SOCIAL,
        // Google- und System-Werkzeuge
        "com.google.android.calculator" to AppCategory.TOOLS, "com.google.android.deskclock" to AppCategory.TOOLS,
        "com.google.android.apps.nbu.files" to AppCategory.TOOLS, "com.google.android.documentsui" to AppCategory.TOOLS,
        "com.android.settings" to AppCategory.TOOLS, "com.google.android.apps.translate" to AppCategory.TOOLS,
        "com.google.android.apps.recorder" to AppCategory.TOOLS, "com.google.android.googlequicksearchbox" to AppCategory.TOOLS,
        "com.google.android.apps.authenticator" to AppCategory.TOOLS, "com.google.android.apps.safetyhub" to AppCategory.TOOLS,
        "com.google.android.apps.pixel." to AppCategory.TOOLS, "com.google.android.apps.wellbeing" to AppCategory.TOOLS,
        "com.google.android.apps.subscriptions.red" to AppCategory.TOOLS, "com.google.android.apps.tips" to AppCategory.TOOLS,
        "com.google.android.apps.adm" to AppCategory.TOOLS, "com.google.android.apps.turbo" to AppCategory.TOOLS,
        "com.google.android.apps.security" to AppCategory.TOOLS, "com.google.ar.lens" to AppCategory.TOOLS,
        "com.android.vending" to AppCategory.TOOLS, "org.fdroid" to AppCategory.TOOLS, "com.aurora.store" to AppCategory.TOOLS,
        "com.bambulab" to AppCategory.TOOLS, "com.x8bit.bitwarden" to AppCategory.TOOLS, "com.agilebits" to AppCategory.TOOLS,
        "com.google.android.keep" to AppCategory.PRODUCTIVITY, "com.google.android.calendar" to AppCategory.PRODUCTIVITY,
        "com.google.android.apps.docs" to AppCategory.PRODUCTIVITY, "com.microsoft.office" to AppCategory.PRODUCTIVITY,
        "com.notion" to AppCategory.PRODUCTIVITY, "com.todoist" to AppCategory.PRODUCTIVITY, "md.obsidian" to AppCategory.PRODUCTIVITY,
        // Einkaufen
        "com.amazon.mshop" to AppCategory.SHOPPING, "com.amazon.windowshop" to AppCategory.SHOPPING, "com.ebay" to AppCategory.SHOPPING,
        "de.zalando" to AppCategory.SHOPPING, "com.zalando" to AppCategory.SHOPPING, "de.ottogroup" to AppCategory.SHOPPING,
        "com.otto" to AppCategory.SHOPPING, "de.lidl" to AppCategory.SHOPPING, "com.lidl" to AppCategory.SHOPPING,
        "de.aldi" to AppCategory.SHOPPING, "de.rewe" to AppCategory.SHOPPING, "de.kaufland" to AppCategory.SHOPPING,
        "com.einnovation.temu" to AppCategory.SHOPPING, "com.alibaba.aliexpress" to AppCategory.SHOPPING, "de.idealo" to AppCategory.SHOPPING,
        "com.ikea" to AppCategory.SHOPPING, "de.dm." to AppCategory.SHOPPING, "de.mediamarkt" to AppCategory.SHOPPING,
        "de.kleinanzeigen" to AppCategory.SHOPPING, "com.ebay.kleinanzeigen" to AppCategory.SHOPPING, "com.vinted" to AppCategory.SHOPPING,
        // Smart Home
        "com.mcu.reolink" to AppCategory.SMART_HOME, "io.homeassistant" to AppCategory.SMART_HOME, "com.philips.lighting" to AppCategory.SMART_HOME,
        "com.signify" to AppCategory.SMART_HOME, "com.google.android.apps.chromecast" to AppCategory.SMART_HOME, "com.avm.android" to AppCategory.SMART_HOME,
        "de.avm" to AppCategory.SMART_HOME, "com.tado" to AppCategory.SMART_HOME, "com.amazon.dee" to AppCategory.SMART_HOME,
        "com.xiaomi.smarthome" to AppCategory.SMART_HOME, "com.tuya" to AppCategory.SMART_HOME, "com.shelly" to AppCategory.SMART_HOME,
        "cc.blynk" to AppCategory.SMART_HOME, "com.ring" to AppCategory.SMART_HOME, "com.netatmo" to AppCategory.SMART_HOME,
        "com.homey" to AppCategory.SMART_HOME, "de.eq3" to AppCategory.SMART_HOME, "com.ikea.inter.homesmart" to AppCategory.SMART_HOME,
        // Auto & Verkehr
        "de.hafas" to AppCategory.TRAVEL, "de.db." to AppCategory.TRAVEL, "com.deutschebahn" to AppCategory.TRAVEL,
        "de.dbnav" to AppCategory.TRAVEL, "com.tesla" to AppCategory.TRAVEL, "de.bmw" to AppCategory.TRAVEL, "com.bmwgroup" to AppCategory.TRAVEL,
        "com.volkswagen" to AppCategory.TRAVEL, "de.mercedes" to AppCategory.TRAVEL, "com.daimler" to AppCategory.TRAVEL,
        "com.ubercab" to AppCategory.TRAVEL, "com.flixbus" to AppCategory.TRAVEL, "net.flixbus" to AppCategory.TRAVEL,
        "de.adac" to AppCategory.TRAVEL, "com.ryanair" to AppCategory.TRAVEL, "com.lufthansa" to AppCategory.TRAVEL,
        "com.booking" to AppCategory.TRAVEL, "com.airbnb" to AppCategory.TRAVEL, "com.chargepoint" to AppCategory.TRAVEL,
        "com.plugsurfing" to AppCategory.TRAVEL, "com.enbw" to AppCategory.TRAVEL, "com.google.android.projection.gearhead" to AppCategory.TRAVEL,
        // Gesundheit & Fitness
        "com.google.android.apps.fitness" to AppCategory.HEALTH, "com.strava" to AppCategory.HEALTH, "com.fitbit" to AppCategory.HEALTH,
        "com.garmin" to AppCategory.HEALTH, "com.samsung.android.app.health" to AppCategory.HEALTH, "de.tk." to AppCategory.HEALTH,
        "com.google.android.apps.healthdata" to AppCategory.HEALTH, "de.aok" to AppCategory.HEALTH, "com.withings" to AppCategory.HEALTH,
        // Bilder, Musik, Video, Karten
        "com.google.android.apps.photos" to AppCategory.IMAGES, "com.google.android.GoogleCamera" to AppCategory.IMAGES,
        "com.spotify" to AppCategory.AUDIO, "com.google.android.apps.youtube.music" to AppCategory.AUDIO, "com.audible" to AppCategory.AUDIO,
        "com.google.android.youtube" to AppCategory.VIDEO, "com.netflix" to AppCategory.VIDEO, "com.amazon.avod" to AppCategory.VIDEO,
        "com.disney" to AppCategory.VIDEO, "de.zdf" to AppCategory.VIDEO, "de.swr.ardplayer" to AppCategory.VIDEO, "tv.twitch" to AppCategory.VIDEO,
        "com.google.android.apps.maps" to AppCategory.MAPS, "net.osmand" to AppCategory.MAPS, "com.waze" to AppCategory.MAPS,
        "com.google.android.apps.weather" to AppCategory.TOOLS, "com.google.android.apps.magazines" to AppCategory.NEWS,
    )

    /** Stichwörter im Paketnamen bzw. App-Namen für die neuen, feineren Kategorien. */
    private val fineKeywords: List<Pair<String, AppCategory>> = listOf(
        "bank" to AppCategory.FINANCE, "sparkasse" to AppCategory.FINANCE, "finanz" to AppCategory.FINANCE, "broker" to AppCategory.FINANCE,
        "wallet" to AppCategory.FINANCE, "crypto" to AppCategory.FINANCE, "invest" to AppCategory.FINANCE, "volksbank" to AppCategory.FINANCE,
        "browser" to AppCategory.BROWSER,
        "chatgpt" to AppCategory.AI, "gemini" to AppCategory.AI, "copilot" to AppCategory.AI, "claude" to AppCategory.AI,
        "shop" to AppCategory.SHOPPING, "markt" to AppCategory.SHOPPING,
        "smarthome" to AppCategory.SMART_HOME, "smart home" to AppCategory.SMART_HOME, "homeassistant" to AppCategory.SMART_HOME,
        "camera.cloud" to AppCategory.SMART_HOME, "thermostat" to AppCategory.SMART_HOME,
        "bahn" to AppCategory.TRAVEL, "parking" to AppCategory.TRAVEL, "parken" to AppCategory.TRAVEL,
        "ticket" to AppCategory.TRAVEL, "flight" to AppCategory.TRAVEL, "carsharing" to AppCategory.TRAVEL,
        "fitness" to AppCategory.HEALTH, "health" to AppCategory.HEALTH, "gesund" to AppCategory.HEALTH, "workout" to AppCategory.HEALTH,
    )

    /**
     * Kategorie einer App: eigene Zuordnung des Nutzers > eingebaute Liste > Stichwörter (Paket, Name) >
     * Angabe der App > alte Stichwörter > Sonstige.
     */
    fun classify(packageName: String, label: String, androidCategory: Int, override: AppCategory? = null): AppCategory {
        if (override != null && override != AppCategory.RECENT) return override
        known.firstOrNull { packageName.startsWith(it.first) }?.let { return it.second }
        val p = packageName.lowercase()
        val l = label.lowercase()
        fineKeywords.firstOrNull { p.contains(it.first) || l.contains(it.first) }?.let { return it.second }
        return fromAndroid(androidCategory, packageName)
    }

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
