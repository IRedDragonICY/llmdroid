package com.ireddragonicy.llmdroid.presentation.ui.common

import com.ireddragonicy.llmdroid.domain.model.ChatMessage

/**
 * Interface untuk menangani formatting prompt dan parsing output
 * yang spesifik untuk model LLM tertentu.
 */
interface UiStateFormatter {
    /**
     * Memformat prompt input pengguna, menggabungkannya dengan histori chat
     * sesuai dengan template yang diharapkan model.
     *
     * @param newPrompt Prompt baru dari pengguna.
     * @param history Histori chat sebelumnya.
     * @return String prompt yang sudah diformat untuk dikirim ke LLM.
     */
    fun formatPrompt(newPrompt: String, history: List<ChatMessage>): String

    /**
     * Memproses hasil partial (streaming) dari LLM.
     * Berguna untuk membersihkan token spesifik, mendeteksi state (misal 'thinking'), dll.
     *
     * @param partialResult Potongan teks yang diterima dari LLM.
     * @param currentBuffer Teks yang sudah terkumpul dari potongan sebelumnya dalam response saat ini.
     * @return Pair: Teks yang sudah diproses untuk ditampilkan, dan flag apakah state berubah (misal, mulai/berhenti thinking).
     *         ATAU bisa juga mengembalikan data class yang lebih kompleks jika perlu.
     */
    fun processPartialResult(partialResult: String, currentBuffer: String): ProcessedResult

     /**
      * Memproses hasil akhir setelah streaming selesai.
      * Berguna untuk pembersihan final.
      *
      * @param fullResult Teks lengkap yang dihasilkan model.
      * @return Teks final yang bersih.
      */
     fun processFinalResult(fullResult: String): String
}

/** Data class untuk hasil pemrosesan partial */
data class ProcessedResult(
    val displayChunk: String, // Teks yang akan ditambahkan ke UI
    val isThinking: Boolean? = null // Null jika tidak ada perubahan state thinking
)