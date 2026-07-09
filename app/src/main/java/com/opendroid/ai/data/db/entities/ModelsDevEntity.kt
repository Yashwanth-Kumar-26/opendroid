package com.opendroid.ai.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room entity for caching models.dev model data
 */
@Entity(tableName = "models_dev_cache")
data class ModelsDevCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "model_id")
    val modelId: String,
    
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    
    @ColumnInfo(name = "model_name")
    val modelName: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "context_length")
    val contextLength: Int? = null,
    
    @ColumnInfo(name = "modalities")
    val modalities: String? = null, // JSON serialized: ["text", "image"]
    
    @ColumnInfo(name = "pricing_json")
    val pricingJson: String? = null, // Full pricing object as JSON
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "cached_timestamp")
    val cachedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Room entity for storing models.dev sync metadata and status
 */
@Entity(tableName = "models_dev_sync")
data class ModelsDevSyncEntity(
    @PrimaryKey
    @ColumnInfo(name = "sync_id")
    val syncId: String = "latest", // Always use same ID to keep only one record
    
    @ColumnInfo(name = "last_sync_timestamp")
    val lastSyncTimestamp: Long = 0,
    
    @ColumnInfo(name = "sync_duration_ms")
    val syncDurationMs: Long = 0,
    
    @ColumnInfo(name = "total_models_count")
    val totalModelsCount: Int = 0,
    
    @ColumnInfo(name = "provider_count")
    val providerCount: Int = 0,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    
    @ColumnInfo(name = "is_successful")
    val isSuccessful: Boolean = false,
    
    @ColumnInfo(name = "next_retry_timestamp")
    val nextRetryTimestamp: Long = 0
)

/**
 * Entity for caching provider metadata
 */
@Entity(tableName = "models_dev_providers")
data class ModelsDevProviderEntity(
    @PrimaryKey
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    
    @ColumnInfo(name = "provider_name")
    val providerName: String,
    
    @ColumnInfo(name = "model_count")
    val modelCount: Int = 0,
    
    @ColumnInfo(name = "logo_url")
    val logoUrl: String? = null,
    
    @ColumnInfo(name = "cached_timestamp")
    val cachedTimestamp: Long = System.currentTimeMillis()
)
