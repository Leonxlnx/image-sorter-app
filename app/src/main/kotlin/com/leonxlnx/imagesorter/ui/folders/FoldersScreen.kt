@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.leonxlnx.imagesorter.ui.folders

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.leonxlnx.imagesorter.ImageSorterApp
import com.leonxlnx.imagesorter.R
import com.leonxlnx.imagesorter.data.SortFolder
import kotlinx.coroutines.launch

@Composable
fun FoldersScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as ImageSorterApp
    val repo = app.folderRepository
    val scope = rememberCoroutineScope()
    val foldersState = repo.folders.collectAsState(initial = emptyList())
    var renameTarget by remember { mutableStateOf<SortFolder?>(null) }

    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val name = DocumentFile.fromTreeUri(context, uri)?.name
                ?: uri.lastPathSegment
                ?: "Folder"
            scope.launch { repo.addFolder(uri, name) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(stringResource(R.string.folders_title), fontWeight = FontWeight.SemiBold)
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { pickFolder.launch(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_folder)) },
            )
        }
    ) { inner ->
        val folders = foldersState.value
        if (folders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                )
                Text(stringResource(R.string.no_folders_yet), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(folders, key = { it.uri.toString() }) { folder ->
                    FolderRow(
                        folder = folder,
                        onRemove = { scope.launch { repo.remove(folder.uri) } },
                        onSetFavorite = { scope.launch { repo.setFavorite(folder.uri) } },
                        onSetDefaultDown = { scope.launch { repo.setDefaultDown(folder.uri) } },
                        onRename = { renameTarget = folder },
                    )
                }
            }
        }
    }

    renameTarget?.let { folder ->
        RenameDialog(
            initial = folder.displayName,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                scope.launch { repo.rename(folder.uri, newName) }
                renameTarget = null
            },
        )
    }
}

@Composable
private fun FolderRow(
    folder: SortFolder,
    onRemove: () -> Unit,
    onSetFavorite: () -> Unit,
    onSetDefaultDown: () -> Unit,
    onRename: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(folder.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        folder.uri.lastPathSegment ?: folder.uri.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = onRename) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.folder_rename)) }
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.folder_remove)) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onSetFavorite,
                    label = { Text(stringResource(R.string.folder_set_favorite)) },
                    leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) },
                    enabled = !folder.isFavorite,
                )
                AssistChip(
                    onClick = onSetDefaultDown,
                    label = { Text(stringResource(R.string.folder_set_default)) },
                    leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                    enabled = !folder.isDefaultDown,
                )
            }
            if (folder.isFavorite || folder.isDefaultDown) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = buildString {
                        if (folder.isFavorite) append("★ Favorites destination")
                        if (folder.isFavorite && folder.isDefaultDown) append(" · ")
                        if (folder.isDefaultDown) append("↓ Default move-down")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_rename)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.ifBlank { initial }) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
