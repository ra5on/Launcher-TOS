package de.umstieg.launcher.ui.icons

import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import de.umstieg.launcher.core.IconOptions
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.IconLoader

/** Lädt das Icon asynchron (Cache zuerst, sonst im Hintergrund). */
@Composable
fun rememberAppIcon(loader: IconLoader, app: AppInfo, legacy: IconOptions): ImageBitmap? {
    val icon by produceState(loader.cached(app, legacy), app.key, legacy, loader.generation) {
        value = loader.load(app, legacy)
    }
    return icon
}

/** Merkt sich die Shape je Rundungswert (Pfad wird pro Größe gebaut). */
@Composable
fun rememberSuperellipse(roundingPercent: Int): SuperellipseShape =
    remember(roundingPercent) { SuperellipseShape(roundingPercent / 100f) }

/**
 * Zeichnet [bitmap] zugeschnitten auf die Superellipse – mit Kantenglättung über einen
 * Bitmap-Shader statt clipPath, plus leichtem Schatten.
 */
fun Modifier.squircleBitmap(bitmap: ImageBitmap?, shape: SuperellipseShape, placeholder: Color = Color.White.copy(alpha = 0.18f)): Modifier =
    drawWithCache {
        val path = shape.path(size)
        val androidPath = path.asAndroidPath()
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(46, 0, 0, 0) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (bitmap != null) {
            val ab = bitmap.asAndroidBitmap()
            paint.shader = BitmapShader(ab, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply { setScale(size.width / ab.width, size.height / ab.height) })
            }
        } else {
            paint.color = android.graphics.Color.argb(
                (placeholder.alpha * 255).toInt(), (placeholder.red * 255).toInt(),
                (placeholder.green * 255).toInt(), (placeholder.blue * 255).toInt(),
            )
        }
        val shadowDy = size.height * 0.035f
        onDrawBehind {
            translate(0f, shadowDy) { drawContext.canvas.nativeCanvas.drawPath(androidPath, shadowPaint) }
            drawContext.canvas.nativeCanvas.drawPath(androidPath, paint)
        }
    }

/** Reines Icon (ohne Beschriftung) in der Größe [size]. */
@Composable
fun AppIconImage(loader: IconLoader, app: AppInfo, legacy: IconOptions, shape: SuperellipseShape, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = rememberAppIcon(loader, app, legacy)
    Box(modifier.size(size).squircleBitmap(bitmap, shape))
}

