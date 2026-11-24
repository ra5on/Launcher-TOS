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
    fun gestures_classifyEdgeSplit() {
        val edge = 0.10f
        assertEquals(Gesture.EDGE_LEFT, GestureClassifier.classify(0.1f, 0.05f, 0f, 0.2f, edge, true, 35, 35))
        assertEquals(Gesture.EDGE_RIGHT, GestureClassifier.classify(0.9f, 0.08f, 0.01f, 0.2f, edge, true, 35, 35))
        assertEquals(Gesture.EDGE_CENTER, GestureClassifier.classify(0.5f, 0.05f, 0f, 0.2f, edge, true, 35, 35))
        // Breitere Zone rechts
        assertEquals(Gesture.EDGE_RIGHT, GestureClassifier.classify(0.55f, 0.05f, 0f, 0.2f, edge, true, 20, 50))
    }

    @Test
    fun gestures_edgeAndMiddleAreSeparate() {
        val edge = 0.10f
        // Gleiche Bewegung, Start im Rand bzw. in der Bildschirmmitte.
        assertEquals(Gesture.EDGE, GestureClassifier.classify(0.1f, 0.05f, 0f, 0.2f, edge, false, 35, 35))
        assertEquals(Gesture.SWIPE_DOWN, GestureClassifier.classify(0.1f, 0.40f, 0f, 0.2f, edge, false, 35, 35))
        assertEquals(Gesture.SWIPE_DOWN, GestureClassifier.classify(0.5f, 0.11f, 0f, 0.2f, edge, true, 35, 35))
        assertEquals(Gesture.SWIPE_UP, GestureClassifier.classify(0.5f, 0.8f, 0f, -0.2f, edge, true, 35, 35))
        assertNull(GestureClassifier.classify(0.5f, 0.5f, 0.3f, 0.1f, edge, true, 35, 35)) // waagerecht
        assertNull(GestureClassifier.classify(0.5f, 0.5f, 0f, 0.02f, edge, true, 35, 35)) // zu kurz
    }

    @Test
    fun gestures_defaultsAreTosPreset() {
        val s = LauncherSettings()
        assertEquals(GesturePreset.TOS, s.gesturePreset)
        assertEquals(GesturePreset.TOS, GesturePresets.detect(s))
        assertEquals(GestureAction.NOTIFICATIONS, GestureClassifier.binding(s, Gesture.EDGE_LEFT).action)
        assertEquals(GestureAction.QUICK_SETTINGS, GestureClassifier.binding(s, Gesture.EDGE_RIGHT).action)
        assertEquals(GestureAction.SEARCH, GestureClassifier.binding(s, Gesture.SWIPE_DOWN).action)
        assertEquals(GestureAction.ASSISTANT, GestureClassifier.binding(s, Gesture.DOUBLE_TAP_BOTTOM).action)
        assertEquals(Gesture.DOUBLE_TAP_BOTTOM, GestureClassifier.doubleTap(0.9f))
        assertEquals(Gesture.DOUBLE_TAP, GestureClassifier.doubleTap(0.5f))
    }

    @Test
    fun gestures_presetsMapToBindings() {
        val android = GesturePresets.apply(LauncherSettings(), GesturePreset.ANDROID)
        assertFalse(android.edgeSplit)
        assertEquals(GestureAction.SYSTEM_NOTIFICATIONS, GestureClassifier.binding(android, Gesture.EDGE).action)
        assertEquals(GestureAction.SYSTEM_NOTIFICATIONS, GestureClassifier.binding(android, Gesture.SWIPE_DOWN).action)
        assertEquals(GestureAction.SEARCH, GestureClassifier.binding(android, Gesture.SWIPE_UP).action)
        assertEquals(GesturePreset.ANDROID, GesturePresets.detect(android))

        val mixed = GesturePresets.apply(android, GesturePreset.ANDROID_SEARCH)
        assertEquals(GestureAction.SYSTEM_NOTIFICATIONS, GestureClassifier.binding(mixed, Gesture.EDGE).action)
        assertEquals(GestureAction.SEARCH, GestureClassifier.binding(mixed, Gesture.SWIPE_DOWN).action)
        assertEquals(GesturePreset.ANDROID_SEARCH, GesturePresets.detect(mixed))

        val back = GesturePresets.apply(mixed, GesturePreset.TOS)
        assertTrue(back.edgeSplit)
        assertEquals(LauncherSettings.DEFAULT_GESTURES, back.gestures)
        // „Eigen“ als Vorlage behält die Belegung.
        assertEquals(mixed.gestures, GesturePresets.apply(mixed, GesturePreset.CUSTOM).gestures)
    }

    @Test
    fun gestures_singleChangeSwitchesToCustom() {
        val s = LauncherSettings()
        val changed = GesturePresets.withGesture(s, Gesture.SWIPE_UP, GestureBinding(GestureAction.APP_DRAWER))
        assertEquals(GesturePreset.CUSTOM, changed.gesturePreset)
        assertEquals(GestureAction.APP_DRAWER, GestureClassifier.binding(changed, Gesture.SWIPE_UP).action)
        // Zurückgestellt → passt wieder zur Vorlage.
        val restored = GesturePresets.withGesture(changed, Gesture.SWIPE_UP, GestureBinding(GestureAction.NOTHING))
        assertEquals(GesturePreset.TOS, restored.gesturePreset)
        // Rand-Aufteilung ändern wird neu zugeordnet: ungeteilt mit Standardbelegung = „Android + Suche“ …
        val unsplit = GesturePresets.withSplit(s, false)
        assertEquals(GesturePreset.ANDROID_SEARCH, unsplit.gesturePreset)
        // … und jede weitere Abweichung ist „Eigen“.
        assertEquals(GesturePreset.CUSTOM, GesturePresets.withGesture(unsplit, Gesture.DOUBLE_TAP, GestureBinding(GestureAction.SEARCH)).gesturePreset)
    }

    @Test
    fun gestures_searchReachability() {
        val s = LauncherSettings()
        assertTrue(GestureClassifier.searchReachableByGesture(s))
        val none = GesturePresets.withGesture(
            GesturePresets.withGesture(s, Gesture.SWIPE_DOWN, GestureBinding(GestureAction.SYSTEM_NOTIFICATIONS)),
            Gesture.EDGE_CENTER, GestureBinding(GestureAction.NOTHING),
        )
        assertFalse(GestureClassifier.searchReachableByGesture(none))
        // Ungespeicherte Gesten werden aus den Standardwerten ergänzt.
        val partial = s.copy(gestures = mapOf(Gesture.SWIPE_UP to GestureBinding(GestureAction.APP_DRAWER))).sanitized()
        assertEquals(GestureAction.SEARCH, GestureClassifier.binding(partial, Gesture.SWIPE_DOWN).action)
    }

    @Test
    fun simpleMode_keepsOnlySearchGesture() {
        val e = LauncherSettings(simpleMode = true, iconSize = 90).effective()
        assertEquals(120, e.iconSize)
        assertEquals(GestureAction.SEARCH, GestureClassifier.binding(e, Gesture.SWIPE_DOWN).action)
        assertEquals(GestureAction.NOTHING, GestureClassifier.binding(e, Gesture.EDGE_LEFT).action)
        assertEquals(LauncherSettings(simpleMode = false).columns, e.columns)
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
