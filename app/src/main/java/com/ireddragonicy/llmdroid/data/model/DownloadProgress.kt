package com.ireddragonicy.llmdroid.data.model

data class DownloadProgress(
    val percentage: Int, // 0-100
    val isComplete: Boolean = false
)