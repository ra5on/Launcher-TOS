package de.umstieg.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import de.umstieg.launcher.data.DefaultLauncher
import de.umstieg.launcher.ui.home.HomeActions
import de.umstieg.launcher.ui.home.HomeScreen
import de.umstieg.launcher.ui.home.HomeViewModel

/** Homescreen-Activity (HOME/DEFAULT, singleTask). */
class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private val isDefault = mutableStateOf(true)
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault.value = DefaultLauncher.isDefault(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Helle Statusleisten-Symbole über dem Systemhintergrund.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val actions = HomeActions(
            openSettings = {
                startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            requestDefault = { runCatching { roleRequest.launch(DefaultLauncher.requestIntent(this)) } },
        )
        setContent { HomeScreen(vm, isDefault.value, actions) }
    }

    override fun onResume() {
        super.onResume()
        isDefault.value = DefaultLauncher.isDefault(this)
    }

    /** Home-Taste bei laufendem Launcher: Panels schließen, Seite 1 (animiert, wenn schon sichtbar). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            vm.onHomePressed(visible = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
    }
}
