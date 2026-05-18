@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.leonxlnx.imagesorter.ui.settings

import android.content.Intent
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
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoSizeSelectActual
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.SwipeRight
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leonxlnx.imagesorter.BuildConfig
import com.leonxlnx.imagesorter.ImageSorterApp
import com.leonxlnx.imagesorter.R
import com.leonxlnx.imagesorter.data.DateRange
import com.leonxlnx.imagesorter.data.FolderRoot
import com.leonxlnx.imagesorter.data.SortOrder
import com.leonxlnx.imagesorter.ui.theme.ThemeMode
import kotlinx.coroutines.launch

private const val REPO_URL = "https://github.com/Leonxlnx/image-sorter-app"

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as ImageSorterApp
    val settings = app.settingsRepository
    val reviewed = app.reviewedRepository
    val scope = rememberCoroutineScope()

    val dateRange by settings.dateRange.collectAsState(initial = DateRange.Any)
    val sortOrder by settings.sortOrder.collectAsState(initial = SortOrder.NewestFirst)
    val includeVideos by settings.includeVideos.collectAsState(initial = false)
    val skipReviewed by settings.skipReviewed.collectAsState(initial = true)
    val batchSize by settings.batchSize.collectAsState(initial = 10)
    val dragThresholdDp by settings.dragThresholdDp.collectAsState(initial = 96)
    val stackDepth by settings.stackDepth.collectAsState(initial = 2)
    val haptics by settings.haptics.collectAsState(initial = true)
    val showHints by settings.showHints.collectAsState(initial = true)
    val showMetadata by settings.showMetadata.collectAsState(initial = true)
    val themeMode by settings.theme.collectAsState(initial = ThemeMode.System)
    val dynamicColor by settings.dynamicColor.collectAsState(initial = true)
    val folderRoot by settings.folderRoot.collectAsState(initial = FolderRoot.Pictures)
    val reduceMotion by settings.reduceMotion.collectAsState(initial = false)

    var resetReviewedDialog by remember { mutableStateOf(false) }
    var resetAllDialog by remember { mutableStateOf(false) }

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
                    SettingHeader(
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        title = stringResource(R.string.settings_sort_order),
                        subtitle = stringResource(R.string.settings_sort_order_subtitle),
                    )
                    ChipRow {
                        SortOrder.entries.forEach { order ->
                            ChoiceChip(
                                label = order.label,
                                selected = order == sortOrder,
                                onClick = { scope.launch { settings.setSortOrder(order) } },
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
                    SettingHeader(
                        icon = Icons.Outlined.SwipeRight,
                        title = stringResource(R.string.settings_drag_threshold),
                        subtitle = stringResource(R.string.settings_drag_threshold_subtitle, dragThresholdDp),
                    )
                    Slider(
                        value = dragThresholdDp.toFloat(),
                        onValueChange = { v -> scope.launch { settings.setDragThresholdDp(v.toInt()) } },
                        valueRange = 40f..200f,
                        steps = 0,
                    )
                    Divider()
                    SettingHeader(
                        icon = Icons.Outlined.Layers,
                        title = stringResource(R.string.settings_stack_depth),
                        subtitle = stringResource(R.string.settings_stack_depth_subtitle, stackDepth),
                    )
                    ChipRow {
                        (0..3).forEach { depth ->
                            ChoiceChip(
                                label = depth.toString(),
                                selected = depth == stackDepth,
                                onClick = { scope.launch { settings.setStackDepth(depth) } },
                            )
                        }
                    }
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
                    ToggleRow(
                        icon = Icons.Outlined.PhotoSizeSelectActual,
                        title = stringResource(R.string.settings_show_metadata),
                        subtitle = stringResource(R.string.settings_show_metadata_subtitle),
                        checked = showMetadata,
                        onCheckedChange = { v -> scope.launch { settings.setShowMetadata(v) } },
                    )
                    ToggleRow(
                        icon = Icons.Outlined.AutoAwesome,
                        title = stringResource(R.string.settings_reduce_motion),
                        subtitle = stringResource(R.string.settings_reduce_motion_subtitle),
                        checked = reduceMotion,
                        onCheckedChange = { v -> scope.launch { settings.setReduceMotion(v) } },
                    )
                }
            }

            item {
                Section(title = stringResource(R.string.settings_storage_header)) {
                    SettingHeader(
                        icon = Icons.Outlined.Folder,
                        title = stringResource(R.string.settings_folder_root),
                        subtitle = stringResource(R.string.settings_folder_root_subtitle),
                    )
                    ChipRow {
                        FolderRoot.entries.forEach { root ->
                            ChoiceChip(
                                label = root.label,
                                selected = root == folderRoot,
                                onClick = { scope.launch { settings.setFolderRoot(root) } },
                            )
                        }
                    }
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
                    Divider()
                    ToggleRow(
                        icon = Icons.Outlined.ColorLens,
                        title = stringResource(R.string.settings_dynamic_color),
                        subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                        checked = dynamicColor,
                        onCheckedChange = { v -> scope.launch { settings.setDynamicColor(v) } },
                    )
                }
            }

            item {
                Section(title = stringResource(R.string.settings_data_header)) {
                    OutlinedButton(
                        onClick = { resetReviewedDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_reset_reviewed))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { resetAllDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_reset_all))
                    }
                }
            }

            item {
                Section(title = stringResource(R.string.settings_about_header)) {
                    SettingHeader(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.app_name),
                        subtitle = stringResource(
                            R.string.settings_about_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.settings_about_blurb),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, REPO_URL.toUri())
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_about_repo))
                    }
                }
            }
        }
    }

    if (resetReviewedDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_reset_reviewed),
            text = stringResource(R.string.settings_reset_reviewed_confirm),
            onConfirm = {
                scope.launch { reviewed.clear() }
                resetReviewedDialog = false
            },
            onDismiss = { resetReviewedDialog = false },
        )
    }
    if (resetAllDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_reset_all),
            text = stringResource(R.string.settings_reset_all_confirm),
            onConfirm = {
                scope.launch { settings.resetAll() }
                resetAllDialog = false
            },
            onDismiss = { resetAllDialog = false },
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
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
    androidx.compose.foundation.layout.FlowRow(
        // Wrap chips onto a new line when they overflow.
        maxItemsInEachRow = 6,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
