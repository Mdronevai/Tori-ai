package com.example.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class VaultCategory {
    PASSWORDS, ACCOUNTS, SITES_APIS, OTHERS
}

@Entity(
    tableName = "vault_entry",
    indices = [
        Index(value = ["category"]),
        Index(value = ["title"]),
        Index(value = ["category", "title"])
    ]
)
data class VaultEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String = VaultCategory.PASSWORDS.name, // PASSWORDS, ACCOUNTS, SITES_APIS, OTHERS
    val title: String,
    val username: String? = null,
    val secret: String, // encrypted via Android Keystore
    @ColumnInfo(name = "extra_field")
    val extraField: String? = null, // recovery email, docs link, etc.
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "task",
    indices = [
        Index(value = ["due_at"])
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    @ColumnInfo(name = "due_at")
    val dueAt: Long? = null,
    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean = false,
    @ColumnInfo(name = "is_done")
    val isDone: Boolean = false,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_log")
data class ChatLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val role: String, // "user" / "assistant"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    // Metadata for confirmation card flow
    val pendingActionType: String? = null, // "SAVE_VAULT", "ADD_TASK", "QUERY_RESULT", "PASSWORD_GENERATED"
    val pendingActionData: String? = null,
    val isConfirmed: Boolean? = null
)

@Entity(
    tableName = "clipboard_entry",
    indices = [
        Index(value = ["category"]),
        Index(value = ["language"]),
        Index(value = ["length_category"]),
        Index(value = ["created_at"])
    ]
)
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String,
    val category: String = "OTHERS", // PASSWORDS, EMAILS, APIS, SITES, PROMPTS, OTHERS
    val language: String = "English", // Bangla, English, Mixed
    @ColumnInfo(name = "length_category")
    val lengthCategory: String = "SHORT", // SHORT (<20), MEDIUM (20-100), LONG (>100)
    @ColumnInfo(name = "site_or_app_name")
    val siteOrAppName: String? = null,
    @ColumnInfo(name = "site_url")
    val siteUrl: String? = null,
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

