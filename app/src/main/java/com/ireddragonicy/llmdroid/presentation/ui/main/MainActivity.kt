package com.ireddragonicy.llmdroid.presentation.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ireddragonicy.llmdroid.presentation.ui.theme.LLMInferenceTheme
import com.ireddragonicy.llmdroid.presentation.navigation.AppDestinations
import com.ireddragonicy.llmdroid.presentation.navigation.AppNavigation
import com.ireddragonicy.llmdroid.presentation.navigation.AppDestinationsArgs
import com.ireddragonicy.llmdroid.presentation.ui.license.LicenseAcknowledgmentActivity
import com.ireddragonicy.llmdroid.presentation.ui.login.LoginActivity
import com.ireddragonicy.llmdroid.presentation.ui.main.component.ModernAppBar
import com.ireddragonicy.llmdroid.presentation.ui.main.component.DrawerContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination // Needed for popUpTo

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        is MainViewModel.NavigationEvent.NavigateToLogin -> {
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        }
                        is MainViewModel.NavigationEvent.NavigateToLicense -> {
                             startActivity(Intent(this@MainActivity, LicenseAcknowledgmentActivity::class.java))
                        }
                         is MainViewModel.NavigationEvent.NavigateToLoading -> {}
                         is MainViewModel.NavigationEvent.NavigateToChat -> {}
                    }
                }
            }
        }

        setContent {
            LLMInferenceTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
                val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                // --- GET currentChatId ---
                val currentChatId = navBackStackEntry?.arguments?.getString(AppDestinationsArgs.CHAT_ID_ARG)

                val showMenuIcon = currentRoute?.startsWith(AppDestinations.CHAT_SCREEN_ROUTE) == true

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                           DrawerContent(
                                chatSessions = mainUiState.chatSessions,
                                isLoading = mainUiState.isLoading,
                                currentChatId = currentChatId, // Pass currentChatId
                                onNewChat = {
                                    scope.launch { drawerState.close() }
                                    viewModel.createNewChat { newChatId ->
                                          navController.navigate("${AppDestinations.CHAT_SCREEN_ROUTE}/$newChatId") {
                                             // Go to the new chat, popping Welcome if it's directly below
                                             popUpTo(AppDestinations.WELCOME_SCREEN) { inclusive = false }
                                             launchSingleTop = true
                                         }
                                    }
                                },
                                onSessionClick = { chatId ->
                                    scope.launch { drawerState.close() }
                                     if (currentChatId != chatId) { // Only navigate if different
                                         navController.navigate("${AppDestinations.CHAT_SCREEN_ROUTE}/$chatId"){
                                             // Navigate or replace the current chat screen
                                             popUpTo(AppDestinations.WELCOME_SCREEN) { inclusive = false }
                                             launchSingleTop = true
                                         }
                                     }
                                },
                               // --- MODIFIED onDeleteSession LAMBDA ---
                               onDeleteSession = { chatIdToDelete, currentId ->
                                   scope.launch { drawerState.close() }
                                   if (chatIdToDelete == currentId) {
                                       // Deleting the currently viewed chat
                                       // Navigate to Welcome Screen first
                                       Log.d("MainActivity", "Deleting current chat ($chatIdToDelete). Navigating to Welcome.")
                                       navController.navigate(AppDestinations.WELCOME_SCREEN) {
                                           // Pop everything up to the start destination to avoid backstack issues
                                           popUpTo(navController.graph.findStartDestination().id) {
                                               inclusive = false // Keep Welcome Screen
                                           }
                                           // Avoid multiple copies of Welcome Screen
                                           launchSingleTop = true
                                       }
                                       // Now call delete in ViewModel (potentially after a tiny delay or assuming navigation happens first)
                                       viewModel.deleteChatSession(chatIdToDelete)
                                   } else {
                                       // Deleting a different chat, just call ViewModel
                                       Log.d("MainActivity", "Deleting chat ($chatIdToDelete), not the current one.")
                                       viewModel.deleteChatSession(chatIdToDelete)
                                   }
                               }
                               // --- END MODIFICATION ---
                           )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            ModernAppBar(
                                selectedModel = selectedModel,
                                onModelSelected = { model ->
                                     viewModel.selectModel(model) { requiresAuth, requiresLicense ->
                                        when {
                                            requiresAuth -> viewModel.triggerLoginNavigation()
                                            requiresLicense -> viewModel.triggerLicenseNavigation()
                                            else -> {
                                                Log.d("MainActivity", "Navigating to Loading Screen for model: ${model.name}")
                                                navController.navigate("${AppDestinations.LOADING_SCREEN_ROUTE}/${model.name}") {
                                                    popUpTo(AppDestinations.WELCOME_SCREEN) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                     }
                                },
                                onMenuClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                showMenu = showMenuIcon
                            )
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                           AppNavigation(
                               navController = navController,
                               startDestination = AppDestinations.WELCOME_SCREEN,
                               mainViewModel = viewModel
                           )
                        }
                    }
                }
            }
        }
    }
}