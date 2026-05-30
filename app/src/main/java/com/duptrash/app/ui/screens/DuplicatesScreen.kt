package com.duptrash.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.duptrash.app.MainViewModel
import com.duptrash.app.data.db.MediaFileEntity
import com.duptrash.app.data.delete.TrashRequestLauncher
import com.duptrash.app.data.model.DuplicateGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(viewModel: MainViewModel, nav: NavController) {
    val ctx = LocalContext.current
    val groups by viewModel.groups.collectAsState()
    val plan by viewModel.plan.collectAsState()

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onTrashRequestResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicates (${groups.size})") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            val toDeleteCount = plan?.toDelete?.size ?: 0
            val reclaim = plan?.reclaimableBytes ?: 0
            val skippedCount = plan?.skipped?.size ?: 0
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (skippedCount > 0) {
                    Button(
                        onClick = { nav.navigate(Routes.REVIEW) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Review $skippedCount group(s) skipped by safety rule") }
                }
                Button(
                    onClick = {
                        val uris = plan?.toDelete?.map { it.uri.toUri() }.orEmpty()
                        val sender = TrashRequestLauncher.buildTrashRequest(ctx.contentResolver, uris)
                        if (sender != null) {
                            trashLauncher.launch(IntentSenderRequest.Builder(sender).build())
                        }
                    },
                    enabled = toDeleteCount > 0 && TrashRequestLauncher.isSupported(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("Move $toDeleteCount file(s) to trash — reclaim ${humanBytes(reclaim)}")
                }
            }
        },
    ) { padding ->
        if (groups.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { Text("No duplicates found. Run a scan first.") }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(groups, key = { it.md5 }) { group ->
                GroupCard(group, plan?.toDelete?.map { it.id }?.toSet() ?: emptySet())
            }
        }
    }
}

@Composable
private fun GroupCard(group: DuplicateGroup, deletionIds: Set<Long>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${group.files.size} copies · ${humanBytes(group.sizeBytes)} each · save ${humanBytes(group.reclaimableBytes)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            group.files.forEach { file ->
                FileRow(file, willDelete = file.id in deletionIds)
            }
        }
    }
}

@Composable
private fun FileRow(file: MediaFileEntity, willDelete: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val ctx = LocalContext.current
        val req = ImageRequest.Builder(ctx)
            .data(file.uri.toUri())
            .videoFrameMillis(500L)
            .crossfade(true)
            .build()
        AsyncImage(
            model = req,
            contentDescription = file.displayName,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(Modifier.padding(start = 10.dp)) {
            Text(
                file.fullPath,
                style = MaterialTheme.typography.bodySmall,
                color = if (willDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
            )
            if (willDelete) {
                Text("→ to trash", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
    }
}

fun humanBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KiB", "MiB", "GiB", "TiB")
    var value = bytes.toDouble() / 1024
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024; unit++
    }
    return String.format("%.1f %s", value, units[unit])
}

