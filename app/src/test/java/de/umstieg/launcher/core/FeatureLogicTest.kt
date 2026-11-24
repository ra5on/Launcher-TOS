package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Suche/Ranking, Rechner, Kategorien, Gesten, Schnellzugriff, Badges. */
class FeatureLogicTest {
    private val labels = listOf("Kalender", "Kamera", "Karten", "Kontakte", "Aufgaben", "Übersetzer", "Straßenbahn", "Google Maps", "Einstellungen")

    private fun ranked(q: String, f: SearchFuzziness = SearchFuzziness.NORMAL) = FuzzySearch.rank(labels, { it }, q, f).map { it.item }

    // ---------- Suche ----------

    @Test
    fun search_prefixBeatsSubsequence() {
        val r = ranked("ka")
        // Präfix-Treffer zuerst, kürzere Namen vor längeren.
        assertEquals(listOf("Kamera", "Karten", "Kalender"), r.take(3))
        assertTrue("Kontakte" in r) // Buchstabenfolge k…a
        assertFalse("Aufgaben" in r)
    }

    @Test
    fun search_exactAndWordStart() {
        assertEquals("Kamera", ranked("kamera").first())
        assertEquals("Google Maps", ranked("maps").first())
        assertEquals("Google Maps", ranked("gm").first()) // Anfangsbuchstaben
    }

    @Test
    fun search_umlautsAndEszett() {
        assertEquals("Übersetzer", ranked("uebers").first())
        assertEquals("Übersetzer", ranked("uber").first())
        assertEquals("Straßenbahn", ranked("strassen").first())
        assertEquals("Straßenbahn", ranked("strasen").first()) // Tippfehler
    }

    @Test
    fun search_typos() {
        assertEquals("Kalender", ranked("kalnder").first())
        assertEquals("Kalender", ranked("klaender").first()) // Vertauschung
        assertTrue(ranked("einstelungen").first() == "Einstellungen")
        // Kurze Suchbegriffe: keine Tippfehler-Treffer.
        assertTrue(ranked("kxm").isEmpty())
    }

    @Test
    fun search_strictIsStricter() {
        assertTrue(ranked("kamra", SearchFuzziness.STRICT).isEmpty())
        assertEquals("Kamera", ranked("kamra").first())
        assertFalse(ranked("kmaera", SearchFuzziness.GENEROUS).isEmpty())
        assertFalse("Kontakte" in ranked("ka", SearchFuzziness.STRICT))
    }

    @Test
    fun search_highlightMapsToOriginal() {
        val hit = FuzzySearch.score("Übersetzer", "über")!!
        assertEquals(listOf(0..3), hit.highlight)
        val sub = FuzzySearch.score("Kontakte", "ka")!!
        assertTrue(sub.similar)
        assertEquals(listOf(0..0, 4..4), sub.highlight)
    }

    @Test
    fun search_boostBreaksTies() {
        val r = FuzzySearch.rank(listOf("Karten", "Kamera"), { it }, "ka", boost = { if (it == "Karten") 50 else 0 })
        assertEquals("Karten", r.first().item)
    }

    @Test
    fun distance_isDamerau() {
        assertEquals(1, FuzzySearch.distance("ab", "ba"))
        assertEquals(1, FuzzySearch.distance("kalender", "kalnder"))
        assertEquals(0, FuzzySearch.distance("x", "x"))
        assertEquals(0, FuzzySearch.allowedTypos(3, SearchFuzziness.NORMAL))
        assertEquals(2, FuzzySearch.allowedTypos(8, SearchFuzziness.NORMAL))
    }

    // ---------- Rechner ----------

    @Test
    fun calculator_basics() {
        assertEquals(7.0, Calculator.evaluate("1+2*3")!!, 1e-9)
        assertEquals(9.0, Calculator.evaluate("(1+2)×3")!!, 1e-9)
        assertEquals(2.5, Calculator.evaluate("5 / 2")!!, 1e-9)
        assertEquals(1.5, Calculator.evaluate("1,2+0,3")!!, 1e-9)
        assertEquals(8.0, Calculator.evaluate("2^3")!!, 1e-9)
        assertEquals(19.0, Calculator.evaluate("20-5%*20")!!, 1e-9)
        assertEquals(-3.0, Calculator.evaluate("-1-2")!!, 1e-9)
        assertEquals("2,5", Calculator.format(2.5))
        assertEquals("1234567", Calculator.format(1234567.0))
        assertEquals("0,3333333333", Calculator.format(1.0 / 3))
    }

    @Test
    fun calculator_rejectsNonExpressions() {
        assertNull(Calculator.evaluate("42"))
        assertNull(Calculator.evaluate("kamera"))
        assertNull(Calculator.evaluate("1/0"))
        assertNull(Calculator.evaluate("2*(3"))
        assertNotNull(Calculator.evaluate("2*(3)"))
    }

    // ---------- Kategorien ----------

    @Test
    fun categories_fromAndroidAndKeywords() {
        assertEquals(AppCategory.GAMES, Categories.fromAndroid(0, "x"))
        assertEquals(AppCategory.MAPS, Categories.fromAndroid(6, "x"))
        assertEquals(AppCategory.IMAGES, Categories.fromAndroid(-1, "org.example.camera2"))
        assertEquals(AppCategory.SOCIAL, Categories.fromAndroid(-1, "com.example.messaging"))
        assertEquals(AppCategory.OTHER, Categories.fromAndroid(-1, "com.example.thing"))
    }

    @Test
    fun categories_groupWithRecentFirst() {
        fun m(n: String, c: AppCategory, t: Long) = AppMeta(AppKey("p.$n", "A", 0), n, c, t)
        val apps = listOf(m("Zug", AppCategory.MAPS, 5), m("Alt", AppCategory.MAPS, 1), m("Spiel", AppCategory.GAMES, 9), m("X", AppCategory.OTHER, 2))
        val g = Categories.group(apps, recentCount = 2)
        assertEquals(AppCategory.RECENT, g.first().first)
        assertEquals(listOf("Spiel", "Zug"), g.first().second.map { it.label })
        assertEquals(listOf(AppCategory.RECENT, AppCategory.GAMES, AppCategory.MAPS, AppCategory.OTHER), g.map { it.first })
        assertEquals(listOf("Alt", "Zug"), g.first { it.first == AppCategory.MAPS }.second.map { it.label })
    }

    @Test
    fun indexLetter_foldsUmlautsAndDigits() {
        assertEquals('U', Categories.indexLetter("Übersetzer"))
        assertEquals('#', Categories.indexLetter("1Password"))
        assertEquals('K', Categories.indexLetter("kalender"))
    }

    // ---------- Gesten ----------

    @Test
    fun gestures_classify() {
        assertEquals(Gesture.SWIPE_TOP_LEFT, GestureClassifier.classify(0.1f, 0.05f, 0f, 0.2f, 35, 35))
        assertEquals(Gesture.SWIPE_TOP_RIGHT, GestureClassifier.classify(0.9f, 0.1f, 0.01f, 0.2f, 35, 35))
        assertEquals(Gesture.SWIPE_DOWN, GestureClassifier.classify(0.5f, 0.1f, 0f, 0.2f, 35, 35))
        assertEquals(Gesture.SWIPE_DOWN, GestureClassifier.classify(0.1f, 0.6f, 0f, 0.2f, 35, 35))
        assertEquals(Gesture.SWIPE_UP, GestureClassifier.classify(0.5f, 0.8f, 0f, -0.2f, 35, 35))
        assertNull(GestureClassifier.classify(0.5f, 0.5f, 0.3f, 0.1f, 35, 35)) // waagerecht
        assertNull(GestureClassifier.classify(0.5f, 0.5f, 0f, 0.02f, 35, 35)) // zu kurz
        // Breitere Zone rechts
        assertEquals(Gesture.SWIPE_TOP_RIGHT, GestureClassifier.classify(0.55f, 0.1f, 0f, 0.2f, 20, 50))
    }

    @Test
    fun gestures_defaultsAndOverrides() {
        val s = LauncherSettings()
        assertEquals(GestureAction.NOTIFICATIONS, GestureClassifier.binding(s, Gesture.SWIPE_TOP_LEFT).action)
        assertEquals(GestureAction.ASSISTANT, GestureClassifier.binding(s, Gesture.DOUBLE_TAP_BOTTOM).action)
        val custom = s.copy(gestures = mapOf(Gesture.SWIPE_UP to GestureBinding(GestureAction.APP_DRAWER))).sanitized()
        assertEquals(GestureAction.APP_DRAWER, GestureClassifier.binding(custom, Gesture.SWIPE_UP).action)
        assertEquals(GestureAction.SEARCH, GestureClassifier.binding(custom, Gesture.SWIPE_DOWN).action)
        assertEquals(Gesture.DOUBLE_TAP_BOTTOM, GestureClassifier.doubleTap(0.9f))
        assertEquals(Gesture.DOUBLE_TAP, GestureClassifier.doubleTap(0.5f))
    }

    // ---------- Schnellzugriff ----------

    @Test
    fun quickSettings_packRows() {
        val items = listOf(
            QsItem(QsTile.WIFI), QsItem(QsTile.BLUETOOTH), QsItem(QsTile.MEDIA, QsSize.L),
            QsItem(QsTile.DND, QsSize.M), QsItem(QsTile.CAMERA), QsItem(QsTile.ALARM), QsItem(QsTile.QR),
        )
        val rows = QsOps.packRows(items)
        assertEquals(listOf(2, 1, 3, 1), rows.map { it.size })
        rows.forEach { row -> assertTrue(row.sumOf { it.size.units } <= 4) }
    }

    @Test
    fun quickSettings_editOps() {
        var l = QsLayout.DEFAULT
        val id = l.pages.first().id
        val before = l.pages.first().items.size
        l = QsOps.addItem(l, id, QsTile.HOTSPOT)
        assertEquals(before + 1, l.pages.first().items.size)
        assertEquals(l, QsOps.addItem(l, id, QsTile.HOTSPOT)) // kein Duplikat
        assertFalse(QsTile.HOTSPOT in QsOps.available(l, id))
        l = QsOps.moveItem(l, id, before, 0)
        assertEquals(QsTile.HOTSPOT, l.pages.first().items.first().tile)
        l = QsOps.setSize(l, id, 0, QsSize.L)
        assertEquals(QsSize.L, l.pages.first().items.first().size)
        l = QsOps.removeItem(l, id, 0)
        assertEquals(before, l.pages.first().items.size)
        l = QsOps.addPage(l, "x", "Meins")
        l = QsOps.movePage(l, 3, 0)
        assertEquals("x", l.pages.first().id)
        var single = QsLayout(listOf(QsPage("a", "", emptyList())))
        single = QsOps.removePage(single, "a")
        assertEquals(1, single.pages.size)
    }

    // ---------- Badges ----------

    @Test
    fun badges_countAndSumFolders() {
        val n = listOf(
            NotificationFacts("chat", 0, 0, false, false),
            NotificationFacts("chat", 0, 0, false, false),
            NotificationFacts("chat", 0, 0, true, false), // Zusammenfassung
            NotificationFacts("mail", 0, 3, false, false), // App meldet 3
            NotificationFacts("music", 0, 0, false, true), // laufend
            NotificationFacts("chat", 10, 0, false, false), // Arbeitsprofil
        )
        val counts = BadgeCounter.count(n)
        val chat = AppKey("chat", "Main", 0)
        val mail = AppKey("mail", "Main", 0)
        assertEquals(2, BadgeCounter.forApp(counts, chat, emptySet()))
        assertEquals(1, BadgeCounter.forApp(counts, AppKey("chat", "Main", 10), emptySet()))
        assertEquals(0, BadgeCounter.forApp(counts, AppKey("music", "M", 0), emptySet()))
        val folder = FolderItem("f", "Ordner", listOf(chat, mail))
        assertEquals(5, BadgeCounter.forItem(counts, folder, emptySet()))
        assertEquals(3, BadgeCounter.forItem(counts, folder, setOf("chat")))
        assertEquals("99+", BadgeCounter.label(120))
    }
}
