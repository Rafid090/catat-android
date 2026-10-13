package com.notetodo.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notetodo.app.data.TagDao
import com.notetodo.app.data.TagEntity
import com.notetodo.app.data.TaskDao
import com.notetodo.app.data.TaskEntity
import com.notetodo.app.data.TaskWithTags
import com.notetodo.app.reminder.ReminderScheduler
import com.notetodo.app.ui.notes.NO_TAG_FILTER
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter(val doneFlag: Int) {
    ALL(-1),
    ACTIVE(0),
    DONE(1),
}

class TasksViewModel(
    private val dao: TaskDao,
    private val tagDao: TagDao,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _tagFilter = MutableStateFlow(NO_TAG_FILTER)
    val tagFilter: StateFlow<Long> = _tagFilter.asStateFlow()

    val tags: StateFlow<List<TagEntity>> = tagDao.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var recentlyDeleted: TaskEntity? = null
    private var recentlyDeletedTagIds: List<Long> = emptyList()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskWithTags>> =
        combine(_query, _filter, _tagFilter) { query, filter, tagId ->
            Triple(query.trim(), filter, tagId)
        }
            .flatMapLatest { (query, filter, tagId) ->
                dao.observeTasks(query, filter.doneFlag, tagId)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onFilterChange(value: TaskFilter) {
        _filter.value = value
    }

    /** Tapping the active tag clears the filter. */
    fun onTagFilterClick(tagId: Long) {
        _tagFilter.value = if (_tagFilter.value == tagId) NO_TAG_FILTER else tagId
    }

    fun toggleDone(task: TaskEntity) {
        val updated = task.copy(isDone = !task.isDone, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            dao.update(updated)
            syncReminder(updated)
        }
    }

    fun delete(item: TaskWithTags) {
        recentlyDeleted = item.task
        recentlyDeletedTagIds = item.tags.map { it.id }
        scheduler.cancel(item.task.id)
        viewModelScope.launch { dao.delete(item.task) }
    }

    fun undoDelete() {
        val task = recentlyDeleted ?: return
        val tagIds = recentlyDeletedTagIds
        recentlyDeleted = null
        recentlyDeletedTagIds = emptyList()
        viewModelScope.launch {
            dao.insert(task)
            // The cascade dropped the links along with the task; put them back.
            tagDao.setTaskTags(task.id, tagIds)
            syncReminder(task)
        }
    }

    /** A finished or reminder-less task must not keep a pending alarm around. */
    private fun syncReminder(task: TaskEntity) {
        val reminderAt = task.reminderAt
        if (reminderAt != null && !task.isDone) {
            scheduler.schedule(task.id, task.title, reminderAt)
        } else {
            scheduler.cancel(task.id)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch { dao.clearCompleted() }
    }
}
