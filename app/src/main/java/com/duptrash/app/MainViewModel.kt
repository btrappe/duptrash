package com.duptrash.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duptrash.app.data.db.DeletePatternEntity
import com.duptrash.app.data.delete.DeletionPlanner
import com.duptrash.app.data.model.DeletionPlan
import com.duptrash.app.data.model.DuplicateGroup
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

    private val _plan = MutableStateFlow<DeletionPlan?>(null)
    val plan: StateFlow<DeletionPlan?> = _plan.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val patterns: StateFlow<List<DeletePatternEntity>> =
        database.deletePatternDao().observeAll().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList(),
        )

    fun startScan() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { mediaScanner.scan() } finally { _busy.value = false }
        }
    }

    fun findDuplicates() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val found = duplicateFinder.find()
                _groups.value = found
                recomputePlan(found, patterns.value)
            } finally {
                _busy.value = false
            }
        }
    }

    fun recomputePlan() {
        recomputePlan(_groups.value, patterns.value)
    }

    private fun recomputePlan(groups: List<DuplicateGroup>, patterns: List<DeletePatternEntity>) {
        _plan.value = DeletionPlanner.plan(groups, patterns)
    }

    fun addPattern(pattern: String) {
        if (pattern.isBlank()) return
        viewModelScope.launch {
            val dao = database.deletePatternDao()
            val nextPriority = (patterns.value.maxOfOrNull { it.priority } ?: -1) + 1
            dao.insert(DeletePatternEntity(pattern = pattern, priority = nextPriority, enabled = true))
            recomputePlan()
        }
    }

    fun updatePattern(p: DeletePatternEntity) {
        viewModelScope.launch {
            database.deletePatternDao().update(p)
            recomputePlan()
        }
    }

    fun deletePattern(p: DeletePatternEntity) {
        viewModelScope.launch {
            database.deletePatternDao().delete(p)
            recomputePlan()
        }
    }

    fun onTrashRequestResult(success: Boolean) {
        if (success) {
            viewModelScope.launch {
                val plan = _plan.value ?: return@launch
                database.mediaFileDao().deleteByIds(plan.toDelete.map { it.id })
                val remaining = duplicateFinder.find()
                _groups.value = remaining
                recomputePlan(remaining, patterns.value)
            }
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
