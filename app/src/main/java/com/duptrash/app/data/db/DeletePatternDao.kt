package com.duptrash.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletePatternDao {

    @Query("SELECT * FROM delete_patterns ORDER BY priority ASC, id ASC")
    fun observeAll(): Flow<List<DeletePatternEntity>>

    @Query("SELECT * FROM delete_patterns ORDER BY priority ASC, id ASC")
    suspend fun listAll(): List<DeletePatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pattern: DeletePatternEntity): Long

    @Update
    suspend fun update(pattern: DeletePatternEntity)

    @Delete
    suspend fun delete(pattern: DeletePatternEntity)
}
