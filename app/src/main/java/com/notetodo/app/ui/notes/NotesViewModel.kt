package com.notetodo.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notetodo.app.data.NoteDao
import com.notetodo.app.data.NoteEntity
import com.notetodo.app.data.NoteWithTags
import com.notetodo.app.data.TagDao
import com.notetodo.app.data.TagEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val NO_TAG_FILTER = 0L

class NotesViewModel(
    private val dao: NoteDao,
    private val tagDao: TagDao,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _tagFilter = MutableStateFlow(NO_TAG_FILTER)
    val tagFilter: StateFlow<Long> = _tagFilter.asStateFlow()

    val tags: StateFlow<List<TagEntity>> = tagDao.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Held in memory only, so undo survives a snackbar but not a process death. */
    private var recentlyDeleted: NoteEntity? = null
    private var recentlyDeletedTagIds: List<Long> = emptyList()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteWithTags>> =
        combine(_query, _tagFilter) { query, tagId -> query.trim() to tagId }
            .flatMapLatest { (query, tagId) -> dao.observeNotes(query, tagId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Tapping the active tag clears the filter. */
    fun onTagFilterClick(tagId: Long) {
        _tagFilter.value = if (_tagFilter.value == tagId) NO_TAG_FILTER else tagId
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            dao.update(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun delete(item: NoteWithTags) {
        recentlyDeleted = item.note
        recentlyDeletedTagIds = item.tags.map { it.id }
        viewModelScope.launch { dao.delete(item.note) }
    }

    fun undoDelete() {
        val note = recentlyDeleted ?: return
        val tagIds = recentlyDeletedTagIds
        recentlyDeleted = null
        recentlyDeletedTagIds = emptyList()
        viewModelScope.launch {
            dao.insert(note)
            // The cascade dropped the links along with the note; put them back.
            tagDao.setNoteTags(note.id, tagIds)
        }
    }
}
