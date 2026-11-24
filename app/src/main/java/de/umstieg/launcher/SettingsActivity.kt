package de.umstieg.launcher

import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
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
import de.umstieg.launcher.ui.settings.Section
import de.umstieg.launcher.ui.settings.SectionRequest
import de.umstieg.launcher.ui.settings.SettingsActions
import de.umstieg.launcher.ui.settings.SettingsScreen
import de.umstieg.launcher.ui.settings.SettingsViewModel
import de.umstieg.launcher.ui.theme.TosTheme
import de.umstieg.launcher.ui.theme.isDark
import de.umstieg.launcher.ui.theme.styleFor

/** Einstellungen (erreichbar über den Bearbeitungsmodus, die Suche und den Eintrag „Launcher TOS“). */
class SettingsActivity : ComponentActivity() {
    private val vm: SettingsViewModel by viewModels()
    private var pendingAuth: (() -> Unit)? = null

    /** Angeforderte Seite (auch bei erneutem Aufruf, z. B. aus der Suche, wenn die Einstellungen schon offen sind). */
    private val requested = mutableStateOf(SectionRequest(Section.OVERVIEW, 0))

    private val keyguardRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) pendingAuth?.invoke()
        pendingAuth = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requested.value = SectionRequest(sectionOf(intent), System.nanoTime())
        setContent { SettingsRoot(requested.value) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Neue Anforderung (eigene Kennung), damit auch dieselbe Seite erneut geöffnet wird.
        requested.value = SectionRequest(sectionOf(intent), System.nanoTime())
    }

    private fun sectionOf(intent: Intent?): Section =
        intent?.getStringExtra(EXTRA_SECTION)?.let { runCatching { Section.valueOf(it) }.getOrNull() } ?: Section.OVERVIEW

    @Composable
    private fun SettingsRoot(start: SectionRequest) {
        val data by vm.data.collectAsStateWithLifecycle()
        val settings = data?.settings
        val dark = isDark(settings?.theme ?: ThemeMode.SYSTEM)
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
                authenticate = ::authenticate,
                openIntent = { intent -> runCatching { startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
            )
        }
        val style = settings?.let { styleFor(it, null, false) } ?: de.umstieg.launcher.ui.theme.TosStyle()
        TosTheme(dark = dark, style = style) {
            SettingsScreen(vm, isDefault, versionName(), start, ::loadLicenses, actions)
        }
    }

    /**
     * Geräte-Sperre bestätigen (für versteckte Apps). Ohne eingerichtete Sperre gibt es nichts zu schützen:
     * dann direkt weiter.
     */
    private fun authenticate(onSuccess: () -> Unit) {
        val km = getSystemService(KeyguardManager::class.java)
        if (km == null || !km.isDeviceSecure) {
            onSuccess()
            return
        }
        val title = getString(R.string.auth_title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val prompt = BiometricPrompt.Builder(this)
                .setTitle(title)
                .setAllowedAuthenticators(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build()
            prompt.authenticate(
                CancellationSignal(),
                mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onSuccess()
                },
            )
        } else {
            @Suppress("DEPRECATION")
            val intent = km.createConfirmDeviceCredentialIntent(title, null)
            if (intent == null) {
                onSuccess()
            } else {
                pendingAuth = onSuccess
                keyguardRequest.launch(intent)
            }
        }
    }

    private fun versionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()

    /** Lizenztexte aus den Assets (beim Bau aus LICENSE und THIRD_PARTY_LICENSES kopiert). */
    private fun loadLicenses(): List<Pair<String, String>> = listOf("THIRD_PARTY_LICENSES", "LICENSE").mapNotNull { name ->
        runCatching { assets.open("licenses/$name").bufferedReader().use { it.readText() } }.getOrNull()?.let { name to it }
    }

    companion object {
        const val EXTRA_SECTION = "de.umstieg.launcher.SECTION"
    }
}
