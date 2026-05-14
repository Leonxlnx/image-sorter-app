package com.leonxlnx.imagesorter.ui.swipe

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.leonxlnx.imagesorter.ImageSorterApp
import com.leonxlnx.imagesorter.data.DateRange
import com.leonxlnx.imagesorter.data.FolderRepository
import com.leonxlnx.imagesorter.data.Photo
import com.leonxlnx.imagesorter.data.PhotoRepository
import com.leonxlnx.imagesorter.data.ReviewedRepository
import com.leonxlnx.imagesorter.data.SettingsRepository
import com.leonxlnx.imagesorter.data.SortAction
import com.leonxlnx.imagesorter.data.SortActions
import com.leonxlnx.imagesorter.data.SortFolder
import com.leonxlnx.imagesorter.data.SortResult
import com.leonxlnx.imagesorter.data.UndoToken
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class SwipeDirection { Left, Right, Up, Down }

data class SwipeUiState(
    val isLoading: Boolean = true,
    val queue: List<Photo> = emptyList(),
    val cursor: Int = 0,
    val showHints: Boolean = true,
    val undo: UndoToken? = null,
    val folders: List<SortFolder> = emptyList(),
) {
    val isEmpty: Boolean get() = queue.isEmpty() || cursor >= queue.size
    val currentPhoto: Photo? get() = queue.getOrNull(cursor)
    val nextPhoto: Photo? get() = queue.getOrNull(cursor + 1)
    val nextNextPhoto: Photo? get() = queue.getOrNull(cursor + 2)
    val totalProcessed: Int get() = cursor.coerceAtMost(queue.size)
}

/** Event consumed by [SwipeScreen]. Each emission is one-shot. */
sealed interface SwipeEvent {
    data class LaunchIntent(val intentSender: android.content.IntentSender) : SwipeEvent
    data class ChooseFolderForDown(val photoId: Long) : SwipeEvent
    data class Error(val message: String) : SwipeEvent
}

class SwipeViewModel(
    application: Application,
    private val photoRepo: PhotoRepository,
    private val settingsRepo: SettingsRepository,
    private val folderRepo: FolderRepository,
    private val reviewedRepo: ReviewedRepository,
    private val sortActions: SortActions,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SwipeUiState())
    val state: StateFlow<SwipeUiState> = _state.asStateFlow()

    private val _events = Channel<SwipeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Tracks the last sort action so we can offer a single-level Undo. */
    private var lastSwipe: PendingUndo? = null

    init {
        viewModelScope.launch {
            folderRepo.folders.collect { _state.value = _state.value.copy(folders = it) }
        }
        viewModelScope.launch {
            settingsRepo.showHints.collect { _state.value = _state.value.copy(showHints = it) }
        }
        viewModelScope.launch { reload() }
    }

    fun reload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val dateRange = settingsRepo.dateRange.first()
            val includeVideos = settingsRepo.includeVideos.first()
            val skipReviewed = settingsRepo.skipReviewed.first()
            val reviewed = if (skipReviewed) reviewedRepo.ids.first() else emptySet()
            val photos = photoRepo.load(
                dateRange = dateRange,
                includeVideos = includeVideos,
                excludeIds = reviewed,
            )
            _state.value = SwipeUiState(
                isLoading = false,
                queue = photos,
                cursor = 0,
                showHints = _state.value.showHints,
                folders = _state.value.folders,
            )
        }
    }

    fun onSwipe(direction: SwipeDirection) {
        val photo = _state.value.currentPhoto ?: return
        when (direction) {
            SwipeDirection.Right -> execute(photo, SortAction.Keep(photo.id))
            SwipeDirection.Left -> execute(photo, SortAction.Delete(photo.id, photo.uri))
            SwipeDirection.Up -> {
                val favFolder = _state.value.folders.firstOrNull { it.isFavorite }
                if (favFolder == null) {
                    viewModelScope.launch {
                        _events.send(SwipeEvent.Error("Pick a Favorites folder in the Folders tab first."))
                    }
                    return
                }
                execute(
                    photo,
                    SortAction.CopyTo(photo.id, photo.uri, favFolder.uri, photo.displayName, photo.mimeType),
                )
            }
            SwipeDirection.Down -> {
                val defaultDown = _state.value.folders.firstOrNull { it.isDefaultDown }
                if (defaultDown != null && _state.value.folders.size == 1) {
                    execute(
                        photo,
                        SortAction.MoveTo(photo.id, photo.uri, defaultDown.uri, photo.displayName, photo.mimeType),
                    )
                } else {
                    viewModelScope.launch {
                        _events.send(SwipeEvent.ChooseFolderForDown(photo.id))
                    }
                }
            }
        }
    }

    fun onFolderChosenForDown(folderUri: Uri) {
        val photo = _state.value.currentPhoto ?: return
        execute(
            photo,
            SortAction.MoveTo(photo.id, photo.uri, folderUri, photo.displayName, photo.mimeType),
        )
    }

    fun advanceAfterDownDismissed() {
        // User opened the folder picker via a down-swipe but cancelled it. Reset undo state
        // so the previous photo can still be brought back, then bring this photo back on top.
        _state.value = _state.value.copy(undo = null)
    }

    private fun execute(photo: Photo, action: SortAction) {
        viewModelScope.launch {
            when (val result = sortActions.execute(action)) {
                is SortResult.Done -> {
                    lastSwipe = PendingUndo(photo, action)
                    _state.value = _state.value.copy(
                        cursor = _state.value.cursor + 1,
                        undo = result.undo,
                    )
                }
                is SortResult.NeedsConfirmation -> {
                    lastSwipe = PendingUndo(photo, action)
                    _state.value = _state.value.copy(cursor = _state.value.cursor + 1)
                    _events.send(SwipeEvent.LaunchIntent(result.intentSender))
                }
                is SortResult.Failed -> {
                    _events.send(SwipeEvent.Error(result.message))
                }
            }
        }
    }

    fun consumeUndo() {
        val pending = lastSwipe ?: return
        lastSwipe = null
        viewModelScope.launch {
            reviewedRepo.unmark(pending.photo.id)
            // Bring the photo back at the current cursor.
            val newQueue = _state.value.queue.toMutableList()
            val insertAt = (_state.value.cursor - 1).coerceAtLeast(0)
            if (insertAt < newQueue.size) {
                // Replace duplicates if any
                newQueue.add(insertAt, pending.photo)
            } else {
                newQueue.add(pending.photo)
            }
            _state.value = _state.value.copy(
                queue = newQueue,
                cursor = insertAt,
                undo = null,
            )
        }
    }

    fun dismissUndo() {
        _state.value = _state.value.copy(undo = null)
    }

    private data class PendingUndo(val photo: Photo, val action: SortAction)

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as ImageSorterApp
                return SwipeViewModel(
                    application,
                    application.photoRepository,
                    application.settingsRepository,
                    application.folderRepository,
                    application.reviewedRepository,
                    application.sortActions,
                ) as T
            }
        }
    }
}
