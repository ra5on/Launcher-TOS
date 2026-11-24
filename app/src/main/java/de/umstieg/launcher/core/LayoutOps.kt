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

    fun itemAt(layout: HomeLayout, page: Int, x: Int, y: Int): HomeItem? =
        layout.pages.getOrNull(page)?.firstOrNull { it.x == x && it.y == y }?.item

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
    fun findFreeCell(layout: HomeLayout, spec: GridSpec, startPage: Int = 0): Slot.Grid {
        val from = startPage.coerceAtLeast(0)
        for (p in from until layout.pages.size) {
            val used = layout.pages[p].mapTo(HashSet()) { it.y * spec.columns + it.x }
            for (cell in 0 until spec.cellsPerPage) {
                if (cell !in used) return Slot.Grid(p, cell % spec.columns, cell / spec.columns)
            }
        }
        return Slot.Grid(maxOf(layout.pages.size, from), 0, 0)
    }

    // ---------- Grundoperationen ----------

    /** Setzt [item] auf die Zelle (überschreibt einen evtl. vorhandenen Eintrag), legt fehlende Seiten an. */
    fun setAt(layout: HomeLayout, page: Int, x: Int, y: Int, item: HomeItem): HomeLayout {
        val pages = layout.pages.toMutableList()
        while (pages.size <= page) pages.add(emptyList())
        pages[page] = pages[page].filterNot { it.x == x && it.y == y } + Placed(x, y, item)
        return layout.copy(pages = pages)
    }

    private fun clearCell(layout: HomeLayout, page: Int, x: Int, y: Int): HomeLayout {
        if (page !in layout.pages.indices) return layout
        val pages = layout.pages.toMutableList()
        pages[page] = pages[page].filterNot { it.x == x && it.y == y }
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
     * - sonst belegt: tauschen (aus einem Ordner heraus weicht das Ziel auf die nächste freie Stelle aus)
     * - Dock: einfügen, solange Platz ist; voll: tauschen.
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

        if (target.merge && allowFolders && dragged is AppItem && targetItem != null && targetItem.id != dragged.id) {
            val merged = when (targetItem) {
                is AppItem -> newFolder(targetItem.app, dragged.app)
                is FolderItem -> targetItem.copy(apps = targetItem.apps + dragged.app)
            }
            return replaceById(removeFrom(layout, from), targetItem.id, merged)
        }

        return when (target) {
            is DropTarget.Grid -> dropOnGrid(layout, spec, from, target, dragged, targetItem)
            is DropTarget.Dock -> dropOnDock(layout, spec, dockCapacity, from, target, dragged, targetItem)
        }
    }

    private fun dropOnGrid(
        layout: HomeLayout,
        spec: GridSpec,
        from: Slot,
        target: DropTarget.Grid,
        dragged: HomeItem,
        targetItem: HomeItem?,
    ): HomeLayout {
        if (targetItem == null) {
            return setAt(removeFrom(layout, from), target.page, target.x, target.y, dragged)
        }
        return when (from) {
            is Slot.Grid -> {
                val l = setAt(layout, from.page, from.x, from.y, targetItem)
                setAt(l, target.page, target.x, target.y, dragged)
            }
            is Slot.Dock -> {
                val dock = layout.dock.toMutableList().apply { set(from.index, targetItem) }
                setAt(layout.copy(dock = dock), target.page, target.x, target.y, dragged)
            }
            is Slot.InFolder -> {
                var l = removeFrom(layout, from)
                l = setAt(l, target.page, target.x, target.y, dragged)
                val free = findFreeCell(l, spec, target.page)
                setAt(l, free.page, free.x, free.y, targetItem)
            }
        }
    }

    private fun dropOnDock(
        layout: HomeLayout,
        spec: GridSpec,
        dockCapacity: Int,
        from: Slot,
        target: DropTarget.Dock,
        dragged: HomeItem,
        targetItem: HomeItem?,
    ): HomeLayout {
        if (from is Slot.Dock) {
            val dock = layout.dock.toMutableList()
            val item = dock.removeAt(from.index)
            dock.add(target.index.coerceIn(0, dock.size), item)
            return layout.copy(dock = dock)
        }
        if (layout.dock.size < dockCapacity) {
            val l = removeFrom(layout, from)
            val dock = l.dock.toMutableList()
            dock.add(target.index.coerceIn(0, dock.size), dragged)
            return l.copy(dock = dock)
        }
        if (targetItem == null) return layout
        return when (from) {
            is Slot.Grid -> {
                val l = setAt(layout, from.page, from.x, from.y, targetItem)
                l.copy(dock = l.dock.toMutableList().apply { set(target.index, dragged) })
            }
            is Slot.InFolder -> {
                var l = removeFrom(layout, from)
                val idx = l.dock.indexOfFirst { it.id == targetItem.id }
                l = l.copy(dock = l.dock.toMutableList().apply { set(idx, dragged) })
                val free = findFreeCell(l, spec, 0)
                setAt(l, free.page, free.x, free.y, targetItem)
            }
            is Slot.Dock -> layout
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
    ): ReconcileResult {
        val installedSet = installed.toSet()
        fun keep(k: AppKey) = k in installedSet || k.packageName in keepPackages
        var l = removeApps(layout) { !keep(it) }
        val placed = placedApps(l)
        val added = installed.filter { it !in known && it !in placed }
        if (autoAdd) added.forEach { l = addApp(l, spec, it, 0) }
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
    fun normalize(layout: HomeLayout, spec: GridSpec, dockCapacity: Int): HomeLayout {
        val seen = HashSet<AppKey>()
        fun dedupe(item: HomeItem): HomeItem? = when (item) {
            is AppItem -> if (seen.add(item.app)) item else null
            is FolderItem -> collapse(item.copy(apps = item.apps.filter { seen.add(it) }))
        }

        val dockItems = layout.dock.mapNotNull { dedupe(it) }
        val overflow = mutableListOf<Pair<Int, HomeItem>>()
        dockItems.drop(dockCapacity.coerceAtLeast(0)).forEach { overflow += 0 to it }

        val pages = layout.pages.mapIndexed { p, page ->
            val used = HashSet<Int>()
            val kept = mutableListOf<Placed>()
            page.sortedWith(compareBy({ it.y }, { it.x })).forEach { placed ->
                val item = dedupe(placed.item) ?: return@forEach
                val inside = placed.x in 0 until spec.columns && placed.y in 0 until spec.rows
                if (inside && used.add(placed.y * spec.columns + placed.x)) {
                    kept += placed.copy(item = item)
                } else {
                    overflow += p to item
                }
            }
            kept.toList()
        }

        var l = HomeLayout(pages = pages.ifEmpty { listOf(emptyList()) }, dock = dockItems.take(dockCapacity.coerceAtLeast(0)))
        for ((page, item) in overflow) {
            val free = findFreeCell(l, spec, page)
            l = setAt(l, free.page, free.x, free.y, item)
        }
        return trimTrailingEmptyPages(l)
    }

    fun trimTrailingEmptyPages(layout: HomeLayout): HomeLayout {
        val pages = layout.pages.toMutableList()
        while (pages.size > 1 && pages.last().isEmpty()) pages.removeAt(pages.lastIndex)
        if (pages.isEmpty()) pages.add(emptyList())
        return layout.copy(pages = pages)
    }
}
