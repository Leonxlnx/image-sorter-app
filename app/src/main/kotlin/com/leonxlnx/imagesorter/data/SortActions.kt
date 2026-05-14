package com.leonxlnx.imagesorter.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SortAction {
    val photoId: Long
    data class Keep(override val photoId: Long) : SortAction
    data class Delete(override val photoId: Long, val uri: Uri) : SortAction
    data class CopyTo(override val photoId: Long, val srcUri: Uri, val destFolder: Uri, val displayName: String, val mimeType: String) : SortAction
    data class MoveTo(override val photoId: Long, val srcUri: Uri, val destFolder: Uri, val displayName: String, val mimeType: String) : SortAction
}

/** Outcome of a sort action that the UI/ViewModel needs to react to. */
sealed interface SortResult {
    /** Operation finished without user intervention. */
    data class Done(val undo: UndoToken) : SortResult
    /** Pre-Android-11 surfaced a [RecoverableSecurityException]; the UI must launch the intent. */
    data class NeedsConfirmation(val intentSender: IntentSender, val pending: SortAction) : SortResult
    data class Failed(val message: String) : SortResult
}

/** Lightweight record we use to surface a single "Undo" toast after every swipe. */
data class UndoToken(val description: String, val photoId: Long)

class SortActions(
    private val context: Context,
    private val folders: FolderRepository,
    private val reviewed: ReviewedRepository,
) {

    suspend fun execute(action: SortAction): SortResult = withContext(Dispatchers.IO) {
        when (action) {
            is SortAction.Keep -> {
                reviewed.markReviewed(action.photoId)
                SortResult.Done(UndoToken("Kept photo", action.photoId))
            }
            is SortAction.Delete -> deletePhoto(action)
            is SortAction.CopyTo -> copy(action.srcUri, action.destFolder, action.displayName, action.mimeType)?.let {
                reviewed.markReviewed(action.photoId)
                SortResult.Done(UndoToken("Copied to folder", action.photoId))
            } ?: SortResult.Failed("Could not write to destination folder")
            is SortAction.MoveTo -> {
                val copied = copy(action.srcUri, action.destFolder, action.displayName, action.mimeType)
                if (copied == null) {
                    SortResult.Failed("Could not write to destination folder")
                } else {
                    // After a successful copy delete the source; this may still require user confirmation
                    // on Android 11+ so it follows the same code path as a regular delete.
                    deletePhoto(SortAction.Delete(action.photoId, action.srcUri))
                }
            }
        }
    }

    private fun deletePhoto(action: SortAction.Delete): SortResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = MediaStore.createDeleteRequest(context.contentResolver, listOf(action.uri))
            SortResult.NeedsConfirmation(intent.intentSender, action)
        } else {
            try {
                val rows = context.contentResolver.delete(action.uri, null, null)
                if (rows > 0) {
                    SortResult.Done(UndoToken("Deleted", action.photoId))
                } else {
                    SortResult.Failed("Delete returned 0 rows")
                }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    SortResult.NeedsConfirmation(e.userAction.actionIntent.intentSender, action)
                } else {
                    SortResult.Failed(e.message ?: "Permission denied")
                }
            }
        }
    }

    /** Streams the source content to a new file inside [destFolder]. */
    private fun copy(src: Uri, destFolder: Uri, displayName: String, mimeType: String): Uri? {
        val tree = DocumentFile.fromTreeUri(context, destFolder) ?: return null
        // De-duplicate display name by appending a numeric suffix on collision.
        val safeName = uniqueName(tree, displayName)
        val newFile = tree.createFile(mimeType.ifBlank { "image/*" }, safeName) ?: return null
        runCatching {
            context.contentResolver.openInputStream(src).use { input ->
                context.contentResolver.openOutputStream(newFile.uri).use { output ->
                    if (input == null || output == null) return@runCatching null
                    input.copyTo(output)
                }
            }
        }.onFailure {
            newFile.delete()
            return null
        }
        return newFile.uri
    }

    private fun uniqueName(tree: DocumentFile, displayName: String): String {
        if (tree.findFile(displayName) == null) return displayName
        val dot = displayName.lastIndexOf('.')
        val base = if (dot > 0) displayName.substring(0, dot) else displayName
        val ext = if (dot > 0) displayName.substring(dot) else ""
        var i = 1
        while (tree.findFile("$base ($i)$ext") != null) {
            i++
            if (i > 999) return "$base-${System.currentTimeMillis()}$ext"
        }
        return "$base ($i)$ext"
    }

    @Suppress("unused")
    fun foldersRepository(): FolderRepository = folders
}
