package com.ireddragonicy.llmdroid.presentation.ui.oauth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ireddragonicy.llmdroid.R
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.presentation.ui.license.LicenseAcknowledgmentActivity
import com.ireddragonicy.llmdroid.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull // Import filterNotNull
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException // Tetap dibutuhkan untuk logging/debugging jika perlu

@AndroidEntryPoint
class OAuthCallbackActivity : AppCompatActivity() {

    private val viewModel: OAuthCallbackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModel()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Langsung teruskan Intent ke ViewModel
        viewModel.handleAuthorizationCallback(intent)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.callbackResult.filterNotNull().collect { result ->
                    when (result) {
                        is Result.Success -> {
                            Toast.makeText(
                                this@OAuthCallbackActivity,
                                getString(R.string.auth_success_message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is Result.Error -> {
                            Toast.makeText(
                                this@OAuthCallbackActivity,
                                getString(R.string.error_auth_failed, result.exception.message ?: "Unknown error"),
                                Toast.LENGTH_LONG
                            ).show()
                            navigateToMainWithError()
                        }
                        is Result.Loading -> {
                            Toast.makeText(
                                this@OAuthCallbackActivity,
                                getString(R.string.auth_processing_message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        OAuthCallbackViewModel.NavigationEvent.NavigateToLicense -> navigateToLicense()
                        OAuthCallbackViewModel.NavigationEvent.NavigateToLoading -> navigateToLoading()
                        OAuthCallbackViewModel.NavigationEvent.NavigateToMain -> navigateToMain()
                    }
                }
            }
        }
    }

    private fun navigateToLicense() {
        startActivity(Intent(this, LicenseAcknowledgmentActivity::class.java))
        finish()
    }

    private fun navigateToLoading() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToMainWithError() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToMain() {
        navigateToMainWithError()
    }
}