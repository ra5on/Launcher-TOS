package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchExtrasTest {
    @Test
    fun urlDetection() {
        assertEquals("https://example.com", SearchExtras.asUrl("example.com"))
        assertEquals("https://heise.de/news", SearchExtras.asUrl(" heise.de/news "))
        assertEquals("https://www.test", SearchExtras.asUrl("www.test"))
        assertEquals("http://localhost.lan:8080", SearchExtras.asUrl("http://localhost.lan:8080"))
        assertNull(SearchExtras.asUrl("wetter morgen"))
        assertNull(SearchExtras.asUrl("3.14"))
        assertNull(SearchExtras.asUrl("Chrome"))
        assertNull(SearchExtras.asUrl("12*7"))
    }

    @Test
    fun webSearchUrls() {
        assertEquals("https://duckduckgo.com/?q=K%C3%A4se+%26+Brot", SearchExtras.webSearchUrl(SearchEngine.DUCKDUCKGO, "Käse & Brot"))
        assertEquals("https://www.google.com/search?q=wetter", SearchExtras.webSearchUrl(SearchEngine.GOOGLE, " wetter "))
        assertTrue(SearchExtras.playStoreUrl("rechner").startsWith("market://search?q=rechner"))
    }

    @Test
    fun androidSettingsMatchKeywords() {
        assertEquals("wifi", SearchExtras.matchAndroidSettings("wlan").first().id)
        assertEquals("battery", SearchExtras.matchAndroidSettings("akk").first().id)
        assertTrue(SearchExtras.matchAndroidSettings("helligkeit").any { it.id == "display" })
        assertTrue(SearchExtras.matchAndroidSettings("nicht").any { it.id == "dnd" })
        assertTrue(SearchExtras.matchAndroidSettings("x").isEmpty())
    }
}
