package de.umstieg.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import de.umstieg.launcher.core.IconOptions
import de.umstieg.launcher.core.LegacyBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Lädt Original-Icons und rendert sie als volle quadratische Bitmap (S × S).
 * Adaptive Icons: Hinter- und Vordergrund mit Bounds (−S/4 … 5S/4) gezeichnet, sichtbar ist
 * damit der innere Bereich (72 von 108 dp). Die Maske wird NICHT eingebacken; zugeschnitten
 * wird beim Zeichnen mit der Superellipse-Shape. Ältere Icons: auf ca. 70 % verkleinert,
 * mittig auf Weiß oder der dominanten Farbe des Icons.
 */
class IconLoader(context: Context) {
    private val appContext = context.applicationContext
    private val density = appContext.resources.displayMetrics.densityDpi

    /** Größe der Bitmap: reicht für 130 % Icongröße auf dem Gerät. */
    val sizePx: Int = (appContext.resources.displayMetrics.density * MAX_ICON_DP).roundToInt().coerceIn(96, 320)

    /** Wird bei jedem Verwerfen erhöht, damit sichtbare Icons neu laden (Compose-State). */
    var generation by mutableIntStateOf(0)
        private set

    private val cache = object : LruCache<String, ImageBitmap>(cacheBytes()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    // Höchstens vier Icons gleichzeitig rendern.
    private val permits = Semaphore(4)

    /** Hauptfarbe je App (für Badge-Farbe „aus Icon“), beim Rendern ermittelt. */
    private val colors = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun colorOf(app: AppInfo): Int? = colors[app.key.id]

    private fun cacheKey(app: AppInfo, legacy: IconOptions) = "${app.key.id}|$legacy"

    fun cached(app: AppInfo, legacy: IconOptions): ImageBitmap? = cache.get(cacheKey(app, legacy))

    suspend fun load(app: AppInfo, legacy: IconOptions): ImageBitmap {
        val key = cacheKey(app, legacy)
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.Default) {
            permits.withPermit {
                cache.get(key) ?: render(app, legacy).also { cache.put(key, it) }
            }
        }
    }

    /** Icons eines Pakets verwerfen (nach Update/Änderung). */
    fun invalidate(packageName: String) {
        cache.snapshot().keys.filter { it.startsWith("$packageName/") }.forEach { cache.remove(it) }
        generation++
    }

    private fun render(app: AppInfo, legacy: IconOptions): ImageBitmap {
        val s = sizePx
        val bitmap = createBitmap(s, s)
        val canvas = Canvas(bitmap)
        val drawable: Drawable? = runCatching { app.activityInfo.getIcon(density) }.getOrNull()?.mutate()
        val mono = if (legacy.themed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (drawable as? AdaptiveIconDrawable)?.monochrome
        } else {
            null
        }
        when {
            drawable == null -> canvas.drawColor(Color.GRAY)
            mono != null -> {
                // Themen-Icon: einfarbige Ebene auf ruhigem Hintergrund.
                canvas.drawColor(if (legacy.themeDark) THEMED_BG_DARK else THEMED_BG_LIGHT)
                mono.setTint(if (legacy.themeDark) THEMED_FG_DARK else THEMED_FG_LIGHT)
                mono.setBounds(-s / 4, -s / 4, s * 5 / 4, s * 5 / 4)
                mono.draw(canvas)
            }
            drawable is AdaptiveIconDrawable -> {
                drawable.background?.let { it.setBounds(-s / 4, -s / 4, s * 5 / 4, s * 5 / 4); it.draw(canvas) }
                drawable.foreground?.let { it.setBounds(-s / 4, -s / 4, s * 5 / 4, s * 5 / 4); it.draw(canvas) }
            }
            else -> {
                val bg = when (legacy.legacy) {
                    LegacyBackground.WHITE -> Color.WHITE
                    LegacyBackground.DOMINANT -> dominantColor(drawable)
                    LegacyBackground.CUSTOM -> legacy.legacyColor.toInt()
                }
                canvas.drawColor(bg)
                val inset = (s * LEGACY_INSET).roundToInt()
                drawable.setBounds(inset, inset, s - inset, s - inset)
                drawable.draw(canvas)
            }
        }
        colors[app.key.id] = averageColor(bitmap)
        bitmap.prepareToDraw()
        return bitmap.asImageBitmap()
    }

    /** Häufigste (grob gerasterte) deckende Farbe des Icons; fast Weiß/Transparent zählt nicht. */
    private fun dominantColor(drawable: Drawable): Int {
        val n = 24
        val small = createBitmap(n, n)
        drawable.setBounds(0, 0, n, n)
        drawable.draw(Canvas(small))
        val counts = HashMap<Int, Int>()
        val pixels = IntArray(n * n)
        small.getPixels(pixels, 0, n, 0, 0, n, n)
        small.recycle()
        for (p in pixels) {
            if (Color.alpha(p) < 200) continue
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            if (r > 235 && g > 235 && b > 235) continue
            val bucket = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            counts[bucket] = (counts[bucket] ?: 0) + 1
        }
        val best = counts.maxByOrNull { it.value }?.key ?: return Color.WHITE
        return Color.rgb(((best shr 8) and 0xF) * 17, ((best shr 4) and 0xF) * 17, (best and 0xF) * 17)
    }

    /** Mittlere Farbe eines inneren Ausschnitts (kräftig genug für ein Badge). */
    private fun averageColor(b: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(b, 8, 8, true)
        var r = 0
        var g = 0
        var bl = 0
        for (x in 1 until 7) for (y in 1 until 7) {
            val p = small.getPixel(x, y)
            r += Color.red(p)
            g += Color.green(p)
            bl += Color.blue(p)
        }
        if (small != b) small.recycle()
        return Color.rgb(r / 36, g / 36, bl / 36)
    }

    private fun cacheBytes(): Int {
        val max = Runtime.getRuntime().maxMemory()
        return (max / 8).coerceAtMost(48L * 1024 * 1024).toInt()
    }

    companion object {
        private const val MAX_ICON_DP = 80f
        private const val LEGACY_INSET = 0.15f
        private const val THEMED_BG_DARK = 0xFF1E2A33.toInt()
        private const val THEMED_FG_DARK = 0xFFB8EDE1.toInt()
        private const val THEMED_BG_LIGHT = 0xFFE3F4EF.toInt()
        private const val THEMED_FG_LIGHT = 0xFF1B5E52.toInt()
    }
}
