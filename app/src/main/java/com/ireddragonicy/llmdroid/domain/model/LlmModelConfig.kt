package com.ireddragonicy.llmdroid.domain.model

import android.net.Uri
import com.google.mediapipe.tasks.genai.llminference.LlmInference
// Import UiStateFormatter dari presentation layer common (atau definisikan interface di domain jika mau lebih strict)
import com.ireddragonicy.llmdroid.presentation.ui.common.UiStateFormatter
import com.ireddragonicy.llmdroid.presentation.ui.common.formatter.DeepSeekUiStateFormatter
import com.ireddragonicy.llmdroid.presentation.ui.common.formatter.GenericUiStateFormatter

// Enum class untuk model yang didukung
enum class LlmModelConfig(
    val path: String, // Path default jika di-bundle atau lokasi preferensi
    val url: String, // URL untuk download
    val licenseUrl: String,
    val needsAuth: Boolean,
    val preferredBackend: LlmInference.Backend?, // Nullable jika tidak ada preferensi
    val uiState: UiStateFormatter, // Menggunakan interface formatter
    // Parameter LLM
    val temperature: Float,
    val topK: Int,
    val topP: Float,
    val maxTokens: Int = 1024 // Max tokens untuk model ini (default)
) {
    GEMMA3_CPU(
        path = "/data/local/tmp/llm/gemma3-1b-it-int4.task", // Update path jika berubah
        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
        licenseUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT", // Pastikan URL valid
        needsAuth = true,
        preferredBackend = LlmInference.Backend.CPU,
        uiState = GenericUiStateFormatter(),
        temperature = 1f,
        topK = 64,
        topP = 0.95f
    ),
    GEMMA3_GPU(
        path = "/data/local/tmp/llm/gemma3-1b-it-int4.task", // Update path jika berubah
        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
        licenseUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
        needsAuth = true,
        preferredBackend = LlmInference.Backend.GPU,
        uiState = GenericUiStateFormatter(),
        temperature = 1f,
        topK = 64,
        topP = 0.95f
    ),
    DEEPSEEK_CPU(
        path = "/data/local/tmp/llm/deepseek3k_q8_ekv1280.task", // Update path jika berubah
        url = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/deepseek_q8_ekv1280.task",
        licenseUrl = "", // Isi jika ada URL lisensi
        needsAuth = false,
        preferredBackend = LlmInference.Backend.CPU, // Specify backend if known
        uiState = DeepSeekUiStateFormatter(),
        temperature = 0.6f,
        topK = 40,
        topP = 0.7f
    ),
    PHI4_CPU(
        path = "/data/local/tmp/llm/phi4_q8_ekv1280.task", // Update path jika berubah
        url = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/phi4_q8_ekv1280.task",
        licenseUrl = "", // Isi jika ada URL lisensi
        needsAuth = false,
        preferredBackend = LlmInference.Backend.CPU, // Specify backend if known
        uiState = GenericUiStateFormatter(), // Asumsi generic, sesuaikan jika perlu
        temperature = 0.0f, // Note: temperature 0 bisa membuat output sangat deterministik
        topK = 40,
        topP = 1.0f
    ); // Tambahkan model lain jika ada

    // Helper untuk mendapatkan nama file dari URL atau path
    fun getFileName(): String {
        return if (url.isNotEmpty()) {
             Uri.parse(url).lastPathSegment ?: path.substringAfterLast('/')
        } else {
             path.substringAfterLast('/')
        }
    }
}