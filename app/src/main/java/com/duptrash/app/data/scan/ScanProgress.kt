package com.duptrash.app.data.scan

sealed interface ScanProgress {
    data object Idle : ScanProgress
    data class Enumerating(val seen: Int) : ScanProgress
    data class Hashing(val done: Int, val total: Int, val currentName: String) : ScanProgress
    data class Done(val totalFiles: Int, val hashedFiles: Int, val durationMs: Long) : ScanProgress
    data class Failed(val message: String) : ScanProgress
}
