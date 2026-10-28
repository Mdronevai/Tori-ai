package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.data.*
import com.example.core.data.ClipboardEntry
import com.example.core.security.SecurityPreferences
import com.example.feature.assistant.ChatScreen
import com.example.feature.clipboard.ClipboardVaultScreen
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.security.LockScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.tasks.AddEditTaskDialog
import com.example.feature.tasks.TasksScreen
import com.example.feature.tools.CreationToolSheet
import com.example.feature.vault.*
import com.example.feature.voice.LiveVoiceModeScreen
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val intentActionState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentActionState.value = intent?.getStringExtra("EXTRA_ACTION")
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ToriApp(
                    incomingAction = intentActionState.value,
                    onActionHandled = { intentActionState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("EXTRA_ACTION")?.let {
            intentActionState.value = it
        }
        val navTab = intent.getStringExtra("EXTRA_NAV_TAB")
        if (navTab == "VAULT") {
            intentActionState.value = "NAV_VAULT"
        } else if (navTab == "HOME") {
            intentActionState.value = "NAV_HOME"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToriApp(
    viewModel: MainViewModel = viewModel(),
    incomingAction: String? = null,
    onActionHandled: () -> Unit = {}
) {
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val vaultEntries by viewModel.vaultEntries.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val chatLogs by viewModel.chatLogs.collectAsStateWithLifecycle()
    val clipboardEntries by viewModel.clipboardEntries.collectAsStateWithLifecycle()

    val selectedDetailItem by viewModel.selectedVaultEntryForDetail.collectAsStateWithLifecycle()
    val addEditVaultEntry by viewModel.addEditVaultEntry.collectAsStateWithLifecycle()
    val isAddingNewVault by viewModel.isAddingNewVault.collectAsStateWithLifecycle()
    val initialSecretForAdd by viewModel.initialSecretForAdd.collectAsStateWithLifecycle()

    val addEditTask by viewModel.addEditTask.collectAsStateWithLifecycle()
    val isAddingNewTask by viewModel.isAddingNewTask.collectAsStateWithLifecycle()

    val showGenerator by viewModel.showPasswordGenerator.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showLiveVoiceMode by remember { mutableStateOf(false) }
    var showCreationSheet by remember { mutableStateOf(false) }

    // Speech Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.sendChatMessage(spoken)
            }
        }
    }

    LaunchedEffect(incomingAction) {
        when (incomingAction) {
            "LAUNCH_VOICE" -> {
                showLiveVoiceMode = true
                onActionHandled()
            }
            "NAV_VAULT" -> {
                viewModel.selectTab(ScreenTab.VAULT)
                onActionHandled()
            }
            "NAV_HOME" -> {
                viewModel.selectTab(ScreenTab.HOME)
                onActionHandled()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.securityPrefs.isFloatingSidebarEnabled) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context)) {
                viewModel.toggleFloatingSidebar(context, true)
            }
        }
    }

    // Handle back button: return to HOME if on sub-tab
    BackHandler(enabled = !isLocked && currentTab != ScreenTab.HOME) {
        viewModel.selectTab(ScreenTab.HOME)
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (isLocked) {
        LockScreen(
            securityPrefs = viewModel.securityPrefs,
            onUnlocked = { viewModel.unlockVault() }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !showLiveVoiceMode,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = OledPureBlack,
                    drawerContentColor = Color.White,
                    modifier = Modifier.width(300.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    // Drawer Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(ToriMagenta, ToriElectricPink, ToriNeonViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✦", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(
                                text = "Tori Assistant",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Hardware AES-256 Vault",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                    )

                    // Navigation Items
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null, tint = ToriElectricPink) },
                        label = { Text("Home & Orb") },
                        selected = currentTab == ScreenTab.HOME,
                        onClick = {
                            viewModel.selectTab(ScreenTab.HOME)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ToriFrostedCard,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = ToriNeonViolet) },
                        label = { Text("Tori AI Chat") },
                        selected = currentTab == ScreenTab.CHAT,
                        onClick = {
                            viewModel.selectTab(ScreenTab.CHAT)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ToriFrostedCard,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFF10B981)) },
                        label = { Text("Permanent Clipboard") },
                        selected = currentTab == ScreenTab.CLIPBOARD,
                        onClick = {
                            viewModel.selectTab(ScreenTab.CLIPBOARD)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ToriFrostedCard,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = ToriCyan) },
                        label = { Text("Secrets & Passwords Vault") },
                        selected = currentTab == ScreenTab.VAULT,
                        onClick = {
                            viewModel.selectTab(ScreenTab.VAULT)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ToriFrostedCard,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Checklist, contentDescription = null, tint = Color(0xFFF59E0B)) },
                        label = { Text("Tasks & Reminders") },
                        selected = currentTab == ScreenTab.TASKS,
                        onClick = {
                            viewModel.selectTab(ScreenTab.TASKS)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ToriFrostedCard,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        label = { Text("Settings & Security") },
                        selected = currentTab == ScreenTab.SETTINGS,
                        onClick = {
                            viewModel.selectTab(ScreenTab.SETTINGS)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ToriFrostedCard,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Quick Lock Button in Drawer
                    Button(
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.lockVault()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ToriFrostedCard,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .testTag("drawer_lock_vault_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock Vault", fontSize = 13.sp)
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (currentTab != ScreenTab.HOME) {
                        CenterAlignedTopAppBar(
                            navigationIcon = {
                                IconButton(onClick = { viewModel.selectTab(ScreenTab.HOME) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Home",
                                        tint = Color.White
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = when (currentTab) {
                                        ScreenTab.CHAT -> "Tori AI Chat"
                                        ScreenTab.CLIPBOARD -> "Clipboard Vault"
                                        ScreenTab.VAULT -> "Encrypted Secrets"
                                        ScreenTab.TASKS -> "Tasks & Reminders"
                                        ScreenTab.SETTINGS -> "Settings"
                                        else -> "Tori"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.lockVault() },
                                    modifier = Modifier.testTag("quick_lock_vault_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock Vault",
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = OledPureBlack
                            )
                        )
                    }
                },
                bottomBar = {
                    if (currentTab != ScreenTab.HOME) {
                        NavigationBar(
                            containerColor = OledPureBlack
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.HOME,
                                onClick = { viewModel.selectTab(ScreenTab.HOME) },
                                icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                modifier = Modifier.testTag("nav_tab_home")
                            )
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.CHAT,
                                onClick = { viewModel.selectTab(ScreenTab.CHAT) },
                                icon = { Icon(imageVector = Icons.Default.ChatBubble, contentDescription = "Chat") },
                                label = { Text("Chat") },
                                modifier = Modifier.testTag("nav_tab_chat")
                            )
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.CLIPBOARD,
                                onClick = { viewModel.selectTab(ScreenTab.CLIPBOARD) },
                                icon = { Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Clips") },
                                label = { Text("Clips") },
                                modifier = Modifier.testTag("nav_tab_clipboard")
                            )
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.VAULT,
                                onClick = { viewModel.selectTab(ScreenTab.VAULT) },
                                icon = { Icon(imageVector = Icons.Default.VpnKey, contentDescription = "Vault") },
                                label = { Text("Vault") },
                                modifier = Modifier.testTag("nav_tab_vault")
                            )
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.TASKS,
                                onClick = { viewModel.selectTab(ScreenTab.TASKS) },
                                icon = { Icon(imageVector = Icons.Default.Checklist, contentDescription = "Tasks") },
                                label = { Text("Tasks") },
                                modifier = Modifier.testTag("nav_tab_tasks")
                            )
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.SETTINGS,
                                onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                                icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = OledPureBlack
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        ScreenTab.HOME -> {
                            DashboardScreen(
                                userName = userName,
                                vaultEntries = vaultEntries,
                                tasks = tasks,
                                chatLogs = chatLogs,
                                isThinking = isThinking,
                                onSendMessage = { query -> viewModel.sendChatMessage(query) },
                                onConfirmAction = { log, cat, title, secret, user ->
                                    viewModel.confirmDraftAction(log, cat, title, secret, user)
                                },
                                onRejectAction = { log -> viewModel.rejectDraftAction(log) },
                                onDraftVaultFromPassword = { pass -> viewModel.draftVaultFromPassword(pass) },
                                onNavigateToTab = { tab -> viewModel.selectTab(tab) },
                                onAddNewEntry = { viewModel.isAddingNewVault.value = true },
                                onAddNewTask = { viewModel.isAddingNewTask.value = true },
                                onSelectVaultItem = { item -> viewModel.selectedVaultEntryForDetail.value = item },
                                onToggleTaskCompleted = { task -> viewModel.toggleTaskDone(task) },
                                onOpenVoice = { showLiveVoiceMode = true },
                                onOpenCreationSheet = { showCreationSheet = true },
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                onShowSnackbar = { msg -> viewModel.showSnackbar(msg) }
                            )
                        }
                        ScreenTab.CHAT -> {
                            ChatScreen(
                                messages = chatLogs,
                                isThinking = isThinking,
                                onSendMessage = { text -> viewModel.sendChatMessage(text) },
                                onConfirmAction = { log, cat, title, secret, user ->
                                    viewModel.confirmDraftAction(log, cat, title, secret, user)
                                },
                                onRejectAction = { log -> viewModel.rejectDraftAction(log) },
                                onDraftVaultFromPassword = { pass -> viewModel.draftVaultFromPassword(pass) },
                                onActionCopied = { viewModel.showSnackbar("Copied to clipboard") }
                            )
                        }
                        ScreenTab.CLIPBOARD -> {
                            ClipboardVaultScreen(
                                clipboardList = clipboardEntries,
                                onSaveClip = { content -> viewModel.saveClipboardEntry(content) },
                                onTogglePin = { clip -> viewModel.toggleClipPinned(clip) },
                                onDeleteClip = { id -> viewModel.deleteClipboardEntry(id) },
                                onClearAll = { viewModel.clearClipboard() },
                                onTestNotification = { viewModel.triggerTestNotification(context) },
                                onShowSnackbar = { msg -> viewModel.showSnackbar(msg) }
                            )
                        }
                        ScreenTab.VAULT -> {
                            VaultScreen(
                                vaultEntries = vaultEntries,
                                onAddNewItem = { viewModel.isAddingNewVault.value = true },
                                onOpenGenerator = { viewModel.showPasswordGenerator.value = true },
                                onSelectItem = { item -> viewModel.selectedVaultEntryForDetail.value = item },
                                onQuickCopy = { item ->
                                    val decrypted = viewModel.decryptSecret(item.secret)
                                    clipboardManager.setText(AnnotatedString(decrypted))
                                    viewModel.showSnackbar("Secret for \"${item.title}\" copied to clipboard")
                                },
                                onEditItem = { item -> viewModel.addEditVaultEntry.value = item },
                                onDeleteItem = { item -> viewModel.deleteVaultEntry(item.id) }
                            )
                        }
                        ScreenTab.TASKS -> {
                            TasksScreen(
                                tasks = tasks,
                                onAddNewTask = { viewModel.isAddingNewTask.value = true },
                                onToggleCompleted = { task -> viewModel.toggleTaskDone(task) },
                                onEditTask = { task -> viewModel.addEditTask.value = task },
                                onDeleteTask = { task -> viewModel.deleteTask(task.id) }
                            )
                        }
                        ScreenTab.SETTINGS -> {
                            SettingsScreen(
                                securityPrefs = viewModel.securityPrefs,
                                userName = userName,
                                vaultEntries = vaultEntries,
                                tasks = tasks,
                                decryptSecret = { viewModel.decryptSecret(it) },
                                onUpdateUserName = { newName -> viewModel.updateUserName(newName) },
                                onChangePin = {
                                    viewModel.securityPrefs.resetPin()
                                    viewModel.lockVault()
                                },
                                onLockVault = { viewModel.lockVault() },
                                onConfirmImport = { preview ->
                                    viewModel.importBackup(preview)
                                },
                                onResetAllData = {
                                    viewModel.resetAllData()
                                },
                                onShowSnackbar = { msg -> viewModel.showSnackbar(msg) },
                                onToggleFloatingSidebar = { enabled ->
                                    viewModel.toggleFloatingSidebar(context, enabled)
                                },
                                onTestGeminiKey = { key, callback ->
                                    viewModel.testGeminiApiKey(key, callback)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Live Voice Mode Screen Full-Screen Overlay
        AnimatedVisibility(
            visible = showLiveVoiceMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LiveVoiceModeScreen(
                onDismiss = { showLiveVoiceMode = false },
                onMicTrigger = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Tori...")
                        }
                        speechLauncher.launch(intent)
                    } catch (_: Exception) {
                        viewModel.selectTab(ScreenTab.CHAT)
                    }
                }
            )
        }

        // Creation & Tools Modal Bottom Sheet
        if (showCreationSheet) {
            CreationToolSheet(
                onDismiss = { showCreationSheet = false },
                onExecuteAction = { mode, prompt ->
                    showCreationSheet = false
                    viewModel.sendChatMessage("[$mode] $prompt")
                },
                onScanClipboard = {
                    clipboardEntries.firstOrNull()?.content
                }
            )
        }

        // Vault Entry Detail Dialog
        selectedDetailItem?.let { item ->
            val decrypted = remember(item) { viewModel.decryptSecret(item.secret) }
            VaultItemDetailDialog(
                item = item,
                decryptedSecret = decrypted,
                onDismiss = { viewModel.selectedVaultEntryForDetail.value = null },
                onEdit = {
                    viewModel.selectedVaultEntryForDetail.value = null
                    viewModel.addEditVaultEntry.value = item
                },
                onDelete = { viewModel.deleteVaultEntry(item.id) },
                onSecretCopied = { viewModel.showSnackbar("Secret copied to clipboard") }
            )
        }

        // Add / Edit Vault Entry Dialog
        if (isAddingNewVault || addEditVaultEntry != null) {
            val itemToEdit = addEditVaultEntry
            val plainSecret = remember(itemToEdit, initialSecretForAdd) {
                if (itemToEdit != null) viewModel.decryptSecret(itemToEdit.secret)
                else initialSecretForAdd
            }
            AddEditVaultItemDialog(
                initialItem = itemToEdit,
                initialPlainSecret = plainSecret,
                onDismiss = {
                    viewModel.isAddingNewVault.value = false
                    viewModel.addEditVaultEntry.value = null
                    viewModel.initialSecretForAdd.value = ""
                },
                onSave = { id, cat, title, user, secret, extra, notes ->
                    viewModel.saveVaultEntry(id, cat, title, user, secret, extra, notes)
                }
            )
        }

        // Add / Edit Task Dialog
        if (isAddingNewTask || addEditTask != null) {
            AddEditTaskDialog(
                initialTask = addEditTask,
                onDismiss = {
                    viewModel.isAddingNewTask.value = false
                    viewModel.addEditTask.value = null
                },
                onSave = { id, title, due, reminder, done, notes ->
                    viewModel.saveTask(id, title, due, reminder, done, notes)
                }
            )
        }

        // Password Generator Sheet
        if (showGenerator) {
            PasswordGeneratorSheet(
                onDismiss = { viewModel.showPasswordGenerator.value = false },
                onUsePassword = { generated ->
                    viewModel.draftVaultFromPassword(generated)
                }
            )
        }
    }
}
