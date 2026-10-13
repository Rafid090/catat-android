package com.notetodo.app.ui.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notetodo.app.data.TagDao
import com.notetodo.app.data.TagEntity
import com.notetodo.app.data.TaskDao
import com.notetodo.app.data.TaskEntity
import com.notetodo.app.reminder.ReminderScheduler
import com.notetodo.app.util.toEpochMillis
import com.notetodo.app.util.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val dao: TaskDao,
    private val tagDao: TagDao,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>(TASK_ID_ARG) ?: NEW_TASK_ID

    var task by mutableStateOf(TaskEntity())
        private set

    var selectedTagIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    val allTags: StateFlow<List<TagEntity>> = tagDao.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isNew: Boolean get() = task.id == NEW_TASK_ID

    /** Blocks saving an untitled task; drives the Save button's enabled state. */
    val canSave: Boolean get() = task.title.isNotBlank()

    /** False once Android 12+ has denied exact alarms, so the UI can warn about drift. */
    val canScheduleExact: Boolean get() = scheduler.canScheduleExact()

    init {
        if (taskId != NEW_TASK_ID) {
            viewModelScope.launch {
                dao.getById(taskId)?.let { task = it }
                selectedTagIds = tagDao.tagIdsForTask(taskId).toSet()
            }
        }
    }

    fun onTitleChange(value: String) {
        task = task.copy(title = value)
    }

    fun onNoteChange(value: String) {
        task = task.copy(note = value)
    }

    fun onDueDateChange(millis: Long?) {
        task = task.copy(dueAt = millis)
        // Keep an existing reminder on the same day as the due date.
        task.reminderAt?.let { existing ->
            val time = java.time.Instant.ofEpochMilli(existing)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalTime()
            onReminderTimeChange(time.hour, time.minute)
        }
    }

    /**
     * The reminder lands on the due date, or today when there is none. Picking a
     * time that already passed with no due date is read as "tomorrow" rather than
     * silently producing a reminder that can never fire.
     */
    fun onReminderTimeChange(hour: Int, minute: Int) {
        val baseDate = task.dueAt?.toLocalDate() ?: LocalDate.now()
        var instant = baseDate.atTime(hour, minute).toEpochMillis()
        if (task.dueAt == null && instant <= System.currentTimeMillis()) {
            instant = baseDate.plusDays(1).atTime(hour, minute).toEpochMillis()
        }
        task = task.copy(reminderAt = instant)
    }

    fun clearReminder() {
        task = task.copy(reminderAt = null)
    }

    fun toggleTag(tagId: Long) {
        selectedTagIds = if (tagId in selectedTagIds) {
            selectedTagIds - tagId
        } else {
            selectedTagIds + tagId
        }
    }

    /** Creates the tag if it is new, then attaches it to this task. */
    fun addTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = tagDao.getOrCreate(name)
            if (id > 0) selectedTagIds = selectedTagIds + id
        }
    }

    fun toggleHighPriority() {
        task = task.copy(isHighPriority = !task.isHighPriority)
    }

    fun save() {
        if (!canSave) return
        val stamped = task.copy(title = task.title.trim(), updatedAt = System.currentTimeMillis())
        val tagIds = selectedTagIds.toList()
        viewModelScope.launch {
            val id = if (stamped.id == NEW_TASK_ID) {
                dao.insert(stamped)
            } else {
                dao.update(stamped)
                stamped.id
            }
            tagDao.setTaskTags(id, tagIds)
            val reminderAt = stamped.reminderAt
            if (reminderAt != null && !stamped.isDone) {
                scheduler.schedule(id, stamped.title, reminderAt)
            } else {
                scheduler.cancel(id)
            }
        }
    }

    fun delete() {
        val current = task
        if (current.id == NEW_TASK_ID) return
        scheduler.cancel(current.id)
        viewModelScope.launch { dao.delete(current) }
    }

    companion object {
        const val TASK_ID_ARG = "taskId"
        const val NEW_TASK_ID = 0L
    }
}
