@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.leonxlnx.imagesorter.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.leonxlnx.imagesorter.ImageSorterApp
import com.leonxlnx.imagesorter.R
import com.leonxlnx.imagesorter.data.DateRange
import com.leonxlnx.imagesorter.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as ImageSorterApp
    val settings = app.settingsRepository
    val reviewed = app.reviewedRepository
    val scope = rememberCoroutineScope()

    val dateRange by settings.dateRange.collectAsState(initial = DateRange.Any)
    val includeVideos by settings.includeVideos.collectAsState(initial = false)
    val skipReviewed by settings.skipReviewed.collectAsState(initial = true)
    val batchSize by settings.batchSize.collectAsState(initial = 5)
    val haptics by settings.haptics.collectAsState(initial = true)
    val showHints by settings.showHints.collectAsState(initial = true)
    val themeMode by settings.theme.collectAsState(initial = ThemeMode.System)

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold)
            })
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Section(title = stringResource(R.string.settings_filter_header)) {
                    Text(stringResource(R.string.settings_date_range), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DateRange.Presets.forEach { preset ->
                            AssistChip(
                                onClick = { scope.launch { settings.setDateRange(preset) } },
                                label = { Text(preset.label) },
                                colors = if (preset.id == dateRange.id) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        labelColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else AssistChipDefaults.assistChipColors(),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ToggleRow(
                        label = stringResource(R.string.settings_include_videos),
                        checked = includeVideos,
                        onCheckedChange = { v -> scope.launch { settings.setIncludeVideos(v) } },
                    )
                    ToggleRow(
                        label = stringResource(R.string.settings_skip_reviewed),
                        checked = skipReviewed,
                        onCheckedChange = { v -> scope.launch { settings.setSkipReviewed(v) } },
                    )
                }
            }

            item {
                Section(title = stringResource(R.string.settings_behavior_header)) {
                    Text(
                        text = "${stringResource(R.string.settings_batch_size)}: $batchSize",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Slider(
                        value = batchSize.toFloat(),
                        onValueChange = { v -> scope.launch { settings.setBatchSize(v.toInt()) } },
                        valueRange = 1f..20f,
                        steps = 19,
                    )
                    ToggleRow(
                        label = stringResource(R.string.settings_haptics),
                        checked = haptics,
                        onCheckedChange = { v -> scope.launch { settings.setHaptics(v) } },
                    )
                    ToggleRow(
                        label = stringResource(R.string.settings_show_hints),
                        checked = showHints,
                        onCheckedChange = { v -> scope.launch { settings.setShowHints(v) } },
                    )
                }
            }

            item {
                Section(title = stringResource(R.string.settings_appearance_header)) {
                    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val items = listOf(
                            ThemeMode.System to stringResource(R.string.settings_theme_system),
                            ThemeMode.Light to stringResource(R.string.settings_theme_light),
                            ThemeMode.Dark to stringResource(R.string.settings_theme_dark),
                        )
                        items.forEach { (mode, label) ->
                            AssistChip(
                                onClick = { scope.launch { settings.setTheme(mode) } },
                                label = { Text(label) },
                                colors = if (mode == themeMode) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        labelColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else AssistChipDefaults.assistChipColors(),
                            )
                        }
                    }
                }
            }

            item {
                Section(title = stringResource(R.string.settings_data_header)) {
                    OutlinedButton(
                        onClick = { scope.launch { reviewed.clear() } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_reset_reviewed))
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
