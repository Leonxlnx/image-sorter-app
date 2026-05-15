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

    val showMetadata: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.SHOW_METADATA] ?: true
    }

    val theme: Flow<ThemeMode> = context.dataStore.data.map { p ->
        when (p[Keys.THEME]) {
            ThemeMode.Light.id -> ThemeMode.Light
            ThemeMode.Dark.id -> ThemeMode.Dark
            else -> ThemeMode.System
        }
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.DYNAMIC_COLOR] ?: true
    }

    val sortOrder: Flow<SortOrder> = context.dataStore.data.map { p ->
        SortOrder.fromId(p[Keys.SORT_ORDER])
    }

    /** Drag distance (dp) needed to commit a swipe. */
    val dragThresholdDp: Flow<Int> = context.dataStore.data.map { p ->
        (p[Keys.DRAG_THRESHOLD_DP] ?: DEFAULT_DRAG_THRESHOLD_DP).coerceIn(40, 200)
    }

    /** Number of cards visible underneath the active card (0..3). */
    val stackDepth: Flow<Int> = context.dataStore.data.map { p ->
        (p[Keys.STACK_DEPTH] ?: DEFAULT_STACK_DEPTH).coerceIn(0, 3)
    }

    val folderRoot: Flow<FolderRoot> = context.dataStore.data.map { p ->
        FolderRoot.fromId(p[Keys.FOLDER_ROOT])
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

    suspend fun setShowMetadata(value: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_METADATA] = value }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.id }
    }

    suspend fun setDynamicColor(value: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    }

    suspend fun setSortOrder(value: SortOrder) {
        context.dataStore.edit { it[Keys.SORT_ORDER] = value.id }
    }

    suspend fun setDragThresholdDp(value: Int) {
        context.dataStore.edit { it[Keys.DRAG_THRESHOLD_DP] = value.coerceIn(40, 200) }
    }

    suspend fun setStackDepth(value: Int) {
        context.dataStore.edit { it[Keys.STACK_DEPTH] = value.coerceIn(0, 3) }
    }

    suspend fun setFolderRoot(value: FolderRoot) {
        context.dataStore.edit { it[Keys.FOLDER_ROOT] = value.id }
    }

    /** Wipe every persisted setting back to defaults. */
    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }

    private object Keys {
        val DATE_RANGE_ID = stringPreferencesKey("date_range_id")
        val INCLUDE_VIDEOS = booleanPreferencesKey("include_videos")
        val SKIP_REVIEWED = booleanPreferencesKey("skip_reviewed")
        val BATCH_SIZE = intPreferencesKey("batch_size")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SHOW_HINTS = booleanPreferencesKey("show_hints")
        val SHOW_METADATA = booleanPreferencesKey("show_metadata")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val DRAG_THRESHOLD_DP = intPreferencesKey("drag_threshold_dp")
        val STACK_DEPTH = intPreferencesKey("stack_depth")
        val FOLDER_ROOT = stringPreferencesKey("folder_root")
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 10
        const val DEFAULT_DRAG_THRESHOLD_DP = 96
        const val DEFAULT_STACK_DEPTH = 2
    }
}
