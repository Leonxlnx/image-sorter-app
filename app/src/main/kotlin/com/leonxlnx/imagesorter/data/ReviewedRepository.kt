package com.leonxlnx.imagesorter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reviewedStore by preferencesDataStore(name = "image_sorter_reviewed")

/**
 * Tracks the MediaStore IDs the user has already swiped on so we don't re-show them on the
 * next session (unless they reset the list from Settings).
 */
class ReviewedRepository(private val context: Context) {

    val ids: Flow<Set<Long>> = context.reviewedStore.data.map { p ->
        p[Keys.IDS]?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun markReviewed(id: Long) {
        context.reviewedStore.edit { p ->
            val existing = p[Keys.IDS]?.split(",")?.mapNotNull { it.toLongOrNull() }?.toMutableSet()
                ?: mutableSetOf()
            existing.add(id)
            p[Keys.IDS] = existing.joinToString(",")
        }
    }

    suspend fun unmark(id: Long) {
        context.reviewedStore.edit { p ->
            val existing = p[Keys.IDS]?.split(",")?.mapNotNull { it.toLongOrNull() }?.toMutableSet()
                ?: mutableSetOf()
            existing.remove(id)
            p[Keys.IDS] = existing.joinToString(",")
        }
    }

    suspend fun clear() {
        context.reviewedStore.edit { it.remove(Keys.IDS) }
    }

    private object Keys {
        val IDS = stringPreferencesKey("ids_csv")
    }
}
