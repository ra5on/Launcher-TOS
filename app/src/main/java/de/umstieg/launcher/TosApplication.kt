package de.umstieg.launcher

import android.app.Application
import de.umstieg.launcher.data.AppRepository
import de.umstieg.launcher.data.IconLoader
import de.umstieg.launcher.data.LauncherController
import de.umstieg.launcher.data.LauncherStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hält die prozessweiten Einzelobjekte (Speicher, App-Liste, Icon-Cache). */
class TosApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val controller: LauncherController by lazy {
        LauncherController(
            context = this,
            scope = appScope,
            store = LauncherStore(this, CoroutineScope(SupervisorJob() + Dispatchers.IO)),
            apps = AppRepository(this, appScope),
            icons = IconLoader(this),
        ).also { it.start() }
    }
}
