package com.opendroid.ai.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.opendroid.ai.data.db.dao.ConversationDao
import com.opendroid.ai.data.db.dao.MacroDao
import com.opendroid.ai.data.db.dao.MemoryDao
import com.opendroid.ai.data.db.dao.PlanDao
import com.opendroid.ai.data.db.dao.TaskHistoryDao
import com.opendroid.ai.data.db.dao.ModelsDevCacheDao
import com.opendroid.ai.data.db.dao.ModelsDevSyncDao
import com.opendroid.ai.data.db.dao.ModelsDevProviderDao
import com.opendroid.ai.data.db.entities.ConversationEntity
import com.opendroid.ai.data.db.entities.MacroEntity
import com.opendroid.ai.data.db.entities.MemoryEntity
import com.opendroid.ai.data.db.entities.PlanEntity
import com.opendroid.ai.data.db.entities.TaskHistoryEntity
import com.opendroid.ai.data.db.entities.ModelsDevCacheEntity
import com.opendroid.ai.data.db.entities.ModelsDevSyncEntity
import com.opendroid.ai.data.db.entities.ModelsDevProviderEntity

import com.opendroid.ai.data.db.dao.NotificationDao
import com.opendroid.ai.data.db.dao.UnknownActionDao
import com.opendroid.ai.data.db.entities.NotificationEntity
import com.opendroid.ai.data.db.entities.UnknownActionEntity

@Database(
    entities = [
        ConversationEntity::class,
        PlanEntity::class,
        MemoryEntity::class,
        TaskHistoryEntity::class,
        MacroEntity::class,
        UnknownActionEntity::class,
        NotificationEntity::class,
        ModelsDevCacheEntity::class,
        ModelsDevSyncEntity::class,
        ModelsDevProviderEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class OpenDroidDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun planDao(): PlanDao
    abstract fun memoryDao(): MemoryDao
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun macroDao(): MacroDao
    abstract fun unknownActionDao(): UnknownActionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun modelsDevCacheDao(): ModelsDevCacheDao
    abstract fun modelsDevSyncDao(): ModelsDevSyncDao
    abstract fun modelsDevProviderDao(): ModelsDevProviderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE conversations ADD COLUMN contactPickerData TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        title TEXT NOT NULL,
                        text TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        category TEXT NOT NULL DEFAULT 'OTHER',
                        isAutoReplied INTEGER NOT NULL DEFAULT 0,
                        autoReplyText TEXT,
                        contactName TEXT,
                        isRead INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notifications ADD COLUMN senderEmail TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create models.dev cache table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS models_dev_cache (
                        model_id TEXT PRIMARY KEY NOT NULL,
                        provider_id TEXT NOT NULL,
                        model_name TEXT NOT NULL,
                        description TEXT,
                        context_length INTEGER,
                        modalities TEXT,
                        pricing_json TEXT,
                        created_at INTEGER NOT NULL,
                        cached_timestamp INTEGER NOT NULL
                    )
                """)
                
                // Create index for faster provider lookups
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_models_dev_provider_id 
                    ON models_dev_cache(provider_id)
                """)
                
                // Create models.dev sync metadata table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS models_dev_sync (
                        sync_id TEXT PRIMARY KEY NOT NULL,
                        last_sync_timestamp INTEGER NOT NULL,
                        sync_duration_ms INTEGER NOT NULL,
                        total_models_count INTEGER NOT NULL,
                        provider_count INTEGER NOT NULL,
                        error_message TEXT,
                        is_successful INTEGER NOT NULL,
                        next_retry_timestamp INTEGER NOT NULL
                    )
                """)
                
                // Create provider info table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS models_dev_providers (
                        provider_id TEXT PRIMARY KEY NOT NULL,
                        provider_name TEXT NOT NULL,
                        model_count INTEGER NOT NULL,
                        logo_url TEXT,
                        cached_timestamp INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
