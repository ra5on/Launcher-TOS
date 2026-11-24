package de.umstieg.launcher.core

import kotlin.math.abs

/** Ordnet eine Wischbewegung einer [Gesture] zu (reine Geometrie, Werte relativ 0..1). */
object GestureClassifier {
    /** Mindeststrecke relativ zur Bildschirmhöhe. */
    const val MIN_DISTANCE = 0.08f

    /**
     * [startX]/[startY]: Startpunkt relativ zu Breite/Höhe, [dx]/[dy]: Strecke relativ zur Höhe,
     * [edgeBottom]: Unterkante der oberen Randzone relativ zur Höhe.
     * Nach unten mit Start in der Randzone: geteilt links/Mitte/rechts (Breiten [zoneLeft]/[zoneRight] %)
     * oder ungeteilt eine Zone. Nach unten mit Start darunter: „Bildschirmmitte“. Nach oben: [Gesture.SWIPE_UP].
     * Waagerecht: keine Geste (das Blättern übernimmt der Pager).
     */
    fun classify(
        startX: Float,
        startY: Float,
        dx: Float,
        dy: Float,
        edgeBottom: Float,
        split: Boolean,
        zoneLeft: Int,
        zoneRight: Int,
    ): Gesture? {
        if (abs(dy) < MIN_DISTANCE || abs(dy) < abs(dx) * 1.3f) return null
        if (dy < 0) return Gesture.SWIPE_UP
        if (startY > edgeBottom) return Gesture.SWIPE_DOWN
        if (!split) return Gesture.EDGE
        return when {
            startX <= zoneLeft / 100f -> Gesture.EDGE_LEFT
            startX >= 1f - zoneRight / 100f -> Gesture.EDGE_RIGHT
            else -> Gesture.EDGE_CENTER
        }
    }

    /** Doppeltipp: unten (untere 20 %) oder auf leerer Fläche. */
    fun doubleTap(y: Float): Gesture = if (y >= 0.80f) Gesture.DOUBLE_TAP_BOTTOM else Gesture.DOUBLE_TAP

    fun binding(settings: LauncherSettings, gesture: Gesture): GestureBinding =
        settings.gestures[gesture] ?: LauncherSettings.DEFAULT_GESTURES.getValue(gesture)

    /** Gesten, die bei der aktuellen Rand-Aufteilung gelten (für die Einstellungsseite). */
    fun active(split: Boolean): List<Gesture> =
        (if (split) listOf(Gesture.EDGE_LEFT, Gesture.EDGE_CENTER, Gesture.EDGE_RIGHT) else listOf(Gesture.EDGE)) +
            listOf(Gesture.SWIPE_DOWN, Gesture.SWIPE_UP, Gesture.DOUBLE_TAP, Gesture.DOUBLE_TAP_BOTTOM)

    /** Öffnet irgendeine aktive Geste die Suche? (Sonst Hinweis: Lupe im Seitenindikator.) */
    fun searchReachableByGesture(s: LauncherSettings): Boolean =
        active(s.edgeSplit).any { binding(s, it).action == GestureAction.SEARCH }
}

/** Gesten-Vorlagen und das Umspringen auf „Eigen“. */
object GesturePresets {
    private fun b(a: GestureAction) = GestureBinding(a)

    /** Rand-Aufteilung einer Vorlage. */
    fun split(preset: GesturePreset): Boolean = preset == GesturePreset.TOS

    /** Belegung einer Vorlage ([GesturePreset.CUSTOM] hat keine eigene). */
    fun bindings(preset: GesturePreset): Map<Gesture, GestureBinding>? = when (preset) {
        GesturePreset.TOS -> LauncherSettings.DEFAULT_GESTURES
        GesturePreset.ANDROID -> LauncherSettings.DEFAULT_GESTURES + mapOf(
            Gesture.EDGE_LEFT to b(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.EDGE_CENTER to b(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.EDGE_RIGHT to b(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.EDGE to b(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.SWIPE_DOWN to b(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.SWIPE_UP to b(GestureAction.SEARCH),
        )
        GesturePreset.ANDROID_SEARCH -> LauncherSettings.DEFAULT_GESTURES + mapOf(
            Gesture.EDGE_LEFT to b(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.EDGE_CENTER to b(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.EDGE_RIGHT to b(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.EDGE to b(GestureAction.SYSTEM_NOTIFICATIONS),
            Gesture.SWIPE_DOWN to b(GestureAction.SEARCH),
            Gesture.SWIPE_UP to b(GestureAction.NOTHING),
        )
        GesturePreset.CUSTOM -> null
    }

    /** Vorlage anwenden (Belegung + Rand-Aufteilung). „Eigen“ behält die aktuelle Belegung. */
    fun apply(s: LauncherSettings, preset: GesturePreset): LauncherSettings {
        val map = bindings(preset) ?: return s.copy(gesturePreset = GesturePreset.CUSTOM)
        return s.copy(gestures = map, edgeSplit = split(preset), gesturePreset = preset)
    }

    /** Welche Vorlage passt zur Belegung der aktiven Gesten? Keine → „Eigen“. */
    fun detect(s: LauncherSettings): GesturePreset {
        val active = GestureClassifier.active(s.edgeSplit)
        return GesturePreset.entries.firstOrNull { p ->
            val map = bindings(p) ?: return@firstOrNull false
            split(p) == s.edgeSplit && active.all { g -> GestureClassifier.binding(s, g) == map.getValue(g) }
        } ?: GesturePreset.CUSTOM
    }

    /** Eine Geste einzeln ändern: die Vorlage springt auf „Eigen“ (außer die Belegung entspricht wieder einer Vorlage). */
    fun withGesture(s: LauncherSettings, gesture: Gesture, binding: GestureBinding): LauncherSettings {
        val next = s.copy(gestures = s.gestures + (gesture to binding))
        return next.copy(gesturePreset = detect(next))
    }

    /** Rand-Aufteilung ändern (wirkt wie eine Einzeländerung). */
    fun withSplit(s: LauncherSettings, split: Boolean): LauncherSettings {
        val next = s.copy(edgeSplit = split)
        return next.copy(gesturePreset = detect(next))
    }
}
