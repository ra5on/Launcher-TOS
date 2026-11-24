package de.umstieg.launcher.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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

/** Seitenindikator: Punkte (aktive Seite breiter), Balken, Zahl oder aus. Im Bearbeitungsmodus „+“ für die neue Seite. */
@Composable
fun PageIndicator(
    style: IndicatorStyle,
    pageCount: Int,
    current: Int,
    hasNewPage: Boolean,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    if (pageCount <= 0) return
    val realPages = if (hasNewPage) pageCount - 1 else pageCount
    val showPages = style != IndicatorStyle.NONE && (realPages > 1 || hasNewPage)
    if (!showPages && onSearch == null) return
    val description = stringResource(R.string.home_page_description, current + 1, pageCount)
    Row(
        modifier
            .semantics { contentDescription = description }
            .glass(Tos.PillShape, fill = Tos.GlassDark)
            .padding(horizontal = 14.dp, vertical = if (onSearch != null) 6.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onSearch != null) {
            Icon(
                painterResource(R.drawable.ic_lucide_search),
                stringResource(R.string.search_open),
                tint = Tos.Ink,
                modifier = Modifier.size(18.dp).clickable(role = Role.Button, onClick = onSearch),
            )
        }
        if (showPages) when (style) {
            IndicatorStyle.NUMBER -> Text(
                text = "${current + 1} / $pageCount",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Tos.Ink),
            )
            IndicatorStyle.BAR -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(pageCount) { i ->
                    Box(
                        Modifier.width(18.dp).height(4.dp).clip(Tos.PillShape)
                            .background(if (i == current) Tos.Ink else Tos.Ink3),
                    )
                }
            }
            else -> repeat(pageCount) { i ->
                if (hasNewPage && i == pageCount - 1) {
                    Icon(
                        painterResource(R.drawable.ic_lucide_plus),
                        contentDescription = null,
                        tint = if (i == current) Tos.Ink else Tos.Ink2,
                        modifier = Modifier.size(12.dp),
                    )
                } else {
                    val w by animateDpAsState(if (i == current) 20.dp else 7.dp, label = "dot")
                    Box(
                        Modifier.width(w).height(7.dp).clip(CircleShape)
                            .background(if (i == current) Tos.Ink else Tos.Ink3),
                    )
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
