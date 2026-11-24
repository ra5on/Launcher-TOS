package de.umstieg.launcher.demo

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

/**
 * Sendet die Demo-Benachrichtigungen dieser App (deutsche Beispieltexte) und startet bei „Musik“
 * eine Mediensitzung mit erfundenem Titel für die Medienkarte des Schnellzugriffs.
 */
class NotifyActivity : Activity() {
    private var session: MediaSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST)
        } else {
            post()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST) post()
    }

    private fun show(text: String) {
        setContentView(TextView(this).apply {
            this.text = text
            textSize = 20f
            gravity = Gravity.CENTER
        })
    }

    private fun post() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            show(getString(R.string.notify_denied))
            return
        }
        val name = getString(R.string.app_name)
        // Leise: erscheint in der Liste und als Badge, aber ohne Symbol in der Statusleiste (sauberere Store-Bilder).
        nm.deleteNotificationChannel("demo")
        nm.createNotificationChannel(NotificationChannel(CHANNEL, name, NotificationManager.IMPORTANCE_LOW))
        nm.cancelAll()
        val entries = resources.getStringArray(R.array.demo_notifications)
        val now = System.currentTimeMillis()
        entries.forEachIndexed { i, entry ->
            val (title, text) = entry.split('|', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            val n = Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_demo_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                // Gestaffelte Zeiten, damit „Neu“/„Früher“ und die Zeitangaben echt aussehen.
                .setWhen(now - i * 17L * 60_000L)
                .setShowWhen(true)
                .build()
            nm.notify(i + 1, n)
        }
        if (resources.getBoolean(R.bool.demo_media)) startMedia()
        show(getString(R.string.notify_done, entries.size))
    }

    private fun startMedia() {
        val s = MediaSession(this, "demo")
        s.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, getString(R.string.media_title))
                .putString(MediaMetadata.METADATA_KEY_ARTIST, getString(R.string.media_artist))
                .putString(MediaMetadata.METADATA_KEY_ALBUM, getString(R.string.media_album))
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 215_000L)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover())
                .build(),
        )
        s.setPlaybackState(
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 84_000L, 1f)
                .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .build(),
        )
        s.isActive = true
        session = s
    }

    /** Selbst gemaltes Cover (Verlauf mit Kreis), keine fremden Bilder. */
    private fun cover(): Bitmap {
        val size = 512
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), 0xFFF0A86A.toInt(), 0xFF7A4BE0.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        p.shader = null
        p.color = 0x55FFFFFF
        p.style = Paint.Style.STROKE
        p.strokeWidth = 28f
        c.drawCircle(size * 0.7f, size * 0.72f, size * 0.32f, p)
        return b
    }

    override fun onDestroy() {
        session?.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "demo_silent"
        private const val REQUEST = 1
    }
}
