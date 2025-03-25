package com.ireddragonicy.llmdroid.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import com.ireddragonicy.llmdroid.data.model.DownloadProgress
import com.ireddragonicy.llmdroid.data.remote.downloader.ModelDownloader
import com.ireddragonicy.llmdroid.di.IoDispatcher
import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

private const val MAX_TOKENS = 1024 // Move to constants or config
private const val DECODE_TOKEN_OFFSET = 256

@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloader: ModelDownloader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ModelRepository {

    private val TAG = "ModelRepositoryImpl"
    private val repositoryScope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val _selectedModel = MutableStateFlow<LlmModelConfig?>(null)
    override val selectedModel: StateFlow<LlmModelConfig?> = _selectedModel.asStateFlow()

    // --- LLM Inference State ---
    private var llmInference: LlmInference? = null
    private var llmSession: LlmInferenceSession? = null
    private var currentModelPath: String? = null


    override suspend fun selectModel(model: LlmModelConfig) {
        withContext(ioDispatcher) {
            if (_selectedModel.value != model) {
                Log.d(TAG, "Selecting model: $model")
                cleanupLlmResources() // Clean up old resources before switching
                _selectedModel.value = model
                // Defer loading LLM until actually needed (e.g., in loadOrRefreshSession)
            }
        }
    }

    override suspend fun getModelPath(model: LlmModelConfig): String = withContext(ioDispatcher) {
        val modelFile = File(model.path)
        if (modelFile.exists()) {
            model.path
        } else if (model.url.isNotEmpty()) {
            val fileName = Uri.parse(model.url).lastPathSegment ?: model.path.substringAfterLast('/')
            File(context.filesDir, fileName).absolutePath
        } else {
            model.path // Fallback, might not exist
        }
    }

    override suspend fun checkModelExists(model: LlmModelConfig): Boolean = withContext(ioDispatcher) {
        File(getModelPath(model)).exists()
    }

    override fun downloadModel(model: LlmModelConfig): Flow<Result<DownloadProgress>> {
       return flow {
            val path = getModelPath(model)
            emitAll(modelDownloader.downloadModel(model, path))
       }.flowOn(ioDispatcher)
    }

     override fun cancelModelDownload() {
        modelDownloader.cancelDownload()
    }


    override suspend fun deleteModelFile(model: LlmModelConfig): Boolean = withContext(ioDispatcher) {
        try {
            val file = File(getModelPath(model))
            if (file.exists()) {
                file.delete()
            } else {
                true // Already non-existent
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting model file ${model.path}", e)
            false
        }
    }

    // --- LLM Interaction ---

     override suspend fun loadOrRefreshSession(): Result<Unit> = withContext(ioDispatcher) {
        val currentModel = _selectedModel.value
        if (currentModel == null) {
            return@withContext Result.Error(IllegalStateException("No model selected"))
        }

        val path = getModelPath(currentModel)
        if (!File(path).exists()) {
             return@withContext Result.Error(IOException("Model file not found at $path"))
        }

        // Check if we need to reload
        if (llmInference == null || llmSession == null || currentModelPath != path) {
            cleanupLlmResources() // Clean up previous instance if path/model changed
            currentModelPath = path
            Log.d(TAG, "Loading LLM Inference engine for path: $path")
            try {
                val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(path)
                    .setMaxTokens(MAX_TOKENS) // Make configurable
                    .apply { currentModel.preferredBackend?.let { setPreferredBackend(it) } }
                    .build()
                llmInference = LlmInference.createFromOptions(context, inferenceOptions)
                Log.d(TAG, "Engine loaded. Creating session.")

                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTemperature(currentModel.temperature)
                    .setTopK(currentModel.topK)
                    .setTopP(currentModel.topP)
                    .build()
                llmSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
                Log.d(TAG, "LLM Session created successfully.")
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load LLM engine or session for ${currentModel.name}", e)
                cleanupLlmResources() // Clean up potentially corrupted state
                Result.Error(ModelLoadException("Failed to initialize LLM: ${e.message}", e))
            }
        } else {
             Log.d(TAG, "LLM Session already loaded.")
             Result.Success(Unit) // Already loaded
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun generateResponse(prompt: String, currentMessages: List<ChatMessage>): Flow<Result<Pair<String, Boolean>>> {
        val currentModel = _selectedModel.value
        val session = llmSession

        if (currentModel == null || session == null) {
            return flow { emit(Result.Error(IllegalStateException("LLM session not initialized. Call loadOrRefreshSession first."))) }
        }

        // Simple UiState lookup based on model name - could be more robust
        val uiStateLogic = currentModel.uiState

        // Construct the full context for the model
        // WARNING: This simple join might exceed token limits quickly.
        // A more robust solution would involve summarizing or truncating older messages.
        // Also, the specific formatting (roles, separators) depends heavily on the model.
        // The UiState pattern tried to address this, keep it or refine it.
        val fullPromptContext = buildPromptContext(prompt, currentMessages, uiStateLogic)
        Log.d(TAG, "Formatted Prompt: $fullPromptContext") // Be careful logging potentially large prompts


        // Use callbackFlow to bridge the Listener pattern
       return callbackFlow {
            val progressListener = ProgressListener<String> { partialResult, done ->
                // Need to handle potential thinking markers or other model-specific formatting here
                val processedResult = partialResult ?: "" // Handle null case
                // Example: If using DeepSeekUiState logic, apply it here
                // val formattedChunk = uiStateLogic.processPartialResult(processedResult)

                val sendResult = trySend(Result.Success(Pair(processedResult, done)))
                if (sendResult.isFailure) {
                    Log.w(TAG, "Failed to send partial result to flow consumer: ${sendResult.exceptionOrNull()?.message}")
                }
                if (done) {
                    Log.d(TAG,"LLM generation finished.")
                    close() // Close the flow when done
                }
            }

            try {
                 Log.d(TAG, "Starting async generation...")
                // Add the formatted prompt/query to the session history if needed by the model/library
                 session.addQueryChunk(fullPromptContext) // Or however the library expects context/history

                val future = session.generateResponseAsync(progressListener)

                // Wait for the future to complete or flow to be cancelled
                val result = future.await() // This waits for the final result if needed, but listener handles streaming
                Log.d(TAG,"Async generation future completed (final result ignored as stream is used).")
                // Flow is closed by the 'done' flag in the listener

            } catch (e: Exception) {
                Log.e(TAG, "Error during LLM generation", e)
                trySend(Result.Error(ModelExecutionException("LLM generation failed: ${e.message}", e)))
                close(e) // Close the flow with error
            }

            awaitClose {
                Log.d(TAG, "LLM generation flow closing.")
                // Future cancellation can be tricky with Guava ListenableFuture.
                // If the underlying task supports cancellation, trigger it here.
                // future.cancel(true) might work depending on the implementation.
            }
        }.flowOn(ioDispatcher).catch {
             Log.e(TAG, "Caught exception in LLM generation flow", it)
             emit(Result.Error(ModelExecutionException("LLM flow error: ${it.message}", Exception(it))))
        }
    }

     // Helper to build context - adapt based on how UiState formatting worked
    private fun buildPromptContext(
        newPrompt: String,
        history: List<ChatMessage>,
        uiStateLogic: com.ireddragonicy.llmdroid.presentation.ui.common.UiStateFormatter // Assuming UiState interface refactored
    ): String {
        // This needs to replicate the logic from your previous UiState implementations
        // For GenericUiState: just the new prompt? Or history + new?
        // For DeepSeekUiState: apply START_TOKEN, PROMPT_PREFIX, history, PROMPT_SUFFIX etc.
        return uiStateLogic.formatPrompt(newPrompt, history) // Pass history to formatter
    }


    override suspend fun estimateTokensRemaining(prompt: String, currentMessages: List<ChatMessage>): Result<Int> = withContext(ioDispatcher){
        val session = llmSession
        val currentModel = _selectedModel.value
        if (session == null || currentModel == null) return@withContext Result.Error(IllegalStateException("LLM session not initialized."))

        try {
            val uiStateLogic = currentModel.uiState
            // Replicate context building logic used for generation
            val contextString = buildPromptContext(prompt, currentMessages, uiStateLogic)

            if (contextString.isEmpty()) return@withContext Result.Success(MAX_TOKENS) // Or -1 ?

            // The token calculation logic might need adjustment based on the model's specifics
            val totalTokens = session.sizeInTokens(contextString) // + DECODE_TOKEN_OFFSET // Add offsets if necessary
            val remaining = max(0, MAX_TOKENS - totalTokens)
            Result.Success(remaining)
        } catch (e: Exception) {
            Log.e(TAG, "Error estimating tokens", e)
            Result.Error(ModelExecutionException("Token estimation failed: ${e.message}", e))
        }
    }

    override suspend fun resetSession(): Result<Unit> = withContext(ioDispatcher) {
        val inference = llmInference
        val currentModel = _selectedModel.value
        if (inference == null || currentModel == null) {
            Log.w(TAG, "Reset requested but inference engine or model not available.")
            // Optionally try to load it first?
            // return loadOrRefreshSession()
             return@withContext Result.Error(IllegalStateException("Cannot reset, LLM not loaded."))
        }
        Log.d(TAG, "Resetting LLM session.")
        try {
            llmSession?.close() // Close old session first

            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(currentModel.temperature)
                .setTopK(currentModel.topK)
                .setTopP(currentModel.topP)
                .build()
            llmSession = LlmInferenceSession.createFromOptions(inference, sessionOptions)
            Log.d(TAG, "LLM Session reset successfully.")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset LLM session", e)
             Result.Error(ModelExecutionException("Session reset failed: ${e.message}", e))
        }
    }

     override fun cleanup() {
         Log.d(TAG, "Cleaning up ModelRepository resources.")
         repositoryScope.cancel() // Cancel ongoing operations
         cleanupLlmResources()
     }

     private fun cleanupLlmResources() {
        Log.d(TAG, "Closing LLM session and inference engine.")
        llmSession?.close()
        llmInference?.close()
        llmSession = null
        llmInference = null
        currentModelPath = null
        Log.d(TAG, "LLM resources closed.")
    }
}

// Custom Exceptions
class ModelLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ModelExecutionException(message: String, cause: Throwable? = null) : Exception(message, cause)