package de.umstieg.launcher.core

/** Reine Logik der Widget-Seite. */
object WidgetOps {
    val ROWS = 1..6

    fun add(page: List<WidgetEntry>, entry: WidgetEntry): List<WidgetEntry> =
        if (page.any { it.widgetId == entry.widgetId }) page else page + entry.copy(rows = entry.rows.coerceIn(ROWS))

    fun remove(page: List<WidgetEntry>, widgetId: Int): List<WidgetEntry> = page.filterNot { it.widgetId == widgetId }

    fun move(page: List<WidgetEntry>, widgetId: Int, delta: Int): List<WidgetEntry> {
        val i = page.indexOfFirst { it.widgetId == widgetId }
        if (i < 0) return page
        val list = page.toMutableList()
        val e = list.removeAt(i)
        list.add((i + delta).coerceIn(0, list.size), e)
        return list
    }

    fun resize(page: List<WidgetEntry>, widgetId: Int, rows: Int): List<WidgetEntry> =
        page.map { if (it.widgetId == widgetId) it.copy(rows = rows.coerceIn(ROWS)) else it }

    /** Zeilen aus der Mindesthöhe des Anbieters (≈ 80 dp je Zeile). */
    fun rowsFor(minHeightDp: Int): Int = ((minHeightDp + 79) / 80).coerceIn(ROWS)

    /** Zellen aus Mindestbreite/-höhe für das Homescreen-Raster (Zelle ≈ 74 × 80 dp). */
    fun spanFor(minWidthDp: Int, minHeightDp: Int, spec: GridSpec): Pair<Int, Int> =
        ((minWidthDp + 73) / 74).coerceIn(1, spec.columns) to ((minHeightDp + 79) / 80).coerceIn(1, spec.rows)

    /** Alle Widget-IDs, die das Layout noch verwendet (Widget-Seite + Raster). */
    fun usedIds(data: LauncherData): Set<Int> =
        data.widgetPage.mapTo(HashSet()) { it.widgetId } +
            LayoutOps.allItems(data.layout).filterIsInstance<WidgetItem>().map { it.widgetId }
}
