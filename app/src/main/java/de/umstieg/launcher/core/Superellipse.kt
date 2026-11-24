package de.umstieg.launcher.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Superellipse |x|^n + |y|^n = 1 als Icon-Maske.
 * Rundung r = 0..1 ergibt n = 12 − r·10: r = 0 fast eckig (n = 12), r = 1 Kreis (n = 2).
 */
object Superellipse {
    fun exponent(rounding: Float): Float = 12f - rounding.coerceIn(0f, 1f) * 10f

    /** Punkte auf dem Rand im Einheitsquadrat [-1, 1], abwechselnd x und y. */
    fun points(rounding: Float, steps: Int = 128): FloatArray {
        val n = exponent(rounding).toDouble()
        val out = FloatArray(steps * 2)
        for (k in 0 until steps) {
            val t = 2.0 * PI * k / steps
            val c = cos(t)
            val s = sin(t)
            out[2 * k] = (sign(c) * abs(c).pow(2.0 / n)).toFloat()
            out[2 * k + 1] = (sign(s) * abs(s).pow(2.0 / n)).toFloat()
        }
        return out
    }
}
