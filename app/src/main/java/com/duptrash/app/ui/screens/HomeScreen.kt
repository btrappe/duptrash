package com.duptrash.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.duptrash.app.MainViewModel
import com.duptrash.app.data.scan.ScanProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, nav: NavController) {
    val ctx = LocalContext.current
    val progress by viewModel.scanProgress.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val groups by viewModel.groups.collectAsState()

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            pendingAction?.invoke()
        }
        pendingAction = null
    }

    fun ensurePermsThen(block: () -> Unit) {
        val perms = requiredMediaPermissions()
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) block() else {
            pendingAction = block
            permLauncher.launch(missing.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DupTrash") },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.PATTERNS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Patterns")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = { ensurePermsThen { viewModel.startScan() } },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) { Text("Scan media files", fontWeight = FontWeight.SemiBold) }

            Button(
                onClick = { ensurePermsThen {
                    viewModel.findDuplicates()
                    nav.navigate(Routes.DUPLICATES)
                } },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) { Text("Scan for duplicates", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(8.dp))

            ProgressBlock(progress)

            if (groups.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { nav.navigate(Routes.DUPLICATES) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Show ${groups.size} duplicate groups") }
            }
        }
    }
}

@Composable
private fun ProgressBlock(p: ScanProgress) {
    when (p) {
        ScanProgress.Idle -> Text("Idle. Tap a button above to begin.")
        is ScanProgress.Enumerating -> {
            Text("Enumerating MediaStore… ${p.seen} files")
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        is ScanProgress.Hashing -> {
            val frac = if (p.total > 0) p.done.toFloat() / p.total else 0f
            Text("Hashing ${p.done} / ${p.total} — ${p.currentName}")
            LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
        }
        is ScanProgress.Done -> Text("Done. ${p.totalFiles} files, ${p.hashedFiles} hashed in ${p.durationMs} ms.")
        is ScanProgress.Failed -> Text("Failed: ${p.message}")
    }
}

private fun requiredMediaPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
