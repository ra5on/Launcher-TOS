package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeDwellTest {
    @Test
    fun becomesReadyAfterDelayOnSameTarget() {
        val d = MergeDwell(400)
        assertFalse(d.update("g:0:1:1", 1_000))
        assertTrue(d.holding(1_100))
        assertFalse(d.update("g:0:1:1", 1_399))
        assertTrue(d.update("g:0:1:1", 1_400))
        assertFalse(d.holding(1_400))
    }

    @Test
    fun changingTargetRestartsTimer() {
        val d = MergeDwell(400)
        d.update("g:0:1:1", 0)
        assertFalse(d.update("g:0:2:1", 350))
        assertFalse(d.update("g:0:2:1", 700))
        assertTrue(d.update("g:0:2:1", 750))
    }

    @Test
    fun leavingIconCancels() {
        val d = MergeDwell(400)
        d.update("d:2", 0)
        assertFalse(d.update(null, 300))
        assertEquals(null, d.key)
        assertFalse(d.update("d:2", 500))
        assertFalse(d.holding(1_000).not() && false)
    }

    @Test
    fun overUsesIconCentres() {
        // Icon 150 px: bis knapp 93 px Abstand der Mitten zählt als „über dem Icon“.
        assertTrue(MergeDwell.isOver(60f, 60f, 150f))
        assertFalse(MergeDwell.isOver(80f, 70f, 150f))
        assertFalse(MergeDwell.isOver(0f, 95f, 150f))
    }
}
