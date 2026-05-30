package com.duptrash.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaFileDao {

    @Query("SELECT * FROM media_files WHERE id = :id")
    suspend fun findById(id: Long): MediaFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<MediaFileEntity>)

    @Query("UPDATE media_files SET lastSeenScanId = :scanId WHERE id = :id")
    suspend fun touch(id: Long, scanId: Long)

    @Query("DELETE FROM media_files WHERE lastSeenScanId != :scanId")
    suspend fun purgeStale(scanId: Long): Int

    @Query("SELECT COUNT(*) FROM media_files")
    suspend fun count(): Int

    @Query(
        """
        SELECT sizeBytes FROM media_files
        WHERE md5 IS NULL
        GROUP BY sizeBytes
        HAVING COUNT(*) > 1
        """
    )
    suspend fun sizesNeedingHash(): List<Long>

    @Query("SELECT * FROM media_files WHERE md5 IS NULL AND sizeBytes = :size")
    suspend fun rowsBySizeMissingHash(size: Long): List<MediaFileEntity>

    @Query("UPDATE media_files SET md5 = :md5 WHERE id = :id")
    suspend fun setMd5(id: Long, md5: String)

    @Query(
        """
        SELECT * FROM media_files
        WHERE md5 IS NOT NULL
          AND md5 IN (
              SELECT md5 FROM media_files
              WHERE md5 IS NOT NULL
              GROUP BY md5
              HAVING COUNT(*) > 1
          )
        ORDER BY md5, relativePath, displayName
        """
    )
    suspend fun duplicateRows(): List<MediaFileEntity>

    @Query("DELETE FROM media_files WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
