package com.ireddragonicy.llmdroid.presentation.ui.license

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.llmdroid.domain.usecase.model.GetSelectedModelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LicenseUiState(
    val licenseUrl: String? = null,
    val isAcknowledged: Boolean = false, // TODO: Persist this state per model?
    val error: String? = null,
    val navigateToLoading: Boolean = false
)

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val getSelectedModelUseCase: GetSelectedModelUseCase
    // Inject use case to save acknowledged state if needed
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    fun initialize() {
        viewModelScope.launch {
            val model = getSelectedModelUseCase().firstOrNull() // Get currently selected model
            if (model?.licenseUrl?.isNotBlank() == true) {
                _uiState.update { it.copy(licenseUrl = model.licenseUrl) }
            } else {
                Log.w("LicenseViewModel", "No license URL found for selected model or no model selected.")
                _uiState.update { it.copy(error = "License information not available.", navigateToLoading = true) } // Skip if no URL
            }
            // TODO: Load previously acknowledged state if persisted
        }
    }

    fun setAcknowledged(acknowledged: Boolean) {
        _uiState.update { it.copy(isAcknowledged = acknowledged) }
        // TODO: Save acknowledged state persistence here if required
    }

    fun proceedToLoading() {
        if (_uiState.value.isAcknowledged || _uiState.value.licenseUrl.isNullOrBlank()) {
             _uiState.update { it.copy(navigateToLoading = true) }
        } else {
             _uiState.update { it.copy(error = "Please acknowledge the license first.") }
        }
    }
}