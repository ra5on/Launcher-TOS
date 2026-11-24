package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GridHitTest {
    // Pixel-ähnlich: Pager unter Statusleiste und Kopfzeile bis Bildschirmende, unten 420 px für Punkte + Dock.
    private val pager = Area(0f, 200f, 1080f, 2316f)
    private val area = GridHit.gridArea(pager, sideMarginPx = 42f, bottomReservedPx = 420f)

    @Test
    fun areaExcludesReservedBottomAndMargins() {
        assertEquals(Area(42f, 200f, 1038f, 1896f), area)
    }

    @Test
    fun eachRowHitsItsOwnCell() {
        val rows = 6
        val cellH = area.height / rows // 282,67 px
        for (r in 0 until rows) {
            // Mitte jeder Zelle und knapp über ihrer Unterkante gehören zu genau dieser Reihe.
            val mid = area.top + cellH * (r + 0.5f)
            val nearBottom = area.top + cellH * (r + 1) - 2f
            assertEquals(1 to r, GridHit.cellAt(area, 4, rows, 300f, mid))
            assertEquals(1 to r, GridHit.cellAt(area, 4, rows, 300f, nearBottom))
        }
    }

    @Test
    fun oldPagerAreaWouldPickRowAbove() {
        // Fehler bis 1.2.1: Fläche bis zur Pager-Unterkante → zu hohe Zellen → unterste App eine Reihe zu hoch.
        val wrong = Area(42f, 200f, 1038f, 2316f)
        val bottomRowCentre = area.top + area.height / 6 * 5.5f
        assertEquals(4, GridHit.cellAt(wrong, 4, 6, 300f, bottomRowCentre)!!.second)
        assertEquals(5, GridHit.cellAt(area, 4, 6, 300f, bottomRowCentre)!!.second)
    }

    @Test
    fun outsideIsNull() {
        assertNull(GridHit.cellAt(area, 4, 6, 20f, 500f)) // Seitenrand
        assertNull(GridHit.cellAt(area, 4, 6, 300f, 1950f)) // Bereich von Punkten/Dock
        assertNull(GridHit.cellAt(area, 4, 6, 300f, 150f)) // über dem Raster
    }
}
