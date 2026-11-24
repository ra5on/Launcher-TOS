package de.umstieg.launcher.data

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.net.toUri
import de.umstieg.launcher.core.SetupLogic

/**
 * Merkt sich Versuche, Sonderzugriffe zu erteilen, und erkennt „Eingeschränkte Einstellungen“
 * (Android 13+, Installation nicht aus einem Store): Dann lässt sich der Schalter erst nach
 * App-Info → ⋮ → „Eingeschränkte Einstellungen zulassen“ einschalten.
 */
class PermissionHelp(context: Context) {
    private val ctx = context.applicationContext
    private val prefs = ctx.getSharedPreferences("permission_help", Context.MODE_PRIVATE)

    enum class Kind { LISTENER, DND }

    fun markAttempt(kind: Kind) = prefs.edit { putBoolean("attempt_$kind", true) }
    fun attempted(kind: Kind): Boolean = prefs.getBoolean("attempt_$kind", false)

    /** App-Info wurde aus der Hilfe geöffnet (bei Rückkehr direkt weiter zur Zugriffs-Einstellung). */
    var appInfoOpenedFor: Kind?
        get() = prefs.getString("appinfo", null)?.let { runCatching { Kind.valueOf(it) }.getOrNull() }
        set(v) = prefs.edit { putString("appinfo", v?.name) }

    val installer: String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ctx.packageManager.getInstallSourceInfo(ctx.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            ctx.packageManager.getInstallerPackageName(ctx.packageName)
        }
    }.getOrNull()

    /** Paketquelle (Store, Datei, Download …) ab Android 13. */
    val packageSource: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching { ctx.packageManager.getInstallSourceInfo(ctx.packageName).packageSource }.getOrNull()
    } else {
        null
    }

    val fromStore: Boolean get() = SetupLogic.isStore(installer)

    /** AppOps-Status „Eingeschränkte Einstellungen“ (true = freigegeben), null wenn nicht lesbar. */
    fun restrictedAllowed(): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return runCatching {
            val ops = ctx.getSystemService(AppOpsManager::class.java)
            ops.unsafeCheckOpNoThrow("android:access_restricted_settings", ctx.applicationInfo.uid, ctx.packageName) == AppOpsManager.MODE_ALLOWED
        }.getOrNull()
    }

    /** Gelten gerade „Eingeschränkte Einstellungen“ (Android 13+, aus Datei/Download, nicht freigegeben)? */
    fun restricted(): Boolean = SetupLogic.isRestricted(Build.VERSION.SDK_INT, packageSource, installer, restrictedAllowed())

    @Suppress("UNUSED_PARAMETER")
    fun showRestrictedHelp(kind: Kind, granted: Boolean): Boolean = SetupLogic.showRestrictedHelp(restricted(), granted)

    fun appDetailsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${ctx.packageName}".toUri())
}
