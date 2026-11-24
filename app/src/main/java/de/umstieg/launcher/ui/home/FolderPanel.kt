package de.umstieg.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.umstieg.launcher.R
import de.umstieg.launcher.core.AppItem
import de.umstieg.launcher.core.AppKey
import de.umstieg.launcher.core.FolderItem
import de.umstieg.launcher.core.LauncherSettings
import de.umstieg.launcher.core.iconOptions
import de.umstieg.launcher.core.Slot
import de.umstieg.launcher.data.AppInfo
import de.umstieg.launcher.ui.icons.SuperellipseShape
import de.umstieg.launcher.ui.theme.Inter
import de.umstieg.launcher.ui.theme.Tos
import de.umstieg.launcher.ui.theme.LocalPanelColors
import de.umstieg.launcher.ui.theme.glass
import kotlin.math.ceil

/**
 * Geöffneter Ordner als Glas-Panel: Name (antippen zum Umbenennen), Anzahl, Raster der Apps.
 * Ziehen innerhalb sortiert um, Herausziehen schließt das Panel und legt auf dem Homescreen ab.
 */
@Composable
fun FolderOverlay(
    folder: FolderItem,
    apps: Map<AppKey, AppInfo>,
    settings: LauncherSettings,
    shape: SuperellipseShape,
    editMode: Boolean,
    drag: DragState,
    vm: HomeViewModel,
    onLaunch: (AppInfo, Rect) -> Unit,
    onGeometry: (Rect, GridGeometry) -> Unit,
) {
    val c = LocalPanelColors.current
    val density = LocalDensity.current
    val columns = when {
        folder.apps.size > 16 -> 5
        folder.apps.size > 9 -> 4
        else -> 3
    }
    val rows = ceil(folder.apps.size / columns.toFloat()).toInt().coerceAtLeast(1)
    val ink = if (c.dark) Tos.Ink else c.content
    var panelBounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.openFolder(null) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned { panelBounds = it.boundsInRoot() }
                    .glass(Tos.PanelShape, if (c.dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.95f), c.glassLine)
                    // Tipps im Panel schließen den Ordner nicht.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(horizontal = 18.dp, vertical = 22.dp),
            ) {
                FolderTitle(folder, ink, c.secondary, vm)
                Spacer(Modifier.height(18.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cellW = maxWidth / columns
                    val labelSpace = if (settings.labelsFolder) (labelHeight(settings.fontScale).value * density.fontScale).dp + 6.dp else 0.dp
                    val iconSize = iconDp(settings.iconSize, cellW * 0.78f)
                    val cellH = iconSize + labelSpace + 18.dp
                    val style = IconStyle(vm.icons, settings.iconOptions(), shape, iconSize, settings.fontScale, settings.largeTouch)
                    val iconPx = with(density) { iconSize.toPx() }
                    val dy = with(density) { -(labelSpace.toPx() / 2f) }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(cellH * rows)
                            .onGloballyPositioned { coords ->
                                onGeometry(panelBounds, GridGeometry(coords.boundsInRoot(), columns, rows, dy, iconPx))
                            },
                    ) {
                        folder.apps.forEachIndexed { index, appKey ->
                            key(appKey.id) {
                                Box(
                                    Modifier.offset(cellW * (index % columns), cellH * (index / columns)).size(cellW, cellH),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ItemCell(
                                        item = AppItem(appKey), slot = Slot.InFolder(folder.folderId, index), apps = apps,
                                        style = style, showLabel = settings.labelsFolder, editMode = editMode, wobble = settings.wobble,
                                        drag = drag, highlighted = false,
                                        onLaunch = { app, bounds ->
                                            vm.openFolder(null)
                                            onLaunch(app, bounds)
                                        },
                                        vm = vm, labelColor = ink,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.folder_rename_hint),
                style = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Tos.Ink2, textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun FolderTitle(folder: FolderItem, ink: Color, secondary: Color, vm: HomeViewModel) {
    var editing by remember { mutableStateOf(false) }
    var text by remember(folder.folderId) { mutableStateOf(folder.name) }
    val focus = remember { FocusRequester() }
    val renameLabel = stringResource(R.string.folder_rename)
    val titleStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = ink)
    fun commit() {
        if (editing) {
            editing = false
            vm.renameFolder(folder.folderId, text)
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (editing) {
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(40) },
                singleLine = true,
                textStyle = titleStyle,
                cursorBrush = SolidColor(Tos.Accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus)
                    .onFocusChanged { if (!it.isFocused) commit() },
            )
            LaunchedEffect(Unit) { focus.requestFocus() }
        } else {
            Row(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(onClickLabel = renameLabel, role = Role.Button) {
                        text = folder.name
                        editing = true
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(folder.name, style = titleStyle, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.width(10.dp))
                Icon(painterResource(R.drawable.ic_lucide_pencil), null, tint = secondary, modifier = Modifier.size(24.dp))
            }
        }
        Text(folderCountText(folder.apps.size), style = TextStyle(fontFamily = Inter, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = secondary))
    }
}
