package com.leonxlnx.imagesorter.data

import android.net.Uri

/** A single sortable media item from [android.provider.MediaStore]. */
data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateTakenMillis: Long,
    val sizeBytes: Long,
    val isVideo: Boolean,
)
