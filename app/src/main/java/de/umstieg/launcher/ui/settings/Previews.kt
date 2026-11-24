package de.umstieg.launcher.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.umstieg.launcher.R
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.IconLoader
import de.umstieg.launcher.ui.home.BASE_ICON_DP
import de.umstieg.launcher.ui.home.IconLabel
import de.umstieg.launcher.ui.icons.AppIconImage
import de.umstieg.launcher.ui.icons.rememberSuperellipse
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Klar
import kotlin.math.max

/** Eigener Verlaufs-Hintergrund für Vorschauen (selbst erzeugt). */
@Composable
fun PreviewBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(28.dp))) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(listOf(Color(0xFF1B2A4A), Color(0xFF14243A), Color(0xFF2A1E3A)), Offset.Zero, Offset(size.width * 0.3f, size.height)))
            fun blob(cx: Float, cy: Float, r: Float, c: Color) = drawRect(
                Brush.radialGradient(listOf(c.copy(alpha = 0.85f), Color.Transparent), Offset(size.width * cx, size.height * cy), max(size.width, size.height) * r),
            )
            blob(0.15f, 0.1f, 0.55f, Color(0xFFF0A86A))
            blob(0.92f, 0.45f, 0.5f, Color(0xFF7A4BA8))
            blob(0.25f, 1.0f, 0.45f, Color(0xFF10656A))
            blob(0.9f, 1.05f, 0.4f, Color(0xFFE0697A))
        }
        content()
    }
}

/** Vorschau: vier Original-Icons mit aktueller Rundung, Größe und Beschriftung. */
@Composable
fun IconRowPreview(apps: List<AppInfo>, loader: IconLoader, s: LauncherSettings, caption: Boolean) {
    val shape = rememberSuperellipse(s.iconRounding)
    PreviewBackdrop(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                apps.take(4).forEach { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                        AppIconImage(loader, app, s.legacyBackground, shape, (BASE_ICON_DP * s.iconSize / 100f).dp.coerceAtMost(76.dp))
                        if (s.labelsHome) {
                            Spacer(Modifier.height(6.dp))
                            IconLabel(app.label, s.fontScale)
                        }
                    }
                }
            }
            if (caption) {
                Text(
                    stringResource(R.string.icons_preview_caption),
                    style = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Klar.Ink2),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Vorschau: Raster (Spalten × Reihen, Seitenrand) mit echten Icons und optional Dock. */
@Composable
fun GridPreview(apps: List<AppInfo>, loader: IconLoader, s: LauncherSettings) {
    val shape = rememberSuperellipse(s.iconRounding)
    PreviewBackdrop(Modifier.fillMaxWidth().height(300.dp)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(vertical = 16.dp).padding(horizontal = (24 + s.sideMarginDp / 2).dp)
                .clearAndSetSemantics { },
        ) {
            val dockH = if (s.dockEnabled) 44.dp else 0.dp
            val cellW = maxWidth / s.columns
            val cellH = (maxHeight - dockH) / s.rows
            val icon = minOf(cellW, cellH) * 0.62f * (s.iconSize / 100f)
            for (i in 0 until s.columns * s.rows) {
                val x = i % s.columns
                val y = i / s.columns
                val app = apps.getOrNull(i)
                Box(Modifier.offset(cellW * x, cellH * y).size(cellW, cellH), contentAlignment = Alignment.Center) {
                    if (app != null) {
                        AppIconImage(loader, app, s.legacyBackground, shape, icon)
                    } else {
                        Box(Modifier.size(icon).border(1.dp, Color.White.copy(alpha = 0.25f), shape))
                    }
                }
            }
            if (s.dockEnabled) {
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(dockH)
                        .background(Klar.Glass, RoundedCornerShape(16.dp)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    apps.drop(s.columns * s.rows).take(s.dockCount).forEach { app ->
                        AppIconImage(loader, app, s.legacyBackground, shape, minOf(icon, 32.dp))
                    }
                }
            }
        }
    }
}
