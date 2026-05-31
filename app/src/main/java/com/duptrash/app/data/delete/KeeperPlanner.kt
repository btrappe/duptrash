package com.duptrash.app.data.delete

import com.duptrash.app.data.db.DeletePatternEntity
import com.duptrash.app.data.db.MediaFileEntity
import com.duptrash.app.data.model.DuplicateGroup
import com.duptrash.app.data.model.KeeperGroup
import com.duptrash.app.data.model.KeeperPlan
import com.duptrash.app.data.model.KeeperReason

object KeeperPlanner {

    fun plan(
        groups: List<DuplicateGroup>,
        patterns: List<DeletePatternEntity>,
        overrides: Map<String, Long> = emptyMap(),
    ): KeeperPlan {
        val regexes = patterns
            .filter { it.enabled }
            .mapNotNull { runCatching { Regex(it.pattern) }.getOrNull() }

        val seenKeepers = mutableListOf<MediaFileEntity>()
        val results = mutableListOf<KeeperGroup>()

        for (group in groups) {
            val overrideId = overrides[group.md5]
            val keeper: MediaFileEntity
            val reason: KeeperReason

            if (overrideId != null) {
                val chosen = group.files.firstOrNull { it.id == overrideId }
                if (chosen != null) {
                    keeper = chosen
                    reason = KeeperReason.USER_OVERRIDE
                } else {
                    val pick = pickAutomatic(group, regexes, seenKeepers)
                    keeper = pick.first
                    reason = pick.second
                }
            } else {
                val pick = pickAutomatic(group, regexes, seenKeepers)
                keeper = pick.first
                reason = pick.second
            }

            seenKeepers += keeper
            results += KeeperGroup(
                md5 = group.md5,
                keeper = keeper,
                victims = group.files.filter { it.id != keeper.id },
                reason = reason,
                sizeBytes = group.sizeBytes,
            )
        }
        return KeeperPlan(results)
    }

    private fun pickAutomatic(
        group: DuplicateGroup,
        regexes: List<Regex>,
        seenKeepers: List<MediaFileEntity>,
    ): Pair<MediaFileEntity, KeeperReason> {
        val unmatched = if (regexes.isEmpty()) group.files
                        else group.files.filter { f -> regexes.none { it.containsMatchIn(f.fullPath) } }

        // Regex narrowed to exactly one survivor — clear winner.
        if (regexes.isNotEmpty() && unmatched.size == 1) {
            return unmatched.first() to KeeperReason.REGEX
        }

        // Candidates: prefer unmatched copies; if every copy matched a regex, fall back to all copies.
        val candidates = if (unmatched.isNotEmpty()) unmatched else group.files

        // Name-series heuristic: same folder, names like foo.jpg / foo(1).jpg / foo(2).jpg.
        // Prefer the lowest-numbered copy (treating the unnumbered original as N=0).
        pickByCanonicalName(candidates)?.let { return it to KeeperReason.NAME }

        // No prior keepers to compare against — pick canonically (shortest full path) and tag RANDOM.
        if (seenKeepers.isEmpty()) {
            return candidates.minBy { it.fullPath.length } to KeeperReason.RANDOM
        }

        // Otherwise pick the candidate whose path is most similar to existing keepers.
        val best = candidates.maxBy { c -> lcpScore(c.fullPath, seenKeepers) }
        // If similarity is genuinely zero across the board (no shared prefix at all), call it RANDOM
        // so the user knows the choice was uninformed.
        return if (lcpScore(best.fullPath, seenKeepers) == 0)
            best to KeeperReason.RANDOM
        else
            best to KeeperReason.SIMILARITY
    }

    private fun lcpScore(path: String, pool: List<MediaFileEntity>): Int =
        pool.maxOf { longestCommonPrefixLength(path, it.fullPath) }

    private fun longestCommonPrefixLength(a: String, b: String): Int {
        val n = minOf(a.length, b.length)
        var i = 0
        while (i < n && a[i] == b[i]) i++
        return i
    }

    private fun pickByCanonicalName(candidates: List<MediaFileEntity>): MediaFileEntity? {
        val series = candidates.groupBy { it.relativePath to canonicalBaseName(it.displayName) }
        val biggest = series.maxByOrNull { it.value.size } ?: return null
        if (biggest.value.size < 2) return null
        return biggest.value.minByOrNull { copyNumber(it.displayName) }
    }

    private val NUMBERED_RE = Regex("""^(.*?)\((\d+)\)(\.[^.]+)?$""")

    private fun canonicalBaseName(name: String): String =
        NUMBERED_RE.matchEntire(name)?.let { it.groupValues[1] + it.groupValues[3] } ?: name

    private fun copyNumber(name: String): Int =
        NUMBERED_RE.matchEntire(name)?.groupValues?.get(2)?.toIntOrNull() ?: 0
}
