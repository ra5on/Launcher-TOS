package de.umstieg.launcher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import de.umstieg.launcher.KlarApplication
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
import android.app.ActivityOptions
import android.graphics.Rect

/** Zustand des Homescreens: Bearbeitungsmodus, geöffneter Ordner, Aktionen auf dem Layout. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val controller = (application as KlarApplication).controller
    val data = controller.data
    val apps: StateFlow<Map<AppKey, AppInfo>> = controller.appsByKey
    val icons = controller.icons

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    private val _openFolder = MutableStateFlow<String?>(null)
    val openFolder: StateFlow<String?> = _openFolder.asStateFlow()

    private val _addSheet = MutableStateFlow(false)
    val addSheet: StateFlow<Boolean> = _addSheet.asStateFlow()

    private val _toFirstPage = MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Aufforderung, zu Seite 1 zu blättern (true = animiert). */
    val toFirstPage: SharedFlow<Boolean> = _toFirstPage.asSharedFlow()

    fun setEditMode(on: Boolean) {
        _editMode.value = on
        if (!on) _addSheet.value = false
    }

    fun openFolder(id: String?) {
        _openFolder.value = id
    }

    fun showAddSheet(show: Boolean) {
        _addSheet.value = show
    }

    /** Home-Taste: Panels schließen, zurück auf Seite 1. */
    fun onHomePressed(visible: Boolean) {
        _openFolder.value = null
        _addSheet.value = false
        _editMode.value = false
        _toFirstPage.tryEmit(visible)
    }

    /** Zurück: schließt nur Ordner, „+“-Liste oder Bearbeitungsmodus. */
    fun onBack() {
        when {
            _addSheet.value -> _addSheet.value = false
            _openFolder.value != null -> _openFolder.value = null
            _editMode.value -> _editMode.value = false
        }
    }

    fun launch(app: AppInfo, bounds: Rect?, options: ActivityOptions?): Boolean = controller.apps.launch(app, bounds, options)

    fun drop(from: Slot, target: DropTarget) = controller.drop(from, target)
    fun remove(slot: Slot) = controller.removeFromHome(slot)
    fun addApp(app: AppKey, page: Int) = controller.addApp(app, page)
    fun renameFolder(id: String, name: String) = controller.renameFolder(id, name)
    fun moveInFolder(id: String, from: Int, to: Int) = controller.moveInFolder(id, from, to)
    fun dismissDefaultHint() = controller.dismissDefaultHint()
}
