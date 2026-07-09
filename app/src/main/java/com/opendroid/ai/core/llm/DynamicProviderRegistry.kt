package com.opendroid.ai.core.llm

import android.util.Log
import com.opendroid.ai.core.llm.providers.modelsdev.ModelsDevManager
import com.opendroid.ai.data.db.entities.ModelsDevProviderEntity
import com.opendroid.ai.data.models.ModelsDevModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamic provider registry that merges hardcoded and models.dev providers
 * Provides unified access to all available LLM providers
 */
@Singleton
class DynamicProviderRegistry @Inject constructor(
    private val llmProviderFactory: LLMProviderFactory,
    private val modelsDevManager: ModelsDevManager
) {
    companion object {
        private const val TAG = "DynamicProviderRegistry"
    }

    // Hardcoded providers (fallback if models.dev unavailable)
    private val hardcodedProviders = listOf(
        "Google Gemini",
        "OpenAI",
        "Anthropic Claude",
        "Groq",
        "Mistral AI",
        "OpenRouter",
        "Together AI",
        "Cohere",
        "DeepSeek",
        "Copilot API",
        "Custom OpenAI Compatible",
        "Ollama"
    )

    /**
     * Get all available providers (hardcoded + dynamic)
     */
    suspend fun getAllProviders(): List<String> {
        return try {
            val dynamicProviders = modelsDevManager.getAllProviders()
                .first()
                .map { provider: ModelsDevProviderEntity -> 
                    provider.providerName
                }
                .filter { it !in hardcodedProviders }
            
            (hardcodedProviders + dynamicProviders).distinct()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load dynamic providers: ${e.message}, using hardcoded only")
            hardcodedProviders
        }
    }

    /**
     * Get provider info with model count
     */
    suspend fun getProvidersWithInfo(): List<ProviderInfoWithModels> {
        return try {
            // Get dynamic providers from models.dev
            val dynamicProviders = modelsDevManager.getAllProviders().first()
            
            // Create list with both hardcoded and dynamic
            val allProviders = mutableListOf<ProviderInfoWithModels>()
            
            // Add hardcoded providers with 0 dynamic models
            hardcodedProviders.forEach { name ->
                allProviders.add(
                    ProviderInfoWithModels(
                        name = name,
                        totalModels = 1, // Represents the standard model from hardcoded list
                        dynamicModels = 0,
                        isHardcoded = true,
                        logoUrl = null
                    )
                )
            }
            
            // Add/merge dynamic providers
            dynamicProviders.forEach { provider ->
                val humanName = provider.providerName
                val existing = allProviders.find { it.name == humanName }
                
                if (existing != null) {
                    // Update existing
                    val index = allProviders.indexOf(existing)
                    allProviders[index] = existing.copy(
                        dynamicModels = provider.modelCount,
                        totalModels = existing.totalModels + provider.modelCount,
                        logoUrl = provider.logoUrl
                    )
                } else {
                    // Add new
                    allProviders.add(
                        ProviderInfoWithModels(
                            name = humanName,
                            totalModels = provider.modelCount,
                            dynamicModels = provider.modelCount,
                            isHardcoded = false,
                            logoUrl = provider.logoUrl
                        )
                    )
                }
            }
            
            allProviders.sortedByDescending { it.totalModels }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get provider info: ${e.message}")
            // Fallback: return hardcoded only
            hardcodedProviders.map { name ->
                ProviderInfoWithModels(
                    name = name,
                    totalModels = 1,
                    dynamicModels = 0,
                    isHardcoded = true,
                    logoUrl = null
                )
            }
        }
    }

    /**
     * Get models available for a provider
     */
    suspend fun getModelsForProvider(providerName: String): List<String> {
        return try {
            val providerId = providerName.lowercase()
            modelsDevManager.getModelsByProvider(providerId)
                .first()
                .map { it.modelId }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get models for $providerName: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check if provider is hardcoded
     */
    fun isHardcodedProvider(name: String): Boolean = name in hardcodedProviders
}

/**
 * Provider info with model counts
 */
data class ProviderInfoWithModels(
    val name: String,
    val totalModels: Int,
    val dynamicModels: Int = 0,
    val isHardcoded: Boolean = false,
    val logoUrl: String? = null
) {
    val hardcodedModels: Int get() = if (isHardcoded) totalModels - dynamicModels else 0
    val hasModelsDevModels: Boolean get() = dynamicModels > 0
}
