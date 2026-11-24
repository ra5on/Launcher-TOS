package de.umstieg.launcher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log

/** Ein AppWidgetHost für den ganzen Prozess (Widget-Seite und Raster). */
class WidgetHostHolder(context: Context) {
    private val ctx = context.applicationContext
    val manager: AppWidgetManager = AppWidgetManager.getInstance(ctx)
    val host = AppWidgetHost(ctx, HOST_ID)

    fun providers(): List<AppWidgetProviderInfo> = runCatching { manager.installedProviders }.getOrDefault(emptyList())

    fun info(widgetId: Int): AppWidgetProviderInfo? = runCatching { manager.getAppWidgetInfo(widgetId) }.getOrNull()

    fun allocate(): Int = host.allocateAppWidgetId()

    fun bindIfAllowed(widgetId: Int, provider: ComponentName, info: AppWidgetProviderInfo): Boolean = runCatching {
        manager.bindAppWidgetIdIfAllowed(widgetId, info.profile, provider, null)
    }.getOrDefault(false)

    fun delete(widgetId: Int) {
        runCatching { host.deleteAppWidgetId(widgetId) }
    }

    /** Größe an den Anbieter melden (dp). */
    fun updateSize(widgetId: Int, widthDp: Int, heightDp: Int) {
        runCatching {
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            }
            manager.updateAppWidgetOptions(widgetId, options)
        }
    }

    /** IDs, die der Host kennt, das Layout aber nicht mehr nutzt, freigeben. */
    fun cleanup(used: Set<Int>) {
        runCatching { host.appWidgetIds.filter { it !in used }.forEach { delete(it) } }
            .onFailure { Log.w("TosWidgets", "Aufräumen fehlgeschlagen", it) }
    }

    companion object {
        const val HOST_ID = 0x70A5
    }
}
