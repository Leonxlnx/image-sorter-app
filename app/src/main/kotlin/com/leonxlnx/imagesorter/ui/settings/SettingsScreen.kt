@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.leonxlnx.imagesorter.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val batchSize by settings.batchSize.collectAsState(initial = 10)
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Section(title = stringResource(R.string.settings_filter_header)) {
                    SettingHeader(
                        icon = Icons.Outlined.DateRange,
                        title = stringResource(R.string.settings_date_range),
                        subtitle = stringResource(R.string.settings_date_range_subtitle),
                    )
                    ChipRow {
                        DateRange.Presets.forEach { preset ->
                            ChoiceChip(
                                label = preset.label,
                                selected = preset.id == dateRange.id,
                                onClick = { scope.launch { settings.setDateRange(preset) } },
                            )
                        }
                    }
                    Divider()
                    ToggleRow(
                        icon = Icons.Outlined.PlayCircle,
                        title = stringResource(R.string.settings_include_videos),
                        subtitle = stringResource(R.string.settings_include_videos_subtitle),
                        checked = includeVideos,
                        onCheckedChange = { v -> scope.launch { settings.setIncludeVideos(v) } },
                    )
                    ToggleRow(
                        icon = Icons.Outlined.AutoAwesome,
                        title = stringResource(R.string.settings_skip_reviewed),
                        subtitle = stringResource(R.string.settings_skip_reviewed_subtitle),
                        checked = skipReviewed,
                        onCheckedChange = { v -> scope.launch { settings.setSkipReviewed(v) } },
                    )
                }
            }

            item {
                Section(title = stringResource(R.string.settings_behavior_header)) {
                    SettingHeader(
                        icon = Icons.Outlined.DeleteSweep,
                        title = stringResource(R.string.settings_batch_size),
                        subtitle = stringResource(R.string.settings_batch_size_subtitle, batchSize),
                    )
                    Slider(
                        value = batchSize.toFloat(),
                        onValueChange = { v -> scope.launch { settings.setBatchSize(v.toInt()) } },
                        valueRange = 1f..50f,
                        steps = 48,
                    )
                    Divider()
                    ToggleRow(
                        icon = Icons.Outlined.Vibration,
                        title = stringResource(R.string.settings_haptics),
                        subtitle = stringResource(R.string.settings_haptics_subtitle),
                        checked = haptics,
                        onCheckedChange = { v -> scope.launch { settings.setHaptics(v) } },
                    )
                    ToggleRow(
                        icon = Icons.Outlined.Visibility,
                        title = stringResource(R.string.settings_show_hints),
                        subtitle = stringResource(R.string.settings_show_hints_subtitle),
                        checked = showHints,
                        onCheckedChange = { v -> scope.launch { settings.setShowHints(v) } },
                    )
                }
            }

            item {
                Section(title = stringResource(R.string.settings_appearance_header)) {
                    SettingHeader(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.settings_theme),
                        subtitle = stringResource(R.string.settings_theme_subtitle),
                    )
                    ChipRow {
                        val items = listOf(
                            ThemeMode.System to stringResource(R.string.settings_theme_system),
                            ThemeMode.Light to stringResource(R.string.settings_theme_light),
                            ThemeMode.Dark to stringResource(R.string.settings_theme_dark),
                        )
                        items.forEach { (mode, label) ->
                            ChoiceChip(
                                label = label,
                                selected = mode == themeMode,
                                onClick = { scope.launch { settings.setTheme(mode) } },
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
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        colors = if (selected) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else AssistChipDefaults.assistChipColors(),
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Divider() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(1.dp)),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
    ) { Spacer(modifier = Modifier.height(1.dp)) }
}
