package de.umstieg.launcher.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.umstieg.launcher.R
import de.umstieg.launcher.core.IndicatorStyle
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.glass

/** Beschriftung unter Icons: weiß mit leichtem Schatten, damit sie auf jedem Hintergrund lesbar ist. */
fun labelStyle(fontScale: Int, color: Color = Tos.Ink): TextStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Medium,
    fontSize = (12f * fontScale / 100f).sp,
    color = color,
    textAlign = TextAlign.Center,
    shadow = if (color == Tos.Ink) Shadow(Color.Black.copy(alpha = 0.40f), Offset(0f, 2f), 6f) else null,
)

@Composable
fun IconLabel(text: String, fontScale: Int, modifier: Modifier = Modifier, color: Color = Tos.Ink) {
    Text(
        text = text,
        style = labelStyle(fontScale, color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * Seitenindikator: Punkte (die aktive Seite breiter, folgt dem Wisch), Balken, Zahl oder aus. Im
 * Bearbeitungsmodus „+“ für die neue Seite. [position] ist die Seite mit Bruchteil beim Blättern.
 * [onSearch]: Tipp auf die Punkte öffnet die Suche.
 */
@Composable
fun PageIndicator(
    style: IndicatorStyle,
    pageCount: Int,
    position: () -> Float,
    hasNewPage: Boolean,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    if (pageCount <= 0) return
    // Nur für Text und TalkBack; die Punkte lesen die Position erst beim Zeichnen (kein Neuaufbau je Frame).
    val current = position().roundToInt().coerceIn(0, pageCount - 1)
    val description = stringResource(R.string.home_page_description, current + 1, pageCount)
    val searchLabel = stringResource(R.string.search_open)
    Row(
        modifier
            .clip(Tos.PillShape)
            .then(if (onSearch != null) Modifier.clickable(onClickLabel = searchLabel, role = Role.Button, onClick = onSearch) else Modifier)
            .semantics { contentDescription = if (onSearch != null) "$description, $searchLabel" else description }
            .background(Tos.GlassDark, Tos.PillShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (style) {
            IndicatorStyle.NUMBER -> Text(
                text = "${current + 1} / $pageCount",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Tos.Ink),
            )
            IndicatorStyle.BAR -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(pageCount) { i ->
                    Box(Modifier.width(18.dp).height(4.dp).clip(Tos.PillShape).background(if (i == current) Tos.Ink else Tos.Ink3))
                }
            }
            else -> {
                val dots = if (hasNewPage) pageCount - 1 else pageCount
                val ink = Tos.Ink
                val ink3 = Tos.Ink3
                // Breite und Farbe folgen der Wischbewegung stufenlos (gezeichnet, nicht neu zusammengesetzt).
                androidx.compose.foundation.Canvas(Modifier.width(7.dp * dots + 13.dp + 8.dp * (dots - 1).coerceAtLeast(0)).height(7.dp)) {
                    val pos = position()
                    val h = size.height
                    val gap = 8.dp.toPx()
                    var x = 0f
                    for (i in 0 until dots) {
                        val near = (1f - abs(i - pos)).coerceIn(0f, 1f)
                        val w = 7.dp.toPx() + 13.dp.toPx() * near
                        drawRoundRect(
                            androidx.compose.ui.graphics.lerp(ink3, ink, near),
                            topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                            size = androidx.compose.ui.geometry.Size(w, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f),
                        )
                        x += w + gap
                    }
                }
                if (hasNewPage) {
                    Icon(painterResource(R.drawable.ic_lucide_plus), contentDescription = null, tint = if (current == pageCount - 1) Tos.Ink else Tos.Ink2, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

/** Grundgröße eines Icons in dp bei 100 %. */
const val BASE_ICON_DP = 58f

/** Icongröße in dp: gewünschte Größe, begrenzt durch die Zelle, aber nie kleiner als 24 dp (enge Raster, Querformat). */
fun iconDp(percent: Int, maxDp: Dp): Dp = minOf((BASE_ICON_DP * percent / 100f).dp, maxDp).coerceAtLeast(24.dp)

fun labelHeight(fontScale: Int): TextUnit = (12f * fontScale / 100f * 1.35f).sp
