package de.umstieg.launcher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppCategory
import de.umstieg.launcher.core.Categories
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.data.IconLoader
import de.umstieg.launcher.ui.icons.AppIconImage
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import kotlinx.coroutines.launch

/** Sichtbare Apps für Übersicht und Suche (ohne ausgeblendete), alphabetisch. */
fun visibleApps(d: LauncherData, apps: Map<de.umstieg.launcher.core.AppKey, AppInfo>): List<AppInfo> =
    apps.values.filter { it.key !in d.hiddenApps }.sortedBy { it.label.lowercase() }

@Composable
fun categoryName(c: AppCategory): String = stringResource(
    when (c) {
        AppCategory.RECENT -> R.string.category_recent
        AppCategory.SOCIAL -> R.string.category_social
        AppCategory.PRODUCTIVITY -> R.string.category_productivity
        AppCategory.IMAGES -> R.string.category_images
        AppCategory.VIDEO -> R.string.category_video
        AppCategory.AUDIO -> R.string.category_audio
        AppCategory.GAMES -> R.string.category_games
        AppCategory.NEWS -> R.string.category_news
        AppCategory.MAPS -> R.string.category_maps
        AppCategory.ACCESSIBILITY -> R.string.category_accessibility
        AppCategory.OTHER -> R.string.category_other
    },
)

fun categoryIcon(c: AppCategory): Int = when (c) {
    AppCategory.RECENT -> R.drawable.ic_lucide_sparkles
    AppCategory.SOCIAL -> R.drawable.ic_lucide_message_circle
    AppCategory.PRODUCTIVITY -> R.drawable.ic_lucide_list_todo
    AppCategory.IMAGES -> R.drawable.ic_lucide_image
    AppCategory.VIDEO -> R.drawable.ic_lucide_clapperboard
    AppCategory.AUDIO -> R.drawable.ic_lucide_music
    AppCategory.GAMES -> R.drawable.ic_lucide_gamepad_2
    AppCategory.NEWS -> R.drawable.ic_lucide_newspaper
    AppCategory.MAPS -> R.drawable.ic_lucide_map
    AppCategory.ACCESSIBILITY -> R.drawable.ic_lucide_accessibility
    AppCategory.OTHER -> R.drawable.ic_lucide_package
}

/** Text mit hervorgehobenen Trefferbereichen (Akzentfarbe, fett). */
@Composable
fun highlighted(text: String, ranges: List<IntRange>): AnnotatedString {
    val accent = Tos.Accent
    return buildAnnotatedString {
        append(text)
        ranges.forEach { r ->
            val start = r.first.coerceIn(0, text.length)
            val end = (r.last + 1).coerceIn(start, text.length)
            addStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold), start, end)
        }
    }
}

/** Zeile mit Icon, Name (mit Hervorhebung) und Untertitel; lange drücken öffnet das App-Menü. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppInfo,
    loader: IconLoader,
    d: LauncherData,
    shape: SuperellipseShape,
    title: AnnotatedString,
    subtitle: String?,
    ink: androidx.compose.ui.graphics.Color,
    secondary: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val menuLabel = stringResource(R.string.app_menu_open)
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .combinedClickable(role = Role.Button, onLongClickLabel = menuLabel, onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(loader, app, de.umstieg.launcher.core.IconOptions(d.settings.legacyBackground, d.settings.legacyColor, d.settings.themedIcons), shape, 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 17.sp, color = ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = secondary), maxLines = 1)
        }
    }
}

/** Buchstaben-Leiste rechts: Tippen oder Ziehen springt zum Buchstaben. */
@Composable
fun LetterRail(letters: List<Char>, listState: LazyListState, indexOf: (Char) -> Int?, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf<Char?>(null) }
    var heightPx by remember { mutableStateOf(1) }
    fun jump(y: Float) {
        if (letters.isEmpty()) return
        val i = ((y / heightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        val letter = letters[i]
        if (letter != active) {
            active = letter
            indexOf(letter)?.let { scope.launch { listState.scrollToItem(it) } }
        }
    }
    val description = stringResource(R.string.letter_rail)
    Column(
        modifier
            .width(28.dp)
            .fillMaxHeight()
            .semantics { contentDescription = description }
            .pointerInput(letters) {
                heightPx = size.height
                detectTapGestures { jump(it.y) }
            }
            .pointerInput(letters) {
                heightPx = size.height
                detectVerticalDragGestures(onDragEnd = { active = null }) { change, _ -> jump(change.position.y) }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { l ->
            val on = l == active
            Box(
                Modifier.size(20.dp).background(if (on) Tos.Accent else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(l.toString(), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (on) Tos.AccentInk else Tos.Ink2))
            }
        }
    }
}

/** Buchstaben der A–Z-Liste für die vorhandenen Apps. */
fun lettersOf(apps: List<AppInfo>): List<Char> = apps.map { Categories.indexLetter(it.label) }.distinct().sortedWith(compareBy { if (it == '#') '~' else it })

/** Kleines rundes Symbol (z. B. Suche, Löschen). */
@Composable
fun RoundGlyph(icon: Int, description: String?, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).padding(4.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f), CircleShape)
            .combinedClickableCompat(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, tint = Tos.Ink, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit): Modifier = this.then(
    Modifier.combinedClickable(role = Role.Button, onClick = onClick),
)
