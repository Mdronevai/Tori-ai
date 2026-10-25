package com.example.feature.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.ClipboardEntry

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardVaultScreen(
    clipboardList: List<ClipboardEntry>,
    onSaveClip: (String) -> Unit,
    onTogglePin: (ClipboardEntry) -> Unit,
    onDeleteClip: (Int) -> Unit,
    onClearAll: () -> Unit,
    onTestNotification: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("ALL") }
    var selectedLength by remember { mutableStateOf("ALL") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedDateRange by remember { mutableStateOf("ALL") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var manualClipText by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val oneDayMs = 86400000L
    val oneWeekMs = 7 * oneDayMs
    val oneMonthMs = 30 * oneDayMs

    val filteredClips: List<ClipboardEntry> = remember(

        clipboardList, searchQuery, selectedLang, selectedLength, selectedCategory, selectedDateRange
    ) {
        clipboardList.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.content.contains(searchQuery, ignoreCase = true) ||
                    (item.siteOrAppName?.contains(searchQuery, ignoreCase = true) == true)

            val matchesLang = selectedLang == "ALL" || item.language.equals(selectedLang, ignoreCase = true)
            val matchesLength = selectedLength == "ALL" || item.lengthCategory.equals(selectedLength, ignoreCase = true)
            val matchesCat = selectedCategory == "ALL" || item.category.equals(selectedCategory, ignoreCase = true)

            val ageMs = now - item.createdAt
            val matchesDate = when (selectedDateRange) {
                "TODAY" -> ageMs <= oneDayMs
                "WEEK" -> ageMs <= oneWeekMs
                "MONTH_OLDER" -> ageMs >= oneMonthMs
                else -> true
            }

            matchesQuery && matchesLang && matchesLength && matchesCat && matchesDate
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Permanent Clipboard Vault",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "${filteredClips.size} items stored • Auto-categorized",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_clip_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Clip", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onTestNotification,
                    modifier = Modifier.testTag("test_notif_button")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Test Notification", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.testTag("clear_clips_button")
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Clear All Clips", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clipboard_search_input"),
            placeholder = { Text("Search Bangla, English, passwords, APIs...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChipGroup(
                    label = "Lang",
                    options = listOf("ALL", "Bangla", "English", "Mixed"),
                    selected = selectedLang,
                    onSelect = { selectedLang = it }
                )
            }
            item {
                FilterChipGroup(
                    label = "Length",
                    options = listOf("ALL", "SHORT", "MEDIUM", "LONG"),
                    selected = selectedLength,
                    onSelect = { selectedLength = it }
                )
            }
            item {
                FilterChipGroup(
                    label = "Cat",
                    options = listOf("ALL", "PASSWORDS", "EMAILS", "APIS", "SITES", "PROMPTS", "OTHERS"),
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
            }
            item {
                FilterChipGroup(
                    label = "Time",
                    options = listOf("ALL", "TODAY", "WEEK", "MONTH_OLDER"),
                    selected = selectedDateRange,
                    onSelect = { selectedDateRange = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedLang != "ALL") "No clips match your filter." else "No copied items saved yet.\nAny text you copy will be saved automatically!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredClips, key = { it.id }) { item ->
                    ClipboardCardItem(
                        item = item,
                        onCopy = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Tori Clip", text))
                            onShowSnackbar("Copied to clipboard!")
                        },
                        onTogglePin = { onTogglePin(item) },
                        onDelete = { onDeleteClip(item.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Clip") },
            text = {
                OutlinedTextField(
                    value = manualClipText,
                    onValueChange = { manualClipText = it },
                    label = { Text("Clip Text / Note / Password") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualClipText.isNotBlank()) {
                            onSaveClip(manualClipText)
                            manualClipText = ""
                            showAddDialog = false
                            onShowSnackbar("Custom clip saved successfully!")
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Clipboard Items?") },
            text = { Text("This will permanently remove all stored clipboard items from your local database.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearConfirm = false
                        onShowSnackbar("Clipboard database cleared.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterChipGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        options.forEach { option ->
            val isSelected = selected.equals(option, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = { Text(option.replace("_", " "), fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun ClipboardCardItem(
    item: ClipboardEntry,
    onCopy: (String) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {

    var isExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
    val formattedDate = remember(item.createdAt) { dateFormat.format(Date(item.createdAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPinned) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BadgeChip(
                        text = item.category,
                        color = when (item.category) {
                            "PASSWORDS" -> MaterialTheme.colorScheme.errorContainer
                            "EMAILS" -> MaterialTheme.colorScheme.primaryContainer
                            "APIS" -> MaterialTheme.colorScheme.tertiaryContainer
                            "SITES" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)
                        }
                    )
                    BadgeChip(
                        text = item.language,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    BadgeChip(
                        text = item.lengthCategory,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = if (item.category == "PASSWORDS" || item.category == "APIS") FontFamily.Monospace else FontFamily.Default,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) 20 else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (item.siteOrAppName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Source: ${item.siteOrAppName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Tap to collapse" else "Tap to expand (${item.content.length} chars)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Clip",
                            tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { onCopy(item.content) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Copy Clip",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete Clip",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
