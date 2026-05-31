package com.duptrash.app.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UiPrefs(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _splitRatio = MutableStateFlow(prefs.getFloat(KEY_SPLIT, DEFAULT_SPLIT))
    val splitRatio: StateFlow<Float> = _splitRatio.asStateFlow()

    private val _folders = MutableStateFlow(
        prefs.getStringSet(KEY_FOLDERS, emptySet())?.toSet() ?: emptySet()
    )
    val customFolderUris: StateFlow<Set<String>> = _folders.asStateFlow()

    fun setSplitRatio(value: Float) {
        val clamped = value.coerceIn(MIN_SPLIT, MAX_SPLIT)
        if (clamped == _splitRatio.value) return
        _splitRatio.value = clamped
        prefs.edit().putFloat(KEY_SPLIT, clamped).apply()
    }

    fun addCustomFolder(uri: String) {
        val next = _folders.value + uri
        if (next == _folders.value) return
        _folders.value = next
        prefs.edit().putStringSet(KEY_FOLDERS, next).apply()
    }

    fun removeCustomFolder(uri: String) {
        val next = _folders.value - uri
        if (next == _folders.value) return
        _folders.value = next
        prefs.edit().putStringSet(KEY_FOLDERS, next).apply()
    }

    /**
     * Drops any stored folder URI that no longer has a live persisted grant
     * (e.g. after reinstall + auto-backup restore, or manual revocation in
     * system Settings). Returns the count of stale URIs that were dropped.
     */
    fun reconcileFolderGrants(): Int {
        val live = appContext.contentResolver.persistedUriPermissions
            .mapTo(HashSet()) { it.uri.toString() }
        val current = _folders.value
        val pruned = current.filterTo(HashSet()) { it in live }
        if (pruned.size == current.size) return 0
        _folders.value = pruned
        prefs.edit().putStringSet(KEY_FOLDERS, pruned).apply()
        return current.size - pruned.size
    }

    companion object {
        private const val FILE = "duptrash_ui"
        private const val KEY_SPLIT = "split_ratio"
        private const val KEY_FOLDERS = "custom_folder_uris"
        private const val DEFAULT_SPLIT = 0.65f
        private const val MIN_SPLIT = 0.25f
        private const val MAX_SPLIT = 0.85f
    }
}
