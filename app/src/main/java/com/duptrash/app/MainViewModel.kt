package com.duptrash.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duptrash.app.data.db.DeletePatternEntity
import com.duptrash.app.data.delete.KeeperPlanner
import com.duptrash.app.data.model.DuplicateGroup
import com.duptrash.app.data.model.KeeperPlan
import com.duptrash.app.data.prefs.UiPrefs
import com.duptrash.app.data.scan.DuplicateFinder
import com.duptrash.app.data.scan.MediaScanner
import com.duptrash.app.data.scan.ScanProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val context get() = getApplication<DupTrashApp>()
    private val database get() = getApplication<DupTrashApp>().database

    private val mediaScanner = MediaScanner(context)
    private val duplicateFinder = DuplicateFinder(context)

    val scanProgress: StateFlow<ScanProgress> = mediaScanner.progress

    private val _groups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val groups: StateFlow<List<DuplicateGroup>> = _groups.asStateFlow()

    private val _plan = MutableStateFlow<KeeperPlan?>(null)
    val plan: StateFlow<KeeperPlan?> = _plan.asStateFlow()

    private val _overrides = MutableStateFlow<Map<String, Long>>(emptyMap())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val patterns: StateFlow<List<DeletePatternEntity>> =
        database.deletePatternDao().observeAll().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList(),
        )

    val splitRatio: StateFlow<Float> get() = getApplication<DupTrashApp>().uiPrefs.splitRatio
    val customFolderUris: StateFlow<Set<String>> get() = getApplication<DupTrashApp>().uiPrefs.customFolderUris

    fun setSplitRatio(value: Float) {
        getApplication<DupTrashApp>().uiPrefs.setSplitRatio(value)
    }

    fun addCustomFolder(uri: String) {
        getApplication<DupTrashApp>().uiPrefs.addCustomFolder(uri)
    }

    fun removeCustomFolder(uri: String) {
        getApplication<DupTrashApp>().uiPrefs.removeCustomFolder(uri)
    }

    fun startScan() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                mediaScanner.scan(getApplication<DupTrashApp>().uiPrefs.customFolderUris.value)
            } finally {
                _busy.value = false
            }
        }
    }

    fun findDuplicates() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val found = duplicateFinder.find()
                _groups.value = found
                _overrides.value = emptyMap()
                recompute()
            } finally {
                _busy.value = false
            }
        }
    }

    fun setKeeperOverride(md5: String, fileId: Long) {
        _overrides.value = _overrides.value + (md5 to fileId)
        recompute()
    }

    fun recomputePlan() = recompute()

    private fun recompute() {
        _plan.value = KeeperPlanner.plan(_groups.value, patterns.value, _overrides.value)
    }

    fun addPattern(pattern: String) {
        if (pattern.isBlank()) return
        viewModelScope.launch {
            val dao = database.deletePatternDao()
            val nextPriority = (patterns.value.maxOfOrNull { it.priority } ?: -1) + 1
            dao.insert(DeletePatternEntity(pattern = pattern, priority = nextPriority, enabled = true))
            recompute()
        }
    }

    fun updatePattern(p: DeletePatternEntity) {
        viewModelScope.launch {
            database.deletePatternDao().update(p)
            recompute()
        }
    }

    fun deletePattern(p: DeletePatternEntity) {
        viewModelScope.launch {
            database.deletePatternDao().delete(p)
            recompute()
        }
    }

    fun onTrashRequestResult(success: Boolean) {
        if (!success) return
        viewModelScope.launch {
            val plan = _plan.value ?: return@launch
            database.mediaFileDao().deleteByIds(plan.toDelete.map { it.id })
            _overrides.value = emptyMap()
            val remaining = duplicateFinder.find()
            _groups.value = remaining
            recompute()
        }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(app) as T
            }
    }
}
