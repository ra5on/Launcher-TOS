package de.umstieg.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.core.net.toUri
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.DropTarget
import de.umstieg.launcher.core.FolderItem
import de.umstieg.launcher.core.HomeLayout
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.LauncherJson
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.core.LayoutOps
import de.umstieg.launcher.core.Slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Verbindet gespeicherte Daten mit der App-Liste: Abgleich (Auto-Add, Deinstallation),
 * alle Layout-Änderungen, Einstellungen, Export/Import. Prozessweit genau einmal vorhanden.
 */
class LauncherController(
    context: Context,
    private val scope: CoroutineScope,
    val store: LauncherStore,
    val apps: AppRepository,
    val icons: IconLoader,
) {
    private val appContext = context.applicationContext

    /** Gespeicherte Daten (null bis zum ersten Lesen). */
    val data: StateFlow<LauncherData?> = store.data.stateIn(scope, SharingStarted.Eagerly, null)

    /** Installierte Apps nach Schlüssel. */
    val appsByKey: StateFlow<Map<AppKey, AppInfo>> = apps.apps.filterNotNull()
        .map { list -> list.associateBy { it.key } }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun start() {
        apps.start()
        // Abgleich erst, wenn Speicher UND vollständige App-Liste da sind; danach bei jeder Änderung der Liste.
        scope.launch {
            store.data.first()
            combine(apps.apps.filterNotNull(), apps.unavailablePackages) { list, unavailable -> list to unavailable }
                .collect { (list, unavailable) -> reconcile(list, unavailable) }
        }
        scope.launch { apps.changedPackages.collect { icons.invalidate(it) } }
    }

    private suspend fun reconcile(list: List<AppInfo>, unavailable: Set<String>) {
        val installed = list.map { it.key }
        store.update { d ->
            val s = d.settings
            if (!d.initialized) {
                val layout = LayoutOps.initialLayout(installed, defaultDockApps(list), s.grid, s.dockCapacity)
                d.copy(layout = layout, knownApps = installed.toSet(), initialized = true)
            } else {
                val r = LayoutOps.reconcile(d.layout, d.knownApps, installed, unavailable, s.grid, s.autoAdd)
                val layout = LayoutOps.normalize(r.layout, s.grid, s.dockCapacity)
                if (layout == d.layout && r.known == d.knownApps) d else d.copy(layout = layout, knownApps = r.known)
            }
        }
    }

    /** Vorschlag fürs Dock: Standard-Apps für Telefon, Nachrichten, Browser, Kamera (sofern vorhanden). */
    private fun defaultDockApps(list: List<AppInfo>): List<AppKey> {
        val pm = appContext.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, "smsto:".toUri()),
            Intent(Intent.ACTION_VIEW, "https://example.org".toUri()),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        val mainUser = list.filter { !it.isWorkProfile }
        return intents.mapNotNull { intent ->
            val pkg = runCatching {
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            }.getOrNull() ?: return@mapNotNull null
            mainUser.firstOrNull { it.component.packageName == pkg }?.key
        }.distinct()
    }

    // ---------- Layout-Änderungen ----------

    private fun updateLayout(block: (LauncherData) -> LauncherData) {
        scope.launch { store.update(block) }
    }

    fun drop(from: Slot, target: DropTarget) = updateLayout { d ->
        val s = d.settings
        val name = appContext.getString(R.string.folder_default_name)
        val l = LayoutOps.drop(d.layout, s.grid, s.dockCapacity, from, target, s.folderByDrag) { a, b ->
            FolderItem(UUID.randomUUID().toString(), name, listOf(a, b))
        }
        d.copy(layout = LayoutOps.normalize(l, s.grid, s.dockCapacity))
    }

    fun removeFromHome(slot: Slot) = updateLayout { d ->
        val s = d.settings
        d.copy(layout = LayoutOps.normalize(LayoutOps.removeFromHome(d.layout, s.grid, slot), s.grid, s.dockCapacity))
    }

    fun addApp(app: AppKey, startPage: Int) = updateLayout { d ->
        val s = d.settings
        d.copy(layout = LayoutOps.addApp(d.layout, s.grid, app, startPage), knownApps = d.knownApps + app)
    }

    fun renameFolder(folderId: String, name: String) = updateLayout { d ->
        val clean = name.trim().ifEmpty { appContext.getString(R.string.folder_default_name) }
        d.copy(layout = LayoutOps.renameFolder(d.layout, folderId, clean))
    }

    fun moveInFolder(folderId: String, from: Int, to: Int) = updateLayout { d ->
        d.copy(layout = LayoutOps.moveInFolder(d.layout, folderId, from, to))
    }

    fun dismissDefaultHint() = updateLayout { it.copy(defaultHintDismissed = true) }

    // ---------- Einstellungen ----------

    /** Ändert Einstellungen; das Layout passt sich an (Raster verkleinert → nichts geht verloren). */
    fun updateSettings(block: (LauncherSettings) -> LauncherSettings) = updateLayout { d ->
        val s = block(d.settings).sanitized()
        d.copy(settings = s, layout = LayoutOps.normalize(d.layout, s.grid, s.dockCapacity))
    }

    // ---------- Sichern ----------

    suspend fun exportJson(): String = LauncherJson.encode(store.data.first())

    /** Importiert JSON; wirft bei ungültigem Inhalt. Anschließend Abgleich mit den installierten Apps. */
    suspend fun importJson(text: String) {
        val imported = LauncherJson.decode(text)
        val list = apps.apps.value.orEmpty()
        val installed = list.map { it.key }.toSet()
        store.update {
            // Installierte, aber im Import fehlende Apps gelten als bekannt (kommen nicht automatisch dazu).
            imported.copy(knownApps = imported.knownApps + installed, initialized = true)
        }
        apps.apps.value?.let { reconcile(it, apps.unavailablePackages.value) }
    }

    /** Layout neu aufbauen; Einstellungen bleiben. */
    suspend fun resetLayout() {
        store.update { it.copy(initialized = false, layout = HomeLayout()) }
        apps.apps.value?.let { reconcile(it, apps.unavailablePackages.value) }
    }
}
