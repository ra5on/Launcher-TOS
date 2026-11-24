package de.umstieg.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.MediaRouter2
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Eine laufende Mediensitzung (Titel, Interpret, Cover, Zustand). */
data class MediaInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val album: String,
    val art: Bitmap?,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    internal val controller: MediaController,
) {
    fun playPause() {
        if (playing) controller.transportControls.pause() else controller.transportControls.play()
    }

    fun next() = controller.transportControls.skipToNext()
    fun previous() = controller.transportControls.skipToPrevious()
    fun seekTo(ms: Long) = controller.transportControls.seekTo(ms)
}

/**
 * Mediensitzungen über MediaSessionManager – nur möglich, solange der Benachrichtigungszugriff
 * erteilt ist (dafür ist der Listener-Dienst die Berechtigung).
 */
class MediaCenter(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext
    private val msm = appContext.getSystemService(MediaSessionManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val _sessions = MutableStateFlow<List<MediaInfo>>(emptyList())
    val sessions: StateFlow<List<MediaInfo>> = _sessions.asStateFlow()

    private var controllers: List<MediaController> = emptyList()
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = publish()
    }
    private val listener = MediaSessionManager.OnActiveSessionsChangedListener { list -> attach(list.orEmpty()) }
    private var registered = false

    fun start() {
        scope.launch {
            NotificationCenter.connected.collect { connected -> handler.post { if (connected) register() else unregister() } }
        }
    }

    private fun register() {
        if (registered) return
        runCatching {
            val component = NotificationCenter.component(appContext)
            msm.addOnActiveSessionsChangedListener(listener, component, handler)
            attach(msm.getActiveSessions(component))
            registered = true
        }.onFailure { Log.w("TosMedia", "Mediensitzungen nicht verfügbar", it) }
    }

    private fun unregister() {
        if (!registered) return
        runCatching { msm.removeOnActiveSessionsChangedListener(listener) }
        attach(emptyList())
        registered = false
    }

    private fun attach(list: List<MediaController>) {
        controllers.forEach { runCatching { it.unregisterCallback(callback) } }
        controllers = list
        list.forEach { runCatching { it.registerCallback(callback, handler) } }
        publish()
    }

    private fun publish() {
        _sessions.value = controllers.mapNotNull { c ->
            val m = c.metadata ?: return@mapNotNull null
            val state = c.playbackState
            MediaInfo(
                packageName = c.packageName,
                title = m.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
                album = m.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
                art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART),
                playing = state?.state == PlaybackState.STATE_PLAYING,
                positionMs = state?.position ?: 0L,
                durationMs = m.getLong(MediaMetadata.METADATA_KEY_DURATION),
                controller = c,
            )
        }.sortedByDescending { it.playing }
    }

    /** Ausgabe-Auswahl des Systems (Lautsprecher, Kopfhörer …). */
    fun showOutputSwitcher(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            if (runCatching { MediaRouter2.getInstance(context).showSystemOutputSwitcher() }.getOrDefault(false)) return
        }
        runCatching {
            context.startActivity(
                Intent("com.android.settings.panel.action.MEDIA_OUTPUT")
                    .putExtra("com.android.settings.panel.extra.PACKAGE_NAME", sessions.value.firstOrNull()?.packageName ?: context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}
