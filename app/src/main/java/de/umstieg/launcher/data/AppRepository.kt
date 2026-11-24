package de.umstieg.launcher.data

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import de.umstieg.launcher.core.AppKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

/** Eine startbare App (eine Launcher-Activity in einem Nutzerprofil). */
class AppInfo(
    val key: AppKey,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val isWorkProfile: Boolean,
    internal val activityInfo: LauncherActivityInfo,
) {
    override fun equals(other: Any?): Boolean = other is AppInfo && other.key == key && other.label == label
    override fun hashCode(): Int = key.hashCode() * 31 + label.hashCode()
}

/**
 * Liste aller startbaren Apps über [LauncherApps] (alle Profile, inkl. Arbeitsprofil).
 * Lädt im Hintergrund und aktualisiert sich über [LauncherApps.Callback].
 */
class AppRepository(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val collator = Collator.getInstance(Locale.GERMAN)

    private val _apps = MutableStateFlow<List<AppInfo>?>(null)

    /** null, solange die erste vollständige Liste noch nicht geladen ist. */
    val apps: StateFlow<List<AppInfo>?> = _apps.asStateFlow()

    private val _unavailablePackages = MutableStateFlow<Set<String>>(emptySet())

    /** Pakete, die gerade nicht verfügbar sind (z. B. SD-Karte); werden nicht als deinstalliert behandelt. */
    val unavailablePackages: StateFlow<Set<String>> = _unavailablePackages.asStateFlow()

    private val _changedPackages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Pakete, deren Icons neu geladen werden müssen (Update, Änderung). */
    val changedPackages: SharedFlow<String> = _changedPackages.asSharedFlow()

    private var reloadJob: Job? = null
    private val callbackThread = HandlerThread("tos-apps").apply { start() }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = scheduleReload()
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            _changedPackages.tryEmit(packageName)
            scheduleReload()
        }
        override fun onPackageChanged(packageName: String, user: UserHandle) {
            _changedPackages.tryEmit(packageName)
            scheduleReload()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            _unavailablePackages.value = _unavailablePackages.value - packageNames.toSet()
            packageNames.forEach { _changedPackages.tryEmit(it) }
            scheduleReload()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            _unavailablePackages.value = _unavailablePackages.value + packageNames.toSet()
            scheduleReload()
        }

        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) = scheduleReload()
        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) = scheduleReload()
    }

    fun start() {
        launcherApps.registerCallback(callback, Handler(callbackThread.looper))
        scheduleReload(immediate = true)
    }

    /** Neu laden, leicht entprellt (mehrere Rückrufe bei Updates). */
    fun scheduleReload(immediate: Boolean = false) {
        reloadJob?.cancel()
        reloadJob = scope.launch(Dispatchers.IO) {
            if (!immediate) delay(RELOAD_DEBOUNCE_MS)
            runCatching { load() }
                .onSuccess { _apps.value = it }
                .onFailure { Log.w(TAG, "App-Liste konnte nicht geladen werden", it) }
        }
    }

    private fun load(): List<AppInfo> {
        val me = Process.myUserHandle()
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user).map { info ->
                val cn = info.componentName
                AppInfo(
                    key = AppKey(cn.packageName, cn.className, serial),
                    label = info.label?.toString()?.trim().orEmpty().ifEmpty { cn.packageName },
                    component = cn,
                    user = user,
                    isWorkProfile = user != me,
                    activityInfo = info,
                )
            }
        }.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** Startet die App; false, wenn das nicht ging (z. B. deaktiviert oder Profil pausiert). */
    fun launch(app: AppInfo, sourceBounds: Rect?, options: ActivityOptions?): Boolean = runCatching {
        launcherApps.startMainActivity(app.component, app.user, sourceBounds, options?.toBundle())
    }.onFailure { Log.w(TAG, "Start fehlgeschlagen: ${app.key.id}", it) }.isSuccess

    companion object {
        private const val TAG = "TosApps"
        private const val RELOAD_DEBOUNCE_MS = 250L
    }
}
