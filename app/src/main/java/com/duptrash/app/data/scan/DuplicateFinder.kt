package com.duptrash.app.data.scan

import android.content.Context
import com.duptrash.app.data.db.AppDatabase
import com.duptrash.app.data.model.DuplicateGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DuplicateFinder(private val context: Context) {

    suspend fun find(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val rows = AppDatabase.get(context).mediaFileDao().duplicateRows()
        rows.groupBy { it.md5!! }
            .map { (md5, files) -> DuplicateGroup(md5, files) }
            .sortedByDescending { it.reclaimableBytes }
    }
}
