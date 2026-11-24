package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

class SuperellipseTest {
    @Test
    fun exponent_matchesFormula() {
        assertEquals(12f, Superellipse.exponent(0f), 1e-6f)
        assertEquals(2f, Superellipse.exponent(1f), 1e-6f)
        assertEquals(5.8f, Superellipse.exponent(0.62f), 1e-4f)
        // Außerhalb des Bereichs wird begrenzt.
        assertEquals(2f, Superellipse.exponent(3f), 1e-6f)
    }

    @Test
    fun fullRounding_isCircle() {
        val p = Superellipse.points(1f, 64)
        for (i in 0 until 64) {
            assertEquals(1.0, hypot(p[2 * i].toDouble(), p[2 * i + 1].toDouble()), 1e-5)
        }
    }

    @Test
    fun zeroRounding_isAlmostSquare() {
        val p = Superellipse.points(0f, 128)
        // Jeder Punkt liegt nahe am Quadratrand (Maximum-Norm ≈ 1) …
        for (i in 0 until 128) {
            val inf = max(abs(p[2 * i]), abs(p[2 * i + 1]))
            assertTrue("Punkt $i zu weit innen: $inf", inf > 0.94f)
        }
        // … auch die Diagonale reicht fast bis in die Ecke (Kreis: 0,707).
        val diag = p[2 * 16] // 45° bei 128 Schritten
        assertTrue(diag > 0.94f)
    }

    @Test
    fun points_satisfyEquation() {
        val r = 0.62f
        val n = Superellipse.exponent(r).toDouble()
        val p = Superellipse.points(r, 96)
        for (i in 0 until 96) {
            val v = Math.pow(abs(p[2 * i]).toDouble(), n) + Math.pow(abs(p[2 * i + 1]).toDouble(), n)
            assertEquals(1.0, v, 1e-4)
        }
    }
}
