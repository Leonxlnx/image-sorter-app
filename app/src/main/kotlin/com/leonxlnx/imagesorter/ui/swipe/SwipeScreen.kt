@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.leonxlnx.imagesorter.ui.swipe

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.leonxlnx.imagesorter.data.Photo
import com.leonxlnx.imagesorter.data.SortFolder
import kotlinx.coroutines.launch
import kotlin.math.abs

private val DragThresholdDp = 96.dp

@Composable
fun SwipeScreen(viewModel: SwipeViewModel = viewModel(factory = SwipeViewModel.factory())) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pickerVisible by rememberSaveable { mutableStateOf(false) }

    val intentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* The result of MediaStore delete is observed implicitly via the next reload. */ }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { ev ->
            when (ev) {
                is SwipeEvent.LaunchIntent ->
                    intentLauncher.launch(IntentSenderRequest.Builder(ev.intentSender).build())
                is SwipeEvent.ChooseFolderForDown -> pickerVisible = true
                is SwipeEvent.Error -> snackbar.showSnackbar(ev.message)
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
            state.isEmpty -> EmptyState(onReload = { viewModel.reload() })
            else -> CardStack(
                state = state,
                onSwiped = { direction ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onSwipe(direction)
                },
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
            folders = state.folders,
            onDismiss = {
                pickerVisible = false
                viewModel.advanceAfterDownDismissed()
            },
            onSelected = { folder ->
                pickerVisible = false
                viewModel.onFolderChosenForDown(folder.uri)
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
private fun EmptyState(onReload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val current = state.currentPhoto ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        ProgressHeader(state)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .onSizeChanged { size = it },
        ) {
            state.nextNextPhoto?.let {
                PhotoCard(
                    photo = it,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 8.dp)
                        .graphicsLayer { alpha = 0.4f },
                )
            }
            state.nextPhoto?.let {
                PhotoCard(
                    photo = it,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .graphicsLayer { alpha = 0.7f },
                )
            }
            DraggableTopCard(
                photo = current,
                size = size,
                showHints = state.showHints,
                onSwiped = onSwiped,
            )
        }
        ActionRow(onAction = onSwiped)
    }
}

@Composable
private fun ProgressHeader(state: SwipeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
                .height(6.dp),
        )
    }
}

@Composable
private fun DraggableTopCard(
    photo: Photo,
    size: IntSize,
    showHints: Boolean,
    onSwiped: (SwipeDirection) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { DragThresholdDp.toPx() }

    val width = size.width.takeIf { it > 0 } ?: 1
    val height = size.height.takeIf { it > 0 } ?: 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = (offsetX.value / width.toFloat()) * 14f
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
                            scope.launch { offsetX.animateTo(0f, tween(180)) }
                            scope.launch { offsetY.animateTo(0f, tween(180)) }
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
                            scope.launch { offsetX.animateTo(targetX, tween(220)) }
                            scope.launch {
                                offsetY.animateTo(targetY, tween(220))
                                onSwiped(direction)
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, tween(180)) }
                        scope.launch { offsetY.animateTo(0f, tween(180)) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                        scope.launch { offsetY.snapTo(offsetY.value + dragAmount.y) }
                    },
                )
            },
    ) {
        PhotoCard(photo = photo, modifier = Modifier.fillMaxSize())
        DirectionOverlay(
            offset = Offset(offsetX.value, offsetY.value),
            threshold = thresholdPx,
        )
        if (showHints) {
            HintCorners()
        }
    }
}

@Composable
private fun PhotoCard(photo: Photo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Column {
                    Text(
                        text = photo.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
                        .format(java.util.Date(photo.dateTakenMillis))
                    val size = Formatter.formatShortFileSize(context, photo.sizeBytes)
                    Text(
                        text = "$date · $size",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
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
        absDx < 32f && absDy < 32f -> return
        absDx >= absDy && offset.x > 0 -> Color(0xFF22C55E) to stringResource(R.string.swipe_right_hint)
        absDx >= absDy && offset.x < 0 -> Color(0xFFEF4444) to stringResource(R.string.swipe_left_hint)
        absDy > absDx && offset.y < 0 -> Color(0xFFEAB308) to stringResource(R.string.swipe_up_hint)
        else -> Color(0xFF3B82F6) to stringResource(R.string.swipe_down_hint)
    }
    val intensity = ((maxOf(absDx, absDy) - 32f) / (threshold + 200f)).coerceIn(0f, 0.55f)
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(24.dp))
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
private fun BoxScope.HintCorners() {
    Box(modifier = Modifier.matchParentSize().padding(20.dp)) {
        HintChip(text = stringResource(R.string.swipe_left_hint), color = Color(0xFFEF4444), align = Alignment.CenterStart)
        HintChip(text = stringResource(R.string.swipe_right_hint), color = Color(0xFF22C55E), align = Alignment.CenterEnd)
        HintChip(text = stringResource(R.string.swipe_up_hint), color = Color(0xFFEAB308), align = Alignment.TopCenter)
        HintChip(text = stringResource(R.string.swipe_down_hint), color = Color(0xFF3B82F6), align = Alignment.BottomCenter)
    }
}

@Composable
private fun BoxScope.HintChip(text: String, color: Color, align: Alignment) {
    Surface(
        modifier = Modifier
            .align(align)
            .wrapContentHeight()
            .padding(4.dp),
        color = color.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ActionRow(onAction: (SwipeDirection) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleAction(color = Color(0xFFEF4444), icon = Icons.Filled.Close) { onAction(SwipeDirection.Left) }
        CircleAction(color = Color(0xFFEAB308), icon = Icons.Filled.ArrowUpward) { onAction(SwipeDirection.Up) }
        CircleAction(color = Color(0xFF3B82F6), icon = Icons.Filled.ArrowDownward) { onAction(SwipeDirection.Down) }
        CircleAction(color = Color(0xFF22C55E), icon = Icons.Filled.Check) { onAction(SwipeDirection.Right) }
    }
}

@Composable
private fun CircleAction(color: Color, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .shadow(8.dp, CircleShape),
        color = color,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun FolderPickerSheet(
    folders: List<SortFolder>,
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
            )
            Spacer(Modifier.height(16.dp))
            if (folders.isEmpty()) {
                Text(
                    stringResource(R.string.no_folders_yet),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                folders.forEach { folder ->
                    ListItem(
                        headlineContent = { Text(folder.displayName) },
                        supportingContent = {
                            Text(
                                buildString {
                                    if (folder.isFavorite) append("Favorites · ")
                                    if (folder.isDefaultDown) append("Default down · ")
                                    append(folder.uri.lastPathSegment ?: "")
                                },
                                maxLines = 1,
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Outlined.Folder, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .padding(vertical = 2.dp),
                    )
                    TextButton(onClick = { onSelected(folder) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Move here")
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
