package de.umstieg.launcher.data

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import de.umstieg.launcher.MainActivity
import de.umstieg.launcher.TosApplication
import de.umstieg.launcher.core.Gesture
import de.umstieg.launcher.core.GestureAction
import de.umstieg.launcher.core.GestureClassifier
import de.umstieg.launcher.core.LauncherSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * Optionale Bedienungshilfe „Systemweite Wischgesten“: schmale, unsichtbare Streifen direkt auf der
 * Statusleiste (links und rechts, oder über die ganze Breite). Ein Wisch nach unten dort öffnet die eigenen
 * Panels von Launcher TOS – auch in anderen Apps, wenn gewünscht. Die Mitte bleibt frei für Android.
 *
 * Grenzen: Ein Overlay-Fenster kann eine begonnene Berührung nicht an das Fenster darunter zurückgeben.
 * Tipps auf den Streifen gehen deshalb nicht an die Statusleiste (die auf Android ohnehin keine
 * Tipp-Funktion hat). Ist die Statusleiste ausgeblendet (Vollbild-Apps), werden die Streifen durchlässig.
 */
class EdgeGestureService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val strips = mutableListOf<View>()
    private var foregroundPackage: String? = null
    private var settings: LauncherSettings? = null
    private var statusBarVisible = true
    private var insetsReliable = false

    override fun onServiceConnected() {
        instance = WeakReference(this)
        val controller = (application as TosApplication).controller
        scope.launch {
            controller.data.filterNotNull().map { it.settings }.distinctUntilChanged().collect {
                settings = it
                rebuild()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Nur der Paketname (für „nur auf dem Homescreen“), keine Inhalte.
            event.packageName?.toString()?.let {
                if (it != foregroundPackage) {
                    foregroundPackage = it
                    updateTouchable()
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        removeStrips()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    private fun wm() = getSystemService(WindowManager::class.java)

    private fun removeStrips() {
        strips.forEach { runCatching { wm().removeView(it) } }
        strips.clear()
    }

    private fun statusBarHeight(): Int {
        val m = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm().currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
        } else {
            // Nur vor Android 11 (ohne WindowMetrics): Höhe aus der System-Ressource.
            @SuppressLint("DiscouragedApi", "InternalInsetResource")
            val id = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (id > 0) resources.getDimensionPixelSize(id) else 0
        }
        return m.coerceAtLeast((24 * resources.displayMetrics.density).toInt())
    }

    /** Streifen nach den Einstellungen neu anlegen. */
    private fun rebuild() {
        removeStrips()
        val s = settings ?: return
        if (!s.systemGestures) return
        val width = resources.displayMetrics.widthPixels
        val height = statusBarHeight()
        if (s.systemGestureFull) {
            addStrip(0, width, height, Gesture.EDGE)
        } else {
            val left = (width * s.swipeZoneLeft / 100f).toInt()
            val right = (width * s.swipeZoneRight / 100f).toInt()
            addStrip(0, left, height, Gesture.EDGE_LEFT)
            addStrip(width - right, right, height, Gesture.EDGE_RIGHT)
        }
        updateTouchable()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addStrip(x: Int, w: Int, h: Int, gesture: Gesture) {
        if (w <= 0) return
        val view = View(this)
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var fired = false
        view.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    fired = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = e.rawY - downY
                    val dx = e.rawX - downX
                    // Nur ein klarer Wisch nach unten löst aus.
                    // Früh auslösen: das System übernimmt schnelle Wische am oberen Rand sonst selbst (ACTION_CANCEL).
                    if (!fired && dy > slop && dy > abs(dx) * 1.2f) {
                        fired = true
                        trigger(gesture)
                    }
                }
            }
            true
        }
        view.setOnApplyWindowInsetsListener { _, insets ->
            // Vollbild (Statusleiste ausgeblendet): Streifen durchlässig, damit Apps oben nicht gestört werden.
            // Manche Systeme melden für Overlay-Fenster die Statusleiste nie als sichtbar; dann gilt sie als sichtbar.
            val v = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || insets.isVisible(WindowInsets.Type.statusBars())
            if (v) insetsReliable = true
            statusBarVisible = if (insetsReliable) v else true
            updateTouchable()
            insets
        }
        val lp = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setFitInsetsTypes(0)
            title = "LauncherTOS-Wischstreifen"
        }
        runCatching { wm().addView(view, lp) }.onSuccess { strips += view }
    }

    /** Berührbar nur, wenn erlaubt (überall bzw. Homescreen im Vordergrund) und die Statusleiste sichtbar ist. */
    private fun updateTouchable() {
        val s = settings ?: return
        val home = foregroundPackage == null || foregroundPackage == packageName
        val active = statusBarVisible && (s.systemGesturesEverywhere || home)
        strips.forEach { v ->
            val lp = v.layoutParams as? WindowManager.LayoutParams ?: return@forEach
            val flags = if (active) lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv() else lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            if (flags != lp.flags) {
                lp.flags = flags
                runCatching { wm().updateViewLayout(v, lp) }
            }
        }
    }

    private fun trigger(gesture: Gesture) {
        val s = settings ?: return
        val action = GestureClassifier.binding(s, gesture).action
        when (action) {
            GestureAction.SYSTEM_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            GestureAction.SYSTEM_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            GestureAction.LOCK_SCREEN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            GestureAction.NOTHING -> Unit
            else -> startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.EXTRA_GESTURE_ACTION, action.name),
            )
        }
    }

    companion object {
        @Volatile
        private var instance: WeakReference<EdgeGestureService>? = null

        /** Läuft der Dienst gerade (vom Nutzer in den Bedienungshilfen eingeschaltet)? */
        val running: Boolean get() = instance?.get() != null

        /** Android-Benachrichtigungen bzw. -Schnelleinstellungen über den Dienst öffnen (sauberer Weg). */
        fun openSystemPanel(quickSettings: Boolean): Boolean {
            val s = instance?.get() ?: return false
            return s.performGlobalAction(if (quickSettings) GLOBAL_ACTION_QUICK_SETTINGS else GLOBAL_ACTION_NOTIFICATIONS)
        }

        /** Bildschirm sperren (nur mit Dienst, ab Android 9). */
        fun lockScreen(): Boolean {
            val s = instance?.get() ?: return false
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && s.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        /** Ist der Dienst in den Systemeinstellungen eingeschaltet? */
        fun isEnabled(context: Context): Boolean {
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = "${context.packageName}/${EdgeGestureService::class.java.name}"
            val short = "${context.packageName}/.data.EdgeGestureService"
            return list.split(':').any { it.equals(me, true) || it.equals(short, true) }
        }

        fun settingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
}
