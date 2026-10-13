package com.notetodo.app.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notetodo.app.R
import com.notetodo.app.data.NoteEntity
import com.notetodo.app.data.TagEntity
import com.notetodo.app.ui.AppViewModelProvider
import com.notetodo.app.ui.components.EmptyState
import com.notetodo.app.ui.components.SearchField
import com.notetodo.app.ui.components.SwipeableItem
import com.notetodo.app.ui.components.TagFilterRow
import com.notetodo.app.ui.components.TagLabels
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: NotesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val tagFilter by viewModel.tagFilter.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.deleted_note)
    val undoLabel = stringResource(R.string.undo)

    // Returning from the editor must not hand focus to the search field.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusManager.clearFocus(force = true) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_notes)) }) },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenNote(NoteEditViewModel.NEW_NOTE_ID) },
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_note))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            SearchField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.search_notes),
            )

            TagFilterRow(
                tags = tags,
                selectedTagId = tagFilter,
                onTagClick = viewModel::onTagFilterClick,
            )

            when {
                notes.isNotEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(notes, key = { it.note.id }) { item ->
                        SwipeableItem(
                            onDelete = {
                                viewModel.delete(item)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = deletedMessage,
                                        actionLabel = undoLabel,
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            },
                        ) {
                            NoteCard(
                                note = item.note,
                                tags = item.tags,
                                onClick = { onOpenNote(item.note.id) },
                                onTogglePin = { viewModel.togglePin(item.note) },
                            )
                        }
                    }
                }

                // A tag filter hiding everything is not the same as having no notes.
                query.isBlank() && tagFilter == NO_TAG_FILTER ->
                    EmptyState(stringResource(R.string.empty_notes))
                else -> EmptyState(stringResource(R.string.no_results))
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    tags: List<TagEntity>,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "(Tanpa judul)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                TagLabels(tags = tags, modifier = Modifier.padding(top = 4.dp))
            }

            IconButton(onClick = onTogglePin) {
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
        }
    }
}
