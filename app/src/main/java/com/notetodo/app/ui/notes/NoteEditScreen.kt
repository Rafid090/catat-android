package com.notetodo.app.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notetodo.app.R
import com.notetodo.app.ui.AppViewModelProvider
import com.notetodo.app.ui.components.TagPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    onDone: () -> Unit,
    viewModel: NoteEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val note = viewModel.note
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // The note screen has no explicit save button: leaving the screen persists it.
    // Focus is dropped first, otherwise it lands on the list screen's search field
    // on the way out and pops the keyboard back up.
    val saveAndExit = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        viewModel.save()
        onDone()
    }

    BackHandler(onBack = saveAndExit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isNew) {
                            stringResource(R.string.new_note)
                        } else {
                            stringResource(R.string.nav_notes)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = saveAndExit) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::togglePin) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = stringResource(
                                if (note.isPinned) R.string.unpin else R.string.pin,
                            ),
                            tint = if (note.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (!viewModel.isNew) {
                        IconButton(
                            onClick = {
                                viewModel.delete()
                                onDone()
                            },
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            TextField(
                value = note.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = { Text(stringResource(R.string.title)) },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                colors = transparentTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = note.content,
                onValueChange = viewModel::onContentChange,
                placeholder = { Text(stringResource(R.string.content)) },
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = transparentTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            TagPicker(
                allTags = allTags,
                selectedTagIds = viewModel.selectedTagIds,
                onToggleTag = viewModel::toggleTag,
                onAddTag = viewModel::addTag,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** Borderless fields so the editor reads as a sheet of paper, not a form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)
