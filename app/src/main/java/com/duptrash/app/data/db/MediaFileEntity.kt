package com.duptrash.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_files",
    indices = [
        Index(value = ["sizeBytes"]),
        Index(value = ["md5"]),
        Index(value = ["lastSeenScanId"]),
    ],
)
data class MediaFileEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val relativePath: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateModified: Long,
    val md5: String?,
    val mediaType: Int,
    val lastSeenScanId: Long,
) {
    val fullPath: String get() = relativePath + displayName
}
