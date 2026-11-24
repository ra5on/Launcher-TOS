package de.umstieg.launcher.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import de.umstieg.launcher.BuildConfig
import de.umstieg.launcher.core.ReleaseInfo
import de.umstieg.launcher.core.UpdateLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Zustand des Selbst-Updates (für Einstellungen und Homescreen-Hinweis). */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val done: Long, val total: Long) : UpdateState
    data class Verifying(val release: ReleaseInfo) : UpdateState
    data class Installing(val release: ReleaseInfo) : UpdateState
    /** „Unbekannte Apps installieren“ fehlt noch. */
    data class NeedsPermission(val release: ReleaseInfo) : UpdateState
    data class Failed(val reason: Reason, val detail: String? = null, val release: ReleaseInfo? = null) : UpdateState

    enum class Reason { NETWORK, CHECKSUM, SIGNATURE, PACKAGE, INSTALL }
}

/**
 * Selbst-Update über GitHub: einzige Netzverbindung der App, nur zu GitHub (Releases).
 * Geprüft wird vor der Installation: SHA-256 gegen SHA256SUMS des Releases, Paketname, höhere Version
 * und das Signaturzertifikat gegen den fest hinterlegten Fingerabdruck der Releases. Installiert wird
 * über eine PackageInstaller-Sitzung (Android fragt nach).
 */
class Updater(context: Context, private val scope: CoroutineScope) {
    private val ctx = context.applicationContext
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val installedName: String = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
    private val installedCode: Long = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode }.getOrDefault(0L)

    /** In Debug-Builds lässt sich die Adresse für Tests überschreiben (Datei files/update-url.txt). */
    private fun apiUrl(): String {
        if (BuildConfig.DEBUG) {
            File(ctx.filesDir, "update-url.txt").takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return API_URL
    }

    fun canInstall(): Boolean = ctx.packageManager.canRequestPackageInstalls()

    fun allowInstallIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${ctx.packageName}".toUri())

    /** Sucht nach einer neuen Version; liefert sie (oder null) und setzt den Zustand. */
    suspend fun check(): ReleaseInfo? {
        _state.value = UpdateState.Checking
        return runCatching { withContext(Dispatchers.IO) { UpdateLogic.parseLatest(get(apiUrl())) } }
            .onFailure {
                Log.w(TAG, "Update-Prüfung fehlgeschlagen", it)
                _state.value = UpdateState.Failed(UpdateState.Reason.NETWORK, it.message)
            }
            .getOrNull()
            ?.takeIf { UpdateLogic.isNewer(it.version, installedName, installedCode) }
            .also { found ->
                if (_state.value is UpdateState.Checking) {
                    _state.value = if (found != null) UpdateState.Available(found) else UpdateState.UpToDate
                }
            }
    }

    /** Lädt, prüft und installiert [release]. */
    fun install(release: ReleaseInfo) {
        if (!canInstall()) {
            _state.value = UpdateState.NeedsPermission(release)
            return
        }
        scope.launch(Dispatchers.IO) {
            runCatching { downloadVerifyCommit(release) }.onFailure { e ->
                Log.w(TAG, "Update fehlgeschlagen", e)
                val reason = (e as? UpdateException)?.reason ?: UpdateState.Reason.NETWORK
                _state.value = UpdateState.Failed(reason, e.message, release)
            }
        }
    }

    private class UpdateException(val reason: UpdateState.Reason, message: String) : IOException(message)

    private fun downloadVerifyCommit(release: ReleaseInfo) {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val name = UpdateLogic.apkName(release.version)
        val expected = UpdateLogic.sumFor(get(release.sumsUrl), name)
            ?: throw UpdateException(UpdateState.Reason.CHECKSUM, "$name fehlt in SHA256SUMS")
        val apk = File(dir, name)
        _state.value = UpdateState.Downloading(release, 0, release.apkSize)
        val actual = download(release, apk)
        _state.value = UpdateState.Verifying(release)
        if (!UpdateLogic.checksumMatches(actual, expected)) {
            apk.delete()
            throw UpdateException(UpdateState.Reason.CHECKSUM, "Prüfsumme stimmt nicht")
        }
        verifyArchive(apk, release)
        _state.value = UpdateState.Installing(release)
        commit(apk)
    }

    private fun open(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.instanceFollowRedirects = true
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "LauncherTOS-Updater")
        return c
    }

    private fun get(url: String): String {
        val c = open(url)
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.use { it.readBytes().decodeToString() }
        } finally {
            c.disconnect()
        }
    }

    private fun download(release: ReleaseInfo, to: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val c = open(release.apkUrl)
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            val total = c.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            var done = 0L
            var lastReport = 0L
            c.inputStream.use { input ->
                to.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        done += n
                        if (done - lastReport > 128 * 1024) {
                            lastReport = done
                            _state.value = UpdateState.Downloading(release, done, total)
                        }
                    }
                }
            }
        } finally {
            c.disconnect()
        }
        return UpdateLogic.hex(digest.digest())
    }

    @Suppress("DEPRECATION")
    private fun verifyArchive(apk: File, release: ReleaseInfo) {
        val pm = ctx.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info: PackageInfo = pm.getPackageArchiveInfo(apk.path, flags)
            ?: throw UpdateException(UpdateState.Reason.PACKAGE, "keine gültige APK")
        if (info.packageName != ctx.packageName) throw UpdateException(UpdateState.Reason.PACKAGE, "Paket ${info.packageName}")
        if (info.longVersionCode <= installedCode || info.longVersionCode != release.version.code.toLong()) {
            throw UpdateException(UpdateState.Reason.PACKAGE, "Version ${info.longVersionCode}")
        }
        val certs: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val si = info.signingInfo
            when {
                si == null -> emptyList()
                si.hasMultipleSigners() -> si.apkContentsSigners.map { it.toByteArray() }
                else -> si.signingCertificateHistory.takeLast(1).map { it.toByteArray() }
            }
        } else {
            info.signatures?.map { it.toByteArray() }.orEmpty()
        }
        if (!UpdateLogic.certificateMatches(certs)) throw UpdateException(UpdateState.Reason.SIGNATURE, "Signatur passt nicht")
    }

    private fun commit(apk: File) {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(ctx.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(ctx, InstallReceiver::class.java).setPackage(ctx.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            session.commit(PendingIntent.getBroadcast(ctx, id, intent, flags).intentSender)
        }
    }

    /** Antwort des PackageInstallers. */
    internal fun onInstallResult(status: Int, message: String?) {
        val release = (state.value as? UpdateState.Installing)?.release
        _state.value = when (status) {
            PackageInstaller.STATUS_SUCCESS -> UpdateState.UpToDate
            PackageInstaller.STATUS_PENDING_USER_ACTION -> state.value
            else -> UpdateState.Failed(UpdateState.Reason.INSTALL, message, release)
        }
    }

    fun reset() {
        _state.value = UpdateState.Idle
    }

    companion object {
        private const val TAG = "TosUpdate"
        const val API_URL = "https://api.github.com/repos/ra5on/Launcher-TOS/releases/latest"

        /** Prozessweite Instanz für den Empfänger (gesetzt von der Anwendung). */
        @Volatile
        var instance: Updater? = null
    }
}

/** Ergebnis der Installations-Sitzung: Bestätigung von Android anzeigen oder Zustand melden. */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_INTENT)
            }
            confirm?.let { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
        }
        Updater.instance?.onInstallResult(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
    }
}
