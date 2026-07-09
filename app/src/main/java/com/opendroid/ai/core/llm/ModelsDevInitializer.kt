package com.opendroid.ai.core.llm

import android.content.Context
import android.util.Log
import com.opendroid.ai.core.llm.providers.modelsdev.ModelsDevManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Initializes models.dev integration on app startup
 * Schedules background sync and performs initial cache population if needed
 */
object ModelsDevInitializer {
    private const val TAG = "ModelsDevInitializer"

    /**
     * Initialize models.dev integration
     * Should be called once during app startup (e.g., in Application.onCreate or MainActivity.onCreate)
     */
    fun initialize(context: Context, scope: CoroutineScope, modelsDevManager: ModelsDevManager) {
        Log.d(TAG, "Initializing models.dev integration")
        
        // Schedule background sync worker
        ModelsDevSyncWorker.scheduleSync(context)
        
        // Perform initial sync in background
        scope.launch {
            try {
                Log.d(TAG, "Starting initial models.dev sync")
                val success = modelsDevManager.syncModels()
                if (success) {
                    Log.d(TAG, "Successfully synced models.dev on initialization")
                } else {
                    Log.w(TAG, "Initial sync failed (will retry automatically)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Initial sync failed: ${e.message} (will retry in background)")
            }
        }
    }
}
