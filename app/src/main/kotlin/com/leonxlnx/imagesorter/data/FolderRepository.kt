package com.leonxlnx.imagesorter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** A managed gallery destination folder under `Pictures/PhotoSwipe/<name>/`. */
data class SortFolder(
    val name: String,
    val isFavorite: Boolean = false,
    val isDefaultDown: Boolean = false,
) {
    /** Path inside the public Pictures collection. */
    val relativePath: String get() = "Pictures/$ROOT/$name"

    companion object {
        const val ROOT = "PhotoSwipe"
        const val FAVORITES_DEFAULT_NAME = "Favorites"
    }
}

private val Context.foldersStore by preferencesDataStore(name = "image_sorter_folders_v2")

/**
 * Stores user-created destination folders as plain names. Folders are realised lazily on the
 * filesystem the first time a photo is moved into them — we simply ask MediaStore (or
 * `Environment` on pre-Q) to write into `Pictures/PhotoSwipe/<name>/` and the platform
 * creates the directory if needed and surfaces it in every gallery app.
 */
class FolderRepository(private val context: Context) {

    val folders: Flow<List<SortFolder>> = context.foldersStore.data.map { p ->
        val raw = p[Keys.FOLDERS]
        if (raw.isNullOrBlank()) seedDefaults() else parse(raw)
    }

    /** Initial state surfaced on first launch — one Favorites folder ready to go. */
    private fun seedDefaults(): List<SortFolder> = listOf(
        SortFolder(name = SortFolder.FAVORITES_DEFAULT_NAME, isFavorite = true),
    )

    suspend fun addFolder(name: String) {
        val cleaned = sanitize(name) ?: return
        update { current ->
            if (current.any { it.name.equals(cleaned, ignoreCase = true) }) current
            else current + SortFolder(name = cleaned)
        }
    }

    suspend fun rename(oldName: String, newName: String) {
        val cleaned = sanitize(newName) ?: return
        update { current ->
            current.map { if (it.name == oldName) it.copy(name = cleaned) else it }
        }
    }

    suspend fun remove(name: String) {
        update { current -> current.filterNot { it.name == name } }
    }

    suspend fun setFavorite(name: String) = update { current ->
        current.map { it.copy(isFavorite = it.name == name) }
    }

    suspend fun setDefaultDown(name: String) = update { current ->
        current.map { it.copy(isDefaultDown = it.name == name) }
    }

    private suspend fun update(block: (List<SortFolder>) -> List<SortFolder>) {
        context.foldersStore.edit { prefs ->
            val current = prefs[Keys.FOLDERS]?.let { parse(it) } ?: seedDefaults()
            val next = block(current)
            prefs[Keys.FOLDERS] = serialize(next)
        }
    }

    private fun sanitize(name: String): String? {
        val trimmed = name.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .take(60)
        return trimmed.ifBlank { null }
    }

    private fun parse(raw: String): List<SortFolder> = runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SortFolder(
                name = o.getString("name"),
                isFavorite = o.optBoolean("fav", false),
                isDefaultDown = o.optBoolean("def", false),
            )
        }
    }.getOrDefault(emptyList())

    private fun serialize(folders: List<SortFolder>): String {
        val arr = JSONArray()
        for (f in folders) {
            arr.put(
                JSONObject()
                    .put("name", f.name)
                    .put("fav", f.isFavorite)
                    .put("def", f.isDefaultDown)
            )
        }
        return arr.toString()
    }

    private object Keys {
        val FOLDERS = stringPreferencesKey("folders_json_v2")
    }
}
