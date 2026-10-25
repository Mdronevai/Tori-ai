package com.example.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.data.VaultCategory
import com.example.core.data.VaultEntry
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    vaultEntries: List<VaultEntry>,
    onAddNewItem: () -> Unit,
    onOpenGenerator: () -> Unit,
    onSelectItem: (VaultEntry) -> Unit,
    onQuickCopy: (VaultEntry) -> Unit,
    onEditItem: (VaultEntry) -> Unit = {},
    onDeleteItem: (VaultEntry) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf("ALL") }

    // Categories specified in 03-database-schema and Step 4: [Passwords] [Accounts] [Sites/APIs] [Others]
    val categoryTabs = listOf(
        "ALL" to "All",
        VaultCategory.PASSWORDS.name to "Passwords",
        VaultCategory.ACCOUNTS.name to "Accounts",
        VaultCategory.SITES_APIS.name to "Sites/APIs",
        VaultCategory.OTHERS.name to "Others"
    )

    val filteredItems = remember(vaultEntries, searchQuery, selectedCategoryTab) {
        vaultEntries.filter { item ->
            val matchesCategory = if (selectedCategoryTab == "ALL") true else item.category == selectedCategoryTab
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                        (item.username?.contains(searchQuery, ignoreCase = true) ?: false) ||
                        (item.notes?.contains(searchQuery, ignoreCase = true) ?: false) ||
                        (item.extraField?.contains(searchQuery, ignoreCase = true) ?: false)
            }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = onOpenGenerator,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("open_password_generator_fab")
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = "Password Generator")
                }
                FloatingActionButton(
                    onClick = onAddNewItem,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_vault_item_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Vault Entry")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar on top
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_search_field"),
                placeholder = { Text("Search passwords, accounts, keys...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Category Tabs: [Passwords] [Accounts] [Sites/APIs] [Others]
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(categoryTabs) { (key, label) ->
                    val isSelected = selectedCategoryTab == key
                    val count = if (key == "ALL") vaultEntries.size else vaultEntries.count { it.category == key }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryTab = key },
                        label = {
                            Text("$label ($count)")
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
            }

            // Entries List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No entries match \"$searchQuery\"" else "No Entries in this Category",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to add an entry or ask Tori in Chat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        VaultEntryCard(
                            item = item,
                            onClick = { onSelectItem(item) },
                            onQuickCopy = { onQuickCopy(item) },
                            onEdit = { onEditItem(item) },
                            onDelete = { onDeleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultEntryCard(
    item: VaultEntry,
    onClick: () -> Unit,
    onQuickCopy: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val categoryIcon: ImageVector = when (item.category) {
        VaultCategory.PASSWORDS.name -> Icons.Default.Password
        VaultCategory.ACCOUNTS.name -> Icons.Default.AccountCircle
        VaultCategory.SITES_APIS.name -> Icons.Default.VpnKey
        else -> Icons.Default.Description
    }

    val iconContainerColor = when (item.category) {
        VaultCategory.PASSWORDS.name -> ToriCyan
        VaultCategory.ACCOUNTS.name -> Color(0xFF3B82F6)
        VaultCategory.SITES_APIS.name -> Color(0xFFA855F7)
        else -> Color(0xFFF59E0B)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("vault_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category Icon with glow
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = item.category,
                    tint = iconContainerColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (!item.username.isNullOrEmpty()) {
                    Text(
                        text = item.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                } else if (!item.extraField.isNullOrEmpty()) {
                    Text(
                        text = item.extraField,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Encrypted • Tap to reveal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Quick Copy action
            IconButton(onClick = onQuickCopy, modifier = Modifier.testTag("quick_copy_${item.id}")) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Secret",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
