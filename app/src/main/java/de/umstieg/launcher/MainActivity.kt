package de.umstieg.launcher

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.core.WidgetEntry
import de.umstieg.launcher.core.WidgetItem
import de.umstieg.launcher.core.WidgetOps
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.DefaultLauncher
import de.umstieg.launcher.ui.home.HomeActions
import de.umstieg.launcher.ui.home.HomeScreen
import de.umstieg.launcher.ui.home.HomeViewModel
import de.umstieg.launcher.ui.home.WidgetTarget

/** Homescreen-Activity (HOME/DEFAULT, singleTask). */
class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private val isDefault = mutableStateOf(true)
    private val wallpaperColor = mutableStateOf<androidx.compose.ui.graphics.Color?>(null)

    /** Widget, das gerade gebunden/konfiguriert wird. */
    private var pending: Triple<Int, AppWidgetProviderInfo, WidgetTarget>? = null

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault.value = DefaultLauncher.isDefault(this)
    }

    private val bindRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) configureOrFinish() else cancelPending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Helle Statusleisten-Symbole über dem Systemhintergrund.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        readWallpaperColor()
        val actions = HomeActions(
            openSettings = { section ->
                startActivity(
                    Intent(this, SettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(SettingsActivity.EXTRA_SECTION, section),
                )
            },
            requestDefault = { runCatching { roleRequest.launch(DefaultLauncher.requestIntent(this)) } },
            setWindowBlur = ::setWindowBlur,
            addWidget = ::startAddWidget,
            uninstall = ::uninstall,
            unlockHidden = { it() },
            wallpaperColor = null,
            systemReduceMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f,
        )
        setContent {
            val data by vm.data.collectAsStateWithLifecycle()
            val hideStatus = data?.settings?.hideStatusBar == true
            LaunchedEffect(hideStatus) { applyStatusBar(hideStatus) }
            HomeScreen(vm, isDefault.value, HomeActions(
                actions.openSettings, actions.requestDefault, actions.setWindowBlur, actions.addWidget,
                actions.uninstall, actions.unlockHidden, wallpaperColor.value, actions.systemReduceMotion,
            ))
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { vm.widgets.host.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { vm.widgets.host.stopListening() }
    }

    override fun onResume() {
        super.onResume()
        isDefault.value = DefaultLauncher.isDefault(this)
        readWallpaperColor()
    }

    /** Home-Taste bei laufendem Launcher: Panels schließen, erste Seite (animiert, wenn schon sichtbar). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            vm.onHomePressed(visible = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
    }

    /** Hauptfarbe des Hintergrunds (für „Akzent aus Hintergrund“). */
    private fun readWallpaperColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wallpaperColor.value = runCatching {
                WallpaperManager.getInstance(this).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
            }.getOrNull()?.let { androidx.compose.ui.graphics.Color(it) }
        }
    }

    /** Fenster-Unschärfe des Systemhintergrunds (ab API 31, wenn das Gerät sie erlaubt). */
    private fun setWindowBlur(on: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val enabled = windowManager.isCrossWindowBlurEnabled
        if (on && enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = BLUR_RADIUS_PX }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
    }

    private fun applyStatusBar(hidden: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) controller.hide(WindowInsetsCompat.Type.statusBars()) else controller.show(WindowInsetsCompat.Type.statusBars())
    }

    private fun uninstall(app: AppInfo) {
        if (app.isWorkProfile) {
            vm.controller.apps.showAppDetails(app, null)
            return
        }
        runCatching { startActivity(Intent(Intent.ACTION_DELETE, "package:${app.component.packageName}".toUri())) }
    }

    // ---------- Widgets hinzufügen: binden → ggf. konfigurieren → ins Layout ----------

    private fun startAddWidget(info: AppWidgetProviderInfo, target: WidgetTarget) {
        val id = vm.widgets.allocate()
        pending = Triple(id, info, target)
        if (vm.widgets.bindIfAllowed(id, info.provider, info)) {
            configureOrFinish()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            runCatching { bindRequest.launch(intent) }.onFailure { cancelPending() }
        }
    }

    private fun configureOrFinish() {
        val (id, info, _) = pending ?: return
        if (info.configure != null) {
            val started = runCatching {
                vm.widgets.host.startAppWidgetConfigureActivityForResult(this, id, 0, REQUEST_CONFIGURE, null)
            }.isSuccess
            if (!started) finishWidget()
        } else {
            finishWidget()
        }
    }

    @Deprecated("Ergebnis der Widget-Konfiguration (nur über diesen Weg erreichbar).")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE) {
            if (resultCode == RESULT_OK) finishWidget() else cancelPending()
        }
    }

    private fun finishWidget() {
        val (id, info, target) = pending ?: return
        pending = null
        val density = resources.displayMetrics.density
        val wDp = (info.minWidth / density).toInt()
        val hDp = (info.minHeight / density).toInt()
        val provider = info.provider.flattenToString()
        when (target) {
            WidgetTarget.Page -> vm.controller.addWidgetToPage(WidgetEntry(id, provider, WidgetOps.rowsFor(hDp)))
            is WidgetTarget.Grid -> {
                val s = vm.data.value?.settings ?: return
                val (sx, sy) = WidgetOps.spanFor(wDp, hDp, s.grid)
                vm.controller.addWidgetToGrid(WidgetItem(id, provider, sx, sy), target.page)
            }
        }
    }

    private fun cancelPending() {
        pending?.let { vm.widgets.delete(it.first) }
        pending = null
    }

    companion object {
        private const val REQUEST_CONFIGURE = 0x7A
        private const val BLUR_RADIUS_PX = 60
    }
}
