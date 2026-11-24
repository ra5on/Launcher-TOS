package de.umstieg.launcher.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat

/** App-Kurzbefehl (z. B. „Neuer Tab“) für die Suche. */
class ShortcutEntry(val label: String, val packageName: String, val info: ShortcutInfo)

class ContactEntry(val name: String, val uri: Uri)

class FileEntry(val name: String, val uri: Uri, val mime: String?)

/**
 * Zusätzliche Quellen der Suche. Kontakte und Dateien nur mit erteilter Berechtigung und eingeschaltetem
 * Abschnitt; alles bleibt auf dem Gerät.
 */
class SearchSources(context: Context) {
    private val ctx = context.applicationContext
    private val launcherApps = ctx.getSystemService(LauncherApps::class.java)

    /** Alle Kurzbefehle der Apps (nur als Standard-Launcher erlaubt). */
    fun shortcuts(): List<ShortcutEntry> = runCatching {
        if (!launcherApps.hasShortcutHostPermission()) return emptyList()
        val um = ctx.getSystemService(UserManager::class.java)
        val q = LauncherApps.ShortcutQuery().setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
        )
        um.userProfiles.flatMap { user ->
            runCatching { launcherApps.getShortcuts(q, user).orEmpty() }.getOrDefault(emptyList())
        }.filter { it.isEnabled }.mapNotNull { info ->
            val label = (info.shortLabel ?: info.longLabel)?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ShortcutEntry(label, info.`package`, info)
        }.distinctBy { it.packageName + "/" + it.info.id }
    }.getOrDefault(emptyList())

    fun startShortcut(e: ShortcutEntry): Boolean = runCatching {
        launcherApps.startShortcut(e.info, null, null)
        true
    }.getOrDefault(false)

    fun canReadContacts(): Boolean = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun contacts(query: String, limit: Int = 5): List<ContactEntry> {
        if (!canReadContacts() || query.isBlank()) return emptyList()
        return runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(query.trim()))
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null)?.use { c ->
                val out = mutableListOf<ContactEntry>()
                while (c.moveToNext() && out.size < limit) {
                    val name = c.getString(2) ?: continue
                    out += ContactEntry(name, ContactsContract.Contacts.getLookupUri(c.getLong(0), c.getString(1)))
                }
                out
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    /** Benötigte Berechtigungen für Dateien (Bilder, Videos, Musik). */
    fun filePermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun canReadFiles(): Boolean = filePermissions().any { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }

    fun files(query: String, limit: Int = 5): List<FileEntry> {
        if (!canReadFiles() || query.trim().length < 2) return emptyList()
        return runCatching {
            val collection = MediaStore.Files.getContentUri("external")
            ctx.contentResolver.query(
                collection,
                arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.MIME_TYPE),
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?",
                arrayOf("%" + query.trim().replace("%", "") + "%"),
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
            )?.use { c ->
                val out = mutableListOf<FileEntry>()
                while (c.moveToNext() && out.size < limit) {
                    out += FileEntry(c.getString(1) ?: continue, android.content.ContentUris.withAppendedId(collection, c.getLong(0)), c.getString(2))
                }
                out
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    fun openFileIntent(f: FileEntry): Intent = Intent(Intent.ACTION_VIEW).setDataAndType(f.uri, f.mime ?: "*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

    fun openContactIntent(c: ContactEntry): Intent = Intent(Intent.ACTION_VIEW, c.uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    @Suppress("unused")
    private val me = Process.myUserHandle()
}
