package com.ireddragonicy.llmdroid.presentation.ui.license

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ireddragonicy.llmdroid.R
import com.ireddragonicy.llmdroid.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LicenseAcknowledgmentActivity : AppCompatActivity() {

    private val viewModel: LicenseViewModel by viewModels()
    private lateinit var acknowledgeButton: Button
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license_acknowledgment) // Pastikan layout ada

        acknowledgeButton = findViewById(R.id.btnAcknowledge) // Pastikan ID ada
        continueButton = findViewById(R.id.btnContinue) // Pastikan ID ada

        observeViewModel()
        setupButtonClickListeners()

        viewModel.initialize() // Load license URL
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update UI based on state
                    continueButton.isEnabled = state.isAcknowledged

                    if (state.licenseUrl == null) {
                        // Handle error loading license URL
                        Toast.makeText(this@LicenseAcknowledgmentActivity, "Error: Could not load license information.", Toast.LENGTH_LONG).show()
                        navigateToLoadingOrMain() // Go back if URL fails
                    }

                    if (state.error != null) {
                         Toast.makeText(this@LicenseAcknowledgmentActivity, "Error: ${state.error}", Toast.LENGTH_LONG).show()
                    }

                    if (state.navigateToLoading) {
                         navigateToLoadingOrMain()
                    }
                }
            }
        }
    }

    private fun setupButtonClickListeners() {
        acknowledgeButton.setOnClickListener {
            viewModel.uiState.value.licenseUrl?.let { url ->
                try {
                    val customTabsIntent = CustomTabsIntent.Builder().build()
                    customTabsIntent.launchUrl(this, Uri.parse(url))
                    viewModel.setAcknowledged(true) // Mark as acknowledged after opening
                } catch (e: Exception) {
                     Toast.makeText(this, "Could not open browser.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                 Toast.makeText(this, "License URL not available.", Toast.LENGTH_SHORT).show()
            }
        }

        continueButton.setOnClickListener {
            viewModel.proceedToLoading()
        }
    }

    private fun navigateToLoadingOrMain() {
         val intent = Intent(this, MainActivity::class.java).apply {
            // MainActivity's NavHost will handle going to Loading screen based on selected model
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}