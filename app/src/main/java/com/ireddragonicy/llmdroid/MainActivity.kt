package com.ireddragonicy.llmdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.mediapipe.examples.llminference.ui.theme.LLMInferenceTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val START_SCREEN = "start_screen"
const val LOAD_SCREEN = "load_screen"
const val CHAT_SCREEN = "chat_screen"
const val CHAT_WITH_ID = "chat_screen/{chatId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LLMInferenceTheme {
                val navController = rememberNavController()
                val startDestination = intent.getStringExtra("NAVIGATE_TO") ?: START_SCREEN
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                var selectedModel by remember { mutableStateOf<Model?>(null) }
                var searchQuery by remember { mutableStateOf("") }
                
                val chatRepository = ChatRepository.getInstance()
                val chatSessions by chatRepository.chatSessions.collectAsState()
                
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStack?.destination?.route ?: START_SCREEN
                val isOnChatScreen = currentDestination.startsWith(CHAT_SCREEN) || 
                                    currentDestination.startsWith("chat_screen/")
                
                // Filter sessions based on search query
                val filteredSessions = remember(chatSessions, searchQuery) {
                    if (searchQuery.isBlank()) chatSessions
                    else chatSessions.filter { 
                        it.title.contains(searchQuery, ignoreCase = true) || 
                        it.getPreviewText().contains(searchQuery, ignoreCase = true)
                    }
                }

                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet(
                            modifier = Modifier.width(300.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxHeight()
                            ) {
                                Button(
                                    onClick = {
                                        val newSession = chatRepository.createChatSession()
                                        navController.navigate("chat_screen/${newSession.id}")
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                                    Text("New Chat", modifier = Modifier.padding(start = 8.dp))
                                }
                                
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search chats...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    singleLine = true
                                )
                                
                                Text(
                                    text = "Chat History",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                
                                LazyColumn(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(filteredSessions) { session ->
                                        ChatSessionItem(
                                            session = session,
                                            isSelected = currentBackStack?.arguments?.getString("chatId") == session.id,
                                            onClick = {
                                                navController.navigate("chat_screen/${session.id}")
                                                scope.launch { drawerState.close() }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
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
                                },
                                onMenuClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                showMenu = isOnChatScreen
                            )
                        }
                    ) { innerPadding ->
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
                                                // Create a new chat session
                                                val newSession = chatRepository.createChatSession(model)
                                                navController.navigate("chat_screen/${newSession.id}") {
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

                                composable(
                                    route = "chat_screen/{chatId}",
                                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                                    ChatRoute(chatId = chatId)
                                }
                                
                                composable(CHAT_SCREEN) {
                                    // Fallback for the old route
                                    // Create a new chat session if needed
                                    val newSession = chatRepository.createChatSession()
                                    navController.navigate("chat_screen/${newSession.id}") {
                                        popUpTo(CHAT_SCREEN) { inclusive = true }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatSessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = session.getPreviewText().ifEmpty { "Empty chat" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = session.getFormattedDate(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAppBar(
    selectedModel: Model?,
    onModelSelected: (Model) -> Unit,
    onMenuClick: () -> Unit = {},
    showMenu: Boolean = false
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
            navigationIcon = {
                if (showMenu) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            },
            actions = {
                Spacer(modifier = Modifier.weight(1f))

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
