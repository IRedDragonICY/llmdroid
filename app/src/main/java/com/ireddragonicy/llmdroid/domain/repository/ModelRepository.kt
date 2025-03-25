package com.ireddragonicy.llmdroid.domain.repository

import com.ireddragonicy.llmdroid.data.model.DownloadProgress
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ModelRepository {
    val selectedModel: StateFlow<LlmModelConfig?> // Make selected model observable

    suspend fun selectModel(model: LlmModelConfig)
    suspend fun getModelPath(model: LlmModelConfig): String
    suspend fun checkModelExists(model: LlmModelConfig): Boolean
    fun downloadModel(model: LlmModelConfig): Flow<Result<DownloadProgress>>
    fun cancelModelDownload()
    suspend fun deleteModelFile(model: LlmModelConfig): Boolean

    // LLM Interaction (asynchronous)
    fun generateResponse(prompt: String, currentMessages: List<com.ireddragonicy.llmdroid.domain.model.ChatMessage>): Flow<Result<Pair<String, Boolean>>> // Pair<partialResult, isDone>
    suspend fun estimateTokensRemaining(prompt: String, currentMessages: List<com.ireddragonicy.llmdroid.domain.model.ChatMessage>): Result<Int>
    suspend fun resetSession(): Result<Unit>
    suspend fun loadOrRefreshSession(): Result<Unit> // Ensure LLM session is ready/reset
    fun cleanup() // Close resources
}