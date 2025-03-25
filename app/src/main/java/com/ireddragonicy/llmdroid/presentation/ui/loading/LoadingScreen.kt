package com.ireddragonicy.llmdroid.presentation.ui.loading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ireddragonicy.llmdroid.R
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.presentation.ui.common.ErrorMessage
import com.ireddragonicy.llmdroid.presentation.ui.common.LoadingIndicator
import com.ireddragonicy.llmdroid.presentation.ui.loading.component.DownloadIndicator

// State data class untuk Loading Screen
data class LoadingUiState(
    val isLoadingModel: Boolean = true, // Loading LLM engine or checking files
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0, // 0-100, atau -1 jika indeterminate
    val error: String? = null,
    val modelName: String = ""
)

@Composable
internal fun LoadingRoute(
    model: LlmModelConfig,
    viewModel: LoadingViewModel = hiltViewModel(),
    onModelReady: (chatId: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Trigger load/download when the composable enters composition with a specific model
    LaunchedEffect(model) {
        viewModel.initialize(model)
    }

    // Observe completion event
    LaunchedEffect(viewModel) {
        viewModel.completionEvent.collect { chatId ->
            onModelReady(chatId)
        }
    }

    LoadingScreen(
        uiState = uiState,
        onCancelDownload = viewModel::cancelDownload,
        onRetry = { viewModel.initialize(model) }, // Retry initialization
        onBack = onBack // Propagate back action
    )
}

@Composable
fun LoadingScreen(
    uiState: LoadingUiState,
    onCancelDownload: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Error State
            uiState.error != null -> {
                ErrorMessage(
                    message = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(16.dp)
                )
                // Tombol back mungkin tetap berguna di sini
                 Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter){
                     Button(onClick = onBack) {
                         Text("Go Back")
                     }
                 }
            }
            // Downloading State
            uiState.isDownloading -> {
                DownloadIndicator(
                    progress = uiState.downloadProgress,
                    modelName = uiState.modelName,
                    onCancel = onCancelDownload
                )
            }
            // Loading State (Checking files, Initializing LLM)
            uiState.isLoadingModel -> {
                LoadingIndicator(text = stringResource(R.string.loading_model_message, uiState.modelName))
            }
            // Seharusnya tidak pernah mencapai state ini jika langsung navigasi on complete
            else -> {
                 LoadingIndicator(text = "Finalizing...") // Fallback
            }
        }
    }
}