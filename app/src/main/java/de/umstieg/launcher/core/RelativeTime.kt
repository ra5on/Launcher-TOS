package de.umstieg.launcher.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

/** Zeitangabe einer Benachrichtigung, unabhängig von der Systemsprache (Texte kommen aus den Ressourcen). */
sealed interface RelativeTime {
    data object Now : RelativeTime
    data class Minutes(val n: Int) : RelativeTime
    data class Hours(val n: Int) : RelativeTime
    data object Yesterday : RelativeTime
    data class Date(val date: LocalDate) : RelativeTime

    companion object {
        /** „jetzt“ (< 1 Min.), „vor n Min.“, „vor n Std.“ (heute), „gestern“, sonst Datum. */
        fun of(postMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): RelativeTime {
            val diff = max(0L, nowMillis - postMillis)
            val minutes = (diff / 60_000L).toInt()
            val post = Instant.ofEpochMilli(postMillis).atZone(zone).toLocalDate()
            val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            return when {
                minutes < 1 -> Now
                minutes < 60 -> Minutes(minutes)
                post == today -> Hours(minutes / 60)
                post == today.minusDays(1) -> Yesterday
                else -> Date(post)
            }
        }
    }
}
