package com.ireddragonicy.llmdroid.presentation.ui.common.formatter

import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.presentation.ui.common.ProcessedResult
import com.ireddragonicy.llmdroid.presentation.ui.common.UiStateFormatter

/**
 * Formatter spesifik untuk DeepSeek-style models.
 * (Replikasi logic dari DeepSeekUiState original)
 */
class DeepSeekUiStateFormatter : UiStateFormatter {
    private val START_TOKEN = "<｜begin of sentence｜>"
    private val PROMPT_PREFIX = "<｜User｜>"
    private val PROMPT_SUFFIX = "<｜Assistant｜>"
    private val THINKING_MARKER_START = "<think>"
    private val THINKING_MARKER_END = "</think>"

    override fun formatPrompt(newPrompt: String, history: List<ChatMessage>): String {
        // Deepseek mungkin tidak butuh history di prompt, tapi di context session LLM nya?
        // Sesuaikan format ini berdasarkan dokumentasi Deepseek jika perlu memasukkan history
        return "$START_TOKEN$PROMPT_PREFIX$newPrompt$PROMPT_SUFFIX"
    }

    override fun processPartialResult(partialResult: String, currentBuffer: String): ProcessedResult {
        var displayChunk = partialResult
        var isThinking: Boolean? = null

        // Cek hanya jika marker *mulai* di chunk ini dan *belum* ada di buffer sebelumnya
        if (THINKING_MARKER_START in partialResult && THINKING_MARKER_START !in currentBuffer) {
             isThinking = true
        }
        // Cek hanya jika marker *akhir* di chunk ini dan *belum* ada di buffer sebelumnya
         if (THINKING_MARKER_END in partialResult && THINKING_MARKER_END !in currentBuffer) {
             isThinking = false
         }

        // Hapus marker dari teks yang akan ditampilkan
        displayChunk = displayChunk.replace(THINKING_MARKER_START, "").replace(THINKING_MARKER_END, "")

        return ProcessedResult(displayChunk = displayChunk, isThinking = isThinking)
    }

     override fun processFinalResult(fullResult: String): String {
         // Hapus semua marker pada hasil akhir
         return fullResult.replace(THINKING_MARKER_START, "").replace(THINKING_MARKER_END, "").trim()
     }
}