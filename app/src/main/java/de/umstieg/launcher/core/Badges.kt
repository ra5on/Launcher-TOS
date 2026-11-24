package de.umstieg.launcher.core

/** Eine Benachrichtigung, reduziert auf das, was für Badges zählt. */
data class NotificationFacts(
    val packageName: String,
    val userSerial: Long,
    val number: Int,
    val isGroupSummary: Boolean,
    val isOngoing: Boolean,
)

/** Zählt Badges je App (Paket + Profil) und summiert Ordner. */
object BadgeCounter {
    fun key(packageName: String, userSerial: Long) = "$packageName#$userSerial"

    /**
     * Laufende (z. B. Musik) und Gruppen-Zusammenfassungen zählen nicht; eine von der App gemeldete
     * Anzahl ([NotificationFacts.number] > 0) ersetzt die 1.
     */
    fun count(notifications: List<NotificationFacts>): Map<String, Int> =
        notifications.filter { !it.isOngoing && !it.isGroupSummary }
            .groupBy { key(it.packageName, it.userSerial) }
            .mapValues { (_, list) -> list.sumOf { if (it.number > 0) it.number else 1 } }

    fun forApp(counts: Map<String, Int>, app: AppKey, disabled: Set<String>): Int =
        if (app.packageName in disabled) 0 else counts[key(app.packageName, app.userSerial)] ?: 0

    /** Summe eines Elements; Ordner zeigen die Summe ihrer Apps. */
    fun forItem(counts: Map<String, Int>, item: HomeItem, disabled: Set<String>): Int =
        LayoutOps.appsOf(item).distinctBy { it.packageName to it.userSerial }.sumOf { forApp(counts, it, disabled) }

    /** Text für das Badge: über 99 → „99+“. */
    fun label(count: Int): String = if (count > 99) "99+" else count.toString()
}
