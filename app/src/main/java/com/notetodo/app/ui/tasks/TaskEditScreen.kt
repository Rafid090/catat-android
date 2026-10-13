package com.notetodo.app.ui.tasks

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notetodo.app.R
import com.notetodo.app.ui.AppViewModelProvider
import com.notetodo.app.ui.components.TagPicker
import com.notetodo.app.util.formatDueDate
import com.notetodo.app.util.formatReminder
import com.notetodo.app.util.localStartOfDayToPickerMillis
import com.notetodo.app.util.pickerMillisToLocalStartOfDay
import com.notetodo.app.util.toEpochMillis
import com.notetodo.app.util.toLocalDateTime
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    onDone: () -> Unit,
    viewModel: TaskEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val task = viewModel.task
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isNew) {
                            stringResource(R.string.new_task)
                        } else {
                            stringResource(R.string.nav_tasks)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = task.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = task.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.task_note)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.due_date),
                style = MaterialTheme.typography.titleMedium,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.onDueDateChange(LocalDate.now().toEpochMillis()) },
                ) {
                    Text(stringResource(R.string.today))
                }
                OutlinedButton(
                    onClick = {
                        viewModel.onDueDateChange(LocalDate.now().plusDays(1).toEpochMillis())
                    },
                ) {
                    Text(stringResource(R.string.tomorrow))
                }
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text("Pilih…")
                }
            }

            val dueAt = task.dueAt
            if (dueAt != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDueDate(dueAt),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TextButton(onClick = { viewModel.onDueDateChange(null) }) {
                        Text(stringResource(R.string.clear_due_date))
                    }
                }
            }

            Text(
                text = stringResource(R.string.reminder),
                style = MaterialTheme.typography.titleMedium,
            )

            val reminderAt = task.reminderAt
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(stringResource(R.string.set_reminder))
                }
                if (reminderAt != null) {
                    Text(
                        text = formatReminder(reminderAt),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = viewModel::clearReminder) {
                        Text(stringResource(R.string.clear_reminder))
                    }
                }
            }

            // Without the exact-alarm permission the reminder can drift by up to an
            // hour, so offer the system screen that grants it rather than only warning.
            if (reminderAt != null && !viewModel.canScheduleExact) {
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.fromParts("package", context.packageName, null),
                                ),
                            )
                        }
                    },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reminder_inexact_hint),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Text(
                text = stringResource(R.string.tags),
                style = MaterialTheme.typography.titleMedium,
            )

            TagPicker(
                allTags = allTags,
                selectedTagIds = viewModel.selectedTagIds,
                onToggleTag = viewModel::toggleTag,
                onAddTag = viewModel::addTag,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.priority_high),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = task.isHighPriority,
                    onCheckedChange = { viewModel.toggleHighPriority() },
                )
            }

            Button(
                onClick = {
                    viewModel.save()
                    onDone()
                },
                enabled = viewModel.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (showTimePicker) {
        val existing = task.reminderAt?.toLocalDateTime()
        val timeState = rememberTimePickerState(
            initialHour = existing?.hour ?: 8,
            initialMinute = existing?.minute ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onReminderTimeChange(timeState.hour, timeState.minute)
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = { TimePicker(state = timeState) },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = task.dueAt?.let(::localStartOfDayToPickerMillis),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDueDateChange(
                            pickerState.selectedDateMillis?.let(::pickerMillisToLocalStartOfDay),
                        )
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
