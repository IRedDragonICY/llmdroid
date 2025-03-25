package com.ireddragonicy.llmdroid.presentation.ui.common.formatter

import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.MODEL_PREFIX
import com.ireddragonicy.llmdroid.domain.model.USER_PREFIX
import com.ireddragonicy.llmdroid.presentation.ui.common.ProcessedResult
import com.ireddragonicy.llmdroid.presentation.ui.common.UiStateFormatter

/** Formatter generik, bisa disesuaikan */
class GenericUiStateFormatter : UiStateFormatter {
    // Contoh sederhana: hanya gabungkan pesan dengan role prefix
    // PERLU DISESUAIKAN dengan format yang benar-benar diharapkan model Gemma!
    override fun formatPrompt(newPrompt: String, history: List<ChatMessage>): String {
        val historyString = history.joinToString("\n") {
            "${it.author}: ${it.message}" // Sesuaikan prefix "user:" atau "model:"
        }
        return "$historyString\n$USER_PREFIX: $newPrompt\n$MODEL_PREFIX: " // Gemma mungkin butuh format <start_of_turn> / <end_of_turn>
    }

    override fun processPartialResult(partialResult: String, currentBuffer: String): ProcessedResult {
        // Tidak ada pemrosesan khusus untuk generic state
        return ProcessedResult(displayChunk = partialResult)
    }

     override fun processFinalResult(fullResult: String): String {
         return fullResult.trim()
     }
}