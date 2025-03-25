package com.ireddragonicy.llmdroid.presentation.ui.loading.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadIndicator(
    progress: Int, // 0-100, or -1 for indeterminate
    modelName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Downloading Model",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
         Text(
            text = modelName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (progress >= 0) {
             LinearProgressIndicator(
                 progress = { progress / 100f },
                 modifier = Modifier.fillMaxWidth().height(8.dp).padding(bottom = 8.dp)
             )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            // Indeterminate progress
             LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).padding(bottom = 8.dp))
             Text(
                text = "Starting download...",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Cancel")
        }
    }
}