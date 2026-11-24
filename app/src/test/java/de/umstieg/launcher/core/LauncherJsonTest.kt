package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherJsonTest {
    private fun app(n: String, user: Long = 0) = AppKey("pkg.$n", "pkg.$n.Main", user)

    @Test
    fun roundTrip_keepsEverything() {
        val data = LauncherData(
            settings = LauncherSettings(columns = 5, rows = 7, iconRounding = 30, theme = ThemeMode.DARK, legacyBackground = LegacyBackground.DOMINANT),
            layout = HomeLayout(
                pages = listOf(
                    listOf(
                        Placed(0, 0, AppItem(app("a"))),
                        Placed(2, 1, FolderItem("f1", "Werkzeuge", listOf(app("b"), app("c", user = 10)))),
                    ),
                    listOf(Placed(4, 6, AppItem(app("d")))),
                ),
                dock = listOf(AppItem(app("e")), FolderItem("f2", "Ordner", listOf(app("f"), app("g")))),
            ),
            knownApps = setOf(app("a"), app("b"), app("c", 10), app("d"), app("e"), app("f"), app("g"), app("h")),
            initialized = true,
            defaultHintDismissed = true,
        )
        val text = LauncherJson.encode(data)
        assertTrue(text.contains("\"type\": \"folder\""))
        assertEquals(data, LauncherJson.decode(text))
    }

    @Test
    fun decode_toleratesUnknownKeysAndClampsValues() {
        val text = """{"version":1,"zukunft":true,"settings":{"columns":12,"rows":1,"iconSize":500}}"""
        val data = LauncherJson.decode(text)
        assertEquals(7, data.settings.columns)
        assertEquals(4, data.settings.rows)
        assertEquals(130, data.settings.iconSize)
        assertEquals(1, data.layout.pages.size)
    }

    @Test(expected = Exception::class)
    fun decode_rejectsGarbage() {
        LauncherJson.decode("kein json")
    }
}
