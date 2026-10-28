package com.example.feature.dashboard

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.ChatLog
import com.example.core.data.Task
import com.example.core.data.VaultCategory
import com.example.core.data.VaultEntry
import com.example.ui.ScreenTab
import com.example.ui.components.NebulaOrbComposable
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    vaultEntries: List<VaultEntry>,
    tasks: List<Task>,
    chatLogs: List<ChatLog>,
    isThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onConfirmAction: (ChatLog, updatedCategory: String?, updatedTitle: String?, updatedSecret: String?, updatedUser: String?) -> Unit,
    onRejectAction: (ChatLog) -> Unit,
    onDraftVaultFromPassword: (String) -> Unit,
    onNavigateToTab: (ScreenTab) -> Unit,
    onAddNewEntry: () -> Unit,
    onAddNewTask: () -> Unit = {},
    onSelectVaultItem: (VaultEntry) -> Unit,
    onToggleTaskCompleted: (Task) -> Unit,
    onOpenVoice: () -> Unit = {},
    onOpenCreationSheet: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onShowSnackbar: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var messageInput by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(false) }
    var activeOverlayCard by remember { mutableStateOf<String?>(null) } // "TASKS", "VAULT", or null

    val hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        android.provider.Settings.canDrawOverlays(context)
    } else true

    val overlayLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else true
        if (granted) {
            val intent = Intent(context, com.example.core.overlay.ToriFloatingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    // Scroll to the latest assistant message automatically when new replies arrive
    LaunchedEffect(chatLogs.size, isThinking) {
        if (chatLogs.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.animateScrollToItem(0)
            }
        }
    }

    fun handleUserSubmission(query: String) {
        val lower = query.lowercase().trim()
        // Check for explicit user navigation voice/text commands as requested by user
        if (lower.contains("niye jaw") || lower.contains("niye jao") || lower.contains("page e niye") ||
            lower.contains("page e jao") || lower.startsWith("go to ") || lower.startsWith("open ")
        ) {
            when {
                lower.contains("pass") || lower.contains("vault") || lower.contains("secret") -> {
                    onNavigateToTab(ScreenTab.VAULT)
                    return
                }
                lower.contains("task") || lower.contains("homework") || lower.contains("reminder") -> {
                    onNavigateToTab(ScreenTab.TASKS)
                    return
                }
                lower.contains("clip") || lower.contains("copy") -> {
                    onNavigateToTab(ScreenTab.CLIPBOARD)
                    return
                }
                lower.contains("setting") -> {
                    onNavigateToTab(ScreenTab.SETTINGS)
                    return
                }
                lower.contains("chat") -> {
                    onNavigateToTab(ScreenTab.CHAT)
                    return
                }
            }
        }

        // Otherwise: Everything is handled right here on the All-Rounder Home Page!
        onSendMessage(query)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Frosted Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu Button (Left) -> opens Drawer
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ToriFrostedSurface)
                        .border(1.dp, ToriFrostedBorder, CircleShape)
                        .testTag("dashboard_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Navigation Menu",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center Brand Pill: ✦ Tori AI Assistant
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(ToriFrostedSurface)
                        .border(1.dp, ToriFrostedBorder, RoundedCornerShape(24.dp))
                        .clickable { onOpenVoice() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("dashboard_brand_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(ToriMagenta, ToriElectricPink, ToriNeonViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✦",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Tori",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "All-Rounder Assistant",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Speaker / Mute Toggle Button (Right)
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ToriFrostedSurface)
                        .border(1.dp, ToriFrostedBorder, CircleShape)
                        .testTag("dashboard_sound_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Main Scrollable Content Area (Nebula Orb + Active Assistant Replies & Results)
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) {
                // Nebula Orb & Greeting
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            NebulaOrbComposable(
                                modifier = Modifier.size(220.dp),
                                isActive = true,
                                onClick = onOpenVoice
                            )
                        }

                        Text(
                            text = if (userName.isNotBlank() && userName != "Commander") "Hi, $userName!" else "Hi, I'm Tori!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.65f),
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "How can I help you today?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Thinking Indicator (if AI is analyzing)
                if (isThinking) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ToriFrostedCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ToriElectricPink.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ToriElectricPink
                                )
                                Text(
                                    text = "✦ Tori is analyzing & decrypting...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Recent Assistant Result Cards directly on the Home Screen!
                val recentAssistantLogs = chatLogs.takeLast(4).reversed()
                if (recentAssistantLogs.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TORI INTELLIGENCE FEED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ToriElectricPink,
                                letterSpacing = 1.sp
                            )
                            if (activeOverlayCard != null) {
                                TextButton(
                                    onClick = { activeOverlayCard = null },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Dismiss", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    items(recentAssistantLogs, key = { it.id }) { log ->
                        HomeAssistantResultCard(
                            log = log,
                            onConfirmAction = onConfirmAction,
                            onRejectAction = onRejectAction,
                            onDraftVaultFromPassword = onDraftVaultFromPassword,
                            onNavigateToTab = onNavigateToTab,
                            onShowSnackbar = onShowSnackbar
                        )
                    }
                }

                // Inline Tasks Quick Snapshot (when user taps "Homework / Tasks")
                if (activeOverlayCard == "TASKS") {
                    item {
                        HomeTasksSnapshotCard(
                            tasks = tasks,
                            onToggleCompleted = onToggleTaskCompleted,
                            onAddNewTask = onAddNewTask,
                            onViewAll = { onNavigateToTab(ScreenTab.TASKS) },
                            onClose = { activeOverlayCard = null }
                        )
                    }
                }

                // Inline Vault Quick Snapshot (when user taps "Vault Secrets")
                if (activeOverlayCard == "VAULT") {
                    item {
                        HomeVaultSnapshotCard(
                            vaultEntries = vaultEntries,
                            onSelectItem = onSelectVaultItem,
                            onAddNew = onAddNewEntry,
                            onViewAll = { onNavigateToTab(ScreenTab.VAULT) },
                            onClose = { activeOverlayCard = null },
                            onShowSnackbar = onShowSnackbar
                        )
                    }
                }
            }

            // 3. Two Rows of Frosted Quick Action Chips (Stays on Home!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChipPill(
                        label = "Fast AI",
                        icon = Icons.Default.Bolt,
                        onClick = { handleUserSubmission("Fast analysis: summarize status") },
                        testTag = "chip_fast_ai"
                    )
                    ActionChipPill(
                        label = "Homework / Tasks",
                        icon = Icons.Default.Book,
                        onClick = {
                            activeOverlayCard = if (activeOverlayCard == "TASKS") null else "TASKS"
                        },
                        testTag = "chip_homework_tasks"
                    )
                    ActionChipPill(
                        label = "AI Creation",
                        icon = Icons.Default.AutoAwesome,
                        onClick = onOpenCreationSheet,
                        testTag = "chip_ai_creation"
                    )
                    ActionChipPill(
                        label = "Vault Secrets",
                        icon = Icons.Default.Key,
                        onClick = {
                            activeOverlayCard = if (activeOverlayCard == "VAULT") null else "VAULT"
                        },
                        testTag = "chip_vault_secrets"
                    )
                    ActionChipPill(
                        label = "Screen Scan",
                        icon = Icons.Default.CameraAlt,
                        onClick = {
                            if (!hasOverlayPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                overlayLauncher.launch(intent)
                            } else {
                                val intent = Intent(context, com.example.core.overlay.ToriFloatingService::class.java)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            }
                        },
                        testTag = "chip_screen_scan"
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChipPill(
                        label = "Read Webpage",
                        icon = Icons.Default.Language,
                        onClick = { handleUserSubmission("Scan active page and extract summary") },
                        testTag = "chip_read_webpage"
                    )
                    ActionChipPill(
                        label = "Edit Prompt",
                        icon = Icons.Default.Edit,
                        onClick = onOpenCreationSheet,
                        testTag = "chip_edit_prompt"
                    )
                    ActionChipPill(
                        label = "Translate",
                        icon = Icons.Default.Translate,
                        onClick = { handleUserSubmission("Translate text to Bangla: Hello, how are you?") },
                        testTag = "chip_translate"
                    )
                    ActionChipPill(
                        label = "Find Password",
                        icon = Icons.Default.Search,
                        onClick = { handleUserSubmission("Amar Gmail password ki?") },
                        testTag = "chip_find_password"
                    )
                    ActionChipPill(
                        label = "System Status",
                        icon = Icons.Default.Shield,
                        onClick = { handleUserSubmission("System status") },
                        testTag = "chip_system_status"
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Bottom Floating Input Capsule with Radiant Gradient Mic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(ToriFrostedSurface)
                    .border(1.dp, ToriFrostedBorder, RoundedCornerShape(32.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // + Creation Trigger Button
                    IconButton(
                        onClick = onOpenCreationSheet,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .testTag("dashboard_capsule_plus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tools and Creation Sheet",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Text Input
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_capsule_message_input"),
                        placeholder = {
                            Text(
                                text = "Message Tori...",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageInput.isNotBlank()) {
                                    val query = messageInput
                                    messageInput = ""
                                    handleUserSubmission(query)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = ToriElectricPink
                        )
                    )

                    // Camera / Scanner Button
                    IconButton(
                        onClick = {
                            if (!hasOverlayPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                overlayLauncher.launch(intent)
                            } else {
                                val intent = Intent(context, com.example.core.overlay.ToriFloatingService::class.java)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .testTag("dashboard_capsule_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan Document or Screen",
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Radiant Mic Orb / Voice Trigger
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(ToriMagenta, ToriElectricPink, ToriRubyGlow)
                                )
                            )
                            .clickable { onOpenVoice() }
                            .testTag("dashboard_capsule_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Open Live Voice Mode",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Assistant Card rendered directly on the Home Screen.
 * Displays responses, password reveals, password generators, and draft confirmations.
 */
@Composable
private fun HomeAssistantResultCard(
    log: ChatLog,
    onConfirmAction: (ChatLog, updatedCategory: String?, updatedTitle: String?, updatedSecret: String?, updatedUser: String?) -> Unit,
    onRejectAction: (ChatLog) -> Unit,
    onDraftVaultFromPassword: (String) -> Unit,
    onNavigateToTab: (ScreenTab) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var isSecretRevealed by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ToriFrostedCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (log.role == "user") ToriFrostedBorder else ToriElectricPink.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeBrush = if (log.role == "user") {
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.15f)))
                    } else {
                        Brush.linearGradient(listOf(ToriElectricPink, ToriMagenta))
                    }
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(badgeBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (log.role == "user") "U" else "✦",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (log.role == "user") "You" else "Tori Assistant",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (log.role == "user") Color.White.copy(alpha = 0.6f) else ToriElectricPink
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(log.message))
                        onShowSnackbar("Copied to clipboard")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy message",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = log.message,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Password Query Result: Inline reveal and copy!
            if (log.pendingActionType == "QUERY_RESULT" && !log.pendingActionData.isNullOrEmpty()) {
                val data = remember(log.pendingActionData) {
                    try { JSONObject(log.pendingActionData) } catch (_: Exception) { null }
                }
                data?.let { json ->
                    val title = json.optString("title", "Credential")
                    val username = json.optString("username", "")
                    val secret = json.optString("secret", "")
                    val category = json.optString("category", "PASSWORDS")

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OledPureBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ToriElectricPink.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (username.isNotEmpty()) {
                                        Text(text = "$category • $username", fontSize = 11.sp, color = ToriMutedText)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { isSecretRevealed = !isSecretRevealed },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSecretRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Secret",
                                            tint = ToriElectricPink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(secret))
                                            onShowSnackbar("Password copied securely")
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Secret",
                                            tint = ToriElectricPink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isSecretRevealed) secret else "••••••••••••",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSecretRevealed) ToriElectricPink else Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            TextButton(
                                onClick = { onNavigateToTab(ScreenTab.VAULT) },
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("View in Full Vault →", color = ToriElectricPink, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Generated Password Result: Copy or Draft immediately!
            if (log.pendingActionType == "PASSWORD_GENERATED" && !log.pendingActionData.isNullOrEmpty()) {
                val data = remember(log.pendingActionData) {
                    try { JSONObject(log.pendingActionData) } catch (_: Exception) { null }
                }
                data?.let { json ->
                    val generated = json.optString("generatedPassword", "")
                    val entropy = json.optInt("entropyBits", 96)
                    val label = json.optString("strengthLabel", "Very Strong")

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OledPureBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ToriElectricPink.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = generated,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(generated))
                                        onShowSnackbar("Password copied")
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ToriElectricPink, modifier = Modifier.size(16.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$entropy bits • $label",
                                    fontSize = 11.sp,
                                    color = ToriSuccessGreen
                                )

                                Button(
                                    onClick = { onDraftVaultFromPassword(generated) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ToriElectricPink, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Draft to Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Save Vault Confirmation
            if (log.pendingActionType == "SAVE_VAULT" && log.isConfirmed == null && !log.pendingActionData.isNullOrEmpty()) {
                val data = remember(log.pendingActionData) {
                    try { JSONObject(log.pendingActionData) } catch (_: Exception) { null }
                }
                data?.let { json ->
                    val title = json.optString("title", "Entry")
                    val category = json.optString("category", VaultCategory.PASSWORDS.name)
                    val secret = json.optString("secret", "")
                    val username = json.optString("username", "")

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OledPureBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ToriElectricPink),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "🛡️ Confirm Encrypted Save", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ToriElectricPink)
                            Text(text = "Title: $title ($category)", fontSize = 12.sp, color = Color.White)
                            if (username.isNotEmpty()) Text(text = "User: $username", fontSize = 12.sp, color = ToriMutedText)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onRejectAction(log) }) {
                                    Text("Discard", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { onConfirmAction(log, null, null, null, null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ToriElectricPink, contentColor = Color.White)
                                ) {
                                    Text("Save to Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Task Confirmation
            if (log.pendingActionType == "ADD_TASK" && log.isConfirmed == null && !log.pendingActionData.isNullOrEmpty()) {
                val data = remember(log.pendingActionData) {
                    try { JSONObject(log.pendingActionData) } catch (_: Exception) { null }
                }
                data?.let { json ->
                    val title = json.optString("title", "Task")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OledPureBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ToriNeonViolet),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "✅ Confirm Task Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ToriNeonViolet)
                            Text(text = title, fontSize = 13.sp, color = Color.White)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onRejectAction(log) }) {
                                    Text("Cancel", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { onConfirmAction(log, null, null, null, null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ToriNeonViolet, contentColor = Color.White)
                                ) {
                                    Text("Save Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tasks Snapshot Card shown on Home when user taps "Homework / Tasks".
 */
@Composable
private fun HomeTasksSnapshotCard(
    tasks: List<Task>,
    onToggleCompleted: (Task) -> Unit,
    onAddNewTask: () -> Unit,
    onViewAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ToriFrostedCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, ToriNeonViolet.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Checklist, contentDescription = null, tint = ToriNeonViolet, modifier = Modifier.size(18.dp))
                    Text(text = "Pending Tasks (${tasks.count { !it.isDone }})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }

            val pending = tasks.filter { !it.isDone }.take(4)
            if (pending.isEmpty()) {
                Text(text = "No pending tasks! Everything is complete.", fontSize = 12.sp, color = ToriMutedText)
            } else {
                pending.forEach { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = task.isDone,
                            onCheckedChange = { onToggleCompleted(task) },
                            colors = CheckboxDefaults.colors(checkedColor = ToriNeonViolet, uncheckedColor = ToriFrostedBorder)
                        )
                        Text(
                            text = task.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAddNewTask, contentPadding = PaddingValues(0.dp)) {
                    Text("+ Add Task", color = ToriNeonViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onViewAll, contentPadding = PaddingValues(0.dp)) {
                    Text("Go to Tasks Page →", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Vault Snapshot Card shown on Home when user taps "Vault Secrets".
 */
@Composable
private fun HomeVaultSnapshotCard(
    vaultEntries: List<VaultEntry>,
    onSelectItem: (VaultEntry) -> Unit,
    onAddNew: () -> Unit,
    onViewAll: () -> Unit,
    onClose: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ToriFrostedCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, ToriElectricPink.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = ToriElectricPink, modifier = Modifier.size(18.dp))
                    Text(text = "Encrypted Secrets (${vaultEntries.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }

            val recent = vaultEntries.take(4)
            if (recent.isEmpty()) {
                Text(text = "Vault is empty. Tap '+ Add Entry' to save credentials.", fontSize = 12.sp, color = ToriMutedText)
            } else {
                recent.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectItem(entry) }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text(text = "${entry.category} • ${entry.username ?: "Encrypted"}", fontSize = 11.sp, color = ToriMutedText)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ToriElectricPink, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAddNew, contentPadding = PaddingValues(0.dp)) {
                    Text("+ Add Entry", color = ToriElectricPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onViewAll, contentPadding = PaddingValues(0.dp)) {
                    Text("Go to Vault Page →", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ActionChipPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = ToriFrostedCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, ToriFrostedBorder),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
