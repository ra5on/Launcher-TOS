package de.umstieg.launcher.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import de.umstieg.launcher.core.ThemeMode

/** Eigene Design-Werte (Akzent Mint, Glas-Flächen, Radien). */
object Tos {
    val Accent = Color(0xFF2EC4A6)
    val AccentInk = Color(0xFF04261F)
    val Danger = Color(0xFFF2545B)
    val Ink = Color.White
    val Ink2 = Color.White.copy(alpha = 0.72f)
    val Ink3 = Color.White.copy(alpha = 0.50f)
    val Glass = Color.White.copy(alpha = 0.13f)
    val GlassStrong = Color.White.copy(alpha = 0.20f)
    /** Dunkles Glas für Bedienelemente über hellen Hintergründen (Kontrast). */
    val GlassControl = Color(0xFF0E121A).copy(alpha = 0.55f)
    val GlassDark = Color(0xFF0E121A).copy(alpha = 0.38f)
    val GlassLine = Color.White.copy(alpha = 0.16f)
    val CardShape = RoundedCornerShape(20.dp)
    val PanelShape = RoundedCornerShape(36.dp)
    val PillShape = RoundedCornerShape(999.dp)
}

/** Farben für Panels und Einstellungen je nach Hell/Dunkel. */
@Immutable
data class PanelColors(
    val dark: Boolean,
    val background: Color,
    val backgroundTop: Color,
    val glass: Color,
    val glassLine: Color,
    val content: Color,
    val secondary: Color,
    val track: Color,
    val chip: Color,
    val scrim: Color,
)

val DarkPanel = PanelColors(
    dark = true,
    background = Color(0xFF0E141F),
    backgroundTop = Color(0xFF1C3354),
    glass = Color.White.copy(alpha = 0.08f),
    glassLine = Color.White.copy(alpha = 0.12f),
    content = Color.White,
    secondary = Color.White.copy(alpha = 0.62f),
    track = Color.White.copy(alpha = 0.18f),
    chip = Color.White.copy(alpha = 0.10f),
    scrim = Color(0xFF06080E).copy(alpha = 0.45f),
)

val LightPanel = PanelColors(
    dark = false,
    background = Color(0xFFF2F4F7),
    backgroundTop = Color(0xFFDDEFEA),
    glass = Color.White.copy(alpha = 0.82f),
    glassLine = Color(0xFF10141C).copy(alpha = 0.08f),
    content = Color(0xFF10141C),
    secondary = Color(0xFF10141C).copy(alpha = 0.60f),
    track = Color(0xFF10141C).copy(alpha = 0.12f),
    chip = Color(0xFF10141C).copy(alpha = 0.06f),
    scrim = Color(0xFF06080E).copy(alpha = 0.25f),
)

val LocalPanelColors = staticCompositionLocalOf { DarkPanel }

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private fun typography(): Typography {
    val base = Typography()
    fun TextStyle.inter() = copy(fontFamily = Inter)
    return Typography(
        displayLarge = base.displayLarge.inter(),
        displayMedium = base.displayMedium.inter(),
        displaySmall = base.displaySmall.inter(),
        headlineLarge = base.headlineLarge.inter(),
        headlineMedium = base.headlineMedium.inter(),
        headlineSmall = base.headlineSmall.inter(),
        titleLarge = base.titleLarge.inter(),
        titleMedium = base.titleMedium.inter(),
        titleSmall = base.titleSmall.inter(),
        bodyLarge = base.bodyLarge.inter(),
        bodyMedium = base.bodyMedium.inter(),
        bodySmall = base.bodySmall.inter(),
        labelLarge = base.labelLarge.inter(),
        labelMedium = base.labelMedium.inter(),
        labelSmall = base.labelSmall.inter(),
    )
}

/** Material-Theme mit Inter und Mint-Akzent; [dark] steuert Panels und Einstellungen. */
@Composable
fun TosTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) {
        darkColorScheme(primary = Tos.Accent, onPrimary = Tos.AccentInk, secondary = Tos.Accent, error = Tos.Danger)
    } else {
        lightColorScheme(primary = Color(0xFF14917A), onPrimary = Color.White, secondary = Tos.Accent, error = Tos.Danger)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalPanelColors provides if (dark) DarkPanel else LightPanel) {
        MaterialTheme(colorScheme = scheme, typography = typography(), content = content)
    }
}

/** Glas-Fläche: halbtransparente Füllung mit feiner heller Kante (echte Unschärfe des Systemhintergrunds ist nicht möglich). */
fun Modifier.glass(shape: Shape, fill: Color = Tos.Glass, line: Color = Tos.GlassLine): Modifier =
    this.background(fill, shape).border(1.dp, line, shape)
