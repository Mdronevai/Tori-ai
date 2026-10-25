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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.feature.launcher.JarvisDeskScreen
import com.example.feature.security.LockScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.tasks.AddEditTaskDialog
import com.example.feature.tasks.TasksScreen
import com.example.feature.vault.*
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ToriCyan


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

    // Speech Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.sendChatMessage(spoken)
                viewModel.selectTab(ScreenTab.CHAT)
            }
        }
    }

    LaunchedEffect(incomingAction) {
        when (incomingAction) {
            "LAUNCH_VOICE" -> {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Tori...")
                    }
                    speechLauncher.launch(intent)
                } catch (_: Exception) {
                    viewModel.selectTab(ScreenTab.CHAT)
                }
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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(ToriCyan.copy(alpha = 0.2f))
                                    .border(1.dp, ToriCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = ToriCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "TORI AI",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                )
                                Text(
                                    text = "Personal Assistant & Vault",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.lockVault() },
                            modifier = Modifier.testTag("quick_lock_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
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
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
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
                            onNavigateToChat = { viewModel.selectTab(ScreenTab.CHAT) },
                            onNavigateToVault = { viewModel.selectTab(ScreenTab.VAULT) },
                            onNavigateToTasks = { viewModel.selectTab(ScreenTab.TASKS) },
                            onAddNewEntry = { viewModel.isAddingNewVault.value = true },
                            onAddNewTask = { viewModel.isAddingNewTask.value = true },
                            onSelectVaultItem = { item -> viewModel.selectedVaultEntryForDetail.value = item },
                            onToggleTaskCompleted = { task -> viewModel.toggleTaskDone(task) },
                            onQuickAssistantQuery = { query ->
                                viewModel.sendChatMessage(query)
                                viewModel.selectTab(ScreenTab.CHAT)
                            },
                            onOpenVoice = {
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
