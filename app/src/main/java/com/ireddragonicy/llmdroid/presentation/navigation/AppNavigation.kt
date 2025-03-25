package com.ireddragonicy.llmdroid.presentation.navigation

import android.util.Log // Tambahkan import Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
// Hapus import state selectedModel jika tidak digunakan lagi di sini
// import androidx.compose.runtime.collectAsState
// import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel // Tetap perlukan jika MainViewModel masih dipakai di tempat lain
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig // Import LlmModelConfig
import com.ireddragonicy.llmdroid.presentation.ui.chat.ChatRoute
import com.ireddragonicy.llmdroid.presentation.ui.loading.LoadingRoute
import com.ireddragonicy.llmdroid.presentation.ui.main.MainViewModel // Tetap perlukan jika MainViewModel masih dipakai di tempat lain
import com.ireddragonicy.llmdroid.presentation.ui.main.component.WelcomeScreen

object AppDestinations {
    const val WELCOME_SCREEN = "welcome"
    // Ubah rute LOADING_SCREEN untuk menerima argumen
    const val LOADING_SCREEN_ROUTE = "loading"
    const val LOADING_SCREEN = "$LOADING_SCREEN_ROUTE/{${AppDestinationsArgs.MODEL_NAME_ARG}}" // Rute dengan argumen
    const val CHAT_SCREEN_ROUTE = "chat"
    const val CHAT_SCREEN = "$CHAT_SCREEN_ROUTE/{${AppDestinationsArgs.CHAT_ID_ARG}}"
}

object AppDestinationsArgs {
     const val CHAT_ID_ARG = "chatId"
     const val MODEL_NAME_ARG = "modelName" // Tambahkan argumen untuk nama model
}


@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = AppDestinations.WELCOME_SCREEN,
    mainViewModel: MainViewModel = hiltViewModel() // Masih diperlukan untuk clearSelectedModel
) {
    // Tidak perlu observe selectedModel di sini lagi untuk LoadingScreen
    // val selectedModel by mainViewModel.selectedModel.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppDestinations.WELCOME_SCREEN) {
            WelcomeScreen()
        }

        composable(
            route = AppDestinations.LOADING_SCREEN, // Gunakan rute dengan argumen
            arguments = listOf(navArgument(AppDestinationsArgs.MODEL_NAME_ARG) { type = NavType.StringType }) // Definisikan argumen
        ) { backStackEntry ->
            // Ambil nama model dari argumen
            val modelName = backStackEntry.arguments?.getString(AppDestinationsArgs.MODEL_NAME_ARG)
            // Cari enum LlmModelConfig berdasarkan nama
            val modelToLoad = try {
                modelName?.let { LlmModelConfig.valueOf(it) }
            } catch (e: IllegalArgumentException) {
                Log.e("AppNavigation", "Invalid model name argument: $modelName", e)
                null
            }

             if (modelToLoad == null) {
                 // Handle case where model name is invalid or null
                 Log.e("AppNavigation", "Error: Could not find model for name '$modelName'. Navigating back.")
                 LaunchedEffect(Unit) {
                     // Navigasi kembali ke welcome screen jika model tidak valid
                     navController.popBackStack(AppDestinations.WELCOME_SCREEN, inclusive = false)
                 }
             } else {
                // Lewatkan model yang ditemukan ke LoadingRoute
                LoadingRoute(
                    model = modelToLoad,
                    onModelReady = { chatId ->
                        navController.navigate("${AppDestinations.CHAT_SCREEN_ROUTE}/$chatId") {
                            // Pop up to WELCOME_SCREEN, removing loading and welcome from backstack
                            popUpTo(AppDestinations.WELCOME_SCREEN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack(AppDestinations.WELCOME_SCREEN, false)
                        // Panggil clearSelectedModel di ViewModel saat kembali
                        mainViewModel.clearSelectedModel()
                    }
                )
            }
        }

        composable(
            route = AppDestinations.CHAT_SCREEN,
            arguments = listOf(navArgument(AppDestinationsArgs.CHAT_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString(AppDestinationsArgs.CHAT_ID_ARG)
             if (chatId != null) {
                ChatRoute(chatId = chatId)
             } else {
                  Log.e("AppNavigation", "Error: Chat ID argument is null. Navigating back.")
                  LaunchedEffect(Unit) { navController.popBackStack() }
             }
        }
    }
}