package de.umstieg.launcher.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import de.umstieg.launcher.core.NotificationFacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Prozessweiter Zustand der Benachrichtigungen (nur wenn der Nutzer den Zugriff erlaubt hat).
 * Nichts verlässt das Gerät; die Daten liegen nur im Speicher.
 */
object NotificationCenter {
    private val _notifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
    val notifications: StateFlow<List<StatusBarNotification>> = _notifications.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var service: WeakReference<TosNotificationListener>? = null

    fun component(context: Context) = ComponentName(context, TosNotificationListener::class.java)

    /** Hat der Nutzer den Benachrichtigungszugriff erteilt? */
    fun isPermissionGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    internal fun attach(s: TosNotificationListener) {
        service = WeakReference(s)
        _connected.value = true
        refresh()
    }

    internal fun detach() {
        service = null
        _connected.value = false
        _notifications.value = emptyList()
    }

    internal fun refresh() {
        val s = service?.get() ?: return
        _notifications.value = runCatching { s.activeNotifications?.toList().orEmpty() }
            .onFailure { Log.w("TosNotifications", "Liste nicht lesbar", it) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.notification.`when`.takeIf { w -> w > 0 } ?: it.postTime }
    }

    fun cancel(key: String) {
        runCatching { service?.get()?.cancelNotification(key) }
    }

    fun cancelAll() {
        runCatching { service?.get()?.cancelAllNotifications() }
    }

    /** Mehrere entfernen (z. B. „Gruppe löschen“). */
    fun cancel(keys: Collection<String>) {
        runCatching { service?.get()?.cancelNotifications(keys.toTypedArray()) }
    }

    /** Für eine Weile stummschalten (Android blendet sie aus und zeigt sie danach wieder). */
    fun snooze(key: String, durationMs: Long) {
        runCatching { service?.get()?.snoozeNotification(key, durationMs) }
    }

    /** Aktion einer Benachrichtigung auslösen, bei Direktantwort mit Text. */
    fun runAction(context: Context, action: Notification.Action, reply: String? = null): Boolean = runCatching {
        val inputs = action.remoteInputs
        if (reply != null && inputs != null && inputs.isNotEmpty()) {
            val intent = android.content.Intent()
            val results = android.os.Bundle().apply { inputs.forEach { putCharSequence(it.resultKey, reply) } }
            android.app.RemoteInput.addResultsToIntent(inputs, intent, results)
            action.actionIntent.send(context, 0, intent)
        } else {
            action.actionIntent.send()
        }
        true
    }.getOrDefault(false)

    /** Medien-Benachrichtigungen erscheinen oben als eigene Karte, nicht in der Liste. */
    fun isMedia(sbn: StatusBarNotification): Boolean = sbn.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)

    /** Fakten für die Badge-Zählung. */
    fun facts(context: Context, list: List<StatusBarNotification>): List<NotificationFacts> {
        val um = context.getSystemService(UserManager::class.java)
        return list.map { sbn ->
            NotificationFacts(
                packageName = sbn.packageName,
                userSerial = runCatching { um.getSerialNumberForUser(sbn.user) }.getOrDefault(0L),
                number = sbn.notification.number,
                isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
                isOngoing = sbn.isOngoing || (sbn.notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0,
            )
        }
    }

    /** Laufende Benachrichtigungen und Gruppen-Zusammenfassungen werden in der Liste nicht gezeigt. */
    fun visible(list: List<StatusBarNotification>): List<StatusBarNotification> = list.filter { sbn ->
        !sbn.isOngoing && (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 && !isMedia(sbn)
    }
}

/** Listener-Dienst: nur aktiv, wenn der Nutzer ihn in den Systemeinstellungen einschaltet. */
class TosNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() = NotificationCenter.attach(this)
    override fun onListenerDisconnected() = NotificationCenter.detach()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = NotificationCenter.refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = NotificationCenter.refresh()
}
