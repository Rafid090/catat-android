package com.notetodo.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.notetodo.app.CatatApplication
import com.notetodo.app.ui.notes.NoteEditViewModel
import com.notetodo.app.ui.notes.NotesViewModel
import com.notetodo.app.ui.tasks.TaskEditViewModel
import com.notetodo.app.ui.tasks.TasksViewModel

object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            val container = catatApplication().container
            NotesViewModel(container.noteDao, container.tagDao)
        }
        initializer {
            val container = catatApplication().container
            TasksViewModel(container.taskDao, container.tagDao, container.reminderScheduler)
        }
        initializer {
            val container = catatApplication().container
            NoteEditViewModel(createSavedStateHandle(), container.noteDao, container.tagDao)
        }
        initializer {
            val container = catatApplication().container
            TaskEditViewModel(
                createSavedStateHandle(),
                container.taskDao,
                container.tagDao,
                container.reminderScheduler,
            )
        }
    }
}

private fun CreationExtras.catatApplication(): CatatApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CatatApplication
