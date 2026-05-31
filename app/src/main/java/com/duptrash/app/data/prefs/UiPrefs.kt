package com.duptrash.app.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UiPrefs(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _splitRatio = MutableStateFlow(prefs.getFloat(KEY_SPLIT, DEFAULT_SPLIT))
    val splitRatio: StateFlow<Float> = _splitRatio.asStateFlow()

    fun setSplitRatio(value: Float) {
        val clamped = value.coerceIn(MIN_SPLIT, MAX_SPLIT)
        if (clamped == _splitRatio.value) return
        _splitRatio.value = clamped
        prefs.edit().putFloat(KEY_SPLIT, clamped).apply()
    }

    companion object {
        private const val FILE = "duptrash_ui"
        private const val KEY_SPLIT = "split_ratio"
        private const val DEFAULT_SPLIT = 0.65f
        private const val MIN_SPLIT = 0.25f
        private const val MAX_SPLIT = 0.85f
    }
}
