package com.ireddragonicy.llmdroid.presentation.ui.main.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ireddragonicy.llmdroid.R
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAppBar(
    selectedModel: LlmModelConfig?,
    onModelSelected: (LlmModelConfig) -> Unit,
    onMenuClick: () -> Unit,
    showMenu: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(start = if (!showMenu) 16.dp else 0.dp) // Adjust padding based on menu icon
                )
            },
            navigationIcon = {
                if (showMenu) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_description))
                    }
                }
            },
            actions = {
                // Dropdown for Model Selection
                Box(
                    modifier = Modifier.wrapContentSize(Alignment.TopStart) // Align dropdown properly
                ) {
                    OutlinedButton( // Use OutlinedButton for less emphasis than Button
                        onClick = { expanded = true },
                        modifier = Modifier.padding(end = 8.dp) // Add some padding
                    ) {
                        Text(
                            text = selectedModel?.name ?: stringResource(R.string.select_model_placeholder),
                            style = MaterialTheme.typography.labelLarge,
                             maxLines = 1
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.select_model_description),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.widthIn(min = 200.dp) // Ensure reasonable width
                    ) {
                        LlmModelConfig.entries.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.name) }, // Display model name
                                onClick = {
                                    expanded = false
                                    onModelSelected(model)
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface, // Use surface color
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant // Slightly different color for actions
            ),
            // Add elevation or border if desired
             modifier = Modifier.background(MaterialTheme.colorScheme.surface) //.border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        )

        // Disclaimer Text - Consider if this is always needed or only on specific screens
         Box(
             modifier = Modifier
                 .fillMaxWidth()
                 .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                 .padding(horizontal = 16.dp, vertical = 6.dp),
             contentAlignment = Alignment.Center
         ) {
             Text(
                 text = stringResource(R.string.disclaimer), // Pastikan string R.string.disclaimer ada
                 textAlign = TextAlign.Center,
                 style = MaterialTheme.typography.labelSmall, // Smaller text for disclaimer
                 color = MaterialTheme.colorScheme.onSurfaceVariant
             )
         }
        Divider() // Add a divider below the app bar section
    }
}