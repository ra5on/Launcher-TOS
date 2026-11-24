package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSetupTest {
    // ---------- Versionen ----------

    @Test
    fun version_parseAndCompare() {
        val v = AppVersion.parse("v1.1.0")!!
        assertEquals("1.1.0", v.name)
        assertEquals(110, v.code)
        assertEquals(100, AppVersion.parse("1.0.0")!!.code)
        assertTrue(AppVersion.parse("1.0.10") == null) // Patch zweistellig: nicht im versionCode darstellbar
        assertNull(AppVersion.parse("v1.1"))
        assertNull(AppVersion.parse("1.1.0-beta"))
        assertTrue(AppVersion.parse("1.2.0")!! > AppVersion.parse("1.1.9")!!)
        assertTrue(AppVersion.parse("2.0.0")!! > AppVersion.parse("1.9.9")!!)
    }

    @Test
    fun isNewer_usesNameThenCode() {
        val v110 = AppVersion.parse("1.1.0")!!
        assertTrue(UpdateLogic.isNewer(v110, "1.0.0", 100))
        assertFalse(UpdateLogic.isNewer(v110, "1.1.0", 110))
        assertFalse(UpdateLogic.isNewer(AppVersion.parse("1.0.9")!!, "1.1.0", 110))
        assertTrue(UpdateLogic.isNewer(v110, "irgendwas", 100)) // Fallback auf versionCode
    }

    @Test
    fun parseLatest_readsAssets() {
        val json = """
            {"tag_name":"v1.1.0","draft":false,"prerelease":false,"body":"**Neu**\n- `Updates`\n- [Mehr](https://x)",
             "assets":[{"name":"LauncherTOS-1.1.0.apk","browser_download_url":"https://h/a.apk","size":123},
                       {"name":"SHA256SUMS","browser_download_url":"https://h/s","size":88}]}
        """.trimIndent()
        val r = UpdateLogic.parseLatest(json)!!
        assertEquals("1.1.0", r.version.name)
        assertEquals("https://h/a.apk", r.apkUrl)
        assertEquals(123L, r.apkSize)
        assertEquals("https://h/s", r.sumsUrl)
        assertEquals("Neu\n• Updates\n• Mehr", r.notes)
    }

    @Test
    fun parseLatest_rejectsIncomplete() {
        assertNull(UpdateLogic.parseLatest("""{"tag_name":"v1.1.0","assets":[]}"""))
        assertNull(UpdateLogic.parseLatest("""{"tag_name":"v1.1.0","prerelease":true,"assets":[]}"""))
        assertNull(UpdateLogic.parseLatest("""{"tag_name":"nightly","assets":[]}"""))
        assertNull(UpdateLogic.parseLatest("kein json"))
    }

    // ---------- Prüfsummen ----------

    @Test
    fun sums_findAndCompare() {
        val hex = "e1ffee978cd6e3e06644584f9e941fa758244a34bb4b51ba8f831202402d7151"
        val sums = "$hex  LauncherTOS-1.0.0.apk\n${"a".repeat(64)} *andere.apk\n"
        assertEquals(hex, UpdateLogic.sumFor(sums, "LauncherTOS-1.0.0.apk"))
        assertEquals("a".repeat(64), UpdateLogic.sumFor(sums, "andere.apk"))
        assertNull(UpdateLogic.sumFor(sums, "fehlt.apk"))
        assertNull(UpdateLogic.sumFor("zukurz  x.apk", "x.apk"))
        assertTrue(UpdateLogic.checksumMatches(hex.uppercase(), hex))
        assertFalse(UpdateLogic.checksumMatches(hex, null))
        assertFalse(UpdateLogic.checksumMatches(hex, "b".repeat(64)))
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            UpdateLogic.sha256Hex("hello".toByteArray()),
        )
    }

    // ---------- Zertifikat ----------

    @Test
    fun certificate_pinned() {
        val cert = "zertifikat".toByteArray()
        val fp = UpdateLogic.sha256Hex(cert)
        assertTrue(UpdateLogic.certificateMatches(listOf(cert), fp.uppercase().chunked(2).joinToString(":")))
        assertFalse(UpdateLogic.certificateMatches(listOf("fremd".toByteArray()), fp))
        assertFalse(UpdateLogic.certificateMatches(emptyList(), fp))
        assertFalse(UpdateLogic.certificateMatches(listOf(cert, cert), fp)) // mehrere Signierer: abgelehnt
        assertEquals(64, UpdateLogic.normalizeFingerprint(UpdateLogic.RELEASE_CERT_SHA256).length)
    }

    @Test
    fun dueForCheck_weekly() {
        val now = 1_000_000_000_000L
        assertTrue(UpdateLogic.dueForCheck(0, now))
        assertFalse(UpdateLogic.dueForCheck(now - 3L * 86_400_000, now))
        assertTrue(UpdateLogic.dueForCheck(now - 8L * 86_400_000, now))
        assertTrue(UpdateLogic.dueForCheck(now + 86_400_000, now)) // Uhr zurückgestellt
    }

    // ---------- Einrichtung ----------

    private val none = SetupFacts(false, false, false, false, false, false, false)

    @Test
    fun setup_skipsDoneSteps() {
        val f = none.copy(isDefault = true, listener = true)
        assertEquals(SetupStep.WELCOME, SetupLogic.next(0, f, emptySet(), includeWelcome = true))
        assertEquals(SetupStep.DND, SetupLogic.next(1, f, emptySet(), includeWelcome = true))
        assertEquals(SetupStep.WRITE_SETTINGS, SetupLogic.next(1, f, setOf(SetupStep.DND), includeWelcome = true))
        val all = SetupFacts(true, true, true, true, true, true, true)
        assertNull(SetupLogic.next(1, all, emptySet(), includeWelcome = true))
        assertTrue(SetupLogic.open(all, emptySet()).isEmpty())
    }

    @Test
    fun setup_openCountRespectsDeclined() {
        val f = none.copy(isDefault = true, listener = true, gesturesSeen = true, updateChoiceMade = true)
        assertEquals(listOf(SetupStep.DND, SetupStep.WRITE_SETTINGS, SetupStep.INSTALL_UNKNOWN), SetupLogic.open(f, emptySet()))
        assertEquals(listOf(SetupStep.INSTALL_UNKNOWN), SetupLogic.open(f, setOf(SetupStep.DND, SetupStep.WRITE_SETTINGS)))
    }

    @Test
    fun restricted_isDeterministicFromPackageSource() {
        // Download bzw. Datei ab Android 13: eingeschränkt, bis freigegeben.
        assertTrue(SetupLogic.isRestricted(34, SetupLogic.SOURCE_DOWNLOADED_FILE, "com.google.android.packageinstaller", null))
        assertTrue(SetupLogic.isRestricted(36, SetupLogic.SOURCE_LOCAL_FILE, null, false))
        assertFalse(SetupLogic.isRestricted(36, SetupLogic.SOURCE_LOCAL_FILE, null, true)) // schon freigegeben
        assertFalse(SetupLogic.isRestricted(32, SetupLogic.SOURCE_DOWNLOADED_FILE, null, null)) // vor Android 13
        assertFalse(SetupLogic.isRestricted(34, SetupLogic.SOURCE_STORE, "com.android.vending", null))
        // Quelle unbekannt: adb/Shell frei, unbekannter Installer eingeschränkt, Store frei.
        assertFalse(SetupLogic.isRestricted(34, 0, null, null))
        assertFalse(SetupLogic.isRestricted(34, 0, "com.android.shell", null))
        assertTrue(SetupLogic.isRestricted(34, 1, "com.example.filemanager", null))
        assertFalse(SetupLogic.isRestricted(34, 1, "org.fdroid.fdroid", null))
        assertTrue(SetupLogic.showRestrictedHelp(restricted = true, granted = false))
        assertFalse(SetupLogic.showRestrictedHelp(restricted = true, granted = true))
        assertFalse(SetupLogic.showRestrictedHelp(restricted = false, granted = false))
        assertTrue(SetupLogic.isStore("com.android.vending"))
        assertFalse(SetupLogic.isStore("com.google.android.packageinstaller"))
        assertFalse(SetupLogic.isStore(null))
    }
}
