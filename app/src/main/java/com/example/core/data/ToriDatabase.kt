package com.example.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VaultEntry::class, Task::class, ChatLog::class, ClipboardEntry::class],
    version = 3,
    exportSchema = false
)
abstract class ToriDatabase : RoomDatabase() {
    abstract fun vaultEntryDao(): VaultEntryDao
    abstract fun taskDao(): TaskDao
    abstract fun chatLogDao(): ChatLogDao
    abstract fun clipboardDao(): ClipboardDao


    companion object {
        @Volatile
        private var INSTANCE: ToriDatabase? = null

        fun getInstance(context: Context): ToriDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ToriDatabase::class.java,
                    "tori_vault.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
