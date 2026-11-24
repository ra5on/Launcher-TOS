package de.umstieg.launcher.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import de.umstieg.launcher.core.LauncherData
import de.umstieg.launcher.core.LauncherJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** DataStore-Serializer auf kotlinx-serialization-JSON (Layout + Einstellungen). */
object LauncherDataSerializer : Serializer<LauncherData> {
    override val defaultValue: LauncherData = LauncherData()

    override suspend fun readFrom(input: InputStream): LauncherData = try {
        LauncherJson.decode(input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Layout-Datei beschädigt", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Layout-Datei beschädigt", e)
    }

    override suspend fun writeTo(t: LauncherData, output: OutputStream) {
        withContext(Dispatchers.IO) { output.write(LauncherJson.encode(t).encodeToByteArray()) }
    }
}

/** Einziger DataStore des Prozesses (Homescreen und Einstellungen teilen ihn). */
class LauncherStore(context: Context, scope: CoroutineScope) {
    private val store: DataStore<LauncherData> = DataStoreFactory.create(
        serializer = LauncherDataSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { LauncherData() },
        scope = scope,
        produceFile = { File(context.applicationContext.filesDir, "datastore/launcher.json") },
    )

    val data: Flow<LauncherData> = store.data

    suspend fun update(transform: (LauncherData) -> LauncherData): LauncherData = store.updateData { transform(it) }
}
