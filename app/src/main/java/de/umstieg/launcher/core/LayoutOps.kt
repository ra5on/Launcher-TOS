package de.umstieg.launcher.core

/** Herkunft eines gezogenen Elements. */
sealed interface Slot {
    data class Grid(val page: Int, val x: Int, val y: Int) : Slot
    data class Dock(val index: Int) : Slot
    data class InFolder(val folderId: String, val index: Int) : Slot
}

/** Ablageziel. [merge] = mittig auf einem Element abgelegt (Ordner bilden bzw. hinzufügen). */
sealed interface DropTarget {
    val merge: Boolean

    data class Grid(val page: Int, val x: Int, val y: Int, override val merge: Boolean = false) : DropTarget
    data class Dock(val index: Int, override val merge: Boolean = false) : DropTarget
}

/**
 * Reine Layout-Logik ohne Android-Abhängigkeiten: freie Stelle finden, Verschieben, Ordner,
 * Auto-Add, Entfernen deinstallierter Apps, Raster verkleinern. Alle Funktionen sind unveränderlich
 * (geben ein neues Layout zurück).
 */
object LayoutOps {

    // ---------- Abfragen ----------

    /** Zellen, die ein Element belegt (Widgets mehrere, sonst eine). */
    fun spanOf(item: HomeItem): Pair<Int, Int> = when (item) {
        is WidgetItem -> item.spanX.coerceAtLeast(1) to item.spanY.coerceAtLeast(1)
        else -> 1 to 1
    }

    fun covers(p: Placed, x: Int, y: Int): Boolean {
        val (w, h) = spanOf(p.item)
        return x >= p.x && x < p.x + w && y >= p.y && y < p.y + h
    }

    fun placedAt(layout: HomeLayout, page: Int, x: Int, y: Int): Placed? =
        layout.pages.getOrNull(page)?.firstOrNull { covers(it, x, y) }

    fun itemAt(layout: HomeLayout, page: Int, x: Int, y: Int): HomeItem? = placedAt(layout, page, x, y)?.item

    /** Ist der Bereich frei und innerhalb des Rasters? [ignoreId] zählt nicht als belegt. */
    fun isAreaFree(page: List<Placed>, spec: GridSpec, x: Int, y: Int, w: Int, h: Int, ignoreId: String? = null): Boolean {
        if (x < 0 || y < 0 || x + w > spec.columns || y + h > spec.rows) return false
        return page.none { p ->
            if (p.item.id == ignoreId) return@none false
            val (pw, ph) = spanOf(p.item)
            p.x < x + w && x < p.x + pw && p.y < y + h && y < p.y + ph
        }
    }

    /** Nächster freier Bereich w × h ab [startPage]; sonst neue Seite. */
    fun findFreeArea(layout: HomeLayout, spec: GridSpec, w: Int, h: Int, startPage: Int = 0): Slot.Grid {
        val from = startPage.coerceAtLeast(0)
        for (p in from until layout.pages.size) {
            for (cell in 0 until spec.cellsPerPage) {
                val x = cell % spec.columns
                val y = cell / spec.columns
                if (isAreaFree(layout.pages[p], spec, x, y, w, h)) return Slot.Grid(p, x, y)
            }
        }
        return Slot.Grid(maxOf(layout.pages.size, from), 0, 0)
    }

    fun itemAt(layout: HomeLayout, slot: Slot): HomeItem? = when (slot) {
        is Slot.Grid -> itemAt(layout, slot.page, slot.x, slot.y)
        is Slot.Dock -> layout.dock.getOrNull(slot.index)
        is Slot.InFolder -> findFolder(layout, slot.folderId)?.apps?.getOrNull(slot.index)?.let { AppItem(it) }
    }

    fun findFolder(layout: HomeLayout, folderId: String): FolderItem? =
        allItems(layout).filterIsInstance<FolderItem>().firstOrNull { it.folderId == folderId }

    fun allItems(layout: HomeLayout): List<HomeItem> = layout.dock + layout.pages.flatten().map { it.item }

    /** Alle Apps, die irgendwo (Raster, Ordner, Dock) liegen. */
    fun placedApps(layout: HomeLayout): Set<AppKey> = allItems(layout).flatMapTo(LinkedHashSet()) { appsOf(it) }

    fun appsOf(item: HomeItem): List<AppKey> = when (item) {
        is AppItem -> listOf(item.app)
        is FolderItem -> item.apps
        is WidgetItem -> emptyList()
    }

    /** Position eines Elements im Raster oder Dock. */
    fun locate(layout: HomeLayout, itemId: String): Slot? {
        val d = layout.dock.indexOfFirst { it.id == itemId }
        if (d >= 0) return Slot.Dock(d)
        layout.pages.forEachIndexed { p, page ->
            page.firstOrNull { it.item.id == itemId }?.let { return Slot.Grid(p, it.x, it.y) }
        }
        return null
    }

    /** Nächste freie Zelle ab [startPage] (zeilenweise); ist alles voll, die erste Zelle einer neuen Seite. */
    fun findFreeCell(layout: HomeLayout, spec: GridSpec, startPage: Int = 0): Slot.Grid = findFreeArea(layout, spec, 1, 1, startPage)

    // ---------- Grundoperationen ----------

    /** Setzt [item] ab der Zelle (verdrängt, was den Bereich überlappt), legt fehlende Seiten an. */
    fun setAt(layout: HomeLayout, page: Int, x: Int, y: Int, item: HomeItem): HomeLayout {
        val pages = layout.pages.toMutableList()
        while (pages.size <= page) pages.add(emptyList())
        val (w, h) = spanOf(item)
        pages[page] = pages[page].filterNot { p ->
            val (pw, ph) = spanOf(p.item)
            p.x < x + w && x < p.x + pw && p.y < y + h && y < p.y + ph
        } + Placed(x, y, item)
        return layout.copy(pages = pages)
    }

    private fun clearCell(layout: HomeLayout, page: Int, x: Int, y: Int): HomeLayout {
        if (page !in layout.pages.indices) return layout
        val pages = layout.pages.toMutableList()
        pages[page] = pages[page].filterNot { covers(it, x, y) }
        return layout.copy(pages = pages)
    }

    /** Ersetzt das Element mit [itemId] (Raster oder Dock) durch [replacement]; null entfernt es. */
    fun replaceById(layout: HomeLayout, itemId: String, replacement: HomeItem?): HomeLayout {
        val dock = layout.dock.mapNotNull { if (it.id == itemId) replacement else it }
        val pages = layout.pages.map { page ->
            page.mapNotNull { if (it.item.id == itemId) replacement?.let { r -> it.copy(item = r) } else it }
        }
        return HomeLayout(pages, dock)
    }

    /** Ordner mit weniger als zwei Apps auflösen: eine App bleibt an seiner Stelle, leer verschwindet er. */
    private fun collapse(folder: FolderItem): HomeItem? = when (folder.apps.size) {
        0 -> null
        1 -> AppItem(folder.apps.single())
        else -> folder
    }

    /** Nimmt das Element an [slot] heraus (ohne es irgendwo abzulegen). */
    fun removeFrom(layout: HomeLayout, slot: Slot): HomeLayout = when (slot) {
        is Slot.Grid -> clearCell(layout, slot.page, slot.x, slot.y)
        is Slot.Dock -> if (slot.index in layout.dock.indices) {
            layout.copy(dock = layout.dock.toMutableList().apply { removeAt(slot.index) })
        } else {
            layout
        }
        is Slot.InFolder -> {
            val folder = findFolder(layout, slot.folderId)
            if (folder == null || slot.index !in folder.apps.indices) {
                layout
            } else {
                val apps = folder.apps.toMutableList().apply { removeAt(slot.index) }
                replaceById(layout, folder.id, collapse(folder.copy(apps = apps)))
            }
        }
    }

    // ---------- Ziehen & Ablegen ----------

    /**
     * Legt das Element von [from] auf [target] ab.
     * - leere Zelle: verschieben
     * - mittig auf App/Ordner (merge) und Ordner erlaubt: Ordner bilden bzw. App hinzufügen
     *   (keine verschachtelten Ordner: ein Ordner wird nie in einen Ordner gelegt)
     * - sonst belegt: einschieben – die folgenden Icons rücken eine Zelle weiter (Widgets bleiben stehen),
     *   was nicht mehr auf die Seite passt, rückt auf die nächste freie Stelle der folgenden Seiten
     * - Dock: einschieben; ist es voll, wird das letzte Dock-Element ins Raster verdrängt
     *   (auf die frei gewordene Zelle, sonst die nächste freie Stelle)
     * - Widgets: nur auf einen freien Bereich im Raster, nie ins Dock oder in Ordner.
     */
    fun drop(
        layout: HomeLayout,
        spec: GridSpec,
        dockCapacity: Int,
        from: Slot,
        target: DropTarget,
        allowFolders: Boolean,
        newFolder: (AppKey, AppKey) -> FolderItem,
    ): HomeLayout {
        val dragged = itemAt(layout, from) ?: return layout
        if (from is Slot.Grid && target is DropTarget.Grid &&
            from.page == target.page && from.x == target.x && from.y == target.y
        ) return layout
        if (from is Slot.Dock && target is DropTarget.Dock && from.index == target.index) return layout

        val targetItem: HomeItem? = when (target) {
            is DropTarget.Grid -> itemAt(layout, target.page, target.x, target.y)
            is DropTarget.Dock -> layout.dock.getOrNull(target.index)
        }
        // App aus einem Ordner auf den eigenen Ordner gezogen: nichts tun.
        if (from is Slot.InFolder && targetItem is FolderItem && targetItem.folderId == from.folderId) return layout

        if (dragged is WidgetItem) {
            if (target !is DropTarget.Grid || from !is Slot.Grid) return layout
            val (w, h) = spanOf(dragged)
            val tx = target.x.coerceIn(0, (spec.columns - w).coerceAtLeast(0))
            val ty = target.y.coerceIn(0, (spec.rows - h).coerceAtLeast(0))
            val page = layout.pages.getOrNull(target.page).orEmpty()
            if (!isAreaFree(page, spec, tx, ty, w, h, ignoreId = dragged.id)) return layout
            return setAt(removeFrom(layout, from), target.page, tx, ty, dragged)
        }

        if (target.merge && allowFolders && dragged is AppItem && targetItem != null && targetItem !is WidgetItem &&
            targetItem.id != dragged.id
        ) {
            val merged = when (targetItem) {
                is AppItem -> newFolder(targetItem.app, dragged.app)
                is FolderItem -> targetItem.copy(apps = targetItem.apps + dragged.app)
                is WidgetItem -> return layout
            }
            return replaceById(removeFrom(layout, from), targetItem.id, merged)
        }

        return when (target) {
            is DropTarget.Grid -> dropOnGrid(layout, spec, from, target, dragged)
            is DropTarget.Dock -> dropOnDock(layout, spec, dockCapacity, from, target, dragged)
        }
    }

    private fun dropOnGrid(layout: HomeLayout, spec: GridSpec, from: Slot, target: DropTarget.Grid, dragged: HomeItem): HomeLayout {
        val occupant = itemAt(layout, target.page, target.x, target.y)
        if (from is Slot.Grid && from.page == target.page && occupant != null && occupant !is WidgetItem) {
            return reorderWithinPage(layout, spec, target.page, from.y * spec.columns + from.x, target.y * spec.columns + target.x, dragged)
        }
        return insertAt(removeFrom(layout, from), spec, target.page, target.x, target.y, dragged)
    }

    /**
     * Umsortieren auf derselben Seite: die Zellen zwischen Start und Ziel rücken um eine Zelle
     * in Richtung der frei gewordenen Startzelle (Widgets bleiben stehen), das Element landet am Ziel.
     */
    private fun reorderWithinPage(layout: HomeLayout, spec: GridSpec, page: Int, fromIdx: Int, toIdx: Int, dragged: HomeItem): HomeLayout {
        var l = removeFrom(layout, Slot.Grid(page, fromIdx % spec.columns, fromIdx / spec.columns))
        val range = if (fromIdx < toIdx) fromIdx..toIdx else toIdx..fromIdx
        val cells = range.filter { c -> itemAt(l, page, c % spec.columns, c / spec.columns) !is WidgetItem }
        val contents = cells.map { c -> itemAt(l, page, c % spec.columns, c / spec.columns) }
        cells.forEach { c -> l = clearCell(l, page, c % spec.columns, c / spec.columns) }
        val rotated: List<HomeItem?> = if (fromIdx < toIdx) contents.drop(1) + dragged else listOf<HomeItem?>(dragged) + contents.dropLast(1)
        cells.zip(rotated).forEach { (c, item) -> if (item != null) l = setAt(l, page, c % spec.columns, c / spec.columns, item) }
        return l
    }

    /**
     * Fügt [item] (1 × 1) an der Zelle ein. Ist sie belegt, rücken die folgenden Elemente in
     * Lesereihenfolge je eine Zelle weiter bis zur nächsten Lücke; Widgets werden übersprungen.
     * Was am Seitenende übrig bleibt, wandert auf die nächste freie Stelle ab der Folgeseite.
     */
    fun insertAt(layout: HomeLayout, spec: GridSpec, page: Int, x: Int, y: Int, item: HomeItem): HomeLayout {
        var l = layout
        val start = y * spec.columns + x
        val onWidget = placedAt(l, page, x, y)?.item is WidgetItem
        if (onWidget) {
            val free = findFreeCell(l, spec, page)
            return setAt(l, free.page, free.x, free.y, item)
        }
        var carry = item
        for (cell in start until spec.cellsPerPage) {
            val cx = cell % spec.columns
            val cy = cell / spec.columns
            val occupant = placedAt(l, page, cx, cy)
            if (occupant?.item is WidgetItem) continue
            l = setAt(clearCell(l, page, cx, cy), page, cx, cy, carry)
            if (occupant == null) return l
            carry = occupant.item
        }
        val free = findFreeCell(l, spec, page + 1)
        return setAt(l, free.page, free.x, free.y, carry)
    }

    private fun dropOnDock(
        layout: HomeLayout,
        spec: GridSpec,
        dockCapacity: Int,
        from: Slot,
        target: DropTarget.Dock,
        dragged: HomeItem,
    ): HomeLayout {
        if (dockCapacity <= 0) return layout
        if (from is Slot.Dock) {
            val dock = layout.dock.toMutableList()
            val item = dock.removeAt(from.index)
            dock.add(target.index.coerceIn(0, dock.size), item)
            return layout.copy(dock = dock)
        }
        var l = removeFrom(layout, from)
        val dock = l.dock.toMutableList()
        dock.add(target.index.coerceIn(0, dock.size), dragged)
        if (dock.size <= dockCapacity) return l.copy(dock = dock)
        // Voll: das letzte Element wird ins Raster verdrängt.
        val pushed = dock.removeAt(dock.lastIndex)
        l = l.copy(dock = dock)
        val freed = from as? Slot.Grid
        return if (freed != null && itemAt(l, freed.page, freed.x, freed.y) == null) {
            setAt(l, freed.page, freed.x, freed.y, pushed)
        } else {
            val free = findFreeCell(l, spec, 0)
            setAt(l, free.page, free.x, free.y, pushed)
        }
    }

    // ---------- Entfernen, Hinzufügen ----------

    /**
     * „–“ im Bearbeitungsmodus: App vom Homescreen nehmen (nicht deinstallieren).
     * Ein Ordner wird aufgelöst, seine Apps bleiben erhalten (erste an seiner Stelle, der Rest auf freien Zellen).
     */
    fun removeFromHome(layout: HomeLayout, spec: GridSpec, slot: Slot): HomeLayout {
        val item = itemAt(layout, slot) ?: return layout
        if (item !is FolderItem || slot is Slot.InFolder) return removeFrom(layout, slot)
        // Ordner auflösen: Apps bleiben erhalten.
        val first = AppItem(item.apps.first())
        var l = replaceById(layout, item.id, first)
        val startPage = (slot as? Slot.Grid)?.page ?: 0
        for (app in item.apps.drop(1)) {
            val free = findFreeCell(l, spec, startPage)
            l = setAt(l, free.page, free.x, free.y, AppItem(app))
        }
        return l
    }

    /** App auf die nächste freie Stelle ab [startPage] legen (falls sie noch nirgends liegt). */
    fun addApp(layout: HomeLayout, spec: GridSpec, app: AppKey, startPage: Int = 0): HomeLayout {
        if (app in placedApps(layout)) return layout
        val free = findFreeCell(layout, spec, startPage)
        return setAt(layout, free.page, free.x, free.y, AppItem(app))
    }

    /** Widget auf den nächsten freien Bereich ab [startPage] legen. */
    fun addWidget(layout: HomeLayout, spec: GridSpec, widget: WidgetItem, startPage: Int = 0): HomeLayout {
        val w = widget.copy(spanX = widget.spanX.coerceIn(1, spec.columns), spanY = widget.spanY.coerceIn(1, spec.rows))
        val free = findFreeArea(layout, spec, w.spanX, w.spanY, startPage)
        return setAt(layout, free.page, free.x, free.y, w)
    }

    /** Widget-Größe ändern, sofern der neue Bereich frei ist. */
    fun resizeWidget(layout: HomeLayout, spec: GridSpec, widgetId: Int, spanX: Int, spanY: Int): HomeLayout {
        val slot = locate(layout, "widget:$widgetId") as? Slot.Grid ?: return layout
        val w = itemAt(layout, slot) as? WidgetItem ?: return layout
        val resized = w.copy(spanX = spanX.coerceIn(1, spec.columns), spanY = spanY.coerceIn(1, spec.rows))
        if (!isAreaFree(layout.pages[slot.page], spec, slot.x, slot.y, resized.spanX, resized.spanY, w.id)) return layout
        return replaceById(layout, w.id, resized)
    }

    fun renameFolder(layout: HomeLayout, folderId: String, name: String): HomeLayout {
        val folder = findFolder(layout, folderId) ?: return layout
        return replaceById(layout, folder.id, folder.copy(name = name))
    }

    /** Ordnerinhalt umsortieren. */
    fun moveInFolder(layout: HomeLayout, folderId: String, from: Int, to: Int): HomeLayout {
        val folder = findFolder(layout, folderId) ?: return layout
        if (from !in folder.apps.indices) return layout
        val apps = folder.apps.toMutableList()
        val app = apps.removeAt(from)
        apps.add(to.coerceIn(0, apps.size), app)
        return replaceById(layout, folder.id, folder.copy(apps = apps))
    }

    /** Entfernt Apps überall (Raster, Ordner, Dock); Ordner mit nur einer App lösen sich auf. */
    fun removeApps(layout: HomeLayout, remove: (AppKey) -> Boolean): HomeLayout {
        fun clean(item: HomeItem): HomeItem? = when (item) {
            is AppItem -> if (remove(item.app)) null else item
            is FolderItem -> collapse(item.copy(apps = item.apps.filterNot(remove)))
            is WidgetItem -> item
        }
        return HomeLayout(
            pages = layout.pages.map { page -> page.mapNotNull { p -> clean(p.item)?.let { p.copy(item = it) } } },
            dock = layout.dock.mapNotNull { clean(it) },
        )
    }

    // ---------- Abgleich mit den installierten Apps ----------

    data class ReconcileResult(val layout: HomeLayout, val known: Set<AppKey>, val added: List<AppKey>)

    /**
     * Gleicht das Layout mit den installierten Apps ab.
     * - Deinstallierte Apps verschwinden aus Raster, Ordnern und Dock und aus der Menge bekannter Apps
     *   (eine Neuinstallation gilt wieder als neu).
     * - Pakete aus [keepPackages] (z. B. vorübergehend nicht verfügbar) bleiben unangetastet.
     * - Nur wirklich neue Apps (nicht in [known]) werden bei [autoAdd] auf die nächste freie Stelle gelegt;
     *   per „–“ entfernte Apps bleiben bekannt und kommen nicht zurück.
     * [installed] ist bereits in Anzeigereihenfolge sortiert.
     */
    fun reconcile(
        layout: HomeLayout,
        known: Set<AppKey>,
        installed: List<AppKey>,
        keepPackages: Set<String>,
        spec: GridSpec,
        autoAdd: Boolean,
        addToLastPage: Boolean = false,
    ): ReconcileResult {
        val installedSet = installed.toSet()
        fun keep(k: AppKey) = k in installedSet || k.packageName in keepPackages
        var l = removeApps(layout) { !keep(it) }
        val placed = placedApps(l)
        val added = installed.filter { it !in known && it !in placed }
        if (autoAdd) added.forEach { l = addApp(l, spec, it, if (addToLastPage) l.pages.lastIndex else 0) }
        val newKnown = installedSet + known.filter { it.packageName in keepPackages }
        return ReconcileResult(l, newKnown, if (autoAdd) added else emptyList())
    }

    /** Erstes Layout: [dockApps] ins Dock, alle übrigen Apps der Reihe nach ins Raster. */
    fun initialLayout(installed: List<AppKey>, dockApps: List<AppKey>, spec: GridSpec, dockCapacity: Int): HomeLayout {
        val dock = dockApps.distinct().filter { it in installed }.take(dockCapacity)
        var l = HomeLayout(pages = listOf(emptyList()), dock = dock.map { AppItem(it) })
        installed.filter { it !in dock }.forEach { l = addApp(l, spec, it, 0) }
        return l
    }

    // ---------- Normalisieren ----------

    /**
     * Bringt ein Layout in einen gültigen Zustand für [spec] und [dockCapacity]:
     * doppelte Apps entfernen, Ordner unter zwei Apps auflösen, Elemente außerhalb des Rasters
     * (z. B. nach dem Verkleinern) und überzählige Dock-Elemente auf die nächste freie Stelle bzw.
     * neue Seiten legen (nichts geht verloren), leere Seiten am Ende entfernen (Seite 1 bleibt).
     */
    fun normalize(layout: HomeLayout, spec: GridSpec, dockCapacity: Int, minPages: Int = 1): HomeLayout {
        val seen = HashSet<AppKey>()
        val seenWidgets = HashSet<Int>()
        fun dedupe(item: HomeItem): HomeItem? = when (item) {
            is AppItem -> if (seen.add(item.app)) item else null
            is FolderItem -> collapse(item.copy(apps = item.apps.filter { seen.add(it) }))
            is WidgetItem -> if (seenWidgets.add(item.widgetId)) {
                item.copy(spanX = item.spanX.coerceIn(1, spec.columns), spanY = item.spanY.coerceIn(1, spec.rows))
            } else {
                null
            }
        }

        // Widgets gehören nicht ins Dock.
        val dockItems = layout.dock.mapNotNull { dedupe(it) }.filterNot { it is WidgetItem }
        val overflow = mutableListOf<Pair<Int, HomeItem>>()
        dockItems.drop(dockCapacity.coerceAtLeast(0)).forEach { overflow += 0 to it }

        val pages = layout.pages.mapIndexed { p, page ->
            val kept = mutableListOf<Placed>()
            page.sortedWith(compareBy({ it.y }, { it.x })).forEach { placed ->
                val item = dedupe(placed.item) ?: return@forEach
                val (w, h) = spanOf(item)
                if (isAreaFree(kept, spec, placed.x, placed.y, w, h)) {
                    kept += placed.copy(item = item)
                } else {
                    overflow += p to item
                }
            }
            kept.toList()
        }

        var l = HomeLayout(pages = pages.ifEmpty { listOf(emptyList()) }, dock = dockItems.take(dockCapacity.coerceAtLeast(0)))
        for ((page, item) in overflow) {
            val (w, h) = spanOf(item)
            val free = findFreeArea(l, spec, w, h, page)
            l = setAt(l, free.page, free.x, free.y, item)
        }
        return trimTrailingEmptyPages(l, minPages)
    }

    /** Leere Seiten am Ende entfernen; mindestens [minPages] Seiten bleiben (feste Seitenanzahl). */
    fun trimTrailingEmptyPages(layout: HomeLayout, minPages: Int = 1): HomeLayout {
        val keep = minPages.coerceAtLeast(1)
        val pages = layout.pages.toMutableList()
        while (pages.size > keep && pages.last().isEmpty()) pages.removeAt(pages.lastIndex)
        while (pages.size < keep) pages.add(emptyList())
        return layout.copy(pages = pages)
    }
}
