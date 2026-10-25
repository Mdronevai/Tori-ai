package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.*
import com.example.core.security.SecurityPreferences
import com.example.feature.assistant.ToriAssistantEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class ScreenTab {
    HOME,
    CHAT,
    CLIPBOARD,
    VAULT,
    TASKS,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ToriDatabase.getInstance(application)
    val repository = ToriRepository(database)
    val securityPrefs = SecurityPreferences(application)
    val assistantEngine = ToriAssistantEngine(repository, securityPrefs)

    // User name
    val userName = MutableStateFlow(securityPrefs.userName)

    // Lock session state
    private val _isLocked = MutableStateFlow(!securityPrefs.hasPin() || true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Navigation tab
    private val _currentTab = MutableStateFlow(ScreenTab.HOME)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Data streams
    val vaultEntries: StateFlow<List<VaultEntry>> = repository.vaultEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatLogs: StateFlow<List<ChatLog>> = repository.chatLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clipboardEntries: StateFlow<List<ClipboardEntry>> = repository.allClipboardEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    // Dialog & Sheet States
    var selectedVaultEntryForDetail = MutableStateFlow<VaultEntry?>(null)
    var addEditVaultEntry = MutableStateFlow<VaultEntry?>(null)
    var isAddingNewVault = MutableStateFlow(false)
    var initialSecretForAdd = MutableStateFlow("")

    var addEditTask = MutableStateFlow<Task?>(null)
    var isAddingNewTask = MutableStateFlow(false)

    var showPasswordGenerator = MutableStateFlow(false)

    var isThinking = MutableStateFlow(false)
    var snackbarMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun updateUserName(name: String) {
        securityPrefs.userName = name
        userName.value = name
    }

    fun unlockVault() {
        _isLocked.value = false
    }

    fun lockVault() {
        _isLocked.value = true
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun showSnackbar(message: String) {
        snackbarMessage.value = message
    }

    fun clearSnackbar() {
        snackbarMessage.value = null
    }

    fun testGeminiApiKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = assistantEngine.testGeminiConnection(key)
            onResult(res.first, res.second)
        }
    }

    fun setCustomGeminiApiKey(key: String) {
        securityPrefs.customGeminiApiKey = key
    }

    fun toggleFloatingSidebar(context: android.content.Context, enabled: Boolean) {
        securityPrefs.isFloatingSidebarEnabled = enabled
        val intent = android.content.Intent(context, com.example.core.overlay.ToriFloatingService::class.java)
        try {
            if (enabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        } catch (_: Exception) {}
    }

    // Vault CRUD
    fun saveVaultEntry(
        id: Int,
        category: String,
        title: String,
        username: String?,
        secret: String,
        extraField: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.saveVaultEntry(
                id = id,
                category = category,
                title = title,
                username = username,
                plainSecret = secret,
                extraField = extraField,
                notes = notes
            )
            isAddingNewVault.value = false
            addEditVaultEntry.value = null
            initialSecretForAdd.value = ""
            showSnackbar("Encrypted & saved \"$title\" to Vault")
        }
    }

    fun deleteVaultEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteVaultEntry(id)
            selectedVaultEntryForDetail.value = null
            showSnackbar("Vault entry deleted")
        }
    }

    fun decryptSecret(encrypted: String): String {
        return repository.decryptSecret(encrypted)
    }

    // Task CRUD
    fun saveTask(
        id: Int,
        title: String,
        dueAt: Long? = null,
        reminderEnabled: Boolean = false,
        isDone: Boolean = false,
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.saveTask(
                id = id,
                title = title,
                dueAt = dueAt,
                reminderEnabled = reminderEnabled,
                isDone = isDone,
                notes = notes
            )
            isAddingNewTask.value = false
            addEditTask.value = null
            showSnackbar("Saved task: $title")
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            repository.toggleTaskDone(task)
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTask(id)
            showSnackbar("Task deleted")
        }
    }

    // Chat Assistant actions
    fun sendChatMessage(text: String) {
        viewModelScope.launch {
            repository.insertChatLog(
                ChatLog(
                    role = "user",
                    message = text
                )
            )

            isThinking.value = true
            val toriResponse = assistantEngine.processUserMessage(text)
            repository.insertChatLog(toriResponse)
            isThinking.value = false
        }
    }

    fun confirmDraftAction(
        log: ChatLog,
        overrideCategory: String? = null,
        overrideTitle: String? = null,
        overrideSecret: String? = null,
        overrideUsername: String? = null
    ) {
        viewModelScope.launch {
            val type = log.pendingActionType
            val dataStr = log.pendingActionData
            if (type != null && dataStr != null) {
                val dataJson = JSONObject(dataStr)
                when (type) {
                    "SAVE_VAULT" -> {
                        val title = overrideTitle?.takeIf { it.isNotBlank() } ?: dataJson.optString("title", "New Entry")
                        val category = overrideCategory?.takeIf { it.isNotBlank() } ?: dataJson.optString("category", VaultCategory.PASSWORDS.name)
                        val username = overrideUsername ?: dataJson.optString("username", "")
                        val secret = overrideSecret?.takeIf { it.isNotBlank() } ?: dataJson.optString("secret", "")
                        val extra = dataJson.optString("extraField", "")
                        val notes = dataJson.optString("notes", "")

                        repository.saveVaultEntry(
                            category = category,
                            title = title,
                            username = username.ifEmpty { null },
                            plainSecret = secret,
                            extraField = extra.ifEmpty { null },
                            notes = notes.ifEmpty { null }
                        )
                        showSnackbar("Confirmed and encrypted \"$title\" in local vault")
                    }
                    "ADD_TASK" -> {
                        val title = overrideTitle?.takeIf { it.isNotBlank() } ?: dataJson.optString("title", "Task")
                        val notes = dataJson.optString("notes", "")
                        val dueAt = if (dataJson.has("dueAt")) dataJson.optLong("dueAt") else null
                        val reminder = dataJson.optBoolean("reminderEnabled", false)

                        repository.saveTask(
                            title = title,
                            dueAt = dueAt,
                            reminderEnabled = reminder,
                            notes = notes.ifEmpty { null }
                        )
                        showSnackbar("Task saved: $title")
                    }
                }

                repository.updateChatLog(log.copy(isConfirmed = true))
            }
        }
    }

    fun rejectDraftAction(log: ChatLog) {
        viewModelScope.launch {
            repository.updateChatLog(log.copy(isConfirmed = false))
            showSnackbar("Draft discarded. Nothing saved.")
        }
    }

    fun draftVaultFromPassword(password: String) {
        initialSecretForAdd.value = password
        isAddingNewVault.value = true
    }

    // Backup & Export / Import
    fun exportBackup(passphrase: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val payload = repository.exportEncryptedBackup(passphrase)
            onResult(payload)
        }
    }

    fun parseBackupPreview(encryptedData: String, passphrase: String, onResult: (Result<BackupPreview>) -> Unit) {
        viewModelScope.launch {
            try {
                val preview = repository.parseEncryptedBackup(encryptedData, passphrase)
                onResult(Result.success(preview))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun importBackup(preview: BackupPreview) {
        viewModelScope.launch {
            repository.importBackupItems(preview)
            showSnackbar("Imported ${preview.vaultDrafts.size} vault entries and ${preview.taskDrafts.size} tasks")
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            securityPrefs.resetPin()
            _isLocked.value = true
            showSnackbar("All data wiped and master keys reset")
        }
    }

    // Clipboard Operations
    fun saveClipboardEntry(content: String, source: String? = "Manual") {
        viewModelScope.launch {
            repository.saveClipboardEntry(content, siteOrAppName = source)
            showSnackbar("Clip saved to permanent vault")
        }
    }

    fun toggleClipPinned(clip: ClipboardEntry) {
        viewModelScope.launch {
            repository.toggleClipPinned(clip)
        }
    }


    fun deleteClipboardEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteClipboardEntry(id)
            showSnackbar("Clip deleted")
        }
    }

    fun clearClipboard() {
        viewModelScope.launch {
            repository.clearClipboard()
            showSnackbar("All clipboard items cleared")
        }
    }

    fun triggerTestNotification(context: android.content.Context) {
        com.example.core.service.ToriBackgroundService.start(context)
        showSnackbar("Started Tori Background Service! Test password saved notification queued.")
    }
}

