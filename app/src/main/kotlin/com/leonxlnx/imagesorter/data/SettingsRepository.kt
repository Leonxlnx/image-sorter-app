package com.leonxlnx.imagesorter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leonxlnx.imagesorter.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "image_sorter_settings")

/**
 * Persists user-tunable settings via DataStore Preferences. All exposed flows emit immediately
 * with the default when there is no stored value yet.
 */
class SettingsRepository(private val context: Context) {

    val dateRange: Flow<DateRange> = context.dataStore.data.map { p ->
        DateRange.fromId(p[Keys.DATE_RANGE_ID])
    }

    val includeVideos: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.INCLUDE_VIDEOS] ?: false
    }

    val skipReviewed: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.SKIP_REVIEWED] ?: true
    }

    val batchSize: Flow<Int> = context.dataStore.data.map { p ->
        (p[Keys.BATCH_SIZE] ?: DEFAULT_BATCH_SIZE).coerceIn(1, 50)
    }

    val haptics: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.HAPTICS] ?: true
    }

    val showHints: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.SHOW_HINTS] ?: true
    }

    val theme: Flow<ThemeMode> = context.dataStore.data.map { p ->
        when (p[Keys.THEME]) {
            ThemeMode.Light.id -> ThemeMode.Light
            ThemeMode.Dark.id -> ThemeMode.Dark
            else -> ThemeMode.System
        }
    }

    suspend fun setDateRange(range: DateRange) {
        context.dataStore.edit { it[Keys.DATE_RANGE_ID] = range.id }
    }

    suspend fun setIncludeVideos(value: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_VIDEOS] = value }
    }

    suspend fun setSkipReviewed(value: Boolean) {
        context.dataStore.edit { it[Keys.SKIP_REVIEWED] = value }
    }

    suspend fun setBatchSize(value: Int) {
        context.dataStore.edit { it[Keys.BATCH_SIZE] = value.coerceIn(1, 50) }
    }

    suspend fun setHaptics(value: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = value }
    }

    suspend fun setShowHints(value: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_HINTS] = value }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.id }
    }

    private object Keys {
        val DATE_RANGE_ID = stringPreferencesKey("date_range_id")
        val INCLUDE_VIDEOS = booleanPreferencesKey("include_videos")
        val SKIP_REVIEWED = booleanPreferencesKey("skip_reviewed")
        val BATCH_SIZE = intPreferencesKey("batch_size")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SHOW_HINTS = booleanPreferencesKey("show_hints")
        val THEME = stringPreferencesKey("theme")
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 5
    }
}
