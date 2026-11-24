package de.umstieg.launcher.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/** Version X.Y.Z (Tag „vX.Y.Z“); versionCode = X·100 + Y·10 + Z. */
data class AppVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<AppVersion> {
    val name: String get() = "$major.$minor.$patch"
    val code: Int get() = major * 100 + minor * 10 + patch

    override fun compareTo(other: AppVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    companion object {
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

        fun parse(text: String): AppVersion? {
            val m = PATTERN.matchEntire(text.trim()) ?: return null
            val (a, b, c) = m.destructured
            val v = AppVersion(a.toInt(), b.toInt(), c.toInt())
            // Der versionCode-Aufbau trägt nur einstellige Neben- und Patch-Nummern.
            return v.takeIf { it.minor in 0..9 && it.patch in 0..9 }
        }
    }
}

/** Eine Version auf GitHub, soweit das Update sie braucht. */
data class ReleaseInfo(
    val version: AppVersion,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val sumsUrl: String,
)

/** Reine Logik des Selbst-Updates über GitHub (Versionsvergleich, Prüfsummen, Zertifikat). */
object UpdateLogic {
    const val ASSET_PREFIX = "LauncherTOS-"
    const val SUMS_NAME = "SHA256SUMS"

    /** SHA-256 des Signaturzertifikats der veröffentlichten Releases (Upload-Schlüssel). */
    const val RELEASE_CERT_SHA256 = "2142791db9ee6283bbe1b7c05b5205ae96c433e363f4fe78fe71a5486c4a063c"

    const val CHECK_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000

    @Serializable
    private data class GhAsset(val name: String, @SerialName("browser_download_url") val url: String, val size: Long = 0)

    @Serializable
    private data class GhRelease(
        @SerialName("tag_name") val tag: String,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val body: String? = null,
        val assets: List<GhAsset> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Liest die Antwort von „releases/latest“; null bei Entwurf, Vorabversion oder fehlenden Dateien. */
    fun parseLatest(text: String): ReleaseInfo? {
        val r = runCatching { json.decodeFromString(GhRelease.serializer(), text) }.getOrNull() ?: return null
        if (r.draft || r.prerelease) return null
        val version = AppVersion.parse(r.tag) ?: return null
        val apk = r.assets.firstOrNull { it.name == apkName(version) } ?: return null
        val sums = r.assets.firstOrNull { it.name == SUMS_NAME } ?: return null
        return ReleaseInfo(version, plainNotes(r.body.orEmpty()), apk.url, apk.size, sums.url)
    }

    fun apkName(v: AppVersion) = "$ASSET_PREFIX${v.name}.apk"

    /** Neuer als die installierte Version? */
    fun isNewer(remote: AppVersion, installedName: String?, installedCode: Long): Boolean {
        val installed = installedName?.let { AppVersion.parse(it) }
        return if (installed != null) remote > installed else remote.code > installedCode
    }

    /** Prüfsumme für [name] aus einer SHA256SUMS-Datei (`<hex>  <name>` bzw. `<hex> *<name>`). */
    fun sumFor(sums: String, name: String): String? = sums.lines()
        .map { it.trim().split(Regex("\\s+"), limit = 2) }
        .firstOrNull { it.size == 2 && it[1].trimStart('*') == name }
        ?.get(0)?.lowercase()?.takeIf { it.length == 64 && it.all { c -> c in "0123456789abcdef" } }

    fun sha256Hex(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    /** Fingerabdruck in einheitlicher Form (klein, ohne Doppelpunkte/Leerzeichen). */
    fun normalizeFingerprint(f: String): String = f.lowercase().filter { it in "0123456789abcdef" }

    fun checksumMatches(actualHex: String, expectedHex: String?): Boolean =
        expectedHex != null && normalizeFingerprint(actualHex) == normalizeFingerprint(expectedHex) && expectedHex.length >= 64

    /** Passt eines der Signaturzertifikate (als DER-Bytes) zum erwarteten Fingerabdruck? Genau ein Zertifikat verlangt. */
    fun certificateMatches(certs: List<ByteArray>, expected: String = RELEASE_CERT_SHA256): Boolean =
        certs.size == 1 && sha256Hex(certs.single()) == normalizeFingerprint(expected)

    /** Fällig, wenn seit der letzten Prüfung mehr als eine Woche vergangen ist (oder die Uhr zurückgesprungen ist). */
    fun dueForCheck(lastCheckMs: Long, nowMs: Long, intervalMs: Long = CHECK_INTERVAL_MS): Boolean =
        lastCheckMs <= 0L || nowMs - lastCheckMs >= intervalMs || nowMs < lastCheckMs

    /** Versionshinweise ohne Markdown-Zeichen, für die Anzeige als Text. */
    fun plainNotes(md: String): String = md.lines().joinToString("\n") { line ->
        line.replace(Regex("""\*\*(.+?)\*\*"""), "$1")
            .replace(Regex("""`([^`]+)`"""), "$1")
            .replace(Regex("""\[([^\]]+)]\([^)]+\)"""), "$1")
            .replace(Regex("""^#+\s*"""), "")
            .replace(Regex("""^(\s*)[-*] """), "$1• ")
    }.trim()
}
