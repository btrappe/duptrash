package com.duptrash.app.data.scan

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import com.duptrash.app.data.db.AppDatabase
import com.duptrash.app.data.db.MediaFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MediaScanner(private val context: Context) {

    private val _progress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    suspend fun scan() = withContext(Dispatchers.IO) {
        val started = SystemClock.elapsedRealtime()
        try {
            _progress.value = ScanProgress.Enumerating(0)
            val db = AppDatabase.get(context)
            val mediaDao = db.mediaFileDao()
            val scanId = System.currentTimeMillis()

            val enumerated = enumerateMediaStore(scanId, mediaDao)
            _progress.value = ScanProgress.Enumerating(enumerated)

            mediaDao.purgeStale(scanId)

            val hashed = HashWorker(context).hashCandidates { done, total, name ->
                _progress.value = ScanProgress.Hashing(done, total, name)
            }

            _progress.value = ScanProgress.Done(
                totalFiles = enumerated,
                hashedFiles = hashed,
                durationMs = SystemClock.elapsedRealtime() - started,
            )
        } catch (t: Throwable) {
            _progress.value = ScanProgress.Failed(t.message ?: t::class.java.simpleName)
        }
    }

    @SuppressLint("InlinedApi")
    private suspend fun enumerateMediaStore(scanId: Long, dao: com.duptrash.app.data.db.MediaFileDao): Int {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Files.getContentUri("external")
        }

        val mediaTypeImage = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val mediaTypeVideo = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val args = arrayOf(mediaTypeImage.toString(), mediaTypeVideo.toString())

        val batch = mutableListOf<MediaFileEntity>()
        var count = 0

        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val relCol = c.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mtimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val size = c.getLong(sizeCol)
                if (size <= 0L) continue
                val mtime = c.getLong(mtimeCol)
                val mediaType = c.getInt(typeCol)
                val name = c.getString(nameCol) ?: continue
                val mime = c.getString(mimeCol) ?: ""
                val relativePath = if (relCol >= 0) c.getString(relCol) ?: "" else ""

                val uri = ContentUris.withAppendedId(collection, id).toString()

                val existing = dao.findById(id)
                val keepMd5 = existing?.md5?.takeIf {
                    existing.sizeBytes == size && existing.dateModified == mtime
                }

                batch += MediaFileEntity(
                    id = id,
                    uri = uri,
                    relativePath = relativePath,
                    displayName = name,
                    mimeType = mime,
                    sizeBytes = size,
                    dateModified = mtime,
                    md5 = keepMd5,
                    mediaType = mediaType,
                    lastSeenScanId = scanId,
                )
                count++

                if (batch.size >= 200) {
                    dao.upsertAll(batch)
                    batch.clear()
                    _progress.value = ScanProgress.Enumerating(count)
                }
            }
        }
        if (batch.isNotEmpty()) dao.upsertAll(batch)
        return count
    }
}
