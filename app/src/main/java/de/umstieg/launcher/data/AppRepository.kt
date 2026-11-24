package de.umstieg.launcher.data

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.core.content.ContextCompat
import de.umstieg.launcher.core.AppCategory
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.Categories
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
    val category: AppCategory = AppCategory.OTHER,
    val firstInstallTime: Long = 0,
    /** Profil pausiert (Arbeitsprofil „Pause“). */
    val quiet: Boolean = false,
    /** Eigener Name gesetzt (Originalname in [originalLabel]). */
    val originalLabel: String = label,
    /** Einfarbiges Themen-Icon für diese App erzwungen. */
    val monochrome: Boolean = false,
) {
    fun withOverride(customLabel: String?, mono: Boolean): AppInfo = AppInfo(
        key, customLabel?.takeIf { it.isNotBlank() } ?: originalLabel, component, user, isWorkProfile, activityInfo,
        category, firstInstallTime, quiet, originalLabel, mono,
    )

    override fun equals(other: Any?): Boolean = other is AppInfo && other.key == key && other.label == label &&
        other.quiet == quiet && other.monochrome == monochrome
    override fun hashCode(): Int = (key.hashCode() * 31 + label.hashCode()) * 31 + quiet.hashCode()
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
        // Arbeitsprofil pausiert/fortgesetzt → neu laden (Icons ausgegraut bzw. normal).
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        }
        ContextCompat.registerReceiver(
            appContext,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = scheduleReload()
            },
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
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
        val pm = appContext.packageManager
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val quiet = user != me && runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(false)
            launcherApps.getActivityList(null, user).map { info ->
                val cn = info.componentName
                val appInfo = info.applicationInfo
                val installed = if (user == me) {
                    runCatching { pm.getPackageInfo(cn.packageName, 0).firstInstallTime }.getOrDefault(0L)
                } else {
                    0L
                }
                val label = info.label?.toString()?.trim().orEmpty().ifEmpty { cn.packageName }
                AppInfo(
                    key = AppKey(cn.packageName, cn.className, serial),
                    label = label,
                    component = cn,
                    user = user,
                    isWorkProfile = user != me,
                    activityInfo = info,
                    category = Categories.classify(cn.packageName, label, appInfo.category),
                    firstInstallTime = installed,
                    quiet = quiet,
                )
            }
        }.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** Pausiertes Arbeitsprofil fortsetzen (nur als Standard-Launcher erlaubt). */
    fun resumeProfile(user: UserHandle): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            userManager.requestQuietModeEnabled(false, user)
            true
        } else {
            false
        }
    }.getOrDefault(false).also { scheduleReload() }

    /** Systemseite „App-Info“. */
    fun showAppDetails(app: AppInfo, bounds: Rect?) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, bounds, null) }
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
