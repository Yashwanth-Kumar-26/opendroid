package com.opendroid.ai.core.llm

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.opendroid.ai.core.llm.providers.modelsdev.ModelsDevManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker for periodic models.dev synchronization
 * Syncs every 24 hours when device has network connectivity
 */
@HiltWorker
class ModelsDevSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: androidx.work.WorkerParameters,
    private val modelsDevManager: ModelsDevManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ModelsDevSyncWorker"
        private const val WORK_NAME = "models_dev_sync"

        /**
         * Schedule periodic sync with models.dev
         */
        fun scheduleSync(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<ModelsDevSyncWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffPolicy(
                    BackoffPolicy.EXPONENTIAL,
                    15, // Initial backoff
                    TimeUnit.MINUTES
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Don't reschedule if already scheduled
                syncRequest
            )

            Log.d(TAG, "Scheduled periodic models.dev sync")
        }

        /**
         * Cancel scheduled sync
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled models.dev sync")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting models.dev sync")
            val success = modelsDevManager.syncModels()
            if (success) {
                Log.d(TAG, "Models.dev sync completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "Models.dev sync failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Models.dev sync failed: ${e.message}", e)
            // Retry with exponential backoff on failure
            Result.retry()
        }
    }
}
