package com.opendroid.ai.core.llm.providers.modelsdev

import android.util.Log
import com.opendroid.ai.data.db.dao.ModelsDevCacheDao
import com.opendroid.ai.data.db.dao.ModelsDevProviderDao
import com.opendroid.ai.data.db.dao.ModelsDevSyncDao
import com.opendroid.ai.data.db.entities.ModelsDevCacheEntity
import com.opendroid.ai.data.db.entities.ModelsDevProviderEntity
import com.opendroid.ai.data.db.entities.ModelsDevSyncEntity
import com.opendroid.ai.data.models.ModelsDevCatalog
import com.opendroid.ai.data.models.ModelsDevModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ModelsDevManager"
private const val MODELS_DEV_API = "https://models.dev/api.json"
private const val CACHE_VALIDITY_HOURS = 24L // Cache for 24 hours

/**
 * Manager for fetching and caching models from models.dev
 * Provides reactive access to model metadata and pricing information
 */
@Singleton
class ModelsDevManager @Inject constructor(
    private val cacheDao: ModelsDevCacheDao,
    private val syncDao: ModelsDevSyncDao,
    private val providerDao: ModelsDevProviderDao,
    private val httpClient: OkHttpClient
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    /**
     * Fetch models from models.dev API and update cache
     * Returns true if sync was successful
     */
    suspend fun syncModels(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(
                okhttp3.Request.Builder()
                    .url(MODELS_DEV_API)
                    .build()
            ).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch models.dev: ${response.code}")
                recordSyncFailure("HTTP ${response.code}: ${response.message}")
                return@withContext false
            }
            
            val body = response.body?.string() ?: return@withContext false
            val catalog = json.decodeFromString<ModelsDevCatalog>(body)
            
            // Cache the models
            val entities = catalog.models.map { model ->
                ModelsDevCacheEntity(
                    modelId = model.id,
                    providerId = model.getProvider(),
                    modelName = model.name,
                    description = model.description,
                    contextLength = model.contextLength,
                    modalities = model.architecture?.inputModalities?.joinToString(","),
                    pricingJson = if (model.pricing != null) json.encodeToString(model.pricing) else null,
                    createdAt = model.created
                )
            }
            
            // Clear old cache and insert new models
            cacheDao.deleteAllModels()
            cacheDao.insertModels(entities)
            
            // Update provider info
            updateProviderInfo(catalog.models)
            
            // Record successful sync
            val duration = System.currentTimeMillis() - startTime
            recordSyncSuccess(catalog.models.size, duration)
            
            Log.d(TAG, "Successfully synced ${catalog.models.size} models in $duration ms")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing models.dev", e)
            recordSyncFailure(e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * Update provider information from models
     */
    private suspend fun updateProviderInfo(models: List<ModelsDevModel>) {
        val providerGroups = models.groupBy { it.getProvider() }
        val providers = providerGroups.map { (providerId, providerModels) ->
            ModelsDevProviderEntity(
                providerId = providerId,
                providerName = providerId.replaceFirstChar { it.uppercase() },
                modelCount = providerModels.size,
                logoUrl = "https://models.dev/logos/$providerId.svg"
            )
        }
        
        providerDao.deleteAllProviders()
        providerDao.insertProviders(providers)
    }
    
    /**
     * Check if cache is still valid
     */
    suspend fun isCacheValid(): Boolean = withContext(Dispatchers.IO) {
        val lastSync = syncDao.getLatestSyncOnce() ?: return@withContext false
        if (!lastSync.isSuccessful) return@withContext false
        
        val timeSinceLastSync = System.currentTimeMillis() - lastSync.lastSyncTimestamp
        val validityThreshold = TimeUnit.HOURS.toMillis(CACHE_VALIDITY_HOURS)
        return@withContext timeSinceLastSync < validityThreshold
    }
    
    /**
     * Get all cached models
     */
    fun getAllModels(): Flow<List<ModelsDevCacheEntity>> = cacheDao.getAllModels()
    
    /**
     * Get models by provider
     */
    fun getModelsByProvider(providerId: String): Flow<List<ModelsDevCacheEntity>> =
        cacheDao.getModelsByProvider(providerId)
    
    /**
     * Get all cached providers
     */
    fun getAllProviders(): Flow<List<ModelsDevProviderEntity>> = providerDao.getAllProviders()
    
    /**
     * Get model count
     */
    fun getModelCount(): Flow<Int> = cacheDao.getModelCount()
    
    /**
     * Get last sync status
     */
    fun getLastSyncStatus(): Flow<ModelsDevSyncEntity?> = syncDao.getLatestSync()
    
    /**
     * Record successful sync
     */
    private suspend fun recordSyncSuccess(modelCount: Int, durationMs: Long) {
        val syncEntity = ModelsDevSyncEntity(
            syncId = "latest",
            lastSyncTimestamp = System.currentTimeMillis(),
            syncDurationMs = durationMs,
            totalModelsCount = modelCount,
            providerCount = cacheDao.getProviderCount().value,
            isSuccessful = true,
            nextRetryTimestamp = 0
        )
        syncDao.insertSync(syncEntity)
    }
    
    /**
     * Record failed sync
     */
    private suspend fun recordSyncFailure(errorMessage: String) {
        val currentSync = syncDao.getLatestSyncOnce()
        val nextRetry = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30) // Retry in 30 minutes
        
        val syncEntity = ModelsDevSyncEntity(
            syncId = "latest",
            lastSyncTimestamp = currentSync?.lastSyncTimestamp ?: System.currentTimeMillis(),
            syncDurationMs = currentSync?.syncDurationMs ?: 0,
            totalModelsCount = currentSync?.totalModelsCount ?: 0,
            providerCount = currentSync?.providerCount ?: 0,
            errorMessage = errorMessage,
            isSuccessful = false,
            nextRetryTimestamp = nextRetry
        )
        syncDao.insertSync(syncEntity)
    }
    
    /**
     * Clear all cached data
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheDao.deleteAllModels()
        providerDao.deleteAllProviders()
        syncDao.deleteAllSync()
        Log.d(TAG, "Cache cleared")
    }
}
