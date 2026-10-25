package com.example.core.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultEntryDao {
    @Query("SELECT * FROM vault_entry ORDER BY updated_at DESC")
    fun getAllVaultEntries(): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entry WHERE category = :category ORDER BY updated_at DESC")
    fun getByCategory(category: String): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entry WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' ORDER BY updated_at DESC")
    fun searchByTitle(query: String): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entry WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(username) LIKE '%' || LOWER(:query) || '%' OR LOWER(notes) LIKE '%' || LOWER(:query) || '%' ORDER BY updated_at DESC")
    fun searchAllFuzzy(query: String): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entry WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'")
    suspend fun findMatchesByTitle(query: String): List<VaultEntry>

    @Query("SELECT * FROM vault_entry WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): VaultEntry?

    @Query("SELECT * FROM vault_entry")
    suspend fun getAllSnapshot(): List<VaultEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultEntry): Long

    @Update
    suspend fun update(entry: VaultEntry)

    @Delete
    suspend fun delete(entry: VaultEntry)

    @Query("DELETE FROM vault_entry WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM vault_entry")
    suspend fun deleteAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task ORDER BY is_done ASC, due_at ASC, created_at DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE is_done = 0 AND due_at IS NOT NULL AND due_at <= :timestamp")
    suspend fun getDueTasks(timestamp: Long): List<Task>

    @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Task?

    @Query("SELECT * FROM task")
    suspend fun getAllSnapshot(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM task")
    suspend fun deleteAll()
}

@Dao
interface ChatLogDao {
    @Query("SELECT * FROM chat_log ORDER BY timestamp ASC")
    fun getAllLogs(): Flow<List<ChatLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ChatLog): Long

    @Update
    suspend fun update(log: ChatLog)

    @Query("DELETE FROM chat_log")
    suspend fun clearAll()
}

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_entry ORDER BY is_pinned DESC, created_at DESC")
    fun getAllClips(): Flow<List<ClipboardEntry>>

    @Query("SELECT * FROM clipboard_entry WHERE LOWER(content) LIKE '%' || LOWER(:query) || '%' OR LOWER(category) LIKE '%' || LOWER(:query) || '%' OR LOWER(site_or_app_name) LIKE '%' || LOWER(:query) || '%' ORDER BY is_pinned DESC, created_at DESC")
    fun searchClips(query: String): Flow<List<ClipboardEntry>>

    @Query("SELECT * FROM clipboard_entry WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ClipboardEntry?

    @Query("SELECT * FROM clipboard_entry ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestClip(): ClipboardEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ClipboardEntry): Long

    @Update
    suspend fun update(entry: ClipboardEntry)

    @Delete
    suspend fun delete(entry: ClipboardEntry)

    @Query("DELETE FROM clipboard_entry WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM clipboard_entry")
    suspend fun deleteAll()
}

