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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import de.umstieg.launcher.core.Accent
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.core.ThemeMode
import de.umstieg.launcher.core.TriState

/** Eigene Design-Werte (Akzent Mint, Glas-Flächen, Radien). */
object Tos {
    /** Mint (Standard-Akzent). */
    val Mint = Color(0xFF2EC4A6)

    /** Aktuelle Akzentfarbe (Einstellung „Akzentfarbe“). */
    val Accent: Color
        @Composable @ReadOnlyComposable
        get() = LocalTosStyle.current.accent

    /** Schrift auf Akzentflächen (hell oder dunkel je nach Akzent). */
    val AccentInk: Color
        @Composable @ReadOnlyComposable
        get() = LocalTosStyle.current.accentInk
    val Danger = Color(0xFFF2545B)
    val Ink = Color.White
    val Ink2 = Color.White.copy(alpha = 0.72f)
    val Ink3 = Color.White.copy(alpha = 0.50f)
    /** Glas-Füllung nach „Durchsichtig ↔ Getönt“ und „Kontrast erhöhen“. */
    val Glass: Color
        @Composable @ReadOnlyComposable
        get() = LocalTosStyle.current.glassFill
    val GlassStrong = Color.White.copy(alpha = 0.20f)
    /** Dunkles Glas für Bedienelemente über hellen Hintergründen (Kontrast). */
    val GlassControl = Color(0xFF0E121A).copy(alpha = 0.55f)
    val GlassDark = Color(0xFF0E121A).copy(alpha = 0.38f)
    val GlassLine: Color
        @Composable @ReadOnlyComposable
        get() = LocalTosStyle.current.glassLine
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

/** Laufzeit-Stil aus den Einstellungen: Akzent, Glas, Kontrast, Bewegung. */
@Immutable
data class TosStyle(
    val accent: Color = Tos.Mint,
    val accentInk: Color = Color(0xFF04261F),
    val glassFill: Color = Color.White.copy(alpha = 0.13f),
    val glassLine: Color = Color.White.copy(alpha = 0.16f),
    val highContrast: Boolean = false,
    /** Faktor für Animationsdauern; 0 = keine Animationen. */
    val motionScale: Float = 1f,
    val reduceMotion: Boolean = false,
    val blur: Float = 0.6f,
    val dim: Float = 0f,
    /** Heller Hintergrund (Foto): Glas stärker tönen, Texte mit Schatten und Abdunkelung. */
    val lightWallpaper: Boolean = false,
)

val LocalTosStyle = staticCompositionLocalOf { TosStyle() }

/** Akzentfarbe aus der Einstellung; [wallpaper] ist die Hauptfarbe des Hintergrunds (falls bekannt). */
fun accentColor(s: LauncherSettings, wallpaper: Color?): Color = when (s.accent) {
    Accent.MINT -> Tos.Mint
    Accent.BLUE -> Color(0xFF4F8DF7)
    Accent.VIOLET -> Color(0xFF9B7BF2)
    Accent.CORAL -> Color(0xFFF2785C)
    Accent.AMBER -> Color(0xFFF2B33D)
    Accent.WALLPAPER -> wallpaper ?: Tos.Mint
    Accent.CUSTOM -> Color(s.accentCustom.toInt())
}

fun styleFor(s: LauncherSettings, wallpaper: Color?, systemReduceMotion: Boolean, lightWallpaper: Boolean = false): TosStyle {
    val accent = accentColor(s, wallpaper)
    val t = s.glassTint / 100f
    val clear = Color.White.copy(alpha = 0.10f)
    val tinted = Color(0xFF0E121A).copy(alpha = 0.45f)
    var fill = lerp(clear, tinted, t)
    var line = Color.White.copy(alpha = 0.16f)
    if (lightWallpaper) {
        // Auf hellen Fotos sonst kaum lesbar: deutlich dunkler tönen.
        fill = lerp(fill, Color(0xFF0E121A).copy(alpha = 0.62f), 0.6f)
        line = Color.White.copy(alpha = 0.22f)
    }
    if (s.highContrast) {
        fill = fill.copy(alpha = (fill.alpha + 0.25f).coerceAtMost(0.85f))
        line = Color.White.copy(alpha = 0.40f)
    }
    val reduce = when (s.reduceMotion) {
        TriState.SYSTEM -> systemReduceMotion
        TriState.ON -> true
        TriState.OFF -> false
    } || s.simpleMode
    return TosStyle(
        accent = accent,
        accentInk = if (accent.luminance() > 0.45f) Color(0xFF04261F) else Color.White,
        glassFill = fill,
        glassLine = line,
        highContrast = s.highContrast,
        motionScale = s.animationSpeed / 100f,
        reduceMotion = reduce,
        blur = s.blurStrength / 100f,
        dim = s.dimWallpaper / 100f,
        lightWallpaper = lightWallpaper,
    )
}

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
fun TosTheme(dark: Boolean, style: TosStyle = TosStyle(), content: @Composable () -> Unit) {
    val scheme = if (dark) {
        darkColorScheme(primary = style.accent, onPrimary = style.accentInk, secondary = style.accent, error = Tos.Danger)
    } else {
        lightColorScheme(primary = style.accent, onPrimary = style.accentInk, secondary = style.accent, error = Tos.Danger)
    }
    val panel = (if (dark) DarkPanel else LightPanel).let {
        if (style.highContrast) it.copy(secondary = it.content.copy(alpha = 0.85f), glassLine = it.content.copy(alpha = 0.30f)) else it
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalPanelColors provides panel, LocalTosStyle provides style) {
        MaterialTheme(colorScheme = scheme, typography = typography(), content = content)
    }
}

/** Glas-Fläche: halbtransparente Füllung mit feiner heller Kante (echte Unschärfe des Systemhintergrunds ist nicht möglich). */
fun Modifier.glass(shape: Shape, fill: Color, line: Color = Color.White.copy(alpha = 0.16f)): Modifier =
    this.background(fill, shape).border(1.dp, line, shape)
