package com.duptrash.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "delete_patterns")
data class DeletePatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val priority: Int,
    val enabled: Boolean,
)
