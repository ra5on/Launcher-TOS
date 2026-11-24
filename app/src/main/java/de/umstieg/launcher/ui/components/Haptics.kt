package de.umstieg.launcher.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import de.umstieg.launcher.core.HapticStrength
import de.umstieg.launcher.core.LauncherSettings

/** Haptisches Feedback ohne VIBRATE-Berechtigung (über die View), abschaltbar. */
fun View.tosHaptic(settings: LauncherSettings) {
    if (!settings.haptics) return
    val constant = when (settings.hapticStrength) {
        HapticStrength.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
        HapticStrength.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
        HapticStrength.STRONG -> HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}
