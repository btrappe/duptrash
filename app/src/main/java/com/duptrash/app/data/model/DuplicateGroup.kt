package com.duptrash.app.data.model

import com.duptrash.app.data.db.MediaFileEntity

data class DuplicateGroup(
    val md5: String,
    val files: List<MediaFileEntity>,
) {
    val sizeBytes: Long get() = files.firstOrNull()?.sizeBytes ?: 0L
    val reclaimableBytes: Long get() = sizeBytes * (files.size - 1).coerceAtLeast(0)
}
