package de.umstieg.launcher.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.umstieg.launcher.TosApplication
import de.umstieg.launcher.R
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.core.LayoutOps
import de.umstieg.launcher.data.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Einstellungen: liest und schreibt denselben prozessweiten Speicher wie der Homescreen. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = (application as TosApplication).controller
    val data = controller.data
    val icons = controller.icons

    /** Apps für die Vorschau in Homescreen-Reihenfolge (Dock zuletzt), sonst alphabetisch. */
    val previewApps: StateFlow<List<AppInfo>> = combine(controller.data, controller.appsByKey) { d, apps ->
        val ordered = d?.let { data ->
            val grid = data.layout.pages.flatMap { page -> page.sortedWith(compareBy({ it.y }, { it.x })) }
                .flatMap { LayoutOps.appsOf(it.item) }
            val dock = data.layout.dock.flatMap { LayoutOps.appsOf(it) }
            (grid + dock).mapNotNull { apps[it] }
        }.orEmpty()
        ordered.ifEmpty { apps.values.toList() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /** Kurze Rückmeldungen (String-Ressourcen) für Toasts. */
    val messages = _messages.receiveAsFlow()

    fun update(block: (LauncherSettings) -> LauncherSettings) = controller.updateSettings(block)

    fun export(uri: Uri) = viewModelScope.launch {
        val ok = runCatching {
            val json = controller.exportJson()
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.encodeToByteArray()) }
            }
        }.isSuccess
        _messages.send(if (ok) R.string.backup_export_done else R.string.backup_export_failed)
    }

    fun import(uri: Uri) = viewModelScope.launch {
        val ok = runCatching {
            val text = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)!!.use { input ->
                    // Schutz vor riesigen Dateien: ein Layout ist wenige KB groß.
                    val bytes = input.readNBytesCompat(MAX_IMPORT_BYTES + 1)
                    require(bytes.size <= MAX_IMPORT_BYTES)
                    bytes.decodeToString()
                }
            }
            controller.importJson(text)
        }.isSuccess
        _messages.send(if (ok) R.string.backup_import_done else R.string.backup_import_failed)
    }

    fun resetLayout() = viewModelScope.launch {
        controller.resetLayout()
        _messages.send(R.string.backup_reset_done)
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (out.size() < limit) {
            val n = read(buf, 0, minOf(buf.size, limit - out.size()))
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    companion object {
        private const val MAX_IMPORT_BYTES = 2 * 1024 * 1024
    }
}
