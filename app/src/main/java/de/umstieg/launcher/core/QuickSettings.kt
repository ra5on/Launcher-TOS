package de.umstieg.launcher.core

import kotlinx.serialization.Serializable

/** Elemente des Schnellzugriffs. */
@Serializable
enum class QsTile {
    WIFI, BLUETOOTH, MOBILE_DATA, AIRPLANE, HOTSPOT, NFC, LOCATION,
    MEDIA, BRIGHTNESS, VOLUME,
    FLASHLIGHT, DND, ROTATION, BATTERY_SAVER,
    CALCULATOR, CAMERA, ALARM, QR, SYSTEM_PANEL,
}

/** Größe eines Elements: S = Viertel, M = halbe, L = volle Breite. */
@Serializable
enum class QsSize(val units: Int) { S(1), M(2), L(4) }

@Serializable
data class QsItem(val tile: QsTile, val size: QsSize = QsSize.S)

@Serializable
data class QsPage(val id: String, val name: String, val items: List<QsItem>)

/** Seiten des Schnellzugriffs (Tabs). Leerer Name = Standardname aus den String-Ressourcen (nach id). */
@Serializable
data class QsLayout(val pages: List<QsPage>) {
    companion object {
        val DEFAULT = QsLayout(
            listOf(
                QsPage(
                    "control", "",
                    listOf(
                        QsItem(QsTile.WIFI), QsItem(QsTile.BLUETOOTH), QsItem(QsTile.MOBILE_DATA), QsItem(QsTile.AIRPLANE),
                        QsItem(QsTile.MEDIA, QsSize.L), QsItem(QsTile.BRIGHTNESS, QsSize.L), QsItem(QsTile.VOLUME, QsSize.L),
                        QsItem(QsTile.FLASHLIGHT), QsItem(QsTile.DND), QsItem(QsTile.CALCULATOR), QsItem(QsTile.CAMERA),
                        QsItem(QsTile.ALARM), QsItem(QsTile.ROTATION), QsItem(QsTile.BATTERY_SAVER), QsItem(QsTile.QR),
                    ),
                ),
                QsPage(
                    "connections", "",
                    listOf(
                        QsItem(QsTile.WIFI, QsSize.L), QsItem(QsTile.BLUETOOTH, QsSize.L), QsItem(QsTile.MOBILE_DATA, QsSize.M),
                        QsItem(QsTile.HOTSPOT, QsSize.M), QsItem(QsTile.AIRPLANE, QsSize.M), QsItem(QsTile.NFC, QsSize.M),
                        QsItem(QsTile.LOCATION, QsSize.M), QsItem(QsTile.SYSTEM_PANEL, QsSize.M),
                    ),
                ),
                QsPage("media", "", listOf(QsItem(QsTile.MEDIA, QsSize.L), QsItem(QsTile.VOLUME, QsSize.L))),
            ),
        )
    }
}

/** Reine Logik für das Bearbeiten des Schnellzugriffs. */
object QsOps {
    const val ROW_UNITS = 4

    /** Teilt Elemente in Zeilen zu je 4 Einheiten (S = 1, M = 2, L = 4); Reihenfolge bleibt. */
    fun packRows(items: List<QsItem>): List<List<QsItem>> {
        val rows = mutableListOf<MutableList<QsItem>>()
        var used = ROW_UNITS
        for (item in items) {
            if (used + item.size.units > ROW_UNITS) {
                rows += mutableListOf<QsItem>()
                used = 0
            }
            rows.last() += item
            used += item.size.units
        }
        return rows
    }

    private fun QsLayout.mapPage(pageId: String, f: (QsPage) -> QsPage) =
        copy(pages = pages.map { if (it.id == pageId) f(it) else it })

    fun addItem(l: QsLayout, pageId: String, tile: QsTile, size: QsSize = defaultSize(tile)): QsLayout =
        l.mapPage(pageId) { p -> if (p.items.any { it.tile == tile }) p else p.copy(items = p.items + QsItem(tile, size)) }

    fun removeItem(l: QsLayout, pageId: String, index: Int): QsLayout =
        l.mapPage(pageId) { p -> p.copy(items = p.items.filterIndexed { i, _ -> i != index }) }

    fun moveItem(l: QsLayout, pageId: String, from: Int, to: Int): QsLayout = l.mapPage(pageId) { p ->
        if (from !in p.items.indices) return@mapPage p
        val items = p.items.toMutableList()
        val it = items.removeAt(from)
        items.add(to.coerceIn(0, items.size), it)
        p.copy(items = items)
    }

    fun setSize(l: QsLayout, pageId: String, index: Int, size: QsSize): QsLayout = l.mapPage(pageId) { p ->
        p.copy(items = p.items.mapIndexed { i, it -> if (i == index) it.copy(size = size) else it })
    }

    fun available(l: QsLayout, pageId: String): List<QsTile> {
        val used = l.pages.firstOrNull { it.id == pageId }?.items?.map { it.tile }.orEmpty().toSet()
        return QsTile.entries.filter { it !in used }
    }

    fun addPage(l: QsLayout, id: String, name: String): QsLayout = l.copy(pages = l.pages + QsPage(id, name, emptyList()))

    /** Mindestens eine Seite bleibt. */
    fun removePage(l: QsLayout, pageId: String): QsLayout =
        if (l.pages.size <= 1) l else l.copy(pages = l.pages.filterNot { it.id == pageId })

    fun movePage(l: QsLayout, from: Int, to: Int): QsLayout {
        if (from !in l.pages.indices) return l
        val pages = l.pages.toMutableList()
        val p = pages.removeAt(from)
        pages.add(to.coerceIn(0, pages.size), p)
        return l.copy(pages = pages)
    }

    fun renamePage(l: QsLayout, pageId: String, name: String): QsLayout = l.mapPage(pageId) { it.copy(name = name) }

    fun defaultSize(tile: QsTile): QsSize = when (tile) {
        QsTile.MEDIA, QsTile.BRIGHTNESS, QsTile.VOLUME -> QsSize.L
        else -> QsSize.S
    }

    /** Erlaubte Größen je Element (Regler brauchen Breite). */
    fun allowedSizes(tile: QsTile): List<QsSize> = when (tile) {
        QsTile.BRIGHTNESS, QsTile.VOLUME -> listOf(QsSize.M, QsSize.L)
        QsTile.MEDIA -> listOf(QsSize.M, QsSize.L)
        else -> QsSize.entries
    }
}
