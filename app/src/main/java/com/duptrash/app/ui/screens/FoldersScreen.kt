package com.duptrash.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.duptrash.app.MainViewModel
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(viewModel: MainViewModel, nav: NavController) {
    val ctx = LocalContext.current
    val folders by viewModel.customFolderUris.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { result ->
        val uri = result ?: return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching { ctx.contentResolver.takePersistableUriPermission(uri, flags) }
        viewModel.addCustomFolder(uri.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extra folders") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickerLauncher.launch(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add folder")
            }
        },
    ) { padding ->
        if (folders.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "No extra folders.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Tap + to add one — useful for backup folders that don't show up in your gallery (anything with a .nomedia file, SD-card backups, USB-OTG drives, etc.).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(folders.toList(), key = { it }) { uri ->
                FolderRow(
                    uri = uri,
                    onRemove = {
                        val parsed = uri.toUri()
                        runCatching {
                            ctx.contentResolver.releasePersistableUriPermission(
                                parsed, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        }
                        viewModel.removeCustomFolder(uri)
                    },
                )
            }
        }
    }
}

@Composable
private fun FolderRow(uri: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    readableFolderLabel(uri),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    uri,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove folder")
            }
        }
    }
}

/** Best-effort human label from a SAF tree URI like
 *  content://com.android.externalstorage.documents/tree/primary%3ASdCardBackUp */
private fun readableFolderLabel(uriStr: String): String {
    val parsed = runCatching { uriStr.toUri() }.getOrNull() ?: return uriStr
    val last = parsed.lastPathSegment ?: return uriStr
    val decoded = runCatching { URLDecoder.decode(last, "UTF-8") }.getOrDefault(last)
    val parts = decoded.split(':')
    val volume = parts.getOrNull(0) ?: ""
    val rel = parts.getOrNull(1) ?: decoded
    val display = if (volume == "primary") "/Interner Speicher/$rel" else "/$volume/$rel"
    return display
}
