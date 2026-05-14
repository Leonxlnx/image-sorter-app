package com.leonxlnx.imagesorter.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** A persisted Storage Access Framework destination folder. */
data class SortFolder(
    val uri: Uri,
    val displayName: String,
    val isFavorite: Boolean = false,
    val isDefaultDown: Boolean = false,
)

private val Context.foldersStore by preferencesDataStore(name = "image_sorter_folders")

/**
 * Tracks user-picked destination folders backed by SAF persistable URI permissions.
 *
 * Folders are stored as a JSON blob inside DataStore so we can keep the simple key/value
 * model while still supporting a list of items with flags.
 */
class FolderRepository(private val context: Context) {

    val folders: Flow<List<SortFolder>> = context.foldersStore.data.map { p ->
        val raw = p[Keys.FOLDERS] ?: return@map emptyList()
        parse(raw)
    }

    suspend fun addFolder(uri: Uri, displayName: String) {
        takePersistablePermission(uri)
        update { current ->
            if (current.any { it.uri == uri }) current
            else current + SortFolder(uri = uri, displayName = displayName)
        }
    }

    suspend fun rename(uri: Uri, newName: String) {
        update { current ->
            current.map { if (it.uri == uri) it.copy(displayName = newName) else it }
        }
    }

    suspend fun remove(uri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        update { current -> current.filterNot { it.uri == uri } }
    }

    suspend fun setFavorite(uri: Uri) = update { current ->
        current.map { it.copy(isFavorite = it.uri == uri) }
    }

    suspend fun setDefaultDown(uri: Uri) = update { current ->
        current.map { it.copy(isDefaultDown = it.uri == uri) }
    }

    private suspend fun update(block: (List<SortFolder>) -> List<SortFolder>) {
        context.foldersStore.edit { prefs ->
            val current = parse(prefs[Keys.FOLDERS] ?: "")
            val next = block(current)
            prefs[Keys.FOLDERS] = serialize(next)
        }
    }

    private fun takePersistablePermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    private fun parse(raw: String): List<SortFolder> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SortFolder(
                    uri = Uri.parse(o.getString("uri")),
                    displayName = o.getString("name"),
                    isFavorite = o.optBoolean("fav", false),
                    isDefaultDown = o.optBoolean("def", false),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serialize(folders: List<SortFolder>): String {
        val arr = JSONArray()
        for (f in folders) {
            arr.put(
                JSONObject()
                    .put("uri", f.uri.toString())
                    .put("name", f.displayName)
                    .put("fav", f.isFavorite)
                    .put("def", f.isDefaultDown)
            )
        }
        return arr.toString()
    }

    private object Keys {
        val FOLDERS = stringPreferencesKey("folders_json")
    }
}
