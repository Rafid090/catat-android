package com.notetodo.app.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notetodo.app.R
import com.notetodo.app.data.TagEntity
import com.notetodo.app.data.TaskEntity
import com.notetodo.app.ui.AppViewModelProvider
import com.notetodo.app.ui.components.EmptyState
import com.notetodo.app.ui.components.SearchField
import com.notetodo.app.ui.notes.NO_TAG_FILTER
import com.notetodo.app.ui.components.SwipeableItem
import com.notetodo.app.ui.components.TagFilterRow
import com.notetodo.app.ui.components.TagLabels
import com.notetodo.app.util.formatDueDate
import com.notetodo.app.util.formatReminder
import com.notetodo.app.util.formatTimeOnly
import com.notetodo.app.util.isOverdue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onOpenTask: (Long) -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: TasksViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val tagFilter by viewModel.tagFilter.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.deleted_task)
    val undoLabel = stringResource(R.string.undo)

    var menuOpen by remember { mutableStateOf(false) }

    // Returning from the editor must not hand focus to the search field.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusManager.clearFocus(force = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_tasks)) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Hapus tugas yang selesai") },
                            onClick = {
                                menuOpen = false
                                viewModel.clearCompleted()
                            },
                        )
                    }
                },
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenTask(TaskEditViewModel.NEW_TASK_ID) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_task))
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
                placeholder = stringResource(R.string.search_tasks),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter == TaskFilter.ALL,
                    onClick = { viewModel.onFilterChange(TaskFilter.ALL) },
                    label = { Text(stringResource(R.string.filter_all)) },
                )
                FilterChip(
                    selected = filter == TaskFilter.ACTIVE,
                    onClick = { viewModel.onFilterChange(TaskFilter.ACTIVE) },
                    label = { Text(stringResource(R.string.filter_active)) },
                )
                FilterChip(
                    selected = filter == TaskFilter.DONE,
                    onClick = { viewModel.onFilterChange(TaskFilter.DONE) },
                    label = { Text(stringResource(R.string.filter_done)) },
                )
            }

            TagFilterRow(
                tags = tags,
                selectedTagId = tagFilter,
                onTagClick = viewModel::onTagFilterClick,
            )

            when {
                tasks.isNotEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tasks, key = { it.task.id }) { item ->
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
                            onToggleDone = { viewModel.toggleDone(item.task) },
                            isDone = item.task.isDone,
                        ) {
                            TaskRow(
                                task = item.task,
                                tags = item.tags,
                                onClick = { onOpenTask(item.task.id) },
                                onToggleDone = { viewModel.toggleDone(item.task) },
                            )
                        }
                    }
                }

                // A tag filter hiding everything is not the same as having no tasks.
                query.isBlank() && tagFilter == NO_TAG_FILTER ->
                    EmptyState(stringResource(R.string.empty_tasks))
                else -> EmptyState(stringResource(R.string.no_results))
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    tags: List<TagEntity>,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone() })

            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                TagLabels(tags = tags, modifier = Modifier.padding(top = 2.dp))

                val dueAt = task.dueAt
                val reminderAt = task.reminderAt
                if (dueAt != null || reminderAt != null || task.isHighPriority) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        if (dueAt != null) {
                            val late = !task.isDone && isOverdue(dueAt)
                            Text(
                                text = formatDueDate(dueAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (late) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        if (reminderAt != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = stringResource(R.string.reminder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    // The date is already shown by the due-date label.
                                    text = if (dueAt != null) {
                                        formatTimeOnly(reminderAt)
                                    } else {
                                        formatReminder(reminderAt)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (task.isHighPriority) {
                            Text(
                                text = "!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
