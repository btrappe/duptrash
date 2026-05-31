package com.duptrash.app.data.model

import com.duptrash.app.data.db.MediaFileEntity

enum class KeeperReason { REGEX, NAME, SIMILARITY, RANDOM, USER_OVERRIDE }

data class KeeperGroup(
    val md5: String,
    val keeper: MediaFileEntity,
    val victims: List<MediaFileEntity>,
    val reason: KeeperReason,
    val sizeBytes: Long,
) {
    val reclaimableBytes: Long get() = sizeBytes * victims.size
}

data class KeeperPlan(val groups: List<KeeperGroup>) {
    val toDelete: List<MediaFileEntity> get() = groups.flatMap { it.victims }
    val reclaimableBytes: Long get() = toDelete.sumOf { it.sizeBytes }
    val randomCount: Int get() = groups.count { it.reason == KeeperReason.RANDOM }
}
