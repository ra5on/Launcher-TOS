package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Einschieben, Widgets mit Spannweite, feste Seitenanzahl, Auto-Add-Ziel, Widget-Seite. */
class Phase2LayoutTest {
    private val spec = GridSpec(columns = 3, rows = 2)
    private fun app(n: String) = AppKey("pkg.$n", "pkg.$n.Main", 0)
    private val noFolder: (AppKey, AppKey) -> FolderItem = { a, b -> FolderItem("f", "Ordner", listOf(a, b)) }

    private fun layoutOf(vararg apps: AppKey, dock: List<AppKey> = emptyList()): HomeLayout {
        var l = HomeLayout(dock = dock.map { AppItem(it) })
        apps.forEach { l = LayoutOps.addApp(l, spec, it) }
        return l
    }

    @Test
    fun insert_fromDock_shiftsFollowingIcons() {
        val l = layoutOf(app("a"), app("b"), app("c"), dock = listOf(app("d")))
        val r = LayoutOps.drop(l, spec, 4, Slot.Dock(0), DropTarget.Grid(0, 1, 0), true, noFolder)
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(r, 0, 0, 0))
        assertEquals(AppItem(app("d")), LayoutOps.itemAt(r, 0, 1, 0))
        assertEquals(AppItem(app("b")), LayoutOps.itemAt(r, 0, 2, 0))
        assertEquals(AppItem(app("c")), LayoutOps.itemAt(r, 0, 0, 1))
    }

    @Test
    fun insert_onFullPage_overflowsToNextPage() {
        val apps = (1..6).map { app("a$it") }
        val l = layoutOf(*apps.toTypedArray(), dock = listOf(app("d")))
        val r = LayoutOps.drop(l, spec, 4, Slot.Dock(0), DropTarget.Grid(0, 0, 0), true, noFolder)
        assertEquals(AppItem(app("d")), LayoutOps.itemAt(r, 0, 0, 0))
        assertEquals(AppItem(app("a6")), LayoutOps.itemAt(r, 1, 0, 0))
        assertEquals((apps + app("d")).toSet(), LayoutOps.placedApps(r))
    }

    @Test
    fun reorder_backwardsOnSamePage() {
        val l = layoutOf(app("a"), app("b"), app("c"))
        val r = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 2, 0), DropTarget.Grid(0, 0, 0), true, noFolder)
        assertEquals(listOf("c", "a", "b"), (0..2).map { (LayoutOps.itemAt(r, 0, it, 0) as AppItem).app.packageName.removePrefix("pkg.") })
    }

    @Test
    fun dock_insertWithSpace_doesNotPush() {
        val l = layoutOf(app("a"), dock = listOf(app("d1")))
        val r = LayoutOps.drop(l, spec, 3, Slot.Grid(0, 0, 0), DropTarget.Dock(0), true, noFolder)
        assertEquals(listOf(AppItem(app("a")), AppItem(app("d1"))), r.dock)
    }

    @Test
    fun widget_occupiesSpanAndIsSkippedBySearch() {
        var l = LayoutOps.addWidget(HomeLayout(), spec, WidgetItem(7, "p/W", 2, 2))
        assertTrue(LayoutOps.itemAt(l, 0, 1, 1) is WidgetItem)
        l = LayoutOps.addApp(l, spec, app("a"))
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(l, 0, 2, 0))
        assertEquals(Slot.Grid(0, 2, 1), LayoutOps.findFreeCell(l, spec))
        assertEquals(Slot.Grid(1, 0, 0), LayoutOps.findFreeArea(l, spec, 2, 1))
    }

    @Test
    fun widget_moveOnlyToFreeArea() {
        var l = LayoutOps.addWidget(HomeLayout(), spec, WidgetItem(7, "p/W", 2, 1))
        l = LayoutOps.addApp(l, spec, app("a")) // (2,0)
        val blocked = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 0), DropTarget.Grid(0, 1, 0), true, noFolder)
        assertEquals(l, blocked)
        val moved = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 0), DropTarget.Grid(0, 0, 1), true, noFolder)
        assertTrue(LayoutOps.itemAt(moved, 0, 1, 1) is WidgetItem)
        assertNull(LayoutOps.itemAt(moved, 0, 0, 0))
    }

    @Test
    fun widget_neverMergesOrGoesToDock() {
        var l = LayoutOps.addWidget(HomeLayout(), spec, WidgetItem(7, "p/W", 1, 1))
        l = LayoutOps.addApp(l, spec, app("a"))
        assertEquals(l, LayoutOps.drop(l, spec, 4, Slot.Grid(0, 0, 0), DropTarget.Dock(0), true, noFolder))
        val r = LayoutOps.drop(l, spec, 4, Slot.Grid(0, 1, 0), DropTarget.Grid(0, 0, 0, merge = true), true, noFolder)
        assertTrue(LayoutOps.allItems(r).none { it is FolderItem })
    }

    @Test
    fun insert_skipsWidgetCells() {
        var l = layoutOf(app("a"))
        l = LayoutOps.setAt(l, 0, 1, 0, WidgetItem(3, "p/W", 1, 1))
        l = LayoutOps.setAt(l, 0, 2, 0, AppItem(app("b")))
        val r = LayoutOps.insertAt(l, spec, 0, 0, 0, AppItem(app("n")))
        assertEquals(AppItem(app("n")), LayoutOps.itemAt(r, 0, 0, 0))
        assertTrue(LayoutOps.itemAt(r, 0, 1, 0) is WidgetItem)
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(r, 0, 2, 0))
        assertEquals(AppItem(app("b")), LayoutOps.itemAt(r, 0, 0, 1))
    }

    @Test
    fun resizeWidget_onlyIfFree() {
        var l = LayoutOps.addWidget(HomeLayout(), spec, WidgetItem(7, "p/W", 1, 1))
        l = LayoutOps.setAt(l, 0, 2, 0, AppItem(app("a")))
        val ok = LayoutOps.resizeWidget(l, spec, 7, 2, 2)
        assertEquals(2, (LayoutOps.itemAt(ok, 0, 0, 0) as WidgetItem).spanX)
        assertEquals(l, LayoutOps.resizeWidget(l, spec, 7, 3, 1))
    }

    @Test
    fun normalize_widgetTooWideIsClampedAndCollisionsMove() {
        val l = HomeLayout(
            pages = listOf(
                listOf(
                    Placed(0, 0, WidgetItem(1, "p/W", 5, 1)),
                    Placed(1, 0, AppItem(app("a"))),
                ),
            ),
        )
        val r = LayoutOps.normalize(l, spec, 4)
        assertEquals(3, (LayoutOps.itemAt(r, 0, 0, 0) as WidgetItem).spanX)
        assertEquals(AppItem(app("a")), LayoutOps.itemAt(r, 0, 0, 1))
    }

    @Test
    fun normalize_keepsFixedPageCount() {
        val r = LayoutOps.normalize(HomeLayout(), spec, 4, minPages = 3)
        assertEquals(3, r.pages.size)
        val trimmed = LayoutOps.normalize(HomeLayout(pages = List(5) { emptyList() }), spec, 4, minPages = 2)
        assertEquals(2, trimmed.pages.size)
    }

    @Test
    fun reconcile_addToLastPage() {
        var l = layoutOf(app("a"))
        l = l.copy(pages = l.pages + listOf(listOf(Placed(0, 0, AppItem(app("b"))))))
        val r = LayoutOps.reconcile(l, setOf(app("a"), app("b")), listOf(app("a"), app("b"), app("n")), emptySet(), spec, true, addToLastPage = true)
        assertEquals(AppItem(app("n")), LayoutOps.itemAt(r.layout, 1, 1, 0))
    }

    @Test
    fun reconcile_keepsWidgets() {
        val l = LayoutOps.addWidget(layoutOf(app("a")), spec, WidgetItem(4, "p/W", 1, 1))
        val r = LayoutOps.reconcile(l, setOf(app("a")), emptyList(), emptySet(), spec, true)
        assertEquals(1, LayoutOps.allItems(r.layout).size)
    }

    @Test
    fun widgetPage_addMoveResizeRemove() {
        var p = WidgetOps.add(emptyList(), WidgetEntry(1, "p/A", 2))
        p = WidgetOps.add(p, WidgetEntry(2, "p/B", 9))
        assertEquals(6, p[1].rows)
        p = WidgetOps.move(p, 2, -1)
        assertEquals(listOf(2, 1), p.map { it.widgetId })
        p = WidgetOps.resize(p, 1, 0)
        assertEquals(1, p.first { it.widgetId == 1 }.rows)
        p = WidgetOps.remove(p, 2)
        assertEquals(listOf(1), p.map { it.widgetId })
        assertEquals(p, WidgetOps.add(p, WidgetEntry(1, "p/A")))
    }

    @Test
    fun widget_sizesFromProviderMinimums() {
        assertEquals(2, WidgetOps.rowsFor(110))
        assertEquals(1, WidgetOps.rowsFor(40))
        assertEquals(3 to 2, WidgetOps.spanFor(200, 110, GridSpec(4, 6)))
        assertEquals(4 to 1, WidgetOps.spanFor(900, 10, GridSpec(4, 6)))
    }

    @Test
    fun usedWidgetIds_coverPageAndGrid() {
        val data = LauncherData(
            layout = LayoutOps.addWidget(HomeLayout(), spec, WidgetItem(5, "p/W")),
            widgetPage = listOf(WidgetEntry(9, "p/X")),
        )
        assertEquals(setOf(5, 9), WidgetOps.usedIds(data))
        assertFalse(3 in WidgetOps.usedIds(data))
    }
}
