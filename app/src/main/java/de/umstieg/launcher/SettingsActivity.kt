package de.umstieg.launcher

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.umstieg.launcher.core.ThemeMode
import de.umstieg.launcher.data.DefaultLauncher
import de.umstieg.launcher.ui.settings.SettingsActions
import de.umstieg.launcher.ui.settings.SettingsScreen
import de.umstieg.launcher.ui.settings.SettingsViewModel
import de.umstieg.launcher.ui.theme.TosTheme
import de.umstieg.launcher.ui.theme.isDark

/** Einstellungen (erreichbar über den Bearbeitungsmodus und den Eintrag „Launcher TOS“). */
class SettingsActivity : ComponentActivity() {
    private val vm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SettingsRoot() }
    }

    @Composable
    private fun SettingsRoot() {
        val data by vm.data.collectAsStateWithLifecycle()
        val dark = isDark(data?.settings?.theme ?: ThemeMode.SYSTEM)
        LaunchedEffect(dark) {
            val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        val context = LocalContext.current
        var isDefault by remember { mutableStateOf(DefaultLauncher.isDefault(context)) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { isDefault = DefaultLauncher.isDefault(context) }
        LaunchedEffect(Unit) {
            vm.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }
        val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isDefault = DefaultLauncher.isDefault(context)
        }
        val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) vm.export(uri)
        }
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) vm.import(uri)
        }
        val fileName = getString(R.string.backup_file_name)
        val actions = remember {
            SettingsActions(
                requestDefault = { runCatching { roleLauncher.launch(DefaultLauncher.requestIntent(context)) } },
                export = { runCatching { exportLauncher.launch(fileName) } },
                import = { runCatching { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) } },
            )
        }
        TosTheme(dark = dark) {
            SettingsScreen(vm, isDefault, versionName(), ::loadLicenses, actions)
        }
    }

    private fun versionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()

    /** Lizenztexte aus den Assets (beim Bau aus LICENSE und THIRD_PARTY_LICENSES kopiert). */
    private fun loadLicenses(): List<Pair<String, String>> = listOf("THIRD_PARTY_LICENSES", "LICENSE").mapNotNull { name ->
        runCatching { assets.open("licenses/$name").bufferedReader().use { it.readText() } }.getOrNull()?.let { name to it }
    }
}
