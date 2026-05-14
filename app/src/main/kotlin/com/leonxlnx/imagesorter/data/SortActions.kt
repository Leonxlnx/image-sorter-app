package com.leonxlnx.imagesorter.data

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface SortAction {
    val photoId: Long

    data class Keep(override val photoId: Long) : SortAction
    data class EnqueueDelete(override val photoId: Long, val uri: Uri) : SortAction
    data class CopyTo(
        override val photoId: Long,
        val srcUri: Uri,
        val folder: SortFolder,
        val displayName: String,
        val mimeType: String,
        val isVideo: Boolean,
    ) : SortAction
    data class MoveTo(
        override val photoId: Long,
        val srcUri: Uri,
        val folder: SortFolder,
        val displayName: String,
        val mimeType: String,
        val isVideo: Boolean,
    ) : SortAction
}

/** Outcome of a single sort action. */
sealed interface SortResult {
    data class Done(val undo: UndoToken) : SortResult

    /** Source was copied; the source still needs to be deleted via a batch flush later. */
    data class CopiedPendingDelete(val undo: UndoToken) : SortResult

    /** A pending-delete batch needs system confirmation. The UI launches [intentSender]. */
    data class NeedsConfirmation(val intentSender: IntentSender) : SortResult

    data class Failed(val message: String) : SortResult
}

data class UndoToken(val description: String, val photoId: Long)

/**
 * Applies sort decisions:
 *  - **Keep** → mark reviewed.
 *  - **EnqueueDelete** → defer the actual MediaStore delete; the UI flushes the queue in
 *    batches (one system dialog per batch instead of one per photo).
 *  - **CopyTo / MoveTo** → write the photo into `Pictures/PhotoSwipe/<folder>/` using
 *    MediaStore inserts (or `Environment` on pre-Q). MoveTo also enqueues the source for
 *    the next batch delete.
 */
class SortActions(
    private val context: Context,
    @Suppress("unused") private val folders: FolderRepository,
    private val reviewed: ReviewedRepository,
) {

    /** Per-session queue of pending deletes shared across the app. */
    private val pending: MutableList<Pair<Long, Uri>> = mutableListOf()

    val pendingCount: Int get() = synchronized(pending) { pending.size }

    suspend fun execute(action: SortAction): SortResult = withContext(Dispatchers.IO) {
        when (action) {
            is SortAction.Keep -> {
                reviewed.markReviewed(action.photoId)
                SortResult.Done(UndoToken("Kept", action.photoId))
            }
            is SortAction.EnqueueDelete -> {
                synchronized(pending) { pending += action.photoId to action.uri }
                reviewed.markReviewed(action.photoId)
                SortResult.CopiedPendingDelete(UndoToken("Queued for delete", action.photoId))
            }
            is SortAction.CopyTo -> {
                val copied = copyIntoFolder(action)
                if (copied == null) SortResult.Failed("Could not save to ${action.folder.name}")
                else {
                    reviewed.markReviewed(action.photoId)
                    SortResult.Done(UndoToken("Copied to ${action.folder.name}", action.photoId))
                }
            }
            is SortAction.MoveTo -> {
                val copied = copyIntoFolder(
                    SortAction.CopyTo(
                        photoId = action.photoId,
                        srcUri = action.srcUri,
                        folder = action.folder,
                        displayName = action.displayName,
                        mimeType = action.mimeType,
                        isVideo = action.isVideo,
                    )
                )
                if (copied == null) {
                    SortResult.Failed("Could not save to ${action.folder.name}")
                } else {
                    synchronized(pending) { pending += action.photoId to action.srcUri }
                    reviewed.markReviewed(action.photoId)
                    SortResult.CopiedPendingDelete(
                        UndoToken("Moved to ${action.folder.name}", action.photoId)
                    )
                }
            }
        }
    }

    /** Take any queued deletes and produce an IntentSender for a single system dialog. */
    suspend fun flushPendingDeletes(): SortResult? = withContext(Dispatchers.IO) {
        val snapshot = synchronized(pending) {
            if (pending.isEmpty()) return@withContext null
            val copy = pending.toList()
            pending.clear()
            copy
        }
        deleteBatch(snapshot.map { it.second })
    }

    fun clearPendingDeletes() {
        synchronized(pending) { pending.clear() }
    }

    /** Called when the user pulls a pending-delete back via Undo. */
    fun removePendingDelete(photoId: Long) {
        synchronized(pending) { pending.removeAll { it.first == photoId } }
    }

    private fun deleteBatch(uris: List<Uri>): SortResult {
        if (uris.isEmpty()) return SortResult.Failed("Nothing to delete")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            SortResult.NeedsConfirmation(intent.intentSender)
        } else {
            var deleted = 0
            for (uri in uris) {
                runCatching { deleted += context.contentResolver.delete(uri, null, null) }
            }
            SortResult.Done(UndoToken("Deleted $deleted", -1))
        }
    }

    /** Writes the source photo into the destination folder, returning the new URI. */
    private fun copyIntoFolder(action: SortAction.CopyTo): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyViaMediaStore(action)
        } else {
            copyViaLegacyFile(action)
        }
    }

    private fun copyViaMediaStore(action: SortAction.CopyTo): Uri? {
        val resolver = context.contentResolver
        val collection = if (action.isVideo)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, action.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, action.mimeType.ifBlank {
                if (action.isVideo) "video/*" else "image/*"
            })
            put(MediaStore.MediaColumns.RELATIVE_PATH, action.folder.relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val newUri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(newUri).use { out ->
                resolver.openInputStream(action.srcUri).use { input ->
                    if (input == null || out == null) {
                        resolver.delete(newUri, null, null)
                        return null
                    }
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(newUri, values, null, null)
            newUri
        } catch (t: Throwable) {
            runCatching { resolver.delete(newUri, null, null) }
            null
        }
    }

    private fun copyViaLegacyFile(action: SortAction.CopyTo): Uri? {
        val baseDir = Environment.getExternalStoragePublicDirectory(
            if (action.isVideo) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_PICTURES
        )
        val dir = File(baseDir, "${SortFolder.ROOT}/${action.folder.name}")
        if (!dir.exists() && !dir.mkdirs()) return null
        val target = uniqueFile(dir, action.displayName)
        return runCatching {
            context.contentResolver.openInputStream(action.srcUri).use { input ->
                FileOutputStream(target).use { out ->
                    input?.copyTo(out) ?: return@runCatching null
                }
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(target.absolutePath),
                arrayOf(action.mimeType.ifBlank { if (action.isVideo) "video/*" else "image/*" }),
                null,
            )
            target.toUri()
        }.getOrNull()
    }

    private fun uniqueFile(dir: File, displayName: String): File {
        val candidate = File(dir, displayName)
        if (!candidate.exists()) return candidate
        val dot = displayName.lastIndexOf('.')
        val base = if (dot > 0) displayName.substring(0, dot) else displayName
        val ext = if (dot > 0) displayName.substring(dot) else ""
        var i = 1
        while (File(dir, "$base ($i)$ext").exists()) {
            i++
            if (i > 999) return File(dir, "$base-${System.currentTimeMillis()}$ext")
        }
        return File(dir, "$base ($i)$ext")
    }
}
