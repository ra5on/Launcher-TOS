package de.umstieg.launcher.ui.home

import android.app.ActivityOptions
import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import de.umstieg.launcher.TosApplication
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.DropTarget
import de.umstieg.launcher.core.Slot
import de.umstieg.launcher.data.AppInfo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Vollbild-Ebenen über dem Homescreen. */
enum class Overlay { NONE, SEARCH, NOTIFICATIONS, QUICK_SETTINGS }

/** Wohin ein neues Widget soll. */
sealed interface WidgetTarget {
    data object Page : WidgetTarget
    data class Grid(val page: Int) : WidgetTarget
}

/** Zustand des Homescreens: Bearbeitungsmodus, Ordner, Overlays, Menüs; Aktionen auf dem Layout. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val controller = (application as TosApplication).controller
    val data = controller.data
    val apps: StateFlow<Map<AppKey, AppInfo>> = controller.appsByKey
    val icons = controller.icons
    val badges = controller.badgeCounts
    val media = controller.media
    val system = controller.system
    val widgets = controller.widgets

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    private val _openFolder = MutableStateFlow<String?>(null)
    val openFolder: StateFlow<String?> = _openFolder.asStateFlow()

    private val _addSheet = MutableStateFlow(false)
    val addSheet: StateFlow<Boolean> = _addSheet.asStateFlow()

    private val _overlay = MutableStateFlow(Overlay.NONE)
    val overlay: StateFlow<Overlay> = _overlay.asStateFlow()

    /** Lange gedrückte App (Menü). */
    private val _menuApp = MutableStateFlow<AppInfo?>(null)
    val menuApp: StateFlow<AppInfo?> = _menuApp.asStateFlow()

    /** App, deren Name/Icon angepasst wird. */
    private val _customize = MutableStateFlow<AppInfo?>(null)
    val customize: StateFlow<AppInfo?> = _customize.asStateFlow()

    /** Widget-Auswahl offen für dieses Ziel. */
    private val _widgetPicker = MutableStateFlow<WidgetTarget?>(null)
    val widgetPicker: StateFlow<WidgetTarget?> = _widgetPicker.asStateFlow()

    /** Widget-Seite im Bearbeiten-Modus. */
    private val _widgetEdit = MutableStateFlow(false)
    val widgetEdit: StateFlow<Boolean> = _widgetEdit.asStateFlow()

    /** Pausiertes Profil: App, die gestartet werden sollte. */
    private val _quietApp = MutableStateFlow<AppInfo?>(null)
    val quietApp: StateFlow<AppInfo?> = _quietApp.asStateFlow()

    /** Grid-Widget, dessen Größe gerade geändert wird. */
    private val _resizeWidget = MutableStateFlow<Int?>(null)
    val resizeWidget: StateFlow<Int?> = _resizeWidget.asStateFlow()

    /** Einrichtung erneut zeigen (Karte „Einrichtung fortsetzen“). */
    private val _setupRequested = MutableStateFlow(false)
    val setupRequested: StateFlow<Boolean> = _setupRequested.asStateFlow()

    fun showSetup(show: Boolean) {
        _setupRequested.value = show
    }

    private val _toFirstPage = MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Aufforderung, zur ersten Homescreen-Seite zu blättern (true = animiert). */
    val toFirstPage: SharedFlow<Boolean> = _toFirstPage.asSharedFlow()

    private val _toDrawer = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Aufforderung, zur App-Übersicht zu blättern. */
    val toDrawer: SharedFlow<Unit> = _toDrawer.asSharedFlow()

    fun setEditMode(on: Boolean) {
        _editMode.value = on
        if (!on) {
            _addSheet.value = false
            _resizeWidget.value = null
        }
    }

    fun openFolder(id: String?) {
        _openFolder.value = id
    }

    fun showAddSheet(show: Boolean) {
        _addSheet.value = show
    }

    fun showOverlay(o: Overlay) {
        _openFolder.value = null
        _overlay.value = o
    }

    fun showMenu(app: AppInfo?) {
        _menuApp.value = app
    }

    fun showCustomize(app: AppInfo?) {
        _customize.value = app
    }

    fun showWidgetPicker(target: WidgetTarget?) {
        _widgetPicker.value = target
    }

    fun setWidgetEdit(on: Boolean) {
        _widgetEdit.value = on
    }

    fun showQuietDialog(app: AppInfo?) {
        _quietApp.value = app
    }

    fun showResize(widgetId: Int?) {
        _resizeWidget.value = widgetId
    }

    fun goToDrawer() {
        _overlay.value = Overlay.NONE
        _toDrawer.tryEmit(Unit)
    }

    /** Home-Taste: alles schließen, zurück auf Seite 1. */
    fun onHomePressed(visible: Boolean) {
        _openFolder.value = null
        _addSheet.value = false
        _editMode.value = false
        _overlay.value = Overlay.NONE
        _menuApp.value = null
        _customize.value = null
        _widgetPicker.value = null
        _widgetEdit.value = false
        _resizeWidget.value = null
        _toFirstPage.tryEmit(visible)
    }

    /** Zurück: schließt das oberste Panel; sonst nichts. */
    fun onBack() {
        when {
            _customize.value != null -> _customize.value = null
            _menuApp.value != null -> _menuApp.value = null
            _widgetPicker.value != null -> _widgetPicker.value = null
            _resizeWidget.value != null -> _resizeWidget.value = null
            _addSheet.value -> _addSheet.value = false
            _overlay.value != Overlay.NONE -> _overlay.value = Overlay.NONE
            _openFolder.value != null -> _openFolder.value = null
            _widgetEdit.value -> _widgetEdit.value = false
            _editMode.value -> _editMode.value = false
        }
    }

    fun launch(app: AppInfo, bounds: Rect?, options: ActivityOptions?): Boolean {
        if (app.quiet) {
            _quietApp.value = app
            return true
        }
        return controller.apps.launch(app, bounds, options)
    }

    fun drop(from: Slot, target: DropTarget) = controller.drop(from, target)
    fun remove(slot: Slot) = controller.removeFromHome(slot)
    fun addApp(app: AppKey, page: Int) = controller.addApp(app, page)
    fun renameFolder(id: String, name: String) = controller.renameFolder(id, name)
    fun moveInFolder(id: String, from: Int, to: Int) = controller.moveInFolder(id, from, to)
    fun dismissDefaultHint() = controller.dismissDefaultHint()
}
