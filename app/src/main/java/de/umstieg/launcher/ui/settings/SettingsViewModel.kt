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
        // Zuerst die Apps in Homescreen-Reihenfolge (für Vorschauen), dann alle übrigen (für Listen).
        val placed = ordered.map { it.key }.toSet()
        ordered + apps.values.filter { it.key !in placed }.sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /** Kurze Rückmeldungen (String-Ressourcen) für Toasts. */
    val messages = _messages.receiveAsFlow()

    fun update(block: (LauncherSettings) -> LauncherSettings) = controller.updateSettings(block)

    fun updateData(block: (de.umstieg.launcher.core.LauncherData) -> de.umstieg.launcher.core.LauncherData) = controller.updateData(block)

    fun setHidden(app: de.umstieg.launcher.core.AppKey, hidden: Boolean) = controller.setHidden(app, hidden)

    fun setBadgeDisabled(packageName: String, disabled: Boolean) = controller.setBadgeDisabled(packageName, disabled)

    fun writeSettingsIntent() = controller.system.writeSettingsIntent()

    // ---------- Updates ----------
    val updateState = controller.updater.state
    val installedVersion: String get() = controller.updater.installedName
    fun canInstall() = controller.updater.canInstall()
    fun allowInstallIntent() = controller.updater.allowInstallIntent()
    fun checkForUpdate() = controller.checkForUpdate()
    fun install(release: de.umstieg.launcher.core.ReleaseInfo) = controller.updater.install(release)

    // ---------- Eingeschränkte Einstellungen ----------
    private val help = controller.permissions
    fun markListenerAttempt() = help.markAttempt(de.umstieg.launcher.data.PermissionHelp.Kind.LISTENER)
    fun showRestrictedHelp(granted: Boolean) = help.showRestrictedHelp(de.umstieg.launcher.data.PermissionHelp.Kind.LISTENER, granted)
    fun markAppInfoOpened() {
        help.appInfoOpenedFor = de.umstieg.launcher.data.PermissionHelp.Kind.LISTENER
    }
    fun appDetailsIntent() = help.appDetailsIntent()

    /** Zurück aus der App-Info: direkt weiter zur Listener-Einstellung (einmal). */
    fun afterAppInfo(context: android.content.Context) {
        if (help.appInfoOpenedFor == null) return
        help.appInfoOpenedFor = null
        if (!de.umstieg.launcher.data.NotificationCenter.isPermissionGranted(context)) {
            controller.system.start(context, controller.system.listenerSettingsIntent())
        }
    }

    /** Einrichtung zurücksetzen; sie erscheint beim nächsten Homescreen. */
    fun restartSetup() = controller.updateData {
        it.copy(onboardingDone = false, setupCardDismissed = false, setupDeclined = emptySet(), gesturesSeen = false)
    }

    /** Zustand der Sonderberechtigungen (für die Übersicht). */
    class PermissionStatus(val policy: Boolean, val writeSettings: Boolean)

    fun permissionStatus() = PermissionStatus(controller.system.hasPolicyAccess(), controller.system.canWriteSettings())

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
