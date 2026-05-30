package com.duptrash.app.data.delete

import com.duptrash.app.data.db.DeletePatternEntity
import com.duptrash.app.data.db.MediaFileEntity
import com.duptrash.app.data.model.DeletionPlan
import com.duptrash.app.data.model.DuplicateGroup

object DeletionPlanner {

    fun plan(groups: List<DuplicateGroup>, patterns: List<DeletePatternEntity>): DeletionPlan {
        val compiled = patterns
            .filter { it.enabled }
            .mapNotNull { p -> runCatching { Regex(p.pattern) }.getOrNull() }

        if (compiled.isEmpty()) {
            return DeletionPlan(toDelete = emptyList(), skipped = emptyList(), untouched = groups)
        }

        val toDelete = mutableListOf<MediaFileEntity>()
        val skipped = mutableListOf<DuplicateGroup>()
        val untouched = mutableListOf<DuplicateGroup>()

        for (group in groups) {
            val matched = group.files.filter { f -> compiled.any { it.containsMatchIn(f.fullPath) } }
            when {
                matched.isEmpty() -> untouched += group
                matched.size == group.files.size -> skipped += group
                else -> toDelete += matched
            }
        }
        return DeletionPlan(toDelete, skipped, untouched)
    }
}
