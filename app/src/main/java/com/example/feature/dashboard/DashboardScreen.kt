package com.example.feature.dashboard

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.Task
import com.example.core.data.VaultCategory
import com.example.core.data.VaultEntry
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class LaunchableAppItem(
    val label: String,
    val packageName: String,
    val activityName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    vaultEntries: List<VaultEntry>,
    tasks: List<Task>,
    onNavigateToChat: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onAddNewEntry: () -> Unit,
    onAddNewTask: () -> Unit = {},
    onSelectVaultItem: (VaultEntry) -> Unit,
    onToggleTaskCompleted: (Task) -> Unit,
    onQuickAssistantQuery: (String) -> Unit = {},
    onOpenVoice: () -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    var searchQuery by remember { mutableStateOf("") }
    var assistantInput by remember { mutableStateOf("") }
    var showAppDrawer by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<LaunchableAppItem>>(emptyList()) }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // Update clock for Jarvis HUD
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormatStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormatStr.format(now)
            kotlinx.coroutines.delay(1000)
        }
    }

    // Load installed launchable apps for Assistant Launcher
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveList = pm.queryIntentActivities(intent, 0)
            val apps = resolveList.mapNotNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm).toString()
                val pkg = resolveInfo.activityInfo.packageName
                val name = resolveInfo.activityInfo.name
                if (pkg != context.packageName) {
                    LaunchableAppItem(label = label, packageName = pkg, activityName = name)
                } else null
            }.sortedBy { it.label.lowercase(Locale.ROOT) }
            installedApps = apps
        }
    }

    val passwordsCount = remember(vaultEntries) {
        vaultEntries.count { it.category == VaultCategory.PASSWORDS.name }
    }
    val accountsCount = remember(vaultEntries) {
        vaultEntries.count { it.category == VaultCategory.ACCOUNTS.name }
    }
    val sitesCount = remember(vaultEntries) {
        vaultEntries.count { it.category == VaultCategory.SITES_APIS.name }
    }
    val pendingTasksCount = remember(tasks) {
        tasks.count { !it.isDone }
    }

    val recentActivityList = remember(vaultEntries) {
        vaultEntries.sortedByDescending { it.updatedAt }.take(5)
    }
    val pendingTaskList = remember(tasks) {
        tasks.filter { !it.isDone }.take(3)
    }

    // Filtered items when searching
    val filteredVault = remember(searchQuery, vaultEntries) {
        if (searchQuery.isBlank()) emptyList()
        else vaultEntries.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    (it.username?.contains(searchQuery, ignoreCase = true) == true)
        }.take(5)
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) emptyList()
        else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }.take(6)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // 1. Unified Jarvis Futuristic HUD Header Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ToriSurfaceDark, ToriIndigo.copy(alpha = 0.35f))
                        )
                    )
                    .border(1.dp, ToriCyan.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentTime.ifBlank { "TORI HUD" },
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = ToriCyan
                            )
                            Text(
                                text = currentDate.ifBlank { "SYSTEM ONLINE" },
                                fontSize = 12.sp,
                                color = ToriMutedText
                            )
                        }

                        // Security badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ToriCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ToriCyan.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Encrypted",
                                    tint = ToriCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AES-256",
                                    color = ToriCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Good day, $userName 👋",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Tori Assistant is active. Passwords, accounts, and tasks are synced.",
                        fontSize = 13.sp,
                        color = ToriMutedText
                    )
                }
            }
        }

        // 2. Universal Search Bar (Vault Credentials + Phone Apps)
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_bar"),
                placeholder = {
                    Text("Search passwords, apps, or tasks...", color = ToriMutedText, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = ToriCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = ToriMutedText)
                        }
                    } else {
                        IconButton(onClick = onOpenVoice, modifier = Modifier.testTag("voice_trigger_button")) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Assistant", tint = ToriCyan)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ToriCardDark,
                    unfocusedContainerColor = ToriCardDark,
                    focusedBorderColor = ToriCyan,
                    unfocusedBorderColor = ToriBorderDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // 3. Search Results Overlay (if active)
        if (searchQuery.isNotBlank()) {
            if (filteredApps.isNotEmpty()) {
                item {
                    Text(
                        text = "INSTALLED APPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ToriCyan,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(filteredApps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ToriCardDark)
                            .clickable {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = app.label,
                            tint = ToriIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open",
                            tint = ToriMutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (filteredVault.isNotEmpty()) {
                item {
                    Text(
                        text = "VAULT MATCHES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ToriCyan,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(filteredVault) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ToriCardDark)
                            .clickable { onSelectVaultItem(entry) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = entry.title,
                            tint = ToriCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${entry.category} • ${entry.username ?: "Secret"}",
                                color = ToriMutedText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick AI Assistant Command Bar & Chips
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ToriCardDark)
                    .border(1.dp, ToriBorderDark, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Tori Assistant",
                            tint = ToriCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ask Tori AI (Bangla or English)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = onNavigateToChat,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Full Chat", color = ToriCyan, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = assistantInput,
                            onValueChange = { assistantInput = it },
                            placeholder = {
                                Text("e.g. Amar Netflix password ki?", color = ToriMutedText, fontSize = 13.sp)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_assistant_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ToriSurfaceDark,
                                unfocusedContainerColor = ToriSurfaceDark,
                                focusedBorderColor = ToriCyan,
                                unfocusedBorderColor = ToriBorderDark,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (assistantInput.isNotBlank()) {
                                    val query = assistantInput
                                    assistantInput = ""
                                    onQuickAssistantQuery(query)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ToriCyan)
                                .testTag("dashboard_assistant_send")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = ToriDarkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Prompt Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            AssistChip(
                                onClick = { onQuickAssistantQuery("Generate strong 16-character password") },
                                label = { Text("🔑 Generate Password", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = ToriCyan)
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { onQuickAssistantQuery("Amar Gmail account and password dekhao") },
                                label = { Text("📧 Find Gmail Account", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { onQuickAssistantQuery("Amar bakash/bank account details ki?") },
                                label = { Text("💳 Bank/Account Search", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { onQuickAssistantQuery("Amar pending tasks r list bolo") },
                                label = { Text("✅ Pending Tasks", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                            )
                        }
                    }
                }
            }
        }

        // 5. Memory Stats Grid (4 counters: Passwords, Accounts, Sites/APIs, Tasks)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VAULT & MEMORY STATS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ToriMutedText,
                    fontFamily = FontFamily.Monospace
                )
                TextButton(onClick = onNavigateToVault) {
                    Text("View All", color = ToriCyan, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Passwords",
                    value = "$passwordsCount",
                    icon = Icons.Default.Password,
                    accentColor = ToriCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToVault
                )
                StatCard(
                    title = "Accounts",
                    value = "$accountsCount",
                    icon = Icons.Default.AccountCircle,
                    accentColor = ToriIndigo,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToVault
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Sites & APIs",
                    value = "$sitesCount",
                    icon = Icons.Default.Language,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToVault
                )
                StatCard(
                    title = "Tasks Pending",
                    value = "$pendingTasksCount",
                    icon = Icons.Default.Checklist,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
            }
        }

        // 6. Action Shortcuts Row (+ Add Entry | Ask Chat | + Add Task | App Drawer)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SHORTCUTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ToriMutedText,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutBtn(
                        label = "+ Entry",
                        icon = Icons.Default.Add,
                        color = ToriCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onAddNewEntry
                    )
                    ShortcutBtn(
                        label = "Ask Tori",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        color = ToriIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToChat
                    )
                    ShortcutBtn(
                        label = "+ Task",
                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = onAddNewTask
                    )
                    ShortcutBtn(
                        label = if (showAppDrawer) "Close Apps" else "Apps",
                        icon = Icons.Default.Apps,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { showAppDrawer = !showAppDrawer }
                    )
                }
            }
        }

        // 7. Expanded App Launcher Grid (if toggled)
        if (showAppDrawer && installedApps.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ToriCardDark)
                        .border(1.dp, ToriBorderDark, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "LAUNCH INSTALLED PHONE APPS",
                        fontSize = 11.sp,
                        color = ToriMutedText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val chunkedApps = installedApps.chunked(4)
                    for (row in chunkedApps) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (app in row) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable {
                                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                            if (intent != null) {
                                                context.startActivity(intent)
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ToriSurfaceDark)
                                            .border(1.dp, ToriBorderDark, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.label.take(1).uppercase(Locale.ROOT),
                                            color = ToriCyan,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = app.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            for (i in 0 until (4 - row.size)) {
                                Spacer(modifier = Modifier.width(72.dp))
                            }
                        }
                    }
                }
            }
        }

        // 8. Priority Pending Tasks Section
        if (pendingTaskList.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PENDING TASKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ToriMutedText,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(onClick = onNavigateToTasks) {
                        Text("View Tasks", color = ToriCyan, fontSize = 12.sp)
                    }
                }
            }

            items(pendingTaskList, key = { it.id }) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleTaskCompleted(task) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ToriCardDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = task.isDone,
                            onCheckedChange = { onToggleTaskCompleted(task) },
                            colors = CheckboxDefaults.colors(checkedColor = ToriCyan, uncheckedColor = ToriMutedText)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            if (!task.notes.isNullOrEmpty()) {
                                Text(
                                    text = task.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ToriMutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 9. Recent Activity List (Last 5 vault items)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT VAULT ACTIVITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ToriMutedText,
                    fontFamily = FontFamily.Monospace
                )
                TextButton(onClick = onNavigateToVault) {
                    Text("Vault", color = ToriCyan, fontSize = 12.sp)
                }
            }
        }

        if (recentActivityList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ToriCardDark)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No recent entries. Tap '+ Entry' or ask Tori in Chat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ToriMutedText
                        )
                    }
                }
            }
        } else {
            items(recentActivityList, key = { it.id }) { item ->
                RecentActivityCard(
                    item = item,
                    dateFormatter = dateFormatter,
                    onClick = { onSelectVaultItem(item) }
                )
            }
        }
    }
}

@Composable
private fun RecentActivityCard(
    item: VaultEntry,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    val categoryLabel = when (item.category) {
        VaultCategory.PASSWORDS.name -> "Password"
        VaultCategory.ACCOUNTS.name -> "Account"
        VaultCategory.SITES_APIS.name -> "Site / API"
        else -> "Entry"
    }

    val icon: ImageVector = when (item.category) {
        VaultCategory.PASSWORDS.name -> Icons.Default.Password
        VaultCategory.ACCOUNTS.name -> Icons.Default.AccountCircle
        VaultCategory.SITES_APIS.name -> Icons.Default.VpnKey
        else -> Icons.Default.Description
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("recent_activity_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ToriCardDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ToriCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ToriCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = if (!item.username.isNullOrEmpty()) "${item.username} • $categoryLabel" else categoryLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = ToriMutedText,
                    maxLines = 1
                )
            }

            Text(
                text = dateFormatter.format(Date(item.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = ToriMutedText
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ToriCardDark)
            .border(1.dp, ToriBorderDark, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ToriMutedText
            )
        }
    }
}

@Composable
private fun ShortcutBtn(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = ToriCardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, ToriBorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
