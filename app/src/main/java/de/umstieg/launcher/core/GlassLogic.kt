package de.umstieg.launcher.core

/** Glas-Tönung und Erkennung heller Hintergrundbilder (reine Logik, getestet). */
object GlassLogic {
    /**
     * Deckkraft der Glasfläche zu einer Tönung 0..100. 50 % (Standard) ergibt eine deutlich sichtbare,
     * dunkle Fläche (ca. 0,41), auf hellen Fotos zusätzlich kräftiger.
     */
    fun fillAlpha(tint: Int, lightWallpaper: Boolean): Float {
        val t = tint.coerceIn(0, 100) / 100f
        val base = 0.10f + 0.62f * t
        return if (lightWallpaper) (base + 0.18f).coerceAtMost(0.85f) else base
    }

    /** Anteil Dunkel (0) bis Weiß (1) im Farbton: ganz durchsichtig wirkt hell, getönt dunkel. */
    fun whiteShare(tint: Int): Float = (1f - tint.coerceIn(0, 100) / 25f).coerceIn(0f, 1f)

    /**
     * Hell? Entweder sagt es das System (dunkle Schrift passt) oder die Hauptfarben sind hell. Fotos mit
     * hellem Himmel melden oft keinen Hinweis, deshalb zusätzlich die Helligkeit der Farben.
     */
    fun isLightWallpaper(hintDarkText: Boolean, primary: Float?, secondary: Float?, tertiary: Float?): Boolean {
        if (hintDarkText) return true
        val p = primary ?: return false
        val others = listOfNotNull(secondary, tertiary)
        val mean = (p * 2 + others.sum()) / (2 + others.size)
        return p > 0.5f || mean > 0.45f
    }
}
