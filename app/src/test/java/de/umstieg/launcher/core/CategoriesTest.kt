package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriesTest {
    private val productivity = 7
    private val undefined = -1

    @Test
    fun knownAppsGetFineCategories() {
        assertEquals(AppCategory.FINANCE, Categories.classify("com.starfinanz.smob.android.sfinanzstatus", "Sparkasse", productivity))
        assertEquals(AppCategory.FINANCE, Categories.classify("com.bitpanda.bitpanda", "Bitpanda", undefined))
        assertEquals(AppCategory.BROWSER, Categories.classify("com.android.chrome", "Chrome", productivity))
        assertEquals(AppCategory.BROWSER, Categories.classify("org.mozilla.firefox", "Firefox", undefined))
        assertEquals(AppCategory.AI, Categories.classify("com.openai.chatgpt", "ChatGPT", productivity))
        assertEquals(AppCategory.AI, Categories.classify("com.anthropic.claude", "Claude", productivity))
        assertEquals(AppCategory.TOOLS, Categories.classify("com.google.android.calculator", "Rechner", productivity))
        assertEquals(AppCategory.SHOPPING, Categories.classify("com.amazon.mshop.android.shopping", "Amazon", undefined))
        assertEquals(AppCategory.SMART_HOME, Categories.classify("com.mcu.reolink", "Reolink", undefined))
        assertEquals(AppCategory.SMART_HOME, Categories.classify("io.homeassistant.companion.android", "Home Assistant", undefined))
        assertEquals(AppCategory.TRAVEL, Categories.classify("de.hafas.android.db", "DB Navigator", undefined))
    }

    @Test
    fun keywordsAndFallbacks() {
        assertEquals(AppCategory.FINANCE, Categories.classify("de.example.meinebank", "Meine Bank", undefined))
        assertEquals(AppCategory.HEALTH, Categories.classify("com.example.app", "Fitness Tracker", undefined))
        // Angabe der App selbst, wenn nichts Feineres passt.
        assertEquals(AppCategory.PRODUCTIVITY, Categories.classify("com.example.notes2", "Zettel", productivity))
        assertEquals(AppCategory.OTHER, Categories.classify("com.example.xyz", "Xyz", undefined))
    }

    @Test
    fun userOverrideWins() {
        assertEquals(AppCategory.GAMES, Categories.classify("com.android.chrome", "Chrome", productivity, AppCategory.GAMES))
        // „Zuletzt installiert“ ist keine wählbare Kategorie.
        assertEquals(AppCategory.BROWSER, Categories.classify("com.android.chrome", "Chrome", productivity, AppCategory.RECENT))
    }
}
