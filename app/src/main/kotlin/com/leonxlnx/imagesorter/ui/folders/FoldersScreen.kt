@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.leonxlnx.imagesorter.ui.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leonxlnx.imagesorter.ImageSorterApp
import com.leonxlnx.imagesorter.R
import com.leonxlnx.imagesorter.data.FolderRepository
import com.leonxlnx.imagesorter.data.SortFolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun FoldersScreen() {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as ImageSorterApp }
    val repo: FolderRepository = remember(app) { app.folderRepository }
    val scope = rememberCoroutineScope()

    var folders by remember { mutableStateOf(listOf<SortFolder>()) }
    var addDialogVisible by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<SortFolder?>(null) }
    var deleteTarget by remember { mutableStateOf<SortFolder?>(null) }

    LaunchedEffect(repo) {
        repo.folders.collectLatest { folders = it }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addDialogVisible = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_folder)) },
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Header()
                }
                items(folders, key = { it.name }) { folder ->
                    FolderCard(
                        folder = folder,
                        onMarkFavorite = { scope.launch { repo.setFavorite(folder.name) } },
                        onMarkDefault = { scope.launch { repo.setDefaultDown(folder.name) } },
                        onRename = { renameTarget = folder },
                        onDelete = { deleteTarget = folder },
                    )
                }
                if (folders.isEmpty()) {
                    item {
                        EmptyFolders()
                    }
                }
            }
        }
    }

    if (addDialogVisible) {
        NameDialog(
            title = stringResource(R.string.add_folder_title),
            confirmText = stringResource(R.string.create),
            onDismiss = { addDialogVisible = false },
            onConfirm = { name ->
                addDialogVisible = false
                scope.launch { repo.addFolder(name) }
            },
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = stringResource(R.string.rename_folder_title, target.name),
            confirmText = stringResource(R.string.rename),
            initial = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                scope.launch { repo.rename(target.name, name) }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.remove_folder_title)) },
            text = { Text(stringResource(R.string.remove_folder_subtitle, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    val name = target.name
                    deleteTarget = null
                    scope.launch { repo.remove(name) }
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            stringResource(R.string.folders_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.folders_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FolderCard(
    folder: SortFolder,
    onMarkFavorite: () -> Unit,
    onMarkDefault: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        folder.relativePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRename) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.rename))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.remove))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onMarkFavorite,
                    label = { Text(stringResource(R.string.use_as_favorites)) },
                    leadingIcon = { Icon(Icons.Outlined.Favorite, contentDescription = null) },
                    colors = if (folder.isFavorite)
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    else AssistChipDefaults.assistChipColors(),
                )
                AssistChip(
                    onClick = onMarkDefault,
                    label = { Text(stringResource(R.string.use_as_default_down)) },
                    leadingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
                    colors = if (folder.isDefaultDown)
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    else AssistChipDefaults.assistChipColors(),
                )
            }
        }
    }
}

@Composable
private fun EmptyFolders() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.no_folders_yet), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.no_folders_yet_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    confirmText: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(60) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.folder_name_label)) },
                    placeholder = { Text(stringResource(R.string.folder_name_placeholder)) },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.folder_path_preview, "Pictures/PhotoSwipe/${value.ifBlank { "<name>" }}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value.trim()) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
