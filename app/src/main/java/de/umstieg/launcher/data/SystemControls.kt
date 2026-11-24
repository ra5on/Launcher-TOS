package de.umstieg.launcher.data

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import de.umstieg.launcher.core.QsTile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Was ein Element des Schnellzugriffs braucht, um direkt zu schalten. */
enum class Requirement { NONE, WRITE_SETTINGS, NOTIFICATION_POLICY, NOTIFICATION_LISTENER, SYSTEM_ONLY }

/** Zustand eines Elements: an/aus/unbekannt plus optionaler Hinweis-Schlüssel. */
data class TileStatus(val active: Boolean?, val detail: Detail = Detail.NONE) {
    enum class Detail { NONE, CONNECTED, DISCONNECTED, ON, OFF, UNKNOWN }
}

/**
 * Systemfunktionen für Schnellzugriff, Gesten und Panels – nur über öffentliche APIs.
 * Was Android Apps nicht direkt erlaubt (WLAN, Mobile Daten, Bluetooth ab API 33, Flugmodus,
 * Hotspot, NFC, Standort), öffnet das passende System-Panel bzw. die Einstellungsseite.
 */
class SystemControls(context: Context) {
    private val ctx = context.applicationContext
    private val camera = ctx.getSystemService(CameraManager::class.java)
    private val audio = ctx.getSystemService(AudioManager::class.java)
    private val notifications = ctx.getSystemService(NotificationManager::class.java)

    private val _torch = MutableStateFlow(false)
    val torch: StateFlow<Boolean> = _torch.asStateFlow()
    private var torchId: String? = null

    init {
        runCatching {
            torchId = camera.cameraIdList.firstOrNull { id ->
                camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            camera.registerTorchCallback(
                object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        if (cameraId == torchId) _torch.value = enabled
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }
    }

    val hasTorch: Boolean get() = torchId != null

    fun canWriteSettings(): Boolean = Settings.System.canWrite(ctx)
    fun hasPolicyAccess(): Boolean = notifications.isNotificationPolicyAccessGranted

    fun requirement(tile: QsTile): Requirement = when (tile) {
        QsTile.BRIGHTNESS, QsTile.ROTATION -> Requirement.WRITE_SETTINGS
        QsTile.DND -> Requirement.NOTIFICATION_POLICY
        QsTile.MEDIA -> Requirement.NOTIFICATION_LISTENER
        QsTile.WIFI, QsTile.BLUETOOTH, QsTile.MOBILE_DATA, QsTile.AIRPLANE, QsTile.HOTSPOT, QsTile.NFC,
        QsTile.LOCATION, QsTile.BATTERY_SAVER, QsTile.SYSTEM_PANEL -> Requirement.SYSTEM_ONLY
        else -> Requirement.NONE
    }

    // ---------- Zustand ----------

    fun status(tile: QsTile): TileStatus = runCatching {
        when (tile) {
            QsTile.WIFI -> connected(NetworkCapabilities.TRANSPORT_WIFI)
            QsTile.MOBILE_DATA -> connected(NetworkCapabilities.TRANSPORT_CELLULAR)
            QsTile.BLUETOOTH -> {
                val on = runCatching { ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled }.getOrNull()
                TileStatus(on, if (on == null) TileStatus.Detail.UNKNOWN else onOff(on))
            }
            QsTile.AIRPLANE -> {
                val on = Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
                TileStatus(on, onOff(on))
            }
            QsTile.NFC -> {
                val on = NfcAdapter.getDefaultAdapter(ctx)?.isEnabled
                TileStatus(on, if (on == null) TileStatus.Detail.UNKNOWN else onOff(on))
            }
            QsTile.LOCATION -> {
                val lm = ctx.getSystemService(LocationManager::class.java)
                val on = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) lm.isLocationEnabled else null
                TileStatus(on, if (on == null) TileStatus.Detail.UNKNOWN else onOff(on))
            }
            QsTile.BATTERY_SAVER -> {
                val on = ctx.getSystemService(PowerManager::class.java).isPowerSaveMode
                TileStatus(on, onOff(on))
            }
            QsTile.FLASHLIGHT -> TileStatus(_torch.value, onOff(_torch.value))
            QsTile.DND -> {
                val on = notifications.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
                TileStatus(on, onOff(on))
            }
            QsTile.ROTATION -> {
                val auto = Settings.System.getInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
                // „Drehsperre an“ = automatisches Drehen aus.
                TileStatus(!auto, onOff(!auto))
            }
            else -> TileStatus(null)
        }
    }.getOrDefault(TileStatus(null, TileStatus.Detail.UNKNOWN))

    private fun onOff(on: Boolean) = if (on) TileStatus.Detail.ON else TileStatus.Detail.OFF

    private fun connected(transport: Int): TileStatus {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val on = caps?.hasTransport(transport) == true
        return TileStatus(on, if (on) TileStatus.Detail.CONNECTED else TileStatus.Detail.DISCONNECTED)
    }

    // ---------- Schalten ----------

    fun setTorch(on: Boolean): Boolean = runCatching {
        camera.setTorchMode(torchId ?: return false, on)
        true
    }.getOrDefault(false)

    fun setDnd(on: Boolean): Boolean {
        if (!hasPolicyAccess()) return false
        notifications.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL,
        )
        return true
    }

    fun setRotationLock(locked: Boolean): Boolean {
        if (!canWriteSettings()) return false
        return runCatching { Settings.System.putInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (locked) 0 else 1) }
            .getOrDefault(false)
    }

    /** Helligkeit 0..1 (Systemwert 0..255). */
    fun brightness(): Float = runCatching {
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(0.5f)

    fun setBrightness(value: Float): Boolean {
        if (!canWriteSettings()) return false
        return runCatching {
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (value.coerceIn(0.02f, 1f) * 255).toInt())
        }.getOrDefault(false)
    }

    fun volume(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }

    fun setVolume(value: Float) {
        runCatching {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (value.coerceIn(0f, 1f) * max).toInt(), 0)
        }
    }

    // ---------- Seiten und Apps öffnen ----------

    /** Intent für ein Element, das nur über das System geht (Panel ab API 29, sonst Einstellungsseite). */
    fun settingsIntent(tile: QsTile): Intent? = when (tile) {
        QsTile.WIFI -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI) else Intent(Settings.ACTION_WIFI_SETTINGS)
        QsTile.MOBILE_DATA -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) else Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        QsTile.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        QsTile.AIRPLANE -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        QsTile.HOTSPOT -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
        QsTile.NFC -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_NFC) else Intent(Settings.ACTION_NFC_SETTINGS)
        QsTile.LOCATION -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        QsTile.BATTERY_SAVER -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        QsTile.BRIGHTNESS, QsTile.ROTATION -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
        QsTile.VOLUME -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_VOLUME) else Intent(Settings.ACTION_SOUND_SETTINGS)
        QsTile.DND -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        QsTile.CALCULATOR -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)
        QsTile.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        QsTile.ALARM -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
        // Kein Standard-Intent für QR: die Kamera-App (die meisten erkennen QR-Codes selbst).
        QsTile.QR -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        else -> null
    }

    fun writeSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:${ctx.packageName}".toUri())

    fun policyAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun listenerSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    /** Startet einen Intent sicher; false, wenn es keine passende App gibt. */
    fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.onFailure { Log.w(TAG, "Kein Ziel für ${intent.action}", it) }.getOrDefault(false)

    /** Assistent des Systems (Standard für „Doppeltipp unten“). */
    fun openAssistant(context: Context): Boolean =
        start(context, Intent(Intent.ACTION_ASSIST)) || start(context, Intent(Intent.ACTION_VOICE_COMMAND))

    /** Öffnet die System-Benachrichtigungsleiste bzw. Schnelleinstellungen (nicht öffentlich: per Reflection). */
    @SuppressLint("WrongConstant")
    fun expandStatusBar(quickSettings: Boolean): Boolean = runCatching {
        val service = ctx.getSystemService("statusbar") ?: return false
        val method = service.javaClass.getMethod(if (quickSettings) "expandSettingsPanel" else "expandNotificationsPanel")
        method.invoke(service)
        true
    }.onFailure { Log.w(TAG, "Statusleiste nicht aufklappbar", it) }.getOrDefault(false)

    /** Sendet den Inhalt einer Benachrichtigung (Tippen) – mit Erlaubnis zum Start aus dem Vordergrund. */
    fun send(pending: PendingIntent?): Boolean = runCatching {
        if (pending == null) return false
        val options = if (Build.VERSION.SDK_INT >= 34) {
            ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            ).toBundle()
        } else {
            null
        }
        pending.send(ctx, 0, null, null, null, null, options)
        true
    }.onFailure { Log.w(TAG, "Benachrichtigung nicht zu öffnen", it) }.getOrDefault(false)

    companion object {
        private const val TAG = "TosSystem"
    }
}
