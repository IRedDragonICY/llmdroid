package com.ireddragonicy.llmdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.mediapipe.examples.llminference.ui.theme.LLMInferenceTheme

const val START_SCREEN = "start_screen"
const val LOAD_SCREEN = "load_screen"
const val CHAT_SCREEN = "chat_screen"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LLMInferenceTheme {
                val navController = rememberNavController()
                val startDestination = intent.getStringExtra("NAVIGATE_TO") ?: START_SCREEN

                // Track currently selected model
                var selectedModel by remember { mutableStateOf<Model?>(null) }

                Scaffold(
                    topBar = {
                        ModernAppBar(
                            selectedModel = selectedModel,
                            onModelSelected = { model ->
                                selectedModel = model
                                navController.navigate(LOAD_SCREEN) {
                                    popUpTo(START_SCREEN) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            composable(START_SCREEN) {
                                WelcomeScreen()
                            }

                            composable(LOAD_SCREEN) {
                                selectedModel?.let { model ->
                                    InferenceModel.model = model
                                    LoadingRoute(
                                        onModelLoaded = {
                                            navController.navigate(CHAT_SCREEN) {
                                                popUpTo(LOAD_SCREEN) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        },
                                        onGoBack = {
                                            navController.navigate(START_SCREEN) {
                                                popUpTo(LOAD_SCREEN) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }

                            composable(CHAT_SCREEN) {
                                ChatRoute(
                                    onClose = {
                                        navController.navigate(START_SCREEN) {
                                            popUpTo(LOAD_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAppBar(
    selectedModel: Model?,
    onModelSelected: (Model) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(start = 8.dp)
                )
            },
            actions = {
                Spacer(modifier = Modifier.weight(1f))

                // Model selector in the middle
                Box(
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                ) {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(selectedModel?.toString() ?: "Select Model")
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Model",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(200.dp)
                    ) {
                        Model.entries.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.toString()) },
                                onClick = {
                                    expanded = false
                                    onModelSelected(model)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.disclaimer),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
    }
}

@Composable
fun WelcomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to LLM Droid",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Select a model from the dropdown above to get started",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}