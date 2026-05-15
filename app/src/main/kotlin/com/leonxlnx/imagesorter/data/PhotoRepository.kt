package com.leonxlnx.imagesorter.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoRepository(private val context: Context) {

    /**
     * Loads media from MediaStore, newest first, filtered by [dateRange] and the
     * [excludeIds] reviewed-set. When [includeVideos] is true the query also covers videos.
     */
    suspend fun load(
        dateRange: DateRange,
        includeVideos: Boolean,
        excludeIds: Set<Long>,
        sortOrder: SortOrder = SortOrder.NewestFirst,
        limit: Int = 500,
    ): List<Photo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Photo>()
        results += queryImages(dateRange, excludeIds, limit)
        if (includeVideos) {
            results += queryVideos(dateRange, excludeIds, limit)
        }
        val ordered = when (sortOrder) {
            SortOrder.NewestFirst -> results.sortedByDescending { it.dateTakenMillis }
            SortOrder.OldestFirst -> results.sortedBy { it.dateTakenMillis }
            SortOrder.LargestFirst -> results.sortedByDescending { it.sizeBytes }
            SortOrder.SmallestFirst -> results.sortedBy { it.sizeBytes }
            SortOrder.Random -> results.shuffled()
        }
        ordered.take(limit)
    }

    private fun queryImages(
        dateRange: DateRange,
        excludeIds: Set<Long>,
        limit: Int,
    ): List<Photo> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
        )
        val (selection, args) = buildDateSelection(dateRange, MediaStore.Images.Media.DATE_TAKEN)
        val list = mutableListOf<Photo>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            while (c.moveToNext() && list.size < limit) {
                val id = c.getLong(idCol)
                if (id in excludeIds) continue
                val taken = c.getLong(takenCol).takeIf { it > 0 } ?: (c.getLong(addedCol) * 1000L)
                list += Photo(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = c.getString(nameCol) ?: "image_$id",
                    mimeType = c.getString(mimeCol) ?: "image/*",
                    dateTakenMillis = taken,
                    sizeBytes = c.getLong(sizeCol),
                    isVideo = false,
                )
            }
        }
        return list
    }

    private fun queryVideos(
        dateRange: DateRange,
        excludeIds: Set<Long>,
        limit: Int,
    ): List<Photo> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
        )
        val (selection, args) = buildDateSelection(dateRange, MediaStore.Video.Media.DATE_TAKEN)
        val list = mutableListOf<Photo>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            args,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC, ${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            while (c.moveToNext() && list.size < limit) {
                val id = c.getLong(idCol)
                if (id in excludeIds) continue
                val taken = c.getLong(takenCol).takeIf { it > 0 } ?: (c.getLong(addedCol) * 1000L)
                list += Photo(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = c.getString(nameCol) ?: "video_$id",
                    mimeType = c.getString(mimeCol) ?: "video/*",
                    dateTakenMillis = taken,
                    sizeBytes = c.getLong(sizeCol),
                    isVideo = true,
                )
            }
        }
        return list
    }

    private fun buildDateSelection(
        dateRange: DateRange,
        column: String,
    ): Pair<String?, Array<String>?> {
        val range = dateRange.toMillisRange() ?: return null to null
        // MediaStore stores DATE_TAKEN in milliseconds, DATE_ADDED in seconds.
        val selection = "$column >= ? AND $column <= ?"
        return selection to arrayOf(range.first.toString(), range.second.toString())
    }
}
