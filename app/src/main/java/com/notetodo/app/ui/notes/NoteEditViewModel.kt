package com.notetodo.app.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notetodo.app.data.NoteDao
import com.notetodo.app.data.NoteEntity
import com.notetodo.app.data.TagDao
import com.notetodo.app.data.TagEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val dao: NoteDao,
    private val tagDao: TagDao,
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>(NOTE_ID_ARG) ?: NEW_NOTE_ID

    var note by mutableStateOf(NoteEntity())
        private set

    var selectedTagIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    val allTags: StateFlow<List<TagEntity>> = tagDao.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isNew: Boolean get() = note.id == NEW_NOTE_ID

    init {
        if (noteId != NEW_NOTE_ID) {
            viewModelScope.launch {
                dao.getById(noteId)?.let { note = it }
                selectedTagIds = tagDao.tagIdsForNote(noteId).toSet()
            }
        }
    }

    fun onTitleChange(value: String) {
        note = note.copy(title = value)
    }

    fun onContentChange(value: String) {
        note = note.copy(content = value)
    }

    fun togglePin() {
        note = note.copy(isPinned = !note.isPinned)
    }

    fun toggleTag(tagId: Long) {
        selectedTagIds = if (tagId in selectedTagIds) {
            selectedTagIds - tagId
        } else {
            selectedTagIds + tagId
        }
    }

    /** Creates the tag if it is new, then attaches it to this note. */
    fun addTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = tagDao.getOrCreate(name)
            if (id > 0) selectedTagIds = selectedTagIds + id
        }
    }

    /**
     * Called when leaving the screen. A note the user emptied out is discarded
     * rather than saved as a blank row.
     */
    fun save() {
        val current = note
        val tagIds = selectedTagIds.toList()
        viewModelScope.launch {
            if (current.isBlank) {
                if (current.id != NEW_NOTE_ID) dao.delete(current)
                return@launch
            }
            val stamped = current.copy(updatedAt = System.currentTimeMillis())
            val id = if (stamped.id == NEW_NOTE_ID) {
                dao.insert(stamped)
            } else {
                dao.update(stamped)
                stamped.id
            }
            tagDao.setNoteTags(id, tagIds)
        }
    }

    fun delete() {
        val current = note
        if (current.id == NEW_NOTE_ID) return
        viewModelScope.launch { dao.delete(current) }
    }

    companion object {
        const val NOTE_ID_ARG = "noteId"
        const val NEW_NOTE_ID = 0L
    }
}
