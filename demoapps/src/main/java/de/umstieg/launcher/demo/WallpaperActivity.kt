package de.umstieg.launcher.demo

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.TextView
import kotlin.math.max

/** Setzt einen selbst gemalten Verlauf (Farben aus design/assets/kit.css) als Hintergrund. */
class WallpaperActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dm = resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w * 0.17f, h.toFloat(), intArrayOf(0xFF1B2A4A.toInt(), 0xFF14243A.toInt(), 0xFF2A1E3A.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        fun blob(cx: Float, cy: Float, r: Float, color: Int) {
            p.shader = RadialGradient(w * cx, h * cy, max(w, h) * r, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        }
        blob(0.18f, 0.12f, 0.42f, 0xD9F0A86A.toInt())
        blob(0.90f, 0.30f, 0.40f, 0xD97A4BA8.toInt())
        blob(0.20f, 0.70f, 0.42f, 0xD910656A.toInt())
        blob(0.85f, 0.92f, 0.36f, 0xD9E0697A.toInt())
        WallpaperManager.getInstance(this).setBitmap(b)
        setContentView(TextView(this).apply { text = getString(R.string.wallpaper_done) })
        finish()
    }
}
