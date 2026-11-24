package de.umstieg.launcher.ui.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.umstieg.launcher.core.DropTarget
import de.umstieg.launcher.core.HomeItem
import de.umstieg.launcher.core.Slot
import kotlin.math.floor

/** Zustand eines laufenden Ziehvorgangs (Overlay über dem Pager). */
@Stable
class DragState {
    var item by mutableStateOf<HomeItem?>(null)
        private set
    var from by mutableStateOf<Slot?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
    var target by mutableStateOf<DropTarget?>(null)

    /** Abstand Finger ↔ Icon-Mitte beim Start, damit das Icon nicht unter dem Finger springt. */
    var grab = Offset.Zero
        private set

    /** Verweilen über einem Icon (Ordner erst nach kurzer Pause). */
    val dwell = de.umstieg.launcher.core.MergeDwell()

    /** Ziel, über dem gerade verweilt wird (noch kein Ordner); die Vorschau schiebt dann nichts weg. */
    var holdKey by mutableStateOf<String?>(null)

    /** Mitte des gezogenen Icons (Finger minus Griff-Abstand). */
    val iconCenter: Offset get() = pointer - grab

    /** Die aktuelle Geste begann mit langem Drücken (Tipp beim Loslassen ignorieren). */
    var longPressed = false

    val active: Boolean get() = item != null

    fun start(item: HomeItem, from: Slot, pointer: Offset, iconCenter: Offset) {
        this.item = item
        this.from = from
        this.pointer = pointer
        this.grab = pointer - iconCenter
        this.target = null
        dwell.reset()
        holdKey = null
    }

    fun clear() {
        item = null
        from = null
        target = null
        dwell.reset()
        holdKey = null
    }
}

/** Geometrie des Rasters in Wurzel-Koordinaten (für Trefferprüfung). */
class GridGeometry(
    val area: Rect,
    val columns: Int,
    val rows: Int,
    /** Verschiebung der Icon-Mitte gegenüber der Zellmitte (Beschriftung darunter). */
    val iconCenterDy: Float,
    val iconPx: Float,
) {
    val cellW: Float get() = area.width / columns
    val cellH: Float get() = area.height / rows

    fun cellAt(p: Offset): Pair<Int, Int>? {
        if (!area.contains(p)) return null
        val x = floor((p.x - area.left) / cellW).toInt().coerceIn(0, columns - 1)
        val y = floor((p.y - area.top) / cellH).toInt().coerceIn(0, rows - 1)
        return x to y
    }

    fun iconCenter(x: Int, y: Int): Offset =
        Offset(area.left + (x + 0.5f) * cellW, area.top + (y + 0.5f) * cellH + iconCenterDy)

    fun cellRect(x: Int, y: Int): Rect = Rect(area.left + x * cellW, area.top + y * cellH, area.left + (x + 1) * cellW, area.top + (y + 1) * cellH)

    /** Mittig auf einem Icon (für Ordner). */
    fun isOnIcon(p: Offset, x: Int, y: Int): Boolean {
        val c = iconCenter(x, y)
        return de.umstieg.launcher.core.MergeDwell.isOver(p.x - c.x, p.y - c.y, iconPx)
    }
}

/** Geometrie des Docks: Elemente zentriert in [capacity] gleich breiten Plätzen. */
class DockGeometry(val area: Rect, val capacity: Int, val count: Int, val iconPx: Float) {
    private val slotW: Float get() = area.width / capacity.coerceAtLeast(1)
    private val start: Float get() = area.left + (area.width - count * slotW) / 2f

    fun center(index: Int): Offset = Offset(start + (index + 0.5f) * slotW, area.top + iconPx / 2f)

    /** Einfügeposition 0..count. */
    fun indexAt(p: Offset): Int = floor((p.x - start) / slotW).toInt().coerceIn(0, count)

    fun isOnIcon(p: Offset, index: Int): Boolean {
        if (index !in 0 until count) return false
        val c = center(index)
        return de.umstieg.launcher.core.MergeDwell.isOver(p.x - c.x, p.y - c.y, iconPx)
    }
}

