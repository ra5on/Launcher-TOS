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
            .sortedByDescending { it.postTime }
    }

    fun cancel(key: String) {
        runCatching { service?.get()?.cancelNotification(key) }
    }

    fun cancelAll() {
        runCatching { service?.get()?.cancelAllNotifications() }
    }

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
        !sbn.isOngoing && (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
    }
}

/** Listener-Dienst: nur aktiv, wenn der Nutzer ihn in den Systemeinstellungen einschaltet. */
class TosNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() = NotificationCenter.attach(this)
    override fun onListenerDisconnected() = NotificationCenter.detach()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = NotificationCenter.refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = NotificationCenter.refresh()
}
