package com.example.feature.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.data.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskDialog(
    initialTask: Task? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Int,
        title: String,
        dueAt: Long?,
        reminderEnabled: Boolean,
        isDone: Boolean,
        notes: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
    var reminderEnabled by remember { mutableStateOf(initialTask?.reminderEnabled ?: true) }
    var duePreset by remember { mutableStateOf("TODAY") }
    var titleError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (initialTask == null) "New Task & Reminder" else "Edit Task",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Update master banking password") },
                    isError = titleError,
                    supportingText = if (titleError) { { Text("Task title cannot be empty") } } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Due Presets (Today, Tomorrow, +1 Week, None)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Due Date", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("TODAY" to "Today", "TOMORROW" to "Tomorrow", "WEEK" to "+1 Week", "NONE" to "None").forEach { (key, label) ->
                            FilterChip(
                                selected = duePreset == key,
                                onClick = { duePreset = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // Reminder enabled toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Notification Reminder", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text("Notify when task is due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            val calculatedDue = when (duePreset) {
                                "TODAY" -> System.currentTimeMillis() + (4 * 3600000L)
                                "TOMORROW" -> System.currentTimeMillis() + (24 * 3600000L)
                                "WEEK" -> System.currentTimeMillis() + (7 * 86400000L)
                                else -> null
                            }
                            onSave(
                                initialTask?.id ?: 0,
                                title.trim(),
                                calculatedDue,
                                reminderEnabled,
                                initialTask?.isDone ?: false,
                                notes.trim().ifEmpty { null }
                            )
                        },
                        modifier = Modifier.testTag("save_task_button")
                    ) {
                        Text("Save Task")
                    }
                }
            }
        }
    }
}
