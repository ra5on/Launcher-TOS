package de.umstieg.launcher.ui.icons

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import de.umstieg.launcher.core.Superellipse

/** Compose-Shape aus der Superellipse; [rounding] 0..1 (0 % eckig, 100 % Kreis). */
class SuperellipseShape(val rounding: Float, private val steps: Int = 128) : Shape {
    private val unit = Superellipse.points(rounding, steps)

    fun path(size: Size): Path {
        val hx = size.width / 2f
        val hy = size.height / 2f
        return Path().apply {
            moveTo(hx + unit[0] * hx, hy + unit[1] * hy)
            for (k in 1 until steps) lineTo(hx + unit[2 * k] * hx, hy + unit[2 * k + 1] * hy)
            close()
        }
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(path(size))

    override fun equals(other: Any?): Boolean = other is SuperellipseShape && other.rounding == rounding && other.steps == steps
    override fun hashCode(): Int = rounding.hashCode() * 31 + steps
}
