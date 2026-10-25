package com.example.core.data

import com.example.core.security.CryptoManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ExportFormat {
    JSON_ENCRYPTED,
    JSON_PLAIN,
    CSV,
    TXT
}

object BackupManager {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun exportToJson(
        vaultEntries: List<VaultEntry>,
        tasks: List<Task>,
        decryptSecret: (String) -> String,
        encrypted: Boolean = false,
        passphrase: String = ""
    ): String {
        val rootJson = JSONObject()
        rootJson.put("export_date", isoFormat.format(Date()))
        rootJson.put("app_version", "1.0")
        rootJson.put("app_name", "Tori")
        rootJson.put("encrypted", encrypted)

        val vaultArray = JSONArray()
        for (v in vaultEntries) {
            val vObj = JSONObject()
            vObj.put("id", v.id)
            vObj.put("category", v.category)
            vObj.put("title", v.title)
            vObj.put("username", v.username ?: "")
            vObj.put("secret", decryptSecret(v.secret))
            vObj.put("extra_field", v.extraField ?: "")
            vObj.put("notes", v.notes ?: "")
            vObj.put("created_at", v.createdAt)
            vObj.put("updated_at", v.updatedAt)
            vaultArray.put(vObj)
        }
        rootJson.put("vault_entries", vaultArray)

        val taskArray = JSONArray()
        for (t in tasks) {
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

        val rawJson = rootJson.toString(2)
        return if (encrypted && passphrase.isNotBlank()) {
            CryptoManager.encryptWithPassphrase(rawJson, passphrase)
        } else {
            rawJson
        }
    }

    fun exportToCsv(
        vaultEntries: List<VaultEntry>,
        decryptSecret: (String) -> String
    ): String {
        val sb = StringBuilder()
        sb.append("Category,Title,Username,Secret,ExtraField,Notes,CreatedAt,UpdatedAt\n")

        for (v in vaultEntries) {
            val plainSecret = decryptSecret(v.secret)
            sb.append(escapeCsv(v.category)).append(",")
            sb.append(escapeCsv(v.title)).append(",")
            sb.append(escapeCsv(v.username ?: "")).append(",")
            sb.append(escapeCsv(plainSecret)).append(",")
            sb.append(escapeCsv(v.extraField ?: "")).append(",")
            sb.append(escapeCsv(v.notes ?: "")).append(",")
            sb.append(v.createdAt).append(",")
            sb.append(v.updatedAt).append("\n")
        }
        return sb.toString()
    }

    fun exportToTxt(
        vaultEntries: List<VaultEntry>,
        tasks: List<Task>,
        decryptSecret: (String) -> String
    ): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("       TORI PERSONAL VAULT & ASSISTANT   \n")
        sb.append("=========================================\n")
        sb.append("Export Date: ${isoFormat.format(Date())}\n")
        sb.append("Total Vault Items: ${vaultEntries.size}\n")
        sb.append("Total Tasks: ${tasks.size}\n\n")

        val grouped = vaultEntries.groupBy { it.category }
        for ((category, list) in grouped) {
            sb.append("--- [ $category (${list.size}) ] ---\n")
            for (item in list) {
                sb.append("• Title: ${item.title}\n")
                if (!item.username.isNullOrBlank()) {
                    sb.append("  Username/Email: ${item.username}\n")
                }
                sb.append("  Secret/Password: ${decryptSecret(item.secret)}\n")
                if (!item.extraField.isNullOrBlank()) {
                    sb.append("  Extra Info: ${item.extraField}\n")
                }
                if (!item.notes.isNullOrBlank()) {
                    sb.append("  Notes: ${item.notes}\n")
                }
                sb.append("\n")
            }
        }

        if (tasks.isNotEmpty()) {
            sb.append("--- [ TASKS (${tasks.size}) ] ---\n")
            for (t in tasks) {
                val status = if (t.isDone) "[X] COMPLETED" else "[ ] PENDING"
                sb.append("$status: ${t.title}\n")
                if (!t.notes.isNullOrBlank()) {
                    sb.append("  Notes: ${t.notes}\n")
                }
            }
        }
        sb.append("=========================================\n")
        sb.append("End of Tori Backup.\n")
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\"", "\"\"")
        if (str.contains(",") || str.contains("\n") || str.contains("\"")) {
            str = "\"$str\""
        }
        return str
    }

    fun parseImportContent(rawContent: String, passphrase: String = ""): BackupPreview {
        val trimmed = rawContent.trim()

        // 1. Check if Encrypted JSON or Plain JSON
        if (trimmed.startsWith("{") || (passphrase.isNotBlank() && !trimmed.contains(","))) {
            val jsonString = if (trimmed.startsWith("{")) {
                trimmed
            } else {
                CryptoManager.decryptWithPassphrase(trimmed, passphrase)
            }
            return parseJsonString(jsonString)
        }

        // 2. Check if CSV
        if (trimmed.startsWith("Category,Title,Username,Secret") || trimmed.contains(",")) {
            return parseCsvString(trimmed)
        }

        // 3. Fallback: Parse formatted TXT
        return parseTxtString(trimmed)
    }

    private fun parseJsonString(jsonString: String): BackupPreview {
        val root = JSONObject(jsonString)
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
                    dueAt = if (obj.has("due_at")) obj.optLong("due_at") else null,
                    reminderEnabled = obj.optBoolean("reminder_enabled", false),
                    isDone = obj.optBoolean("is_done", false),
                    notes = obj.optString("notes", "")
                )
            )
        }

        return BackupPreview(
            vaultDrafts = vaultDrafts,
            taskDrafts = taskDrafts,
            exportDate = root.optString("export_date", isoFormat.format(Date()))
        )
    }

    private fun parseCsvString(csvString: String): BackupPreview {
        val lines = csvString.lines().filter { it.isNotBlank() }
        val vaultDrafts = mutableListOf<VaultEntryDraft>()

        for (index in lines.indices) {
            val line = lines[index]
            if (index == 0 && line.startsWith("Category,Title", ignoreCase = true)) {
                continue
            }
            val tokens = parseCsvLine(line)
            if (tokens.size >= 4) {
                val category = tokens.getOrNull(0)?.ifBlank { "PASSWORDS" } ?: "PASSWORDS"
                val title = tokens.getOrNull(1)?.ifBlank { "Untitled" } ?: "Untitled"
                val username = tokens.getOrNull(2) ?: ""
                val secret = tokens.getOrNull(3) ?: ""
                val extraField = tokens.getOrNull(4) ?: ""
                val notes = tokens.getOrNull(5) ?: ""

                vaultDrafts.add(
                    VaultEntryDraft(
                        category = category.uppercase(Locale.US),
                        title = title,
                        username = username,
                        secret = secret,
                        extraField = extraField,
                        notes = notes
                    )
                )
            }
        }

        return BackupPreview(
            vaultDrafts = vaultDrafts,
            taskDrafts = emptyList(),
            exportDate = isoFormat.format(Date())
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun parseTxtString(txt: String): BackupPreview {
        val vaultDrafts = mutableListOf<VaultEntryDraft>()
        val lines = txt.lines()
        var currentTitle = ""
        var currentUsername = ""
        var currentSecret = ""
        var currentCategory = "PASSWORDS"
        var currentNotes = ""

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("--- [") && line.contains("]")) {
                val catPart = line.substringAfter("--- [").substringBefore("]").trim()
                val catName = catPart.substringBefore("(").trim()
                currentCategory = when {
                    catName.contains("ACCOUNT", ignoreCase = true) -> "ACCOUNTS"
                    catName.contains("SITE", ignoreCase = true) || catName.contains("API", ignoreCase = true) -> "SITES_APIS"
                    catName.contains("OTHER", ignoreCase = true) -> "OTHERS"
                    else -> "PASSWORDS"
                }
            } else if (line.startsWith("• Title:")) {
                if (currentTitle.isNotBlank() && currentSecret.isNotBlank()) {
                    vaultDrafts.add(
                        VaultEntryDraft(
                            category = currentCategory,
                            title = currentTitle,
                            username = currentUsername,
                            secret = currentSecret,
                            notes = currentNotes
                        )
                    )
                    currentUsername = ""
                    currentSecret = ""
                    currentNotes = ""
                }
                currentTitle = line.substringAfter("• Title:").trim()
            } else if (line.startsWith("Username/Email:")) {
                currentUsername = line.substringAfter("Username/Email:").trim()
            } else if (line.startsWith("Secret/Password:")) {
                currentSecret = line.substringAfter("Secret/Password:").trim()
            } else if (line.startsWith("Notes:")) {
                currentNotes = line.substringAfter("Notes:").trim()
            }
        }
        if (currentTitle.isNotBlank() && currentSecret.isNotBlank()) {
            vaultDrafts.add(
                VaultEntryDraft(
                    category = currentCategory,
                    title = currentTitle,
                    username = currentUsername,
                    secret = currentSecret,
                    notes = currentNotes
                )
            )
        }

        return BackupPreview(
            vaultDrafts = vaultDrafts,
            taskDrafts = emptyList(),
            exportDate = isoFormat.format(Date())
        )
    }
}
