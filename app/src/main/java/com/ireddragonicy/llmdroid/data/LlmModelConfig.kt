package com.ireddragonicy.llmdroid.data

import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.ireddragonicy.llmdroid.DeepSeekUiState
import com.ireddragonicy.llmdroid.GenericUiState
import com.ireddragonicy.llmdroid.UiState

enum class LlmModelConfig(
    val path: String,
    val url: String,
    val licenseUrl: String,
    val needsAuth: Boolean,
    val preferredBackend: LlmInference.Backend?,
    val uiState: UiState,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
) {
    GEMMA3_CPU(
        path = "/data/local/tmp/gemma3-1b-it-int4.task",
        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
        licenseUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
        needsAuth = true,
        preferredBackend = LlmInference.Backend.CPU,
        uiState = GenericUiState(),
        temperature = 1f,
        topK = 64,
        topP = 0.95f
    ),
    GEMMA3_GPU(
        path = "/data/local/tmp/gemma3-1b-it-int4.task",
        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
        licenseUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
        needsAuth = true,
        preferredBackend = LlmInference.Backend.GPU,
        uiState = GenericUiState(),
        temperature = 1f,
        topK = 64,
        topP = 0.95f
    ),
    DEEPSEEK_CPU(
        path = "/data/local/tmp/llm/deepseek3k_q8_ekv1280.task",
        url = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/deepseek_q8_ekv1280.task",
        licenseUrl = "",
        needsAuth = false,
        preferredBackend = null,
        uiState = DeepSeekUiState(),
        temperature = 0.6f,
        topK = 40,
        topP = 0.7f
    ),
    PHI4_CPU(
        path = "/data/local/tmp/llm/phi4_q8_ekv1280.task",
        url = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/phi4_q8_ekv1280.task",
        licenseUrl = "",
        needsAuth = false,
        preferredBackend = null,
        uiState = GenericUiState(),
        temperature = 0.0f,
        topK = 40,
        topP = 1.0f
    ),
}