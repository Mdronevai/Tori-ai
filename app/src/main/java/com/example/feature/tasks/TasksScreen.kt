package com.example.feature.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.data.Task
import com.example.ui.theme.ToriCyan
import com.example.ui.theme.ToriErrorRed
import com.example.ui.theme.ToriSuccessGreen
import com.example.ui.theme.ToriWarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<Task>,
    onAddNewTask: () -> Unit,
    onToggleCompleted: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to delete \"${taskToDelete?.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDel = taskToDelete
                        taskToDelete = null
                        if (toDel != null) onDeleteTask(toDel)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Filtered by search
    val searchFiltered = remember(tasks, searchQuery) {
        if (searchQuery.isBlank()) tasks
        else tasks.filter {
            it.title.contains(searchQuery, ignoreCase = true) || (it.notes?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    // Grouping strictly according to Section 2.F / Step 6: Today / Upcoming / Done
    val endOfTodayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    val doneTasks = remember(searchFiltered) { searchFiltered.filter { it.isDone } }
    val todayTasks = remember(searchFiltered, endOfTodayMillis) {
        searchFiltered.filter { !it.isDone && it.dueAt != null && it.dueAt <= endOfTodayMillis }
    }
    val upcomingTasks = remember(searchFiltered, endOfTodayMillis) {
        searchFiltered.filter { !it.isDone && (it.dueAt == null || it.dueAt > endOfTodayMillis) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tasks_search_field"),
                placeholder = { Text("Search tasks...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            if (searchFiltered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No tasks match \"$searchQuery\"" else "All caught up!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + or ask Tori: 'Remind me to do something'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    // Group 1: Today
                    if (todayTasks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Today",
                                count = todayTasks.size,
                                color = ToriWarningAmber
                            )
                        }
                        items(todayTasks, key = { it.id }) { task ->
                            ChecklistTaskItem(
                                task = task,
                                onToggle = { onToggleCompleted(task) },
                                onEdit = { onEditTask(task) },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }

                    // Group 2: Upcoming
                    if (upcomingTasks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Upcoming",
                                count = upcomingTasks.size,
                                color = ToriCyan
                            )
                        }
                        items(upcomingTasks, key = { it.id }) { task ->
                            ChecklistTaskItem(
                                task = task,
                                onToggle = { onToggleCompleted(task) },
                                onEdit = { onEditTask(task) },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }

                    // Group 3: Done
                    if (doneTasks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Done",
                                count = doneTasks.size,
                                color = ToriSuccessGreen
                            )
                        }
                        items(doneTasks, key = { it.id }) { task ->
                            ChecklistTaskItem(
                                task = task,
                                onToggle = { onToggleCompleted(task) },
                                onEdit = { onEditTask(task) },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Badge(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color
        ) {
            Text("$count", modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistTaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onEdit
            )
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (task.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.SemiBold,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )

                if (!task.notes.isNullOrEmpty()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (task.reminderEnabled) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Text("Reminder", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    if (task.dueAt != null) {
                        val isOverdue = !task.isDone && task.dueAt < System.currentTimeMillis()
                        Text(
                            text = (if (isOverdue) "Overdue: " else "Due: ") + dateFormatter.format(Date(task.dueAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) ToriErrorRed else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_task_${task.id}")) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
