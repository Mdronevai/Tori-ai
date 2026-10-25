package com.example.core.data

import com.example.core.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ToriRepository(
    private val database: ToriDatabase
) {
    val vaultEntries: Flow<List<VaultEntry>> = database.vaultEntryDao().getAllVaultEntries()
    val allTasks: Flow<List<Task>> = database.taskDao().getAllTasks()
    val chatLogs: Flow<List<ChatLog>> = database.chatLogDao().getAllLogs()
    val allClipboardEntries: Flow<List<ClipboardEntry>> = database.clipboardDao().getAllClips()

    suspend fun saveClipboardEntry(
        content: String,
        siteOrAppName: String? = null,
        siteUrl: String? = null
    ): Long {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return 0L

        // Detect language
        val hasBangla = trimmed.any { it in '\u0980'..'\u09FF' }
        val hasEnglish = trimmed.any { it in 'a'..'z' || it in 'A'..'Z' }
        val language = when {
            hasBangla && hasEnglish -> "Mixed"
            hasBangla -> "Bangla"
            else -> "English"
        }

        // Detect length category
        val lengthCat = when {
            trimmed.length < 20 -> "SHORT"
            trimmed.length <= 100 -> "MEDIUM"
            else -> "LONG"
        }

        // Detect content category
        val lower = trimmed.lowercase()
        val category = when {
            trimmed.contains("@") && trimmed.contains(".") && !trimmed.contains(" ") -> "EMAILS"
            lower.contains("api") || lower.contains("sk_live") || lower.contains("key_") || lower.contains("token") -> "APIS"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("www.") -> "SITES"
            lower.startsWith("prompt:") || lower.contains("generate ") || lower.contains("system:") -> "PROMPTS"
            trimmed.length in 8..30 && trimmed.any { it.isDigit() } && trimmed.any { !it.isLetterOrDigit() } -> "PASSWORDS"
            else -> "OTHERS"
        }

        val entry = ClipboardEntry(
            content = trimmed,
            category = category,
            language = language,
            lengthCategory = lengthCat,
            siteOrAppName = siteOrAppName,
            siteUrl = siteUrl,
            createdAt = System.currentTimeMillis()
        )
        return database.clipboardDao().insert(entry)
    }

    suspend fun toggleClipPinned(clip: ClipboardEntry) {
        database.clipboardDao().update(clip.copy(isPinned = !clip.isPinned))
    }

    suspend fun deleteClipboardEntry(id: Int) {
        database.clipboardDao().deleteById(id)
    }

    suspend fun clearClipboard() {
        database.clipboardDao().deleteAll()
    }


    suspend fun saveVaultEntry(
        id: Int = 0,
        category: String,
        title: String,
        username: String?,
        plainSecret: String,
        extraField: String? = null,
        notes: String? = null
    ): Long {
        val encryptedSecret = CryptoManager.encrypt(plainSecret)
        val entry = VaultEntry(
            id = id,
            category = category,
            title = title,
            username = username,
            secret = encryptedSecret,
            extraField = extraField,
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )
        return if (id == 0) {
            database.vaultEntryDao().insert(entry)
        } else {
            database.vaultEntryDao().update(entry)
            id.toLong()
        }
    }

    suspend fun getVaultEntryById(id: Int): VaultEntry? {
        return database.vaultEntryDao().getById(id)
    }

    suspend fun findMatchesByTitle(query: String): List<VaultEntry> {
        return database.vaultEntryDao().findMatchesByTitle(query)
    }

    fun decryptSecret(encrypted: String): String {
        return CryptoManager.decrypt(encrypted)
    }

    suspend fun deleteVaultEntry(id: Int) {
        database.vaultEntryDao().deleteById(id)
    }

    // Task Operations
    suspend fun saveTask(
        id: Int = 0,
        title: String,
        dueAt: Long? = null,
        reminderEnabled: Boolean = false,
        isDone: Boolean = false,
        notes: String? = null
    ): Long {
        val task = Task(
            id = id,
            title = title,
            dueAt = dueAt,
            reminderEnabled = reminderEnabled,
            isDone = isDone,
            notes = notes
        )
        return if (id == 0) {
            database.taskDao().insert(task)
        } else {
            database.taskDao().update(task)
            id.toLong()
        }
    }

    suspend fun toggleTaskDone(task: Task) {
        database.taskDao().update(task.copy(isDone = !task.isDone))
    }

    suspend fun deleteTask(id: Int) {
        database.taskDao().deleteById(id)
    }

    // Chat Log Operations
    suspend fun insertChatLog(log: ChatLog): Long {
        return database.chatLogDao().insert(log)
    }

    suspend fun updateChatLog(log: ChatLog) {
        database.chatLogDao().update(log)
    }

    suspend fun clearChat() {
        database.chatLogDao().clearAll()
    }

    // Backup JSON Structure specified in 03-database-schema Section 4:
    // {
    //   "export_date": "2026-10-25T12:00:00Z",
    //   "app_version": "1.0",
    //   "encrypted": true,
    //   "vault_entries": [ ... ],
    //   "tasks": [ ... ]
    // }
    suspend fun exportEncryptedBackup(passphrase: String): String {
        val vaultList = database.vaultEntryDao().getAllSnapshot()
        val taskList = database.taskDao().getAllSnapshot()

        val rootJson = JSONObject()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        rootJson.put("export_date", isoFormat.format(Date()))
        rootJson.put("app_version", "1.0")
        rootJson.put("encrypted", true)

        val vaultArray = JSONArray()
        for (v in vaultList) {
            val vObj = JSONObject()
            vObj.put("category", v.category)
            vObj.put("title", v.title)
            vObj.put("username", v.username ?: "")
            // Decrypted into the in-memory JSON structure so the whole file is encrypted with export passphrase
            vObj.put("secret", decryptSecret(v.secret))
            vObj.put("extra_field", v.extraField ?: "")
            vObj.put("notes", v.notes ?: "")
            vObj.put("created_at", v.createdAt)
            vObj.put("updated_at", v.updatedAt)
            vaultArray.put(vObj)
        }
        rootJson.put("vault_entries", vaultArray)

        val taskArray = JSONArray()
        for (t in taskList) {
            val tObj = JSONObject()
            tObj.put("title", t.title)
            if (t.dueAt != null) tObj.put("due_at", t.dueAt)
            tObj.put("reminder_enabled", t.reminderEnabled)
            tObj.put("is_done", t.isDone)
            tObj.put("notes", t.notes ?: "")
            tObj.put("created_at", t.createdAt)
            taskArray.put(tObj)
        }
        rootJson.put("tasks", taskArray)

        val rawJson = rootJson.toString()
        return CryptoManager.encryptWithPassphrase(rawJson, passphrase)
    }

    suspend fun parseEncryptedBackup(encryptedB64: String, passphrase: String): BackupPreview {
        val rawJsonString = CryptoManager.decryptWithPassphrase(encryptedB64, passphrase)
        val root = JSONObject(rawJsonString)

        val vaultArray = root.optJSONArray("vault_entries") ?: root.optJSONArray("vaultItems") ?: JSONArray()
        val taskArray = root.optJSONArray("tasks") ?: JSONArray()

        val vaultDrafts = mutableListOf<VaultEntryDraft>()
        for (i in 0 until vaultArray.length()) {
            val obj = vaultArray.getJSONObject(i)
            vaultDrafts.add(
                VaultEntryDraft(
                    category = obj.optString("category", "PASSWORDS"),
                    title = obj.optString("title", "Untitled"),
                    username = obj.optString("username", ""),
                    secret = obj.optString("secret", ""),
                    extraField = obj.optString("extra_field", obj.optString("extraField", "")),
                    notes = obj.optString("notes", "")
                )
            )
        }

        val taskDrafts = mutableListOf<TaskDraft>()
        for (i in 0 until taskArray.length()) {
            val obj = taskArray.getJSONObject(i)
            taskDrafts.add(
                TaskDraft(
                    title = obj.optString("title", "Untitled"),
                    dueAt = if (obj.has("due_at")) obj.optLong("due_at") else if (obj.has("dueDate")) obj.optLong("dueDate") else null,
                    reminderEnabled = obj.optBoolean("reminder_enabled", false),
                    isDone = obj.optBoolean("is_done", obj.optBoolean("isCompleted", false)),
                    notes = obj.optString("notes", obj.optString("details", ""))
                )
            )
        }

        return BackupPreview(
            vaultDrafts = vaultDrafts,
            taskDrafts = taskDrafts,
            exportDate = root.optString("export_date", "")
        )
    }

    suspend fun importBackupItems(preview: BackupPreview) {
        for (draft in preview.vaultDrafts) {
            saveVaultEntry(
                category = draft.category,
                title = draft.title,
                username = draft.username.ifEmpty { null },
                plainSecret = draft.secret,
                extraField = draft.extraField.ifEmpty { null },
                notes = draft.notes.ifEmpty { null }
            )
        }
        for (task in preview.taskDrafts) {
            saveTask(
                title = task.title,
                dueAt = task.dueAt,
                reminderEnabled = task.reminderEnabled,
                isDone = task.isDone,
                notes = task.notes.ifEmpty { null }
            )
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = database.vaultEntryDao().getAllSnapshot()
        if (existing.isEmpty()) {
            saveVaultEntry(
                category = VaultCategory.PASSWORDS.name,
                title = "Netflix",
                username = "rony@example.com",
                plainSecret = "Nflx#Secure987!",
                extraField = "https://netflix.com",
                notes = "Family plan account"
            )
            saveVaultEntry(
                category = VaultCategory.ACCOUNTS.name,
                title = "Google Workspace",
                username = "rony@work.io",
                plainSecret = "G-Work#Sec2026",
                extraField = "backup@domain.com",
                notes = "Primary development login"
            )
            saveVaultEntry(
                category = VaultCategory.SITES_APIS.name,
                title = "Stripe Payments",
                username = "API Production",
                plainSecret = "sk_live_51Msz...98214",
                extraField = "https://dashboard.stripe.com",
                notes = "Webhook signing key in environment"
            )
            saveTask(
                title = "Update banking master password",
                dueAt = System.currentTimeMillis() + (4 * 3600000L),
                reminderEnabled = true,
                isDone = false,
                notes = "Ensure high entropy"
            )
            saveTask(
                title = "Review encrypted JSON backup export",
                dueAt = System.currentTimeMillis() + 86400000L,
                reminderEnabled = false,
                isDone = false,
                notes = "Test backup export in Settings"
            )
            insertChatLog(
                ChatLog(
                    role = "assistant",
                    message = "Hello Rony! I am Tori (Vyra), your local-first personal assistant.\n\n" +
                            "My core principle: Nothing auto-saves without your explicit confirmation. Ask me to save credentials, schedule tasks, or look up your passwords."
                )
            )

            // Seed sample clipboard items for immediate multi-filter demonstration
            saveClipboardEntry("আমার বিকাশ নম্বর: 01700000000", siteOrAppName = "bKash App")
            saveClipboardEntry("sk_live_51Msz98214PROD_SECRET_KEY", siteOrAppName = "Stripe Console")
            saveClipboardEntry("dev-team@tori-vault.io", siteOrAppName = "Chrome Dev")
            saveClipboardEntry("https://github.com/aistudio/tori-assistant-android", siteOrAppName = "GitHub Mobile")
            saveClipboardEntry("System prompt: Write a clean Android Jetpack Compose component with material3 styling and edge-to-edge support.", siteOrAppName = "AI Studio Prompt")
        }
    }


    suspend fun clearAllData() {
        database.vaultEntryDao().deleteAll()
        database.taskDao().deleteAll()
        database.chatLogDao().clearAll()
    }
}

data class VaultEntryDraft(
    val category: String,
    val title: String,
    val username: String = "",
    val secret: String,
    val extraField: String = "",
    val notes: String = ""
)

data class TaskDraft(
    val title: String,
    val dueAt: Long? = null,
    val reminderEnabled: Boolean = false,
    val isDone: Boolean = false,
    val notes: String = ""
)

data class BackupPreview(
    val vaultDrafts: List<VaultEntryDraft>,
    val taskDrafts: List<TaskDraft>,
    val exportDate: String
)
