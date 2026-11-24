package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class RelativeTimeTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private fun at(h: Int, m: Int, day: Int = 24) = ZonedDateTime.of(2026, 11, day, h, m, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun formats() {
        val now = at(14, 0)
        assertEquals(RelativeTime.Now, RelativeTime.of(now - 20_000, now, zone))
        assertEquals(RelativeTime.Minutes(5), RelativeTime.of(at(13, 55), now, zone))
        assertEquals(RelativeTime.Hours(2), RelativeTime.of(at(11, 30), now, zone))
        assertEquals(RelativeTime.Yesterday, RelativeTime.of(at(23, 0, day = 23), now, zone))
        assertEquals(RelativeTime.Date(LocalDate.of(2026, 11, 20)), RelativeTime.of(at(9, 0, day = 20), now, zone))
        // Uhr in der Zukunft (Zeitumstellung, Fehler der App): nicht negativ.
        assertEquals(RelativeTime.Now, RelativeTime.of(now + 60_000, now, zone))
    }
}
