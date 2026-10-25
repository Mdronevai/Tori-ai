package com.example.feature.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisDeskScreen(
    userName: String,
    vaultEntries: List<VaultEntry>,
    tasks: List<Task>,
    onNavigateToTab: (Int) -> Unit,
    onQuickAssistantQuery: (String) -> Unit,
    onOpenVoice: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var assistantInput by remember { mutableStateOf("") }
    var showAppDrawer by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // Update clock
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            kotlinx.coroutines.delay(1000)
        }
    }

    // Load installed launchable apps
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
                    LaunchableApp(label = label, packageName = pkg, activityName = name)
                } else null
            }.sortedBy { it.label.lowercase(Locale.ROOT) }
            installedApps = apps
        }
    }

    val passwordCount = remember(vaultEntries) {
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

    Scaffold(
        containerColor = ToriDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Jarvis Futuristic Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ToriSurfaceDark, ToriIndigo.copy(alpha = 0.35f))
                            )
                        )
                        .border(1.dp, ToriCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
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
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = ToriCyan
                                )
                                Text(
                                    text = currentDate.ifBlank { "SYSTEM ONLINE" },
                                    fontSize = 13.sp,
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
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Tori is active. Ready to manage accounts, passwords, and phone commands.",
                            fontSize = 13.sp,
                            color = ToriMutedText
                        )
                    }
                }
            }

            // Universal Search Bar (Vault + Apps + Assistant)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jarvis_search_bar"),
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
                            IconButton(onClick = onOpenVoice) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice", tint = ToriCyan)
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

            // Search Results Section (if active)
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
                                modifier = Modifier.size(24.dp)
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
                                .clickable { onNavigateToTab(2) /* Navigate to Vault */ }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = entry.title,
                                tint = ToriCyan,
                                modifier = Modifier.size(22.dp)
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

            // Quick Category HUD Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TORI MEMORY STATS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ToriMutedText,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(onClick = { onNavigateToTab(2) }) {
                        Text("Open Vault", color = ToriCyan, fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeskStatCard(
                        title = "Passwords",
                        count = passwordCount,
                        icon = Icons.Default.Password,
                        accentColor = ToriCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(2) }
                    )
                    DeskStatCard(
                        title = "Accounts",
                        count = accountsCount,
                        icon = Icons.Default.AccountCircle,
                        accentColor = ToriIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(2) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeskStatCard(
                        title = "Sites & APIs",
                        count = sitesCount,
                        icon = Icons.Default.Language,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(2) }
                    )
                    DeskStatCard(
                        title = "Tasks Pending",
                        count = pendingTasksCount,
                        icon = Icons.Default.CheckCircleOutline,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(3) }
                    )
                }
            }

            // Quick Assistant Command Strip
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ToriCardDark)
                        .border(1.dp, ToriBorderDark, RoundedCornerShape(16.dp))
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
                                text = "Ask Tori (English or বাংলা)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { onNavigateToTab(1) /* Chat Tab */ },
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
                                    .testTag("desk_assistant_input"),
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
                                    .testTag("desk_assistant_send")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = ToriDarkBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // App Drawer Toggle Button (for Home Launcher use)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ToriSurfaceDark)
                        .border(1.dp, ToriBorderDark, RoundedCornerShape(14.dp))
                        .clickable { showAppDrawer = !showAppDrawer }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Apps",
                            tint = ToriCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (showAppDrawer) "Hide Installed Apps" else "App Drawer (${installedApps.size} Installed)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = if (showAppDrawer) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        tint = ToriMutedText
                    )
                }
            }

            // Expanded App Drawer Grid
            if (showAppDrawer && installedApps.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ToriCardDark)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "LAUNCH ANY APP",
                            fontSize = 11.sp,
                            color = ToriMutedText,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 4 column grid of installed apps
                        val chunkedApps = installedApps.chunked(4)
                        for (row in chunkedApps) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
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
                                // Fill remaining spaces in row
                                for (i in 0 until (4 - row.size)) {
                                    Spacer(modifier = Modifier.width(72.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DeskStatCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ToriCardDark)
            .border(1.dp, ToriBorderDark, RoundedCornerShape(16.dp))
            .clickable { onClick() }
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
                    text = count.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ToriMutedText
            )
        }
    }
}
