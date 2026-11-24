package de.umstieg.launcher.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.umstieg.launcher.R
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.LocalPanelColors
import de.umstieg.launcher.ui.theme.glass
import kotlin.math.roundToInt

val TitleStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
val SummaryStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp)
val ValueStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)

/** Glas-Karte für eine Gruppe von Einstellungen. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalPanelColors.current
    Column(
        modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), c.glass, c.glassLine).padding(vertical = 6.dp),
        content = content,
    )
}

@Composable
fun CardDivider() {
    HorizontalDivider(Modifier.padding(start = 18.dp, end = 18.dp), color = LocalPanelColors.current.glassLine)
}

/** Kopfzeile einer Unterseite: runder Zurück-Knopf und Titel. */
@Composable
fun SubpageHeader(title: String, onBack: () -> Unit) {
    val c = LocalPanelColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .glass(CircleShape, c.glass, c.glassLine)
                .clickable(onClickLabel = stringResource(R.string.back), role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_lucide_chevron_left), stringResource(R.string.back), tint = c.content, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = c.content),
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** Farbiges Rundquadrat mit Symbol (Übersicht). */
@Composable
fun IconTile(@DrawableRes icon: Int, color: Color) {
    Box(Modifier.size(40.dp).background(color, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** Zeile der Übersicht mit Symbol, Titel, Zusammenfassung und Pfeil. */
@Composable
fun NavRow(@DrawableRes icon: Int, color: Color, title: String, summary: String, onClick: () -> Unit) {
    val c = LocalPanelColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TitleStyle.copy(color = c.content))
            Text(summary, style = SummaryStyle.copy(color = c.secondary))
        }
        Icon(painterResource(R.drawable.ic_lucide_chevron_right), null, tint = c.secondary, modifier = Modifier.size(20.dp))
    }
}

/** Schalter-Zeile; die ganze Zeile ist antippbar. */
@Composable
fun SwitchRow(title: String, summary: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalPanelColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TitleStyle.copy(color = c.content))
            if (summary != null) Text(summary, style = SummaryStyle.copy(color = c.secondary))
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Tos.Accent,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = c.track,
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** Antippbare Zeile mit optionalem Wert rechts. */
@Composable
fun ActionRow(title: String, summary: String? = null, value: String? = null, color: Color? = null, onClick: () -> Unit) {
    val c = LocalPanelColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TitleStyle.copy(color = color ?: c.content))
            if (summary != null) Text(summary, style = SummaryStyle.copy(color = c.secondary))
        }
        if (value != null) Text(value, style = SummaryStyle.copy(color = c.secondary, fontSize = 16.sp))
        Icon(painterResource(R.drawable.ic_lucide_chevron_right), null, tint = c.secondary, modifier = Modifier.size(20.dp))
    }
}

/** Antippbare Zeile mit formatiertem Titel (z. B. hervorgehobene Suchtreffer). */
@Composable
fun ActionRowAnnotated(title: androidx.compose.ui.text.AnnotatedString, onClick: () -> Unit) {
    val c = LocalPanelColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = TitleStyle.copy(color = c.content), modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_lucide_chevron_right), null, tint = c.secondary, modifier = Modifier.size(20.dp))
    }
}

/**
 * Regler mit Titel und Wert. Während des Ziehens zeigt [onPreview] den Wert sofort (Live-Vorschau),
 * gespeichert wird beim Loslassen über [onCommit].
 */
@Composable
fun SliderBlock(
    title: String,
    value: Int,
    range: IntRange,
    valueText: (Int) -> String,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    stepSize: Int = 1,
    start: @Composable (() -> Unit)? = null,
    end: @Composable (() -> Unit)? = null,
) {
    val c = LocalPanelColors.current
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val current = local.roundToInt()
    val steps = ((range.last - range.first) / stepSize - 1).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = TitleStyle.copy(color = c.content), modifier = Modifier.weight(1f))
            Text(valueText(current), style = ValueStyle.copy(color = Tos.Accent))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (start != null) {
                start()
                Spacer(Modifier.width(10.dp))
            }
            Slider(
                value = local,
                onValueChange = {
                    local = it
                    onPreview(it.roundToInt())
                },
                onValueChangeFinished = { onCommit(local.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = if (stepSize > 1 || range.last - range.first <= 12) steps else 0,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Tos.Accent,
                    inactiveTrackColor = c.track,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f).semantics { stateDescription = valueText(current) },
            )
            if (end != null) {
                Spacer(Modifier.width(10.dp))
                end()
            }
        }
    }
}

/** Segmentierte Auswahl (z. B. Aus / Leicht / Stark). */
@Composable
fun <T> SegmentedRow(title: String?, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = LocalPanelColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        if (title != null) {
            Text(title, style = TitleStyle.copy(color = c.content))
            Spacer(Modifier.height(10.dp))
        }
        Row(
            Modifier.fillMaxWidth().background(c.chip, Tos.PillShape).padding(4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (value, label) ->
                val isSel = value == selected
                Box(
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(Tos.PillShape)
                        .background(if (isSel) (if (c.dark) Color.White else Color(0xFF10141C)) else Color.Transparent)
                        .selectable(selected = isSel, role = Role.RadioButton, onClick = { onSelect(value) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = SummaryStyle.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (isSel) (if (c.dark) Color(0xFF10141C) else Color.White) else c.secondary,
                        ),
                    )
                }
            }
        }
    }
}

/** Schnellwahl-Chips (z. B. Eckig / Weich / Squircle / Kreis). */
@Composable
fun ChipRow(options: List<Pair<Int, String>>, selected: Int?, onSelect: (Int) -> Unit) {
    val c = LocalPanelColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(Tos.PillShape)
                    .background(if (isSel) Tos.Accent else c.chip)
                    .selectable(selected = isSel, role = Role.RadioButton, onClick = { onSelect(value) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = SummaryStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (isSel) Tos.AccentInk else c.content))
            }
        }
    }
}

/** Kleines Symbol neben einem Regler (dekorativ). */
@Composable
fun SideGlyph(@DrawableRes icon: Int) {
    Icon(painterResource(icon), null, tint = LocalPanelColors.current.secondary, modifier = Modifier.size(20.dp).clearAndSetSemantics { })
}
