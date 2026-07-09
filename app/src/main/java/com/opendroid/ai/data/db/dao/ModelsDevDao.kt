package com.opendroid.ai.data.db.dao

import androidx.room.*
import com.opendroid.ai.data.db.entities.ModelsDevCacheEntity
import com.opendroid.ai.data.db.entities.ModelsDevSyncEntity
import com.opendroid.ai.data.db.entities.ModelsDevProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelsDevCacheDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelsDevCacheEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelsDevCacheEntity>)
    
    @Query("SELECT * FROM models_dev_cache WHERE model_id = :modelId")
    suspend fun getModel(modelId: String): ModelsDevCacheEntity?
    
    @Query("SELECT * FROM models_dev_cache WHERE provider_id = :providerId")
    fun getModelsByProvider(providerId: String): Flow<List<ModelsDevCacheEntity>>
    
    @Query("SELECT * FROM models_dev_cache ORDER BY model_name ASC")
    fun getAllModels(): Flow<List<ModelsDevCacheEntity>>
    
    @Query("SELECT DISTINCT provider_id FROM models_dev_cache ORDER BY provider_id ASC")
    fun getAllProviders(): Flow<List<String>>
    
    @Query("SELECT COUNT(*) FROM models_dev_cache")
    fun getModelCount(): Flow<Int>
    
    @Query("SELECT COUNT(DISTINCT provider_id) FROM models_dev_cache")
    fun getProviderCount(): Flow<Int>
    
    @Query("DELETE FROM models_dev_cache WHERE cached_timestamp < :expiryTimestamp")
    suspend fun deleteExpiredModels(expiryTimestamp: Long)
    
    @Query("DELETE FROM models_dev_cache")
    suspend fun deleteAllModels()
}

@Dao
interface ModelsDevSyncDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSync(sync: ModelsDevSyncEntity)
    
    @Query("SELECT * FROM models_dev_sync WHERE sync_id = 'latest'")
    fun getLatestSync(): Flow<ModelsDevSyncEntity?>
    
    @Query("SELECT * FROM models_dev_sync WHERE sync_id = 'latest'")
    suspend fun getLatestSyncOnce(): ModelsDevSyncEntity?
    
    @Query("DELETE FROM models_dev_sync")
    suspend fun deleteAllSync()
}

@Dao
interface ModelsDevProviderDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ModelsDevProviderEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviders(providers: List<ModelsDevProviderEntity>)
    
    @Query("SELECT * FROM models_dev_providers WHERE provider_id = :providerId")
    suspend fun getProvider(providerId: String): ModelsDevProviderEntity?
    
    @Query("SELECT * FROM models_dev_providers ORDER BY provider_name ASC")
    fun getAllProviders(): Flow<List<ModelsDevProviderEntity>>
    
    @Query("DELETE FROM models_dev_providers")
    suspend fun deleteAllProviders()
}
