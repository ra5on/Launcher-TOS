package de.umstieg.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.core.net.toUri
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.AppOverride
import de.umstieg.launcher.core.AutoAddTarget
import de.umstieg.launcher.core.BadgeCounter
import de.umstieg.launcher.core.QsLayout
import de.umstieg.launcher.core.WidgetEntry
import de.umstieg.launcher.core.WidgetItem
import de.umstieg.launcher.core.WidgetOps
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val media: MediaCenter,
    val system: SystemControls,
    val widgets: WidgetHostHolder,
    val updater: Updater,
    val permissions: PermissionHelp,
) {
    private val appContext = context.applicationContext

    /** Gespeicherte Daten (null bis zum ersten Lesen). */
    val data: StateFlow<LauncherData?> = store.data.stateIn(scope, SharingStarted.Eagerly, null)

    /** Installierte Apps nach Schlüssel. */
    val appsByKey: StateFlow<Map<AppKey, AppInfo>> = combine(apps.apps.filterNotNull(), store.data.map { it.appOverrides }.distinctUntilChanged()) { list, overrides ->
        list.associate { app ->
            val o = overrides[app.key.id]
            app.key to if (o == null) app else app.withOverride(o.label, o.monochrome)
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Badge-Zahlen je Paket/Profil (leer ohne Benachrichtigungszugriff). */
    val badgeCounts: StateFlow<Map<String, Int>> = NotificationCenter.notifications
        .map { BadgeCounter.count(NotificationCenter.facts(appContext, it)) }
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
        // Icons vorab im Hintergrund dekodieren, damit Seiten, Ordner und Übersicht ohne Nachladen erscheinen.
        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            apps.apps.filterNotNull().collect { list ->
                val opts = data.value?.settings?.let { de.umstieg.launcher.core.IconOptions(it.legacyBackground, it.legacyColor, it.themedIcons) }
                    ?: de.umstieg.launcher.core.IconOptions()
                list.forEach { runCatching { icons.load(it, opts) } }
            }
        }
        media.start()
        // Nicht mehr genutzte Widget-IDs beim Host freigeben.
        scope.launch {
            var previous: Set<Int>? = null
            store.data.collect { d ->
                val used = WidgetOps.usedIds(d)
                val prev = previous
                if (prev == null) widgets.cleanup(used) else (prev - used).forEach { widgets.delete(it) }
                previous = used
            }
        }
    }

    /** Wöchentliche Update-Prüfung beim Start bzw. beim Zurückkehren (wenn eingeschaltet und fällig). */
    fun maybeAutoCheck() {
        val d = data.value ?: return
        if (!d.settings.autoUpdateCheck) return
        val now = System.currentTimeMillis()
        if (!de.umstieg.launcher.core.UpdateLogic.dueForCheck(d.lastUpdateCheck, now)) return
        updateLayout { it.copy(lastUpdateCheck = now) }
        scope.launch { updater.check() }
    }

    /** Manuelle Prüfung („Jetzt suchen“). */
    fun checkForUpdate() {
        updateLayout { it.copy(lastUpdateCheck = System.currentTimeMillis()) }
        scope.launch { updater.check() }
    }

    /** Beliebige Änderung an den gespeicherten Daten (für einfache Schalter/Listen). */
    fun updateData(block: (LauncherData) -> LauncherData) = updateLayout(block)

    fun setHidden(app: AppKey, hidden: Boolean) = updateLayout { d ->
        d.copy(hiddenApps = if (hidden) d.hiddenApps + app else d.hiddenApps - app)
    }

    fun setOverride(app: AppKey, label: String?, monochrome: Boolean) = updateLayout { d ->
        val o = AppOverride(label?.trim()?.takeIf { it.isNotEmpty() }, monochrome)
        d.copy(appOverrides = if (o == AppOverride()) d.appOverrides - app.id else d.appOverrides + (app.id to o))
    }

    fun setBadgeDisabled(packageName: String, disabled: Boolean) = updateLayout { d ->
        d.copy(badgeDisabled = if (disabled) d.badgeDisabled + packageName else d.badgeDisabled - packageName)
    }

    /** Suchbegriff und gestartete App merken (wenn „Zuletzt gesucht“ an ist). */
    fun recordSearch(query: String?, app: AppKey?) = updateLayout { d ->
        if (!d.settings.searchHistory) return@updateLayout d
        val q = query?.trim().orEmpty()
        d.copy(
            recentQueries = if (q.length >= 2) (listOf(q) + d.recentQueries.filterNot { it.equals(q, true) }).take(MAX_RECENT) else d.recentQueries,
            recentApps = if (app != null) (listOf(app) + (d.recentApps - app)).take(MAX_RECENT) else d.recentApps,
        )
    }

    fun clearHistory() = updateLayout { it.copy(recentQueries = emptyList(), recentApps = emptyList()) }

    fun setOnboardingDone(done: Boolean) = updateLayout { it.copy(onboardingDone = done) }

    fun updateQuickSettings(block: (QsLayout) -> QsLayout) = updateLayout { it.copy(quickSettings = block(it.quickSettings)) }

    // ---------- Widgets ----------

    fun addWidgetToPage(entry: WidgetEntry) = updateLayout { it.copy(widgetPage = WidgetOps.add(it.widgetPage, entry)) }

    fun updateWidgetPage(block: (List<WidgetEntry>) -> List<WidgetEntry>) = updateLayout { it.copy(widgetPage = block(it.widgetPage)) }

    fun addWidgetToGrid(widget: WidgetItem, startPage: Int) = updateLayout { d ->
        val s = d.settings
        d.copy(layout = LayoutOps.normalize(LayoutOps.addWidget(d.layout, s.grid, widget, startPage), s.grid, s.dockCapacity, s.minPages))
    }

    fun resizeGridWidget(widgetId: Int, spanX: Int, spanY: Int) = updateLayout { d ->
        d.copy(layout = LayoutOps.resizeWidget(d.layout, d.settings.grid, widgetId, spanX, spanY))
    }

    private suspend fun reconcile(list: List<AppInfo>, unavailable: Set<String>) {
        val installed = list.map { it.key }
        store.update { d ->
            val s = d.settings
            if (!d.initialized) {
                val layout = LayoutOps.initialLayout(installed, defaultDockApps(list), s.grid, s.dockCapacity)
                d.copy(layout = layout, knownApps = installed.toSet(), initialized = true)
            } else {
                val r = LayoutOps.reconcile(d.layout, d.knownApps, installed, unavailable, s.grid, s.autoAdd, s.autoAddTarget == AutoAddTarget.LAST_PAGE)
                val layout = LayoutOps.normalize(r.layout, s.grid, s.dockCapacity, s.minPages)
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
        d.copy(layout = LayoutOps.normalize(l, s.grid, s.dockCapacity, s.minPages))
    }

    fun removeFromHome(slot: Slot) = updateLayout { d ->
        val s = d.settings
        d.copy(layout = LayoutOps.normalize(LayoutOps.removeFromHome(d.layout, s.grid, slot), s.grid, s.dockCapacity, s.minPages))
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
        d.copy(settings = s, layout = LayoutOps.normalize(d.layout, s.grid, s.dockCapacity, s.minPages))
    }

    // ---------- Sichern ----------

    suspend fun exportJson(): String = LauncherJson.encode(store.data.first())

    /** Importiert JSON; wirft bei ungültigem Inhalt. Anschließend Abgleich mit den installierten Apps. */
    suspend fun importJson(text: String) {
        // Widget-IDs gelten nur auf dem Gerät, auf dem sie angelegt wurden: nur behalten, was der Host
        // hier noch unter derselben ID für denselben Anbieter kennt (z. B. Rundlauf auf demselben Gerät).
        val raw = LauncherJson.decode(text)
        fun valid(id: Int, provider: String) = widgets.info(id)?.provider?.flattenToString() == provider
        val imported = raw.copy(
            widgetPage = raw.widgetPage.filter { valid(it.widgetId, it.provider) },
            layout = LayoutOps.normalize(
                raw.layout.copy(
                    pages = raw.layout.pages.map { page ->
                        page.filter { p -> (p.item as? WidgetItem)?.let { valid(it.widgetId, it.provider) } ?: true }
                    },
                ),
                raw.settings.grid, raw.settings.dockCapacity, raw.settings.minPages,
            ),
        )
        val list = apps.apps.value.orEmpty()
        val installed = list.map { it.key }.toSet()
        store.update {
            // Installierte, aber im Import fehlende Apps gelten als bekannt (kommen nicht automatisch dazu).
            imported.copy(knownApps = imported.knownApps + installed, initialized = true)
        }
        apps.apps.value?.let { reconcile(it, apps.unavailablePackages.value) }
    }

    companion object {
        private const val MAX_RECENT = 8
    }

    /** Layout neu aufbauen; Einstellungen bleiben. */
    suspend fun resetLayout() {
        store.update { it.copy(initialized = false, layout = HomeLayout()) }
        apps.apps.value?.let { reconcile(it, apps.unavailablePackages.value) }
    }
}
