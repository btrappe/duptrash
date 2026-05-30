package com.duptrash.app.data.model

import com.duptrash.app.data.db.MediaFileEntity

data class DeletionPlan(
    val toDelete: List<MediaFileEntity>,
    val skipped: List<DuplicateGroup>,
    val untouched: List<DuplicateGroup>,
) {
    val reclaimableBytes: Long get() = toDelete.sumOf { it.sizeBytes }
}
