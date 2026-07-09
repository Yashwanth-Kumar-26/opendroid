package com.opendroid.ai.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data models for models.dev API integration
 * Represents the comprehensive database of AI model specifications, pricing, and capabilities
 */

@Serializable
data class ModelsDevCatalog(
    @SerialName("data")
    val models: List<ModelsDevModel> = emptyList()
)

@Serializable
data class ModelsDevModel(
    @SerialName("id")
    val id: String,
    
    @SerialName("canonical_slug")
    val canonicalSlug: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("created")
    val created: Long,
    
    @SerialName("description")
    val description: String? = null,
    
    @SerialName("context_length")
    val contextLength: Int? = null,
    
    @SerialName("architecture")
    val architecture: ModelArchitecture? = null,
    
    @SerialName("pricing")
    val pricing: ModelPricing? = null,
    
    @SerialName("top_provider")
    val topProvider: TopProviderInfo? = null,
    
    @SerialName("supported_parameters")
    val supportedParameters: List<String>? = null,
    
    @SerialName("default_parameters")
    val defaultParameters: Map<String, String?>? = null,
    
    @SerialName("knowledge_cutoff")
    val knowledgeCutoff: String? = null,
    
    @SerialName("expiration_date")
    val expirationDate: String? = null
) {
    /**
     * Extract provider name from model ID (e.g., "anthropic/claude-3" -> "anthropic")
     */
    fun getProvider(): String = id.split("/").firstOrNull() ?: "unknown"
    
    /**
     * Get estimated total input cost in USD (prompt tokens * price per 1M tokens / 1M)
     */
    fun estimateInputCost(promptTokens: Int): Double {
        val pricePerMillion = pricing?.prompt?.toDoubleOrNull() ?: 0.0
        return (promptTokens * pricePerMillion) / 1_000_000
    }
}

@Serializable
data class ModelArchitecture(
    @SerialName("modality")
    val modality: String? = null, // e.g., "text+image+file->text"
    
    @SerialName("input_modalities")
    val inputModalities: List<String>? = null, // e.g., ["text", "image", "file"]
    
    @SerialName("output_modalities")
    val outputModalities: List<String>? = null, // e.g., ["text"]
    
    @SerialName("tokenizer")
    val tokenizer: String? = null, // e.g., "Claude", "GPT", "Other"
    
    @SerialName("instruct_type")
    val instructType: String? = null
)

@Serializable
data class ModelPricing(
    @SerialName("prompt")
    val prompt: String? = null, // Price per 1M input tokens
    
    @SerialName("completion")
    val completion: String? = null, // Price per 1M output tokens
    
    @SerialName("web_search")
    val webSearch: String? = null,
    
    @SerialName("input_cache_read")
    val inputCacheRead: String? = null,
    
    @SerialName("input_cache_write")
    val inputCacheWrite: String? = null
)

@Serializable
data class TopProviderInfo(
    @SerialName("context_length")
    val contextLength: Int? = null,
    
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int? = null,
    
    @SerialName("is_moderated")
    val isModerated: Boolean? = null
)

/**
 * Lightweight provider info for UI and provider selection
 */
@Serializable
data class ModelsDevProviderInfo(
    val providerId: String,
    val name: String,
    val modelCount: Int = 0,
    val logoUrl: String? = null
)

/**
 * Cache metadata for models.dev sync
 */
@Serializable
data class ModelsDevSyncMetadata(
    val lastSyncTimestamp: Long = 0,
    val syncDurationMs: Long = 0,
    val totalModelsCount: Int = 0,
    val providerCount: Int = 0,
    val errorMessage: String? = null,
    val isSuccessful: Boolean = false
)
