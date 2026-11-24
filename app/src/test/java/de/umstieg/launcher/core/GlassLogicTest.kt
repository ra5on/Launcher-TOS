package de.umstieg.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassLogicTest {
    @Test
    fun defaultTintIsClearlyVisible() {
        assertEquals(0.41f, GlassLogic.fillAlpha(50, false), 0.001f)
        assertTrue(GlassLogic.fillAlpha(50, true) > 0.55f)
        assertEquals(0.10f, GlassLogic.fillAlpha(0, false), 0.001f)
        assertTrue(GlassLogic.fillAlpha(100, true) <= 0.85f)
        assertTrue(GlassLogic.fillAlpha(70, false) > GlassLogic.fillAlpha(30, false))
    }

    @Test
    fun lightPhotoDetectedWithoutSystemHint() {
        assertTrue(GlassLogic.isLightWallpaper(true, 0.1f, null, null))
        assertTrue(GlassLogic.isLightWallpaper(false, 0.62f, 0.3f, 0.2f))
        assertTrue(GlassLogic.isLightWallpaper(false, 0.45f, 0.5f, 0.48f))
        assertFalse(GlassLogic.isLightWallpaper(false, 0.2f, 0.3f, 0.1f))
        assertFalse(GlassLogic.isLightWallpaper(false, null, null, null))
    }
}
