package de.umstieg.launcher.core

/**
 * Verweilen über einem Icon beim Ziehen: Erst wenn das gezogene Icon [delayMs] lang über demselben Ziel
 * bleibt, gilt das Ziel als Ordner-Wunsch. Bis dahin soll nichts ausweichen (sonst flieht das Ziel-Icon).
 */
class MergeDwell(private val delayMs: Long = DEFAULT_MS) {
    /** Aktuelles Ziel (z. B. „g:Seite:x:y“ oder „d:Index“), null = über keinem Icon. */
    var key: String? = null
        private set
    private var since = 0L

    /** Meldet die aktuelle Position; liefert true, sobald lange genug verweilt wurde. */
    fun update(newKey: String?, nowMs: Long): Boolean {
        if (newKey == null) {
            key = null
            return false
        }
        if (newKey != key) {
            key = newKey
            since = nowMs
            return false
        }
        return nowMs - since >= delayMs
    }

    /** Noch nicht bereit, aber über einem Icon (Vorschau nicht verschieben). */
    fun holding(nowMs: Long): Boolean = key != null && nowMs - since < delayMs

    fun reset() {
        key = null
    }

    companion object {
        const val DEFAULT_MS = 400L

        /** Liegt die Mitte des gezogenen Icons nah genug an der Mitte des Ziel-Icons? */
        fun isOver(dx: Float, dy: Float, iconPx: Float, radius: Float = RADIUS): Boolean = dx * dx + dy * dy < (iconPx * radius) * (iconPx * radius)

        /** Anteil der Icongröße (Abstand der Icon-Mitten), ab dem das gezogene Icon „über“ dem Ziel liegt. */
        const val RADIUS = 0.62f
    }
}
