package com.duptrash.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.duptrash.app.MainViewModel
import com.duptrash.app.data.db.DeletePatternEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternsScreen(viewModel: MainViewModel, nav: NavController) {
    val patterns by viewModel.patterns.collectAsState()
    var dialogText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delete patterns") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogText = "" }) {
                Icon(Icons.Default.Add, contentDescription = "Add pattern")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (patterns.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "No patterns yet. Add a regex like .*WhatsApp.* to mark duplicate copies inside WhatsApp folders for deletion.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(patterns, key = { it.id }) { p ->
                        PatternRow(p, viewModel)
                    }
                }
            }
        }
    }

    if (dialogText != null) {
        AddPatternDialog(
            initial = dialogText!!,
            onDismiss = { dialogText = null },
            onConfirm = {
                viewModel.addPattern(it)
                dialogText = null
            },
        )
    }
}

@Composable
private fun PatternRow(p: DeletePatternEntity, viewModel: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(
                checked = p.enabled,
                onCheckedChange = { viewModel.updatePattern(p.copy(enabled = it)) },
            )
            Text(
                text = p.pattern,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.deletePattern(p) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete pattern")
            }
        }
    }
}

@Composable
private fun AddPatternDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val error = remember(text) { runCatching { Regex(text) }.exceptionOrNull()?.message }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add delete pattern") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Regex (matched against full path)") },
                    isError = error != null && text.isNotEmpty(),
                    singleLine = true,
                )
                if (error != null && text.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && error == null,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
