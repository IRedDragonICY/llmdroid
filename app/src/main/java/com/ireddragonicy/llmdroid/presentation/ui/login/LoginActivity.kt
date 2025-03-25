package com.ireddragonicy.llmdroid.presentation.ui.login

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ireddragonicy.llmdroid.R
import com.ireddragonicy.llmdroid.domain.model.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Pastikan layout R.layout.activity_login ada

        val loginButton: ImageButton = findViewById(R.id.btnLogin) // Pastikan ID R.id.btnLogin ada
        loginButton.setOnClickListener {
            viewModel.initiateLogin()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginEvent.collect { result ->
                    when (result) {
                        is Result.Success -> {
                            // Start the intent returned by the use case/viewmodel
                            try {
                                startActivity(result.data)
                                // Don't finish immediately, wait for callback activity
                            } catch (e: Exception) {
                                 Toast.makeText(this@LoginActivity, "Could not start login flow: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        is Result.Error -> {
                            Toast.makeText(this@LoginActivity, "Login failed: ${result.exception.message}", Toast.LENGTH_LONG).show()
                            // Potentially update UI to show error state
                        }
                        is Result.Loading -> {
                            // Show loading indicator if needed
                        }
                    }
                }
            }
        }
         lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // Update UI based on loading state if needed (e.g., disable button)
                }
            }
        }
    }
}