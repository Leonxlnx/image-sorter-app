@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.leonxlnx.imagesorter.ui.swipe

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.leonxlnx.imagesorter.R
import com.leonxlnx.imagesorter.data.FolderRoot
import com.leonxlnx.imagesorter.data.Photo
import com.leonxlnx.imagesorter.data.SortFolder
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeScreen(viewModel: SwipeViewModel = viewModel(factory = SwipeViewModel.factory())) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pickerVisible by rememberSaveable { mutableStateOf(false) }

    val intentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* Delete batches resolve via the next reload. */ }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { ev ->
            when (ev) {
                is SwipeEvent.LaunchIntent ->
                    intentLauncher.launch(IntentSenderRequest.Builder(ev.intentSender).build())
                is SwipeEvent.ChooseFolderForDown -> pickerVisible = true
                is SwipeEvent.Error -> snackbar.showSnackbar(ev.message)
                is SwipeEvent.Info -> snackbar.showSnackbar(ev.message)
            }
        }
    }

    LaunchedEffect(state.undo) {
        val u = state.undo ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = u.description,
            actionLabel = context.getString(R.string.undo),
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.consumeUndo()
        else viewModel.dismissUndo()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingState()
            state.isEmpty -> EmptyState(
                pendingDeletes = state.pendingDeleteCount,
                onFlush = { viewModel.flushPendingDeletes() },
                onReload = { viewModel.reload() },
            )
            else -> CardStack(
                state = state,
                onSwiped = { direction ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onSwipe(direction)
                },
                onFlushQueue = { viewModel.flushPendingDeletes() },
            )
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) { data -> Snackbar(snackbarData = data) }
    }

    if (pickerVisible) {
        FolderPickerSheet(
            folders = state.folders.filter { !it.isFavorite },
            folderRoot = state.folderRoot,
            onDismiss = {
                pickerVisible = false
                viewModel.cancelDownPick()
            },
            onSelected = { folder ->
                pickerVisible = false
                viewModel.moveCurrentTo(folder)
            },
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(pendingDeletes: Int, onFlush: () -> Unit, onReload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.empty_state_subtitle),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        if (pendingDeletes > 0) {
            OutlinedButton(onClick = onFlush) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.flush_pending, pendingDeletes))
            }
            Spacer(Modifier.height(12.dp))
        }
        TextButton(onClick = onReload) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.reload))
        }
    }
}

@Composable
private fun CardStack(
    state: SwipeUiState,
    onSwiped: (SwipeDirection) -> Unit,
    onFlushQueue: () -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val current = state.currentPhoto ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(state = state, onFlushQueue = onFlushQueue)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .onSizeChanged { size = it },
        ) {
            if (state.stackDepth >= 2) {
                state.nextNextPhoto?.let {
                    PhotoCard(
                        photo = it,
                        showMetadata = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 28.dp, start = 28.dp, end = 28.dp, bottom = 4.dp)
                            .graphicsLayer { alpha = 0.35f },
                    )
                }
            }
            if (state.stackDepth >= 1) {
                state.nextPhoto?.let {
                    PhotoCard(
                        photo = it,
                        showMetadata = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 14.dp, start = 14.dp, end = 14.dp, bottom = 8.dp)
                            .graphicsLayer { alpha = 0.75f },
                    )
                }
            }
            DraggableTopCard(
                photo = current,
                size = size,
                showMetadata = state.showMetadata,
                dragThresholdDp = state.dragThresholdDp,
                onSwiped = onSwiped,
            )
        }
        FooterHints(state.showHints)
    }
}

@Composable
private fun TopBar(state: SwipeUiState, onFlushQueue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.totalProcessed}/${state.queue.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (state.queue.isEmpty()) 0f else state.totalProcessed.toFloat() / state.queue.size
                },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        }
        AnimatedVisibility(
            visible = state.pendingDeleteCount > 0,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.pending_delete_status,
                            state.pendingDeleteCount,
                            state.batchSize,
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    TextButton(onClick = onFlushQueue) {
                        Text(
                            stringResource(R.string.delete_now),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableTopCard(
    photo: Photo,
    size: IntSize,
    showMetadata: Boolean,
    dragThresholdDp: Int,
    onSwiped: (SwipeDirection) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { dragThresholdDp.dp.toPx() }

    val width = size.width.takeIf { it > 0 } ?: 1
    val height = size.height.takeIf { it > 0 } ?: 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = (offsetX.value / width.toFloat()) * 12f
            }
            .pointerInput(photo.id) {
                detectDragGestures(
                    onDragEnd = {
                        val dx = offsetX.value
                        val dy = offsetY.value
                        val absDx = abs(dx)
                        val absDy = abs(dy)
                        val direction = when {
                            absDx < thresholdPx && absDy < thresholdPx -> null
                            absDx > absDy -> if (dx > 0) SwipeDirection.Right else SwipeDirection.Left
                            else -> if (dy > 0) SwipeDirection.Down else SwipeDirection.Up
                        }
                        if (direction == null) {
                            scope.launch { offsetX.animateTo(0f, tween(200)) }
                            scope.launch { offsetY.animateTo(0f, tween(200)) }
                        } else {
                            val targetX = when (direction) {
                                SwipeDirection.Right -> width * 1.5f
                                SwipeDirection.Left -> -width * 1.5f
                                SwipeDirection.Up, SwipeDirection.Down -> 0f
                            }
                            val targetY = when (direction) {
                                SwipeDirection.Up -> -height * 1.5f
                                SwipeDirection.Down -> height * 1.5f
                                SwipeDirection.Left, SwipeDirection.Right -> 0f
                            }
                            scope.launch { offsetX.animateTo(targetX, tween(240)) }
                            scope.launch {
                                offsetY.animateTo(targetY, tween(240))
                                onSwiped(direction)
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, tween(200)) }
                        scope.launch { offsetY.animateTo(0f, tween(200)) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                        scope.launch { offsetY.snapTo(offsetY.value + dragAmount.y) }
                    },
                )
            },
    ) {
        PhotoCard(photo = photo, showMetadata = showMetadata, modifier = Modifier.fillMaxSize())
        DirectionOverlay(
            offset = Offset(offsetX.value, offsetY.value),
            threshold = thresholdPx,
        )
    }
}

@Composable
private fun PhotoCard(
    photo: Photo,
    showMetadata: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (showMetadata) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text(
                            text = photo.displayName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium,
                        )
                        val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
                            .format(java.util.Date(photo.dateTakenMillis))
                        val sizeLabel = Formatter.formatShortFileSize(context, photo.sizeBytes)
                        Text(
                            text = "$date \u00B7 $sizeLabel",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.DirectionOverlay(offset: Offset, threshold: Float) {
    val absDx = abs(offset.x)
    val absDy = abs(offset.y)
    val (color, label) = when {
        absDx < 48f && absDy < 48f -> return
        absDx >= absDy && offset.x > 0 -> Color(0xFF22C55E) to stringResource(R.string.swipe_right_hint)
        absDx >= absDy && offset.x < 0 -> Color(0xFFEF4444) to stringResource(R.string.swipe_left_hint)
        absDy > absDx && offset.y < 0 -> Color(0xFFEAB308) to stringResource(R.string.swipe_up_hint)
        else -> Color(0xFF3B82F6) to stringResource(R.string.swipe_down_hint)
    }
    val intensity = ((maxOf(absDx, absDy) - 48f) / (threshold + 240f)).coerceIn(0f, 0.55f)
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(28.dp))
            .background(color.copy(alpha = intensity)),
        contentAlignment = Alignment.Center,
    ) {
        if (intensity > 0.18f) {
            Text(
                text = label.uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun FooterHints(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HintLabel(stringResource(R.string.swipe_left_hint), Color(0xFFEF4444))
            HintLabel(stringResource(R.string.swipe_up_hint), Color(0xFFEAB308))
            HintLabel(stringResource(R.string.swipe_down_hint), Color(0xFF3B82F6))
            HintLabel(stringResource(R.string.swipe_right_hint), Color(0xFF22C55E))
        }
    }
}

@Composable
private fun HintLabel(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FolderPickerSheet(
    folders: List<SortFolder>,
    folderRoot: FolderRoot,
    onDismiss: () -> Unit,
    onSelected: (SortFolder) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                stringResource(R.string.pick_folder_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.pick_folder_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (folders.isEmpty()) {
                Text(
                    stringResource(R.string.no_folders_yet),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                folders.forEach { folder ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(folder.name, fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    folder.relativePathUnder(folderRoot),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                )
                            },
                            leadingContent = {
                                Icon(Icons.Outlined.Folder, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(
                            onClick = { onSelected(folder) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.move_here)) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
