package de.umstieg.launcher.core

import kotlin.math.abs

/** Ordnet eine Wischbewegung einer [Gesture] zu (reine Geometrie, Werte relativ 0..1). */
object GestureClassifier {
    /** Wisch-Bereich oben: obere 30 % des Bildschirms. */
    const val TOP_REGION = 0.30f

    /** Mindeststrecke relativ zur Bildschirmhöhe. */
    const val MIN_DISTANCE = 0.08f

    /**
     * [startX]/[startY]: Startpunkt relativ zur Breite/Höhe, [dx]/[dy]: Strecke relativ zur Höhe.
     * Senkrechte Bewegungen nach unten oben links/rechts (je [zoneLeft]/[zoneRight] % der Breite)
     * sind die Ecken-Gesten, sonst „nach unten“; nach oben ist „nach oben“. Waagerecht: keine Geste
     * (das Blättern übernimmt der Pager).
     */
    fun classify(startX: Float, startY: Float, dx: Float, dy: Float, zoneLeft: Int, zoneRight: Int): Gesture? {
        if (abs(dy) < MIN_DISTANCE || abs(dy) < abs(dx) * 1.3f) return null
        if (dy < 0) return Gesture.SWIPE_UP
        if (startY <= TOP_REGION) {
            if (startX <= zoneLeft / 100f) return Gesture.SWIPE_TOP_LEFT
            if (startX >= 1f - zoneRight / 100f) return Gesture.SWIPE_TOP_RIGHT
        }
        return Gesture.SWIPE_DOWN
    }

    /** Doppeltipp: unten (untere 20 %) oder auf leerer Fläche. */
    fun doubleTap(y: Float): Gesture = if (y >= 0.80f) Gesture.DOUBLE_TAP_BOTTOM else Gesture.DOUBLE_TAP

    fun binding(settings: LauncherSettings, gesture: Gesture): GestureBinding =
        settings.gestures[gesture] ?: LauncherSettings.DEFAULT_GESTURES.getValue(gesture)
}
