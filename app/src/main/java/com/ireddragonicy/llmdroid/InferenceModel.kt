// filepath: d:\Project\llmdroid\app\src\main\java\com\ireddragonicy\llmdroid\InferenceModel.kt
package com.ireddragonicy.llmdroid

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import java.io.File
import kotlin.math.max

const val MAX_TOKENS = 1024
const val DECODE_TOKEN_OFFSET = 256

open class ModelException(message: String) : Exception(message)
class ModelLoadFailException(message: String) : ModelException(message)
class ModelSessionCreateFailException(message: String) : ModelException(message)

class InferenceModel private constructor(context: Context) {
    private val TAG = InferenceModel::class.qualifiedName
    private val llmInference: LlmInference
    private var llmInferenceSession: LlmInferenceSession

    val uiState: UiState = model.uiState

    init {
        if (!modelExists(context)) {
            throw IllegalArgumentException("Model not found at path: ${model.path}")
        }

        llmInference = createEngine(context)
        llmInferenceSession = createSession(llmInference)
    }

    fun close() {
        llmInferenceSession.close()
        llmInference.close()
    }

    fun resetSession() {
        llmInferenceSession.close()
        llmInferenceSession = createSession(llmInference)
    }

    private fun createEngine(context: Context): LlmInference {
        val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath(context))
            .setMaxTokens(MAX_TOKENS)
            .apply { model.preferredBackend?.let { setPreferredBackend(it) } }
            .build()

        try {
            return LlmInference.createFromOptions(context, inferenceOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Load model error: ${e.message}", e)
            throw ModelLoadFailException("Failed to load model, please try again")
        }
    }

    private fun createSession(inference: LlmInference): LlmInferenceSession {
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(model.temperature)
            .setTopK(model.topK)
            .setTopP(model.topP)
            .build()

        try {
            return LlmInferenceSession.createFromOptions(inference, sessionOptions)
        } catch (e: Exception) {
            Log.e(TAG, "LlmInferenceSession create error: ${e.message}", e)
            throw ModelSessionCreateFailException("Failed to create model session, please try again")
        }
    }

    fun generateResponseAsync(prompt: String, progressListener: ProgressListener<String>): ListenableFuture<String> {
        val formattedPrompt = model.uiState.formatPrompt(prompt)
        llmInferenceSession.addQueryChunk(formattedPrompt)
        return llmInferenceSession.generateResponseAsync(progressListener)
    }

    fun estimateTokensRemaining(prompt: String): Int {
        val context = uiState.messages.joinToString { it.rawMessage } + prompt
        if (context.isEmpty()) return -1

        val totalTokens = llmInferenceSession.sizeInTokens(context) +
            (uiState.messages.size * 3) + DECODE_TOKEN_OFFSET
        return max(0, MAX_TOKENS - totalTokens)
    }

    companion object {
        var model: Model = Model.GEMMA3_CPU

        @Volatile private var instance: InferenceModel? = null

        val instanceOrNull: InferenceModel?
            get() = instance

        @Synchronized
        fun getInstance(context: Context): InferenceModel {
            return instance ?: InferenceModel(context).also { instance = it }
        }

        @Synchronized
        fun resetInstance(context: Context): InferenceModel {
            instance?.close()
            return InferenceModel(context).also { instance = it }
        }

        fun modelExists(context: Context): Boolean {
            return File(model.path).exists() ||
                (model.url.isNotEmpty() && File(context.filesDir,
                Uri.parse(model.url).lastPathSegment ?: "").exists())
        }

        fun modelPath(context: Context): String {
            val modelFile = File(model.path)
            if (modelFile.exists()) {
                return model.path
            }

            if (model.url.isNotEmpty()) {
                val fileName = Uri.parse(model.url).lastPathSegment
                if (!fileName.isNullOrEmpty()) {
                    return File(context.filesDir, fileName).absolutePath
                }
            }

            return ""
        }

        fun modelPathFromUrl(context: Context): String {
            if (model.url.isNotEmpty()) {
                val fileName = Uri.parse(model.url).lastPathSegment
                if (!fileName.isNullOrEmpty()) {
                    return File(context.filesDir, fileName).absolutePath
                }
            }
            return model.path
        }
    }
}
