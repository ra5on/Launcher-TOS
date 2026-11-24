package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutOpsTest {
    private val spec = GridSpec(columns = 2, rows = 2)
    private fun app(n: String) = AppKey("pkg.$n", "pkg.$n.Main", 0)
    private val folderFactory: (AppKey, AppKey) -> FolderItem = { a, b -> FolderItem("f1", "Ordner", listOf(a, b)) }

    private fun layoutOf(vararg apps: AppKey, dock: List<AppKey> = emptyList()): HomeLayout {
        var l = HomeLayout(dock = dock.map { AppItem(it) })
        apps.forEach { l = LayoutOps.addApp(l, spec, it) }
        return l
    }

    // ---------- freie Stelle ----------

    @Test
    fun findFreeCell_emptyLayout_isFirstCell() {
        assertEquals(Slot.Grid(0, 0, 0), LayoutOps.findFreeCell(HomeLayout(), spec))
    }

    @Test
    fun findFreeCell_fillsRowByRowThenNewPage() {
        val l = layoutOf(app("a"), app("b"), app("c"))
        assertEquals(Slot.Grid(0, 1, 1), LayoutOps.findFreeCell(l, spec))
        val full = LayoutOps.addApp(l, spec, app("d"))
        assertEquals(Slot.Grid(1, 0, 0), LayoutOps.findFreeCell(full, spec))
    }

    @Test
    fun findFreeCell_usesGapsFirst() {
        var l = layoutOf(app("a"), app("b"), app("c"))
        l = LayoutOps.removeFrom(l, Slot.Grid(0, 1, 0))
        assertEquals(Slot.Grid(0, 1, 0), LayoutOps.findFreeCell(l, spec))
    }

    @Test
    fun addApp_isIdempotent() {
        val l = layoutOf(app("a"))
        assertEquals(l, LayoutOps.addApp(l, spec, app("a")))
    }

    // ---------- verschieben ----------

    @Test
    fun drop_onEmptyCell_moves() {
        val l = layoutOf(app("a"))
        val r = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 0), DropTarget.Grid(0, 1, 1), true, folderFactory)
        assertNull(LayoutOps.itemAt(r, 0, 0, 0))
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(r, 0, 1, 1))
    }

    @Test
    fun drop_onOccupiedCellSamePage_reorders() {
        val l = layoutOf(app("a"), app("b"))
        val r = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 0), DropTarget.Grid(0, 1, 0), true, folderFactory)
        assertEquals(AppItem(app("b")), LayoutOps.itemAt(r, 0, 0, 0))
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(r, 0, 1, 0))
    }

    @Test
    fun drop_ontoNewPage_createsPage() {
        val l = layoutOf(app("a"), app("b"))
        val r = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 0), DropTarget.Grid(1, 0, 0), true, folderFactory)
        assertEquals(2, r.pages.size)
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(r, 1, 0, 0))
    }

    @Test
    fun drop_gridIntoDock_insertsWhileSpace() {
        val l = layoutOf(app("a"), dock = listOf(app("d1")))
        val r = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 0), DropTarget.Dock(0), true, folderFactory)
        assertEquals(listOf(AppItem(app("a")), AppItem(app("d1"))), r.dock)
        assertNull(LayoutOps.itemAt(r, 0, 0, 0))
    }

    @Test
    fun drop_gridIntoFullDock_pushesLastIntoGrid() {
        val l = layoutOf(app("a"), dock = listOf(app("d1"), app("d2")))
        val r = LayoutOps.drop(l, spec, 2, Slot.Grid(0, 0, 0), DropTarget.Dock(1), true, folderFactory)
        assertEquals(listOf(AppItem(app("d1")), AppItem(app("a"))), r.dock)
        assertEquals(AppItem(app("d2")), LayoutOps.itemAt(r, 0, 0, 0))
    }

    @Test
    fun drop_dockToGrid_movesOut() {
        val l = layoutOf(dock = listOf(app("d1"), app("d2")))
        val r = LayoutOps.drop(l, spec, 4, Slot.Dock(0), DropTarget.Grid(0, 1, 1), true, folderFactory)
        assertEquals(listOf(AppItem(app("d2"))), r.dock)
        assertEquals(AppItem(app("d1")), LayoutOps.itemAt(r, 0, 1, 1))
    }

    @Test
    fun drop_withinDock_reorders() {
        val l = layoutOf(dock = listOf(app("d1"), app("d2"), app("d3")))
        val r = LayoutOps.drop(l, spec, 4, Slot.Dock(0), DropTarget.Dock(2), true, folderFactory)
        assertEquals(listOf(app("d2"), app("d3"), app("d1")), r.dock.map { (it as AppItem).app })
    }

    // ---------- Ordner ----------

    @Test
    fun drop_appOnApp_createsFolder() {
        val l = layoutOf(app("a"), app("b"))
        val r = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        val folder = LayoutOps.itemAt(r, 0, 0, 0) as FolderItem
        assertEquals(listOf(app("a"), app("b")), folder.apps)
        assertNull(LayoutOps.itemAt(r, 0, 1, 0))
    }

    @Test
    fun drop_appOnApp_withFoldersDisabled_reorders() {
        val l = layoutOf(app("a"), app("b"))
        val r = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), false, folderFactory)
        assertEquals(AppItem(app("b")), LayoutOps.itemAt(r, 0, 0, 0))
    }

    @Test
    fun drop_appOnFolder_addsToFolder() {
        var l = layoutOf(app("a"), app("b"), app("c"))
        l = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        l = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 1), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        val folder = LayoutOps.itemAt(l, 0, 0, 0) as FolderItem
        assertEquals(listOf(app("a"), app("b"), app("c")), folder.apps)
    }

    @Test
    fun drop_folderOnApp_doesNotNest() {
        var l = layoutOf(app("a"), app("b"), app("c"))
        l = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        val r = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 0), DropTarget.Grid(0, 0, 1, merge = true), true, folderFactory)
        assertTrue(LayoutOps.itemAt(r, 0, 0, 1) is FolderItem)
        assertEquals(AppItem(app("c")), LayoutOps.itemAt(r, 0, 1, 0))
        assertEquals(1, LayoutOps.allItems(r).count { it is FolderItem })
    }

    @Test
    fun folder_dissolvesWhenSingleAppRemains() {
        var l = layoutOf(app("a"), app("b"))
        l = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        val r = LayoutOps.drop(l, spec, 4, Slot.InFolder("f1", 1), DropTarget.Grid(0, 1, 1), true, folderFactory)
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(r, 0, 0, 0))
        assertEquals(AppItem(app("b")), LayoutOps.itemAt(r, 0, 1, 1))
        assertNull(LayoutOps.findFolder(r, "f1"))
    }

    @Test
    fun removeFromFolder_lastButOne_dissolves() {
        var l = layoutOf(app("a"), app("b"))
        l = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        val r = LayoutOps.removeFromHome(l, spec, Slot.InFolder("f1", 0))
        assertEquals(AppItem(app("b")), LayoutOps.itemAt(r, 0, 0, 0))
        assertFalse(app("a") in LayoutOps.placedApps(r))
    }

    @Test
    fun removeFromHome_folder_keepsAllApps() {
        var l = layoutOf(app("a"), app("b"), app("c"))
        l = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        l = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 1), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        val r = LayoutOps.removeFromHome(l, spec, Slot.Grid(0, 0, 0))
        assertEquals(setOf(app("a"), app("b"), app("c")), LayoutOps.placedApps(r))
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(r, 0, 0, 0))
    }

    @Test
    fun renameFolder_changesName() {
        var l = layoutOf(app("a"), app("b"))
        l = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        val r = LayoutOps.renameFolder(l, "f1", "Werkzeuge")
        assertEquals("Werkzeuge", LayoutOps.findFolder(r, "f1")?.name)
    }

    // ---------- Auto-Add und Deinstallation ----------

    @Test
    fun reconcile_addsOnlyNewApps() {
        val l = layoutOf(app("a"))
        // b wurde per „–“ entfernt, ist aber bekannt; c ist neu.
        val known = setOf(app("a"), app("b"))
        val r = LayoutOps.reconcile(l, known, listOf(app("a"), app("b"), app("c")), emptySet(), spec, autoAdd = true)
        assertEquals(setOf(app("a"), app("c")), LayoutOps.placedApps(r.layout))
        assertEquals(listOf(app("c")), r.added)
        assertEquals(setOf(app("a"), app("b"), app("c")), r.known)
    }

    @Test
    fun reconcile_withoutAutoAdd_marksAsKnown() {
        val r = LayoutOps.reconcile(layoutOf(app("a")), setOf(app("a")), listOf(app("a"), app("n")), emptySet(), spec, autoAdd = false)
        assertEquals(setOf(app("a")), LayoutOps.placedApps(r.layout))
        assertTrue(app("n") in r.known)
    }

    @Test
    fun reconcile_newAppGoesToNewPageWhenFull() {
        val l = layoutOf(app("a"), app("b"), app("c"), app("d"))
        val known = setOf(app("a"), app("b"), app("c"), app("d"))
        val r = LayoutOps.reconcile(l, known, known.toList() + app("e"), emptySet(), spec, autoAdd = true)
        assertEquals(AppItem(app("e")), LayoutOps.itemAt(r.layout, 1, 0, 0))
    }

    @Test
    fun reconcile_uninstalledDisappearsEverywhere() {
        var l = layoutOf(app("a"), app("b"), app("c"), dock = listOf(app("x"), app("y")))
        l = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), true, folderFactory)
        val known = setOf(app("a"), app("b"), app("c"), app("x"), app("y"))
        // b (im Ordner), c (Raster) und x (Dock) sind deinstalliert.
        val r = LayoutOps.reconcile(l, known, listOf(app("a"), app("y")), emptySet(), spec, autoAdd = true)
        assertEquals(setOf(app("a"), app("y")), LayoutOps.placedApps(r.layout))
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(r.layout, 0, 0, 0)) // Ordner aufgelöst
        assertEquals(listOf(AppItem(app("y"))), r.layout.dock)
        assertEquals(setOf(app("a"), app("y")), r.known)
    }

    @Test
    fun reconcile_keepsTemporarilyUnavailablePackages() {
        val l = layoutOf(app("a"), app("sd"))
        val r = LayoutOps.reconcile(l, setOf(app("a"), app("sd")), listOf(app("a")), setOf("pkg.sd"), spec, autoAdd = true)
        assertTrue(app("sd") in LayoutOps.placedApps(r.layout))
        assertTrue(app("sd") in r.known)
    }

    @Test
    fun reconcile_reinstalledAppIsNewAgain() {
        val l = layoutOf(app("a"))
        val first = LayoutOps.reconcile(l, setOf(app("a"), app("b")), listOf(app("a")), emptySet(), spec, true)
        val second = LayoutOps.reconcile(first.layout, first.known, listOf(app("a"), app("b")), emptySet(), spec, true)
        assertEquals(listOf(app("b")), second.added)
    }

    @Test
    fun initialLayout_putsDockAppsInDockAndRestInGrid() {
        val apps = listOf(app("a"), app("b"), app("c"), app("d"), app("e"), app("f"))
        val l = LayoutOps.initialLayout(apps, listOf(app("b"), app("zz")), spec, 4)
        assertEquals(listOf(AppItem(app("b"))), l.dock)
        assertEquals(2, l.pages.size)
        assertEquals(setOf(app("a"), app("b"), app("c"), app("d"), app("e"), app("f")), LayoutOps.placedApps(l))
    }

    // ---------- Raster verkleinern, Seiten ----------

    @Test
    fun normalize_shrinkMovesOverflowWithoutLoss() {
        val big = GridSpec(4, 4)
        var l = HomeLayout()
        val apps = (0 until 16).map { app("a$it") }
        apps.forEach { l = LayoutOps.addApp(l, big, it) }
        val r = LayoutOps.normalize(l, GridSpec(3, 3), 4)
        assertEquals(apps.toSet(), LayoutOps.placedApps(r))
        assertEquals(2, r.pages.size)
        r.pages.forEach { page ->
            page.forEach { assertTrue(it.x < 3 && it.y < 3) }
            assertEquals(page.size, page.map { it.x to it.y }.toSet().size)
        }
    }

    @Test
    fun normalize_keepsItemsInsideGridInPlace() {
        val l = layoutOf(app("a"), app("b"))
        assertEquals(l, LayoutOps.normalize(l, spec, 4))
    }

    @Test
    fun normalize_dockOverflowGoesToGrid() {
        val l = layoutOf(dock = listOf(app("d1"), app("d2"), app("d3")))
        val r = LayoutOps.normalize(l, spec, 2)
        assertEquals(2, r.dock.size)
        assertEquals(AppItem(app("d3")), LayoutOps.itemAt(r, 0, 0, 0))
    }

    @Test
    fun normalize_removesTrailingEmptyPagesButKeepsFirst() {
        val l = HomeLayout(pages = listOf(emptyList(), emptyList(), emptyList()))
        assertEquals(1, LayoutOps.normalize(l, spec, 4).pages.size)
        val withApp = HomeLayout(pages = listOf(emptyList(), listOf(Placed(0, 0, AppItem(app("a")))), emptyList()))
        assertEquals(2, LayoutOps.normalize(withApp, spec, 4).pages.size)
    }

    @Test
    fun normalize_removesDuplicatesAndCollisions() {
        val l = HomeLayout(
            pages = listOf(
                listOf(
                    Placed(0, 0, AppItem(app("a"))),
                    Placed(0, 0, AppItem(app("b"))),
                    Placed(1, 0, AppItem(app("a"))),
                    Placed(1, 1, FolderItem("f", "Ordner", listOf(app("a"), app("c")))),
                ),
            ),
        )
        val r = LayoutOps.normalize(l, spec, 4)
        assertEquals(setOf(app("a"), app("b"), app("c")), LayoutOps.placedApps(r))
        assertEquals(3, LayoutOps.allItems(r).size) // Ordner mit nur c aufgelöst
    }
}
