package de.umstieg.launcher.core

import kotlin.math.floor

/** Rechteck in Pixeln (Wurzel-Koordinaten der Homescreen-Ansicht). */
data class Area(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    fun contains(x: Float, y: Float): Boolean = x >= left && x < right && y >= top && y < bottom
}

/**
 * Trefferprüfung des Rasters in EINEM Koordinatenraum (Wurzel der Homescreen-Ansicht, wie Fingerpositionen
 * und boundsInRoot). Die Rasterfläche ist die Pager-Fläche ohne Seitenränder und ohne den unten
 * reservierten Platz für Seitenpunkte und Dock – genau die Fläche, in der das Raster gezeichnet wird.
 */
object GridHit {
    fun gridArea(pager: Area, sideMarginPx: Float, bottomReservedPx: Float): Area =
        Area(pager.left + sideMarginPx, pager.top, pager.right - sideMarginPx, (pager.bottom - bottomReservedPx).coerceAtLeast(pager.top + 1f))

    /** Zelle (Spalte, Reihe) an einem Punkt, oder null außerhalb des Rasters. */
    fun cellAt(area: Area, columns: Int, rows: Int, x: Float, y: Float): Pair<Int, Int>? {
        if (!area.contains(x, y) || columns <= 0 || rows <= 0) return null
        val cx = floor((x - area.left) / (area.width / columns)).toInt().coerceIn(0, columns - 1)
        val cy = floor((y - area.top) / (area.height / rows)).toInt().coerceIn(0, rows - 1)
        return cx to cy
    }
}
