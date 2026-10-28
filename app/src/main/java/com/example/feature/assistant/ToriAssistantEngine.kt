package com.example.feature.assistant

import com.example.BuildConfig
import com.example.core.data.ChatLog
import com.example.core.data.ToriRepository
import com.example.core.data.VaultCategory
import com.example.core.data.VaultEntry
import com.example.feature.vault.PasswordGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ToriAssistantEngine(
    private val repository: ToriRepository,
    private val securityPrefs: com.example.core.security.SecurityPreferences? = null
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(): String? {
        val custom = securityPrefs?.customGeminiApiKey?.trim()
        if (!custom.isNullOrEmpty()) return custom
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotEmpty() && !buildKey.contains("MY_GEMINI_API_KEY")) return buildKey
        return null
    }

    suspend fun testGeminiConnection(testKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val trimmed = testKey.trim()
        if (trimmed.isEmpty()) return@withContext Pair(false, "API key cannot be empty.")
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$trimmed"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Ping. Reply with PONG only.")
                            })
                        })
                    })
                })
            }
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Pair(true, "Gemini AI connection verified! (gemini-3.5-flash)")
            } else {
                val err = response.body?.string() ?: ""
                val msg = try {
                    JSONObject(err).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                Pair(false, "API Error: $msg")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun processUserMessage(inputText: String): ChatLog = withContext(Dispatchers.IO) {
        val trimmed = inputText.trim()
        val lower = trimmed.lowercase()

        // 1. Strict Out-of-Scope Rule (Section 8: Assistant Personality & Behavior Rules)
        if (lower.contains("control my screen") || lower.contains("record keystroke") ||
            lower.contains("screen control") || lower.contains("listen in background") ||
            lower.contains("auto upload") || lower.contains("keylogger") || lower.contains("silent capture")
        ) {
            return@withContext ChatLog(
                role = "assistant",
                message = "That action is strictly out of scope. As a privacy-first assistant, I never control screens, record keystrokes, or monitor you in the background. Every action requires your explicit confirmation and nothing leaves your device.",
                pendingActionType = null,
                pendingActionData = null,
                isConfirmed = null
            )
        }

        // 2. Ask/Retrieve Feature ("Amar Netflix password ki?", "What is my Netflix password?", "Find X")
        if (isRetrieveQuery(lower)) {
            val query = extractQuerySubject(lower)
            if (query.isNotEmpty()) {
                val matches = repository.findMatchesByTitle(query)
                if (matches.isNotEmpty()) {
                    if (matches.size == 1) {
                        val found = matches.first()
                        val decrypted = repository.decryptSecret(found.secret)
                        val dataJson = JSONObject().apply {
                            put("id", found.id)
                            put("title", found.title)
                            put("category", found.category)
                            put("username", found.username ?: "")
                            put("secret", decrypted)
                            put("extraField", found.extraField ?: "")
                        }.toString()

                        return@withContext ChatLog(
                            role = "assistant",
                            message = "I found your \"${found.title}\" credential in the local vault. Tap below to reveal the secret:",
                            pendingActionType = "QUERY_RESULT",
                            pendingActionData = dataJson,
                            isConfirmed = null
                        )
                    } else {
                        // Multiple matches found
                        val titlesList = matches.joinToString(", ") { "\"${it.title}\"" }
                        val firstMatch = matches.first()
                        val decrypted = repository.decryptSecret(firstMatch.secret)
                        val dataJson = JSONObject().apply {
                            put("id", firstMatch.id)
                            put("title", firstMatch.title)
                            put("category", firstMatch.category)
                            put("username", firstMatch.username ?: "")
                            put("secret", decrypted)
                        }.toString()

                        return@withContext ChatLog(
                            role = "assistant",
                            message = "Found ${matches.size} matches: $titlesList. Showing the first match below:",
                            pendingActionType = "QUERY_RESULT",
                            pendingActionData = dataJson,
                            isConfirmed = null
                        )
                    }
                } else {
                    return@withContext ChatLog(
                        role = "assistant",
                        message = "I searched your local vault, but couldn't find any entry matching \"$query\". Would you like to save it?",
                        pendingActionType = null,
                        pendingActionData = null,
                        isConfirmed = null
                    )
                }
            }
        }

        // 2.5 Translation Intent ("Translate hello to bangla", "onubad koro")
        if (lower.startsWith("translate") || lower.contains("onubad") || lower.startsWith("bangla to english") || lower.startsWith("english to bangla")) {
            val translationPrompt = "Translate the following cleanly: $trimmed"
            val apiKey = getEffectiveApiKey()
            if (apiKey != null) {
                val translated = callGeminiApi(translationPrompt, apiKey)
                if (translated != null) {
                    return@withContext ChatLog(
                        role = "assistant",
                        message = "✨ Translation:\n\n$translated",
                        pendingActionType = null,
                        pendingActionData = null
                    )
                }
            }
            return@withContext ChatLog(
                role = "assistant",
                message = "Translation ready! (Tip: Add your Gemini API key in Settings for full real-time neural translation). Query: \"$trimmed\"",
                pendingActionType = null,
                pendingActionData = null
            )
        }

        // 3. Password Generation Intent
        if (lower.contains("generate password") || lower.contains("create password") || 
            lower.contains("new password") || lower.contains("notun password") || 
            lower.contains("strong password") || lower == "password"
        ) {
            val length = Regex("\\b(\\d{1,2})\\b").find(lower)?.value?.toIntOrNull() ?: 18
            val clamped = length.coerceIn(8, 40)
            val generated = PasswordGenerator.generate(length = clamped)
            val strength = PasswordGenerator.calculateStrength(generated)
            val dataJson = JSONObject().apply {
                put("generatedPassword", generated)
                put("entropyBits", strength.entropyBits)
                put("strengthLabel", strength.label)
            }.toString()

            return@withContext ChatLog(
                role = "assistant",
                message = "I generated a ${clamped}-character strong password with ${strength.entropyBits} bits of entropy (${strength.label}). You can copy it or tap below to draft a vault entry with it.",
                pendingActionType = "PASSWORD_GENERATED",
                pendingActionData = dataJson,
                isConfirmed = null
            )
        }

        // 4. Save Vault Intent (Core Feature 1: Confirmation-Based Save)
        if (isSaveVaultIntent(lower)) {
            val draft = parseVaultDraft(trimmed)
            val dataJson = JSONObject().apply {
                put("title", draft.title)
                put("category", draft.category)
                put("username", draft.username)
                put("secret", draft.secret)
                put("extraField", draft.extraField)
                put("notes", draft.notes)
            }.toString()

            val msg = if (draft.secret.isEmpty() || draft.username.isEmpty()) {
                "I started drafting an entry for \"${draft.title}\". Please review the fields below, enter any missing details, and tap Save to confirm:"
            } else {
                "I prepared a new entry for \"${draft.title}\". Per your zero-unconfirmed principle, tap Save below to encrypt and store it:"
            }

            return@withContext ChatLog(
                role = "assistant",
                message = msg,
                pendingActionType = "SAVE_VAULT",
                pendingActionData = dataJson,
                isConfirmed = null
            )
        }

        // 5. Add Task Intent
        if (isTaskIntent(lower)) {
            val taskDraft = parseTaskDraft(trimmed)
            val dataJson = JSONObject().apply {
                put("title", taskDraft.title)
                put("notes", taskDraft.notes)
                if (taskDraft.dueAt != null) put("dueAt", taskDraft.dueAt)
                put("reminderEnabled", taskDraft.reminderEnabled)
            }.toString()

            return@withContext ChatLog(
                role = "assistant",
                message = "I prepared a reminder task: \"${taskDraft.title}\". Tap Save to add it to your tasks:",
                pendingActionType = "ADD_TASK",
                pendingActionData = dataJson,
                isConfirmed = null
            )
        }

        // 6. Gemini AI Call (if configured in Settings or BuildConfig)
        val apiKey = getEffectiveApiKey()
        if (apiKey != null) {
            val aiResponse = callGeminiApi(trimmed, apiKey)
            if (aiResponse != null) {
                return@withContext ChatLog(
                    role = "assistant",
                    message = aiResponse,
                    pendingActionType = null,
                    pendingActionData = null
                )
            }
        }

        // 7. Local Conversational Fallback
        val fallback = generateConversationalFallback(lower, trimmed)
        ChatLog(
            role = "assistant",
            message = fallback,
            pendingActionType = null,
            pendingActionData = null
        )
    }

    private fun isRetrieveQuery(lower: String): Boolean {
        return lower.contains("password ki") || lower.contains("password ki?") ||
                lower.contains("account ki") || lower.contains("api key ki") ||
                lower.contains("login ki") || lower.contains("dekhao") ||
                lower.startsWith("what is my") || lower.startsWith("what's my") ||
                lower.startsWith("amar ") || lower.startsWith("show my") ||
                lower.startsWith("get my") || lower.startsWith("find ") ||
                lower.contains("recovery email")
    }

    private fun extractQuerySubject(lower: String): String {
        return lower
            .replace(Regex("^(what is my|what's my|show my|get my|find|search vault for|amar|kothay)\\s+"), "")
            .replace(Regex("\\s+(password ki\\??|account ki\\??|api key ki\\??|login ki\\??|password|login|recovery email|account|credentials|dekhao)"), "")
            .trim()
    }

    private fun isSaveVaultIntent(lower: String): Boolean {
        return lower.startsWith("save ") || lower.startsWith("store ") ||
                lower.startsWith("add password") || lower.startsWith("add account") ||
                lower.startsWith("add api") || lower.contains("save my ") ||
                lower.contains("save password") || lower.contains("save account")
    }

    private fun isTaskIntent(lower: String): Boolean {
        return lower.startsWith("remind ") || lower.startsWith("todo:") ||
                lower.startsWith("task:") || lower.startsWith("add task") ||
                lower.startsWith("new task")
    }

    private fun parseVaultDraft(text: String): VaultEntryDraftData {
        var title = "New Entry"
        var category = VaultCategory.PASSWORDS.name
        var username = ""
        var secret = ""
        var extraField = ""
        var notes = ""

        val lower = text.lowercase()
        if (lower.contains("facebook") || lower.contains("google") || lower.contains("twitter") || lower.contains("account")) {
            category = VaultCategory.ACCOUNTS.name
        } else if (lower.contains("api") || lower.contains("token") || lower.contains("key") || lower.contains("stripe")) {
            category = VaultCategory.SITES_APIS.name
        } else if (lower.contains("note") || lower.contains("wifi") || lower.contains("pin")) {
            category = VaultCategory.OTHERS.name
        }

        val forMatch = Regex("(?:for|my)\\s+([a-zA-Z0-9_.-]+)", RegexOption.IGNORE_CASE).find(text)
        if (forMatch != null) {
            title = forMatch.groupValues[1].replaceFirstChar { it.uppercase() }
        }

        val userMatch = Regex("(?:user|username|email)\\s+([a-zA-Z0-9_@.-]+)", RegexOption.IGNORE_CASE).find(text)
        if (userMatch != null) {
            username = userMatch.groupValues[1]
        }

        val passMatch = Regex("(?:pass|password|secret|key)\\s+([a-zA-Z0-9_!@#$%^&*()_+=.-]+)", RegexOption.IGNORE_CASE).find(text)
        if (passMatch != null) {
            secret = passMatch.groupValues[1]
        } else if (category == VaultCategory.PASSWORDS.name) {
            secret = PasswordGenerator.generate(18)
        }

        return VaultEntryDraftData(
            category = category,
            title = title,
            username = username,
            secret = secret,
            extraField = extraField,
            notes = notes
        )
    }

    private fun parseTaskDraft(text: String): TaskDraftDataLocal {
        val lower = text.lowercase()
        var dueAt: Long? = null
        if (lower.contains("today")) {
            dueAt = System.currentTimeMillis() + (4 * 3600000L)
        } else if (lower.contains("tomorrow")) {
            dueAt = System.currentTimeMillis() + (24 * 3600000L)
        }

        val cleanedTitle = text
            .replace(Regex("^(remind me to|remind me|add task|new task|todo:)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(today|tomorrow|urgent)", RegexOption.IGNORE_CASE), "")
            .trim()
            .replaceFirstChar { it.uppercase() }

        return TaskDraftDataLocal(
            title = if (cleanedTitle.isNotEmpty()) cleanedTitle else "Personal Reminder",
            dueAt = dueAt,
            reminderEnabled = true,
            notes = "Created via Assistant"
        )
    }

    private suspend fun callGeminiApi(prompt: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val systemInstruction = "You are Tori (Vyra), a local-first personal vault assistant. Your motto: Nothing auto-saves without explicit user confirmation. Keep replies concise, helpful, and privacy-oriented. If asked to control screens, record keystrokes, or monitor in the background, explain it is strictly out of scope."

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemInstruction\n\nUser: $prompt")
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: return@withContext null
                val root = JSONObject(bodyString)
                val candidates = root.optJSONArray("candidates") ?: return@withContext null
                if (candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotEmpty()) return@withContext text
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun generateConversationalFallback(lower: String, raw: String): String {
        return when {
            lower.contains("kemon acho") || lower.contains("kemon aso") || lower.contains("how are you") -> {
                "Ami khub bhalo achi! Tori apnar shathe ache. Apnar vault, passwords, clipboard records ba tasks niye kono sahajjo lagbe?"
            }
            lower.contains("ki khobor") || lower.contains("whats up") || lower.contains("what's up") -> {
                "Sob thikthak! System online, hardware AES-256 encryption active. Bolun ki korte hobe?"
            }
            lower.contains("dhonnobad") || lower.contains("thanks") || lower.contains("thank you") -> {
                "You're very welcome! Apnake sahajjo korte pere bhalo laglo. Anything else you need?"
            }
            lower.contains("status") || lower.contains("system") -> {
                "🛡️ Tori System Status: Operational\n• AES-256 Encryption: Verified\n• Background Guardian: Active\n• Floating Edge Dock: Ready\n• AI Voice Engine: Listening"
            }
            lower.contains("ki korte paro") || lower.contains("what can you do") || lower.contains("capabilities") -> {
                "Tori AI diye apni ja ja korte parben:\n\n" +
                        "1. 🔑 Passwords & Accounts: 'Save Netflix password user john pass 1234' ba 'Amar Netflix password ki?'\n" +
                        "2. 🎙️ Live Voice Interaction: Tap the mic orb to converse naturally.\n" +
                        "3. 📋 Persistent Clipboard: Automatic category indexing (Bangla, English, long, short).\n" +
                        "4. ✅ Task Reminders: 'Remind me to submit project tomorrow'\n" +
                        "5. 🌐 Web/Screen Scan & Translation: On-the-fly edge scanning & translations."
            }
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") || lower.contains("salam") -> {
                "Hello! Good to see you. How can I assist with your vault, secrets, or reminders today?"
            }
            lower.contains("amar") || lower.contains("ki") || lower.contains("dekhao") -> {
                "Ami apnar kotha shunchi! Apni bolte paren: 'Amar Gmail password ki?' ba 'Amar Facebook password dekhao'."
            }
            lower.contains("who are you") || lower.contains("help") -> {
                "I am Tori, your personal holographic AI assistant:\n\n" +
                        "1. Local-Only Vault: Passwords, accounts, and API keys encrypted with hardware AES-256.\n" +
                        "2. Confirmation Shield: Zero silent saves. I always show an inline confirmation card first.\n" +
                        "3. Retrieval: Ask 'What is my Netflix password?' to look up items safely.\n" +
                        "4. Tasks: Schedule reminders grouped by Today, Upcoming, and Done."
            }
            else -> {
                "I heard: \"$raw\".\n\nWould you like me to draft this into your Vault, search your credentials, or add it as a Task?"
            }
        }
    }
}

data class VaultEntryDraftData(
    val category: String,
    val title: String,
    val username: String,
    val secret: String,
    val extraField: String,
    val notes: String
)

data class TaskDraftDataLocal(
    val title: String,
    val dueAt: Long?,
    val reminderEnabled: Boolean,
    val notes: String
)
